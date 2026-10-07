package com.interviewprep.booking;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * AFTER_COMMIT: runs only if the confirm transaction actually committed, so a rolled-back
 * booking never sends a notification. @Async: runs on the notification thread pool, so a slow
 * notifier doesn't delay the confirm response.
 */
@Component
public class BookingNotificationListener {

    private final BookingNotifier notifier;

    public BookingNotificationListener(BookingNotifier notifier) {
        this.notifier = notifier;
    }

    @Async(BookingConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        notifier.bookingConfirmed(event);
    }
}
