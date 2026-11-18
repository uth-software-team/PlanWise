package group16.webapp.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints (test classpath) that raise each exception type handled by GlobalExceptionHandler. */
@RestController
@RequestMapping("/v1/test-probe")
class ExceptionProbeController {

	record ProbeRequest(@NotBlank String title, @Positive int estimatedDurationMinutes) {}

	@PostMapping("/body")
	String validateBody(@Valid @RequestBody ProbeRequest request) {
		return "ok";
	}

	@GetMapping("/param")
	String validateParam(@RequestParam @Min(1) int size) {
		return "ok";
	}

	@GetMapping("/app")
	String app() {
		throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
	}

	@GetMapping("/stale")
	String stale() {
		throw new ObjectOptimisticLockingFailureException("ScheduleSession", 1L);
	}

	@GetMapping("/integrity")
	String integrity() {
		throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"uk_users_email\"");
	}

	@GetMapping("/boom")
	String boom() {
		throw new IllegalStateException("secret internal detail");
	}
}
