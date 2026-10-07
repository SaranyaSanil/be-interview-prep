package com.interviewprep.booking;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

class BookingNotificationListenerTest {

    @Test
    void saturatedPoolDropsTheNotificationWithoutFailing() {
        BookingNotifier notifier = mock(BookingNotifier.class);
        Executor saturated = task -> {
            throw new RejectedExecutionException("queue full");
        };
        BookingNotificationListener listener = new BookingNotificationListener(notifier, saturated);

        assertThatCode(() -> listener.onBookingConfirmed(
                new BookingConfirmedEvent(1L, "Dr. Rao", LocalDateTime.of(2026, 10, 8, 9, 0), "alice")))
                .doesNotThrowAnyException();
        verify(notifier, never()).bookingConfirmed(any());
    }
}
