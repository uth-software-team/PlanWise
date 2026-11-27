package group16.webapp.modules.schedule.engine;

import group16.webapp.modules.availability.api.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlotCalculatorTest {

    private static Instant t(String hhmm) {
        return Instant.parse("2026-11-30T" + hhmm + ":00Z");
    }

    private static TimeRange r(String start, String end) {
        return new TimeRange(t(start), t(end));
    }

    @Test
    void subtractBusy_noBusyReturnsAvailability() {
        assertEquals(List.of(r("09:00", "12:00")), SlotCalculator.subtractBusy(List.of(r("09:00", "12:00")), List.of()));
    }

    @Test
    void subtractBusy_emptyOrNullAvailabilityReturnsNothing() {
        assertTrue(SlotCalculator.subtractBusy(List.of(), List.of(r("09:00", "10:00"))).isEmpty());
        assertTrue(SlotCalculator.subtractBusy(null, null).isEmpty());
    }

    @Test
    void subtractBusy_busyInMiddleSplitsSlot() {
        assertEquals(List.of(r("09:00", "10:00"), r("11:00", "12:00")),
                SlotCalculator.subtractBusy(List.of(r("09:00", "12:00")), List.of(r("10:00", "11:00"))));
    }

    @Test
    void subtractBusy_busyCoveringStartOrEndTrimsSlot() {
        assertEquals(List.of(r("10:00", "11:00")),
                SlotCalculator.subtractBusy(List.of(r("09:00", "12:00")),
                        List.of(r("08:00", "10:00"), r("11:00", "13:00"))));
    }

    @Test
    void subtractBusy_busyCoveringWholeSlotRemovesIt() {
        assertTrue(SlotCalculator.subtractBusy(List.of(r("09:00", "10:00")), List.of(r("08:00", "11:00"))).isEmpty());
    }

    @Test
    void subtractBusy_adjacentBusyDoesNotOverlap_halfOpen() {
        // Busy [08:00,09:00) ends exactly when availability starts; busy [12:00,13:00) starts exactly at its end.
        assertEquals(List.of(r("09:00", "12:00")),
                SlotCalculator.subtractBusy(List.of(r("09:00", "12:00")),
                        List.of(r("08:00", "09:00"), r("12:00", "13:00"))));
    }

    @Test
    void subtractBusy_unsortedOverlappingBusyIsNormalized() {
        assertEquals(List.of(r("09:00", "09:30"), r("11:30", "12:00")),
                SlotCalculator.subtractBusy(List.of(r("09:00", "12:00")),
                        List.of(r("10:30", "11:30"), r("09:30", "11:00"))));
    }

    @Test
    void subtractBusy_multipleAvailabilityRanges() {
        assertEquals(List.of(r("09:00", "10:00"), r("14:00", "15:00"), r("15:30", "16:00")),
                SlotCalculator.subtractBusy(List.of(r("14:00", "16:00"), r("09:00", "10:00")),
                        List.of(r("15:00", "15:30"))));
    }

    @Test
    void mergeOverlapping_mergesOverlapAndAdjacentAndDropsInvalid() {
        var merged = SlotCalculator.mergeOverlapping(java.util.Arrays.asList(
                r("10:00", "11:00"), r("09:00", "10:00"), r("10:30", "12:00"),
                r("13:00", "13:00"), r("15:00", "14:00"), null));
        assertEquals(List.of(r("09:00", "12:00")), merged);
    }

    @Test
    void isContainedWithin_respectsBoundaries() {
        var containers = List.of(r("09:00", "12:00"));
        assertTrue(SlotCalculator.isContainedWithin(r("09:00", "12:00"), containers));
        assertFalse(SlotCalculator.isContainedWithin(r("08:59", "10:00"), containers));
        assertFalse(SlotCalculator.isContainedWithin(r("11:00", "12:01"), containers));
        // Adjacent availability rules count as one continuous range.
        assertTrue(SlotCalculator.isContainedWithin(r("11:00", "13:00"), List.of(r("09:00", "12:00"), r("12:00", "14:00"))));
    }

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh"); // UTC+7, no DST

    @Test
    void hc04_sameLocalDayIsValid_andEndingExactlyAtLocalMidnightIsAllowed() {
        // 23:00–24:00 local (+07) = 16:00Z–17:00Z.
        assertTrue(SlotCalculator.isWithinOneLocalDay(t("16:00"), t("17:00"), VN));
        assertTrue(SlotCalculator.isWithinOneLocalDay(t("02:00"), t("03:00"), VN));
    }

    @Test
    void hc04_crossingLocalMidnightIsInvalid() {
        // 23:30–00:30 local.
        assertFalse(SlotCalculator.isWithinOneLocalDay(t("16:30"), t("17:30"), VN));
    }

    @Test
    void hc04_usesUserZoneNotUtc() {
        // 16:30Z–17:30Z is one UTC day but crosses midnight in +07; 23:30Z–00:30Z is the reverse.
        assertTrue(SlotCalculator.isWithinOneLocalDay(t("16:30"), t("17:30"), ZoneOffset.UTC));
        assertFalse(SlotCalculator.isWithinOneLocalDay(t("16:30"), t("17:30"), VN));
        Instant lateUtc = t("23:30");
        assertFalse(SlotCalculator.isWithinOneLocalDay(lateUtc, lateUtc.plusSeconds(3600), ZoneOffset.UTC));
        assertTrue(SlotCalculator.isWithinOneLocalDay(lateUtc, lateUtc.plusSeconds(3600), VN));
    }

    @Test
    void hc04_emptyOrReversedRangeIsInvalid() {
        assertFalse(SlotCalculator.isWithinOneLocalDay(t("10:00"), t("10:00"), VN));
        assertFalse(SlotCalculator.isWithinOneLocalDay(t("11:00"), t("10:00"), VN));
    }

    @Test
    void hc04_dstDaysUseRealLocalMidnight() {
        ZoneId ny = ZoneId.of("America/New_York");
        // 2026-11-01 is 25 h long (EDT -> EST). 22:00–24:00 EST = 03:00Z–05:00Z on 11-02.
        assertTrue(SlotCalculator.isWithinOneLocalDay(
                Instant.parse("2026-11-02T03:00:00Z"), Instant.parse("2026-11-02T05:00:00Z"), ny));
        assertFalse(SlotCalculator.isWithinOneLocalDay(
                Instant.parse("2026-11-02T03:00:00Z"), Instant.parse("2026-11-02T05:01:00Z"), ny));
        // 2026-03-08 is 23 h long (EST -> EDT). 23:00–24:00 EDT = 03:00Z–04:00Z on 03-09.
        assertTrue(SlotCalculator.isWithinOneLocalDay(
                Instant.parse("2026-03-09T03:00:00Z"), Instant.parse("2026-03-09T04:00:00Z"), ny));
        assertFalse(SlotCalculator.isWithinOneLocalDay(
                Instant.parse("2026-03-09T03:00:00Z"), Instant.parse("2026-03-09T04:01:00Z"), ny));
    }

    @Test
    void splitAtLocalMidnights_splitsOnlyAtLocalMidnight() {
        // 22:00–02:00 in +07 = 15:00Z–19:00Z; midnight = 17:00Z.
        assertEquals(List.of(r("15:00", "17:00"), r("17:00", "19:00")),
                SlotCalculator.splitAtLocalMidnights(r("15:00", "19:00"), VN));
        // Same instants in UTC stay one piece.
        assertEquals(List.of(r("15:00", "19:00")), SlotCalculator.splitAtLocalMidnights(r("15:00", "19:00"), ZoneOffset.UTC));
        // Ending exactly at local midnight is not split.
        assertEquals(List.of(r("10:00", "17:00")), SlotCalculator.splitAtLocalMidnights(r("10:00", "17:00"), VN));
    }

    @Test
    void splitAtLocalMidnights_multiDayRangeGivesOnePiecePerDay() {
        var pieces = SlotCalculator.splitAtLocalMidnights(
                new TimeRange(Instant.parse("2026-11-30T12:00:00Z"), Instant.parse("2026-12-02T12:00:00Z")), ZoneOffset.UTC);
        assertEquals(List.of(
                new TimeRange(Instant.parse("2026-11-30T12:00:00Z"), Instant.parse("2026-12-01T00:00:00Z")),
                new TimeRange(Instant.parse("2026-12-01T00:00:00Z"), Instant.parse("2026-12-02T00:00:00Z")),
                new TimeRange(Instant.parse("2026-12-02T00:00:00Z"), Instant.parse("2026-12-02T12:00:00Z"))), pieces);
    }

    @Test
    void splitAtLocalMidnights_everyPieceSatisfiesTheEditConfirmRule() {
        var range = new TimeRange(Instant.parse("2026-10-30T18:00:00Z"), Instant.parse("2026-11-04T06:00:00Z"));
        for (ZoneId zone : List.of(ZoneOffset.UTC, VN, ZoneId.of("America/New_York"))) {
            var pieces = SlotCalculator.splitAtLocalMidnights(range, zone);
            assertEquals(range.start(), pieces.get(0).start(), zone.toString());
            assertEquals(range.end(), pieces.get(pieces.size() - 1).end(), zone.toString());
            for (int i = 0; i < pieces.size(); i++) {
                var p = pieces.get(i);
                assertTrue(SlotCalculator.isWithinOneLocalDay(p.start(), p.end(), zone), zone + " " + p);
                if (i > 0) assertEquals(pieces.get(i - 1).end(), p.start(), "contiguous in " + zone);
            }
        }
    }

    @Test
    void overlapsAny_isHalfOpen() {
        var others = List.of(r("10:00", "11:00"));
        assertFalse(SlotCalculator.overlapsAny(r("09:00", "10:00"), others));
        assertFalse(SlotCalculator.overlapsAny(r("11:00", "12:00"), others));
        assertTrue(SlotCalculator.overlapsAny(r("10:59", "12:00"), others));
        assertTrue(SlotCalculator.overlapsAny(r("10:15", "10:45"), others));
    }
}
