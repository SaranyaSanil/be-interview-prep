package com.interviewprep.booking;

import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * app.booking.hold-duration (BOOKING_HOLD_DURATION; a bare number means minutes) and the
 * clinic's time zone, used to decide whether a slot's local start time is already in the past.
 */
@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(@DurationUnit(ChronoUnit.MINUTES) Duration holdDuration, ZoneId zone) {

    public BookingProperties {
        if (holdDuration == null || holdDuration.isZero() || holdDuration.isNegative()) {
            throw new IllegalArgumentException("app.booking.hold-duration must be a positive duration");
        }
        if (zone == null) {
            throw new IllegalArgumentException("app.booking.zone must be set");
        }
    }
}
