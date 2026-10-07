package com.interviewprep.booking;

/**
 * AVAILABLE -> HELD (hold) -> BOOKED (confirm) -> AVAILABLE (cancel).
 * A HELD slot whose hold has expired is treated exactly like AVAILABLE.
 */
public enum SlotStatus {
    AVAILABLE,
    HELD,
    BOOKED
}
