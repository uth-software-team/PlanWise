package group16.webapp.modules.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.JwtService;
import group16.webapp.modules.task.api.NewGroupTaskCommand;
import group16.webapp.modules.task.api.SchedulableTask;
import group16.webapp.modules.task.api.TaskApi;
import group16.webapp.modules.task.api.TaskCategory;
import group16.webapp.modules.task.api.TaskPriority;
import group16.webapp.modules.task.api.TaskStatus;
import group16.webapp.modules.task.repository.TaskRepository;
import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

class TaskControllerTests extends AuthenticatedMvcTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskApi taskApi;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanTasks() {
        taskRepository.deleteAll();
    }

    @Test
    void crudLifecycleAndDerivedOverdue() throws Exception {
        Instant pastDeadline = Instant.now().minus(2, ChronoUnit.HOURS);
        String createBody = """
                {
                  "title": "  Finish SRS  ",
                  "deadline": "%s",
                  "priority": "URGENT",
                  "category": "PROJECT",
                  "estimatedDurationMinutes": 120
                }
                """.formatted(pastDeadline);

        String createResponse = callAuthenticated(HttpMethod.POST, "/v1/tasks", createBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Finish SRS"))
                .andExpect(jsonPath("$.data.description").value(""))
                .andExpect(jsonPath("$.data.status").value("TODO"))
                .andExpect(jsonPath("$.data.overdue").value(true))
                .andExpect(jsonPath("$.data.version").value(0))
                .andReturn().getResponse().getContentAsString();

        Number idNum = JsonPath.read(createResponse, "$.data.id");
        long taskId = idNum.longValue();

        callAuthenticated(HttpMethod.GET, "/v1/tasks/" + taskId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.overdue").value(true));

        String updateBody = """
                {
                  "title": "Finish SRS v2",
                  "description": "Reviewed sections",
                  "deadline": "%s",
                  "priority": "HIGH",
                  "category": "STUDY",
                  "estimatedDurationMinutes": 90,
                  "actualDurationMinutes": 45,
                  "version": 0
                }
                """.formatted(pastDeadline);

        callAuthenticated(HttpMethod.PUT, "/v1/tasks/" + taskId, updateBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Finish SRS v2"))
                .andExpect(jsonPath("$.data.description").value("Reviewed sections"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.actualDurationMinutes").value(45))
                .andExpect(jsonPath("$.data.version").value(1));

        // Stale version on PUT -> 409 STALE_VERSION
        callAuthenticated(HttpMethod.PUT, "/v1/tasks/" + taskId, updateBody)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));

        // Completing the task makes overdue false even with past deadline
        callAuthenticated(HttpMethod.PATCH, "/v1/tasks/" + taskId + "/status",
                "{\"status\":\"COMPLETED\",\"version\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.overdue").value(false))
                .andExpect(jsonPath("$.data.version").value(2));

        // Stale version on PATCH -> 409 STALE_VERSION
        callAuthenticated(HttpMethod.PATCH, "/v1/tasks/" + taskId + "/status",
                "{\"status\":\"TODO\",\"version\":1}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));

        // Soft delete -> 204, subsequent GET -> 404
        callAuthenticated(HttpMethod.DELETE, "/v1/tasks/" + taskId, null)
                .andExpect(status().isNoContent());

        callAuthenticated(HttpMethod.GET, "/v1/tasks/" + taskId, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void ownerIsolationReturns404ForOtherUsers() throws Exception {
        String created = callAuthenticated(HttpMethod.POST, "/v1/tasks", """
                {
                  "title": "Secret task",
                  "priority": "MEDIUM",
                  "category": "PERSONAL",
                  "estimatedDurationMinutes": 30
                }
                """)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long taskId = ((Number) JsonPath.read(created, "$.data.id")).longValue();

        User other = userRepository.save(
                new User("other-task-user@planwise.local", "$2a$10$hash", "Other User", null));
        String otherToken = jwtService.issue(other).value();

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.put("/v1/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hijacked",
                                  "priority": "LOW",
                                  "category": "OTHER",
                                  "estimatedDurationMinutes": 10,
                                  "version": 0
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/v1/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(MockMvcRequestBuilders.patch("/v1/tasks/" + taskId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"version\":0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listSupportsFiltersHalfOpenDeadlineAndPagination() throws Exception {
        Instant t1 = Instant.parse("2026-12-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-12-02T10:00:00Z");
        Instant t3 = Instant.parse("2026-12-03T10:00:00Z");

        callAuthenticated(HttpMethod.POST, "/v1/tasks", """
                {"title":"Task 1","deadline":"%s","priority":"HIGH","category":"STUDY","estimatedDurationMinutes":30}
                """.formatted(t1)).andExpect(status().isCreated());
        callAuthenticated(HttpMethod.POST, "/v1/tasks", """
                {"title":"Task 2","deadline":"%s","priority":"HIGH","category":"WORK","estimatedDurationMinutes":45}
                """.formatted(t2)).andExpect(status().isCreated());
        callAuthenticated(HttpMethod.POST, "/v1/tasks", """
                {"title":"Task 3","deadline":"%s","priority":"LOW","category":"OTHER","estimatedDurationMinutes":60}
                """.formatted(t3)).andExpect(status().isCreated());

        // Half-open [t1, t2) includes t1 and excludes t2
        callAuthenticated(HttpMethod.GET,
                "/v1/tasks?priority=HIGH&deadlineFrom=2026-12-01T10:00:00Z&deadlineTo=2026-12-02T10:00:00Z", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Task 1"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // Invalid time range (deadlineFrom > deadlineTo) -> 400 INVALID_TIME_RANGE
        callAuthenticated(HttpMethod.GET,
                "/v1/tasks?deadlineFrom=2026-12-05T00:00:00Z&deadlineTo=2026-12-01T00:00:00Z", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));

        // Paging
        callAuthenticated(HttpMethod.GET, "/v1/tasks?page=0&size=2", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(false));
    }

    @Test
    void taskApiSupportsSchedulableTasksGroupCreationAndSummaries() throws Exception {
        User owner = userRepository.findAll().getFirst();
        User assignee = userRepository.save(
                new User("member-task@planwise.local", "$2a$10$hash", "Member", null));
        long groupId = createGroupAsCaller();

        Long groupTaskId = taskApi.createGroupTask(groupId, owner.getId(),
                new NewGroupTaskCommand("Group Deliverable", "Module API",
                        Instant.parse("2026-12-10T12:00:00Z"), TaskPriority.URGENT, TaskCategory.PROJECT, 120));

        assertThat(taskApi.findActiveGroupTaskGroupId(groupTaskId)).contains(groupId);
        // Contract: empty for personal tasks.
        long personalTaskId = createPersonalTaskAsCaller("Personal note");
        assertThat(taskApi.findActiveGroupTaskGroupId(personalTaskId)).isEmpty();
        assertThat(taskApi.summaries(List.of(groupTaskId, 999999L)))
                .hasSize(1)
                .first()
                .extracting(s -> s.title(), s -> s.status())
                .containsExactly("Group Deliverable", TaskStatus.TODO);

        // Assignee gets accepted group task in schedulableTasks, while creator does NOT get unaccepted group task
        List<SchedulableTask> creatorTasks = taskApi.schedulableTasks(owner.getId(), Set.of());
        assertThat(creatorTasks).extracting(SchedulableTask::taskId).doesNotContain(groupTaskId);

        List<SchedulableTask> assigneeTasks = taskApi.schedulableTasks(assignee.getId(), Set.of(groupTaskId));
        assertThat(assigneeTasks).extracting(SchedulableTask::taskId).containsExactly(groupTaskId);

        // Only group tasks count as accepted ids: another user's personal task id passed in is never scheduled.
        List<SchedulableTask> withForeignPersonalId =
                taskApi.schedulableTasks(assignee.getId(), Set.of(groupTaskId, personalTaskId));
        assertThat(withForeignPersonalId).extracting(SchedulableTask::taskId).containsExactly(groupTaskId);
    }

    @Test
    void personalEndpointsExcludeGroupTasksOwnedByTheCaller() throws Exception {
        // D15: the caller owns the group task (leader who created it), yet /v1/tasks must not expose it.
        User caller = userRepository.findAll().getFirst();
        long groupId = createGroupAsCaller();
        Long groupTaskId = taskApi.createGroupTask(groupId, caller.getId(),
                new NewGroupTaskCommand("Team deliverable", "Shared", null,
                        TaskPriority.HIGH, TaskCategory.PROJECT, 60));
        long personalTaskId = createPersonalTaskAsCaller("My own task");

        callAuthenticated(HttpMethod.GET, "/v1/tasks", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(personalTaskId));
        callAuthenticated(HttpMethod.GET, "/v1/tasks?priority=HIGH&status=TODO", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        String uri = "/v1/tasks/" + groupTaskId;
        callAuthenticated(HttpMethod.GET, uri, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        callAuthenticated(HttpMethod.PUT, uri, """
                {
                  "title": "Renamed",
                  "priority": "LOW",
                  "category": "OTHER",
                  "estimatedDurationMinutes": 10,
                  "version": 0
                }
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        callAuthenticated(HttpMethod.PATCH, uri + "/status", "{\"status\":\"COMPLETED\",\"version\":0}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        callAuthenticated(HttpMethod.DELETE, uri, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        // The group task is untouched by the rejected calls.
        assertThat(taskApi.summaries(List.of(groupTaskId)))
                .singleElement()
                .extracting(s -> s.title(), s -> s.status())
                .containsExactly("Team deliverable", TaskStatus.TODO);
        assertThat(taskApi.findActiveGroupTaskGroupId(groupTaskId)).contains(groupId);
    }

    /** Creates a real group through the group API (caller becomes LEADER) instead of a made-up group id. */
    private long createGroupAsCaller() throws Exception {
        String body = callAuthenticated(HttpMethod.POST, "/v1/groups",
                "{\"groupName\":\"Task test group\",\"description\":\"\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private long createPersonalTaskAsCaller(String title) throws Exception {
        String body = callAuthenticated(HttpMethod.POST, "/v1/tasks", """
                {
                  "title": "%s",
                  "priority": "MEDIUM",
                  "category": "PERSONAL",
                  "estimatedDurationMinutes": 30
                }
                """.formatted(title))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }
}