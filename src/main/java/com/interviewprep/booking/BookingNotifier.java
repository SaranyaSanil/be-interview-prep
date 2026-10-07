package com.interviewprep.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Sends the confirmation; logging stands in for e-mail or SMS. */
@Component
public class BookingNotifier {

    private static final Logger log = LoggerFactory.getLogger(BookingNotifier.class);

    public void bookingConfirmed(BookingConfirmedEvent event) {
        log.info("Booking confirmed: patient {} with {} at {} (slot {})",
                event.patientId(), event.doctorName(), event.startsAt(), event.slotId());
    }
}
