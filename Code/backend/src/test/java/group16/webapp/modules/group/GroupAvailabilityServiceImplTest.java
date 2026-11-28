package group16.webapp.modules.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.availability.api.CalendarApi;
import group16.webapp.modules.availability.api.TimeRange;
import group16.webapp.modules.group.api.GroupAccessApi;
import group16.webapp.modules.group.dto.GroupAvailabilityResponse;
import group16.webapp.modules.group.dto.GroupAvailabilityResponse.BusyInterval;
import group16.webapp.modules.group.dto.GroupAvailabilityResponse.MemberBusy;
import group16.webapp.modules.group.service.impl.GroupAvailabilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * GR-04 service logic with mocked collaborators: check order, the missing calendar provider (501) and the
 * clip / sort / merge rules. Independent of whether a real CalendarApi bean exists (stays valid after CAL-01).
 */
@ExtendWith(MockitoExtension.class)
class GroupAvailabilityServiceImplTest {

	private static final Long CALLER = 1L;
	private static final Long GROUP = 10L;
	private static final Instant START = Instant.parse("2026-11-23T00:00:00Z");
	private static final Instant END = Instant.parse("2026-11-30T00:00:00Z");
	private static final Instant EXACTLY_MAX = START.plus(Duration.ofDays(366));
	private static final Instant OVER_MAX = EXACTLY_MAX.plusSeconds(1);

	@Mock
	private UserApi userApi;

	@Mock
	private GroupAccessApi groupAccessApi;

	@Mock
	private ObjectProvider<CalendarApi> calendarApiProvider;

	@Mock
	private CalendarApi calendarApi;

	private GroupAvailabilityServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new GroupAvailabilityServiceImpl(userApi, groupAccessApi, calendarApiProvider);
	}

	@Test
	void missingCalendarProviderReturns501AfterCallerAndAccessChecks() {
		activeCaller();
		when(calendarApiProvider.getIfAvailable()).thenReturn(null);

		assertThatThrownBy(() -> service.getAvailability(CALLER, GROUP, START, END))
				.isInstanceOfSatisfying(AppException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_IMPLEMENTED));
		verify(groupAccessApi).requireMember(GROUP, CALLER);
		verify(groupAccessApi, never()).memberIds(GROUP);
	}

	@Test
	void inactiveCallerIsRejectedBeforeAnyOtherCheck() {
		when(userApi.findById(CALLER)).thenReturn(Optional.of(new UserSummary(CALLER, "c@example.com", "C", null,
				false)));

		assertError(() -> service.getAvailability(CALLER, GROUP, END, START), ErrorCode.UNAUTHENTICATED);
		verifyNoInteractions(groupAccessApi, calendarApiProvider);
	}

	@Test
	void nonIncreasingRangeIsRejectedBeforeGroupAccess() {
		activeCaller();

		assertError(() -> service.getAvailability(CALLER, GROUP, START, START), ErrorCode.INVALID_TIME_RANGE);
		assertError(() -> service.getAvailability(CALLER, GROUP, END, START), ErrorCode.INVALID_TIME_RANGE);
		verifyNoInteractions(groupAccessApi, calendarApiProvider);
	}

	@Test
	void inactiveCallerWithOverlongRangeStillGets401() {
		when(userApi.findById(CALLER)).thenReturn(Optional.of(new UserSummary(CALLER, "c@example.com", "C", null,
				false)));

		assertError(() -> service.getAvailability(CALLER, GROUP, START, OVER_MAX), ErrorCode.UNAUTHENTICATED);
		verifyNoInteractions(groupAccessApi, calendarApiProvider);
	}

	@Test
	void rangeLongerThan366DaysIsRejectedBeforeGroupAccess() {
		activeCaller();

		assertError(() -> service.getAvailability(CALLER, GROUP, START, OVER_MAX), ErrorCode.INVALID_TIME_RANGE);
		verifyNoInteractions(groupAccessApi, calendarApiProvider);
	}

	@Test
	void exactly366DaysPassesTheRangeCheck() {
		activeCaller();
		when(calendarApiProvider.getIfAvailable()).thenReturn(null);

		// Reaches the membership check and then the provider lookup (501 here), so the range was accepted.
		assertError(() -> service.getAvailability(CALLER, GROUP, START, EXACTLY_MAX), ErrorCode.NOT_IMPLEMENTED);
		verify(groupAccessApi).requireMember(GROUP, CALLER);
	}

	@Test
	void deniedGroupAccessNeverTouchesTheCalendar() {
		activeCaller();
		doThrow(new AppException(ErrorCode.FORBIDDEN)).when(groupAccessApi).requireMember(GROUP, CALLER);

		assertError(() -> service.getAvailability(CALLER, GROUP, START, END), ErrorCode.FORBIDDEN);
		verifyNoInteractions(calendarApiProvider);
	}

	@Test
	void eachMemberGetsOwnNormalizedIntervalsInMembershipOrder() {
		activeCaller();
		when(calendarApiProvider.getIfAvailable()).thenReturn(calendarApi);
		when(groupAccessApi.memberIds(GROUP)).thenReturn(List.of(3L, CALLER, 2L));
		when(calendarApi.busyIntervals(3L, START, END)).thenReturn(List.of(
				range("2026-11-24T10:00:00Z", "2026-11-24T11:00:00Z"),
				range("2026-11-24T09:00:00Z", "2026-11-24T10:00:00Z")));
		when(calendarApi.busyIntervals(CALLER, START, END)).thenReturn(List.of());
		when(calendarApi.busyIntervals(2L, START, END)).thenReturn(List.of(
				range("2026-11-24T09:30:00Z", "2026-11-24T12:00:00Z")));

		GroupAvailabilityResponse response = service.getAvailability(CALLER, GROUP, START, END);

		assertThat(response.members()).containsExactly(
				new MemberBusy(3L, List.of(busy("2026-11-24T09:00:00Z", "2026-11-24T11:00:00Z"))),
				new MemberBusy(CALLER, List.of()),
				new MemberBusy(2L, List.of(busy("2026-11-24T09:30:00Z", "2026-11-24T12:00:00Z"))));
	}

	@Test
	void normalizeClipsDropsEmptySortsAndMergesOverlappingOrAdjacent() {
		List<TimeRange> ranges = List.of(
				range("2026-11-25T14:00:00Z", "2026-11-25T15:00:00Z"),  // separate, unsorted input
				range("2026-11-22T20:00:00Z", "2026-11-23T02:00:00Z"),  // crosses start -> clipped
				range("2026-11-23T01:00:00Z", "2026-11-23T03:00:00Z"),  // overlaps previous
				range("2026-11-23T03:00:00Z", "2026-11-23T04:00:00Z"),  // adjacent -> merged
				range("2026-11-23T05:00:00Z", "2026-11-23T05:00:00Z"),  // empty -> dropped
				range("2026-11-20T00:00:00Z", "2026-11-23T00:00:00Z"),  // ends at start -> dropped
				range("2026-11-30T00:00:00Z", "2026-12-01T00:00:00Z"),  // starts at end -> dropped
				range("2026-11-25T14:15:00Z", "2026-11-25T14:45:00Z"),  // contained -> absorbed
				range("2026-11-29T23:00:00Z", "2026-12-02T00:00:00Z")); // crosses end -> clipped

		assertThat(normalize(ranges)).containsExactly(
				busy("2026-11-23T00:00:00Z", "2026-11-23T04:00:00Z"),
				busy("2026-11-25T14:00:00Z", "2026-11-25T15:00:00Z"),
				busy("2026-11-29T23:00:00Z", "2026-11-30T00:00:00Z"));
	}

	@Test
	void normalizeKeepsSeparatedIntervalsApartAndHandlesEmptyInput() {
		assertThat(normalize(List.of())).isEmpty();
		assertThat(normalize(List.of(
				range("2026-11-24T09:00:00Z", "2026-11-24T10:00:00Z"),
				range("2026-11-24T10:00:01Z", "2026-11-24T11:00:00Z"))))
				.containsExactly(
						busy("2026-11-24T09:00:00Z", "2026-11-24T10:00:00Z"),
						busy("2026-11-24T10:00:01Z", "2026-11-24T11:00:00Z"));
	}

	/** Runs the ranges through the public service for a single-member group (the caller). */
	private List<BusyInterval> normalize(List<TimeRange> ranges) {
		activeCaller();
		when(calendarApiProvider.getIfAvailable()).thenReturn(calendarApi);
		when(groupAccessApi.memberIds(GROUP)).thenReturn(List.of(CALLER));
		when(calendarApi.busyIntervals(CALLER, START, END)).thenReturn(ranges);
		return service.getAvailability(CALLER, GROUP, START, END).members().getFirst().busy();
	}

	private void activeCaller() {
		when(userApi.findById(CALLER)).thenReturn(Optional.of(new UserSummary(CALLER, "c@example.com", "C", null,
				true)));
	}

	private static void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
		assertThatThrownBy(call).isInstanceOfSatisfying(AppException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(code));
	}

	private static TimeRange range(String start, String end) {
		return new TimeRange(Instant.parse(start), Instant.parse(end));
	}

	private static BusyInterval busy(String start, String end) {
		return new BusyInterval(Instant.parse(start), Instant.parse(end));
	}
}
