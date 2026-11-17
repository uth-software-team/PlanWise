package group16.webapp.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class DateUtilsTests {

	@Test
	void acceptsIanaRegionIds() {
		assertThat(DateUtils.isValidTimezone("Asia/Ho_Chi_Minh")).isTrue();
		assertThat(DateUtils.isValidTimezone("Europe/Paris")).isTrue();
		assertThat(DateUtils.isValidTimezone(DateUtils.DEFAULT_TIMEZONE)).isTrue();
	}

	@Test
	void rejectsNullBlankUnknownAndOffsetIds() {
		assertThat(DateUtils.isValidTimezone(null)).isFalse();
		assertThat(DateUtils.isValidTimezone(" ")).isFalse();
		assertThat(DateUtils.isValidTimezone("Mars/Olympus")).isFalse();
		assertThat(DateUtils.isValidTimezone("+07:00")).isFalse();
		assertThat(DateUtils.isValidTimezone("UTC")).isFalse();
	}

	@Test
	void zoneOfThrowsForInvalidId() {
		assertThat(DateUtils.zoneOf("Asia/Tokyo")).isEqualTo(ZoneId.of("Asia/Tokyo"));
		assertThatThrownBy(() -> DateUtils.zoneOf("Nowhere/City"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void todayDependsOnUserTimezone() {
		// 2026-11-17T20:00Z is already 18 Nov in Ho Chi Minh (UTC+7) but still 17 Nov in New York.
		Instant now = Instant.parse("2026-11-17T20:00:00Z");
		assertThat(DateUtils.today(now, ZoneId.of("Asia/Ho_Chi_Minh"))).isEqualTo(LocalDate.of(2026, 11, 18));
		assertThat(DateUtils.today(now, ZoneId.of("America/New_York"))).isEqualTo(LocalDate.of(2026, 11, 17));
	}

	@Test
	void pastDeadlineComparesInstants() {
		Instant deadline = Instant.parse("2026-12-03T10:00:00Z");
		assertThat(DateUtils.isPastDeadline(deadline, deadline.plusSeconds(1))).isTrue();
		assertThat(DateUtils.isPastDeadline(deadline, deadline)).isFalse();
		assertThat(DateUtils.isPastDeadline(deadline, deadline.minusSeconds(1))).isFalse();
		assertThat(DateUtils.isPastDeadline(null, deadline)).isFalse();
	}

}
