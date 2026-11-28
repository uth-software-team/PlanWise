package group16.webapp.modules.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.JwtService;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.availability.service.CalendarService;
import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.entity.GroupMember;
import group16.webapp.modules.group.entity.TeamGroup;
import group16.webapp.modules.group.repository.GroupMemberRepository;
import group16.webapp.modules.group.repository.TeamGroupRepository;
import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GR-04 {@code GET /v1/groups/{id}/availability} through the real API (H2, real JWTs) with a mocked CalendarApi.
 * Verifies access rules, privacy (exact JSON keys, own intervals only) and that denied requests and INACTIVE members
 * never reach the calendar. Integration with real CAL-01 data is not covered here.
 */
class GroupAvailabilityControllerTests extends AuthenticatedMvcTest {

	private static final String START = "2026-11-23T00:00:00Z";
	private static final String END = "2026-11-30T00:00:00Z";
	private static final Instant START_AT = Instant.parse(START);
	private static final Instant END_AT = Instant.parse(END);

	// Mocks the CalendarApi implementation by its class: CalendarController injects CalendarService directly.
	@MockitoBean
	private CalendarService calendarApi;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TeamGroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository memberRepository;

	@Autowired
	private JdbcTemplate jdbc;

	private final List<Long> extraUserIds = new ArrayList<>();

	private User leader;
	private User member1;
	private User member2;
	private User freeMember;
	private User formerMember;
	private TeamGroup group;

	@BeforeEach
	void seedGroup() {
		leader = newUser("leader@example.com");
		member1 = newUser("member1@example.com");
		member2 = newUser("member2@example.com");
		freeMember = newUser("free@example.com");
		formerMember = newUser("former@example.com");
		group = groupRepository.save(new TeamGroup(leader.getId(), "Team A", "seeded"));
		addMember(leader, GroupRole.LEADER);
		addMember(member1, GroupRole.MEMBER);
		addMember(member2, GroupRole.MEMBER);
		addMember(freeMember, GroupRole.MEMBER);
		deactivate(addMember(formerMember, GroupRole.MEMBER));
	}

	@AfterEach
	void cleanUp() {
		memberRepository.deleteAllInBatch();
		groupRepository.deleteAllInBatch();
		userRepository.deleteAllById(extraUserIds);
		extraUserIds.clear();
	}

	// ---------- 200 ----------

	@Test
	void activeMemberSeesEveryActiveMembersOwnMergedBusyIntervals() throws Exception {
		stubOverlappingCalendars();

		assertOwnIntervalsOnly(callAs(member1, availabilityUri(group.getId(), START, END)));
	}

	@Test
	void activeLeaderSeesEveryActiveMembersOwnMergedBusyIntervals() throws Exception {
		stubOverlappingCalendars();

		assertOwnIntervalsOnly(callAs(leader, availabilityUri(group.getId(), START, END)));
	}

	@Test
	void responseHasExactlyTheFrozenKeysAtEveryLevel() throws Exception {
		stubOverlappingCalendars();

		String body = callAs(member1, availabilityUri(group.getId(), START, END))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<?, ?> root = JsonMapper.builder().build().readValue(body, Map.class);
		assertThat(keys(root)).containsExactlyInAnyOrder("success", "code", "message", "data", "timestamp");
		assertThat(root.get("success")).isEqualTo(true);
		assertThat(root.get("code")).isEqualTo("SUCCESS");
		assertThat(root.get("message")).isEqualTo("OK");
		assertThat(root.get("timestamp")).asString().isNotBlank();

		Map<?, ?> data = (Map<?, ?>) root.get("data");
		assertThat(keys(data)).containsExactly("members");
		List<?> members = (List<?>) data.get("members");
		assertThat(members).hasSize(4);
		int intervals = 0;
		for (Object m : members) {
			Map<?, ?> member = (Map<?, ?>) m;
			assertThat(keys(member)).containsExactlyInAnyOrder("userId", "busy");
			for (Object b : (List<?>) member.get("busy")) {
				assertThat(keys(b)).containsExactlyInAnyOrder("startAt", "endAt");
				intervals++;
			}
		}
		assertThat(intervals).isEqualTo(3);
	}

	// ---------- denied: no calendar call ----------

	@Test
	void anonymousCallGets401WithoutCalendarCall() throws Exception {
		callAnonymous(HttpMethod.GET, availabilityUri(group.getId(), START, END), null)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		verifyNoInteractions(calendarApi);
	}

	@Test
	void deactivatedCallerWithValidTokenGets401WithoutCalendarCall() throws Exception {
		String token = jwtService.issue(member1).value();
		member1.deactivate();
		userRepository.save(member1);

		mockMvc.perform(get(availabilityUri(group.getId(), START, END))
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
		verifyNoInteractions(calendarApi);
	}

	@Test
	void outsiderGets403WithoutCalendarCall() throws Exception {
		User outsider = newUser("outsider@example.com");

		callAs(outsider, availabilityUri(group.getId(), START, END))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		verifyNoInteractions(calendarApi);
	}

	@Test
	void formerMemberGets403WithoutCalendarCall() throws Exception {
		callAs(formerMember, availabilityUri(group.getId(), START, END))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		verifyNoInteractions(calendarApi);
	}

	@Test
	void missingGroupGets404WithoutCalendarCall() throws Exception {
		callAs(member1, availabilityUri(group.getId() + 1000, START, END))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
		verifyNoInteractions(calendarApi);
	}

	@Test
	void emptyRangeGets400WithoutCalendarCall() throws Exception {
		callAs(member1, availabilityUri(group.getId(), START, START))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
		verifyNoInteractions(calendarApi);
	}

	@Test
	void reversedRangeGets400WithoutCalendarCall() throws Exception {
		callAs(member1, availabilityUri(group.getId(), END, START))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
		verifyNoInteractions(calendarApi);
	}

	@Test
	void rangeLongerThan366DaysGets400WithoutCalendarCall() throws Exception {
		String overMax = START_AT.plus(Duration.ofDays(366)).plusSeconds(1).toString();

		callAs(member1, availabilityUri(group.getId(), START, overMax))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
		verifyNoInteractions(calendarApi);
	}

	@Test
	void rangeOfExactly366DaysIsAccepted() throws Exception {
		String exactlyMax = START_AT.plus(Duration.ofDays(366)).toString();

		callAs(member1, availabilityUri(group.getId(), START, exactlyMax))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.members.length()").value(4));
	}

	// ---------- helpers ----------

	/**
	 * member1 and member2 are busy at overlapping times; member1's two adjacent ranges and the leader's range that
	 * crosses the window start must come back merged / clipped, each only under its own user id.
	 */
	private void stubOverlappingCalendars() {
		when(calendarApi.busyIntervals(leader.getId(), START_AT, END_AT)).thenReturn(List.of(
				range("2026-11-22T22:00:00Z", "2026-11-23T01:00:00Z")));
		when(calendarApi.busyIntervals(member1.getId(), START_AT, END_AT)).thenReturn(List.of(
				range("2026-11-24T11:00:00Z", "2026-11-24T12:00:00Z"),
				range("2026-11-24T09:00:00Z", "2026-11-24T11:00:00Z")));
		when(calendarApi.busyIntervals(member2.getId(), START_AT, END_AT)).thenReturn(List.of(
				range("2026-11-24T10:00:00Z", "2026-11-24T13:00:00Z")));
		when(calendarApi.busyIntervals(freeMember.getId(), START_AT, END_AT)).thenReturn(List.of());
	}

	private void assertOwnIntervalsOnly(ResultActions result) throws Exception {
		result.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.members.length()").value(4))
				.andExpect(jsonPath("$.data.members[0].userId").value(leader.getId()))
				.andExpect(jsonPath("$.data.members[0].busy.length()").value(1))
				.andExpect(jsonPath("$.data.members[0].busy[0].startAt").value("2026-11-23T00:00:00Z"))
				.andExpect(jsonPath("$.data.members[0].busy[0].endAt").value("2026-11-23T01:00:00Z"))
				.andExpect(jsonPath("$.data.members[1].userId").value(member1.getId()))
				.andExpect(jsonPath("$.data.members[1].busy.length()").value(1))
				.andExpect(jsonPath("$.data.members[1].busy[0].startAt").value("2026-11-24T09:00:00Z"))
				.andExpect(jsonPath("$.data.members[1].busy[0].endAt").value("2026-11-24T12:00:00Z"))
				.andExpect(jsonPath("$.data.members[2].userId").value(member2.getId()))
				.andExpect(jsonPath("$.data.members[2].busy.length()").value(1))
				.andExpect(jsonPath("$.data.members[2].busy[0].startAt").value("2026-11-24T10:00:00Z"))
				.andExpect(jsonPath("$.data.members[2].busy[0].endAt").value("2026-11-24T13:00:00Z"))
				.andExpect(jsonPath("$.data.members[3].userId").value(freeMember.getId()))
				.andExpect(jsonPath("$.data.members[3].busy.length()").value(0));
		verify(calendarApi, never()).busyIntervals(eq(formerMember.getId()), any(), any());
	}

	private static String availabilityUri(Long groupId, String start, String end) {
		return "/v1/groups/" + groupId + "/availability?start=" + start + "&end=" + end;
	}

	private ResultActions callAs(User user, String uri) throws Exception {
		return mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issue(user).value()));
	}

	private User newUser(String email) {
		User user = userRepository.save(new User(email, "$2a$10$hash", "User " + email, null));
		extraUserIds.add(user.getId());
		return user;
	}

	private GroupMember addMember(User user, GroupRole role) {
		return memberRepository.save(new GroupMember(group.getId(), user.getId(), role,
				Instant.now().truncatedTo(ChronoUnit.MILLIS)));
	}

	/** Seeds the INACTIVE state directly (left_at is required for INACTIVE in V7). */
	private void deactivate(GroupMember member) {
		jdbc.update("UPDATE group_members SET status = 'INACTIVE', left_at = ? WHERE id = ?",
				Timestamp.from(member.getJoinedAt().plusSeconds(1)), member.getId());
	}

	private static Set<String> keys(Object jsonObject) {
		return ((Map<?, ?>) jsonObject).keySet().stream().map(String::valueOf).collect(Collectors.toSet());
	}

	private static TimeRange range(String start, String end) {
		return new TimeRange(Instant.parse(start), Instant.parse(end));
	}
}
