package com.interviewprep.booking;

import java.time.LocalDateTime;

public record BookingConfirmedEvent(Long slotId, String doctorName, LocalDateTime startsAt, String patientId) {
}
