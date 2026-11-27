package group16.webapp.modules.schedule.engine;

import group16.webapp.modules.availability.api.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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

    @Test
    void overlapsAny_isHalfOpen() {
        var others = List.of(r("10:00", "11:00"));
        assertFalse(SlotCalculator.overlapsAny(r("09:00", "10:00"), others));
        assertFalse(SlotCalculator.overlapsAny(r("11:00", "12:00"), others));
        assertTrue(SlotCalculator.overlapsAny(r("10:59", "12:00"), others));
        assertTrue(SlotCalculator.overlapsAny(r("10:15", "10:45"), others));
    }
}
