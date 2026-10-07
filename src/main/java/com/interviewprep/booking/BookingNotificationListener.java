package com.interviewprep.booking;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * AFTER_COMMIT: runs only if the confirm transaction actually committed, so a rolled-back
 * booking never sends a notification. The send is handed to the notification thread pool, so a
 * slow notifier doesn't delay the confirm response.
 *
 * <p>Delivery is at-most-once: if the pool is saturated the notification is dropped and logged
 * with enough detail to resend it, and a crash after commit loses queued ones. A transactional
 * outbox (a notification row written in the confirm transaction, sent by a poller) would make it
 * durable.
 */
@Component
public class BookingNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationListener.class);

    private final BookingNotifier notifier;
    private final Executor executor;

    public BookingNotificationListener(BookingNotifier notifier,
                                       @Qualifier(BookingConfig.NOTIFICATION_EXECUTOR) Executor executor) {
        this.notifier = notifier;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        try {
            executor.execute(() -> notifier.bookingConfirmed(event));
        } catch (RejectedExecutionException e) {
            // Thrown here it would only be swallowed by the transaction infrastructure; make it visible.
            log.error("Booking confirmation notification dropped (pool saturated): slot {}, patient {}",
                    event.slotId(), event.patientId());
        }
    }
}
