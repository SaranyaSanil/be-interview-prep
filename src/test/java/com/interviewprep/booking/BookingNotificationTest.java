package com.interviewprep.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.interviewprep.common.ConflictException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class BookingNotificationTest {

    @MockitoBean
    private BookingNotifier notifier;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private long slotId;

    @BeforeEach
    void setUp() {
        slotRepository.deleteAll();
        doctorRepository.deleteAll();
        Doctor doctor = scheduleService.createDoctor("Dr. Rao");
        LocalDate tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        slotId = scheduleService.createSlots(doctor.getId(), tomorrow, LocalTime.of(9, 0), LocalTime.of(9, 30))
                .get(0).getId();
    }

    @Test
    void confirmedBookingSendsOneNotification() {
        bookingService.hold(slotId, "alice");
        bookingService.confirm(slotId, "alice");

        ArgumentCaptor<BookingConfirmedEvent> event = ArgumentCaptor.forClass(BookingConfirmedEvent.class);
        verify(notifier, timeout(2000).times(1)).bookingConfirmed(event.capture());
        assertThat(event.getValue().slotId()).isEqualTo(slotId);
        assertThat(event.getValue().patientId()).isEqualTo("alice");
        assertThat(event.getValue().doctorName()).isEqualTo("Dr. Rao");
    }

    @Test
    void failedConfirmSendsNoNotification() {
        assertThatThrownBy(() -> bookingService.confirm(slotId, "alice")).isInstanceOf(ConflictException.class);

        verify(notifier, after(500).never()).bookingConfirmed(any());
    }

    @Test
    void rolledBackConfirmSendsNoNotification() {
        bookingService.hold(slotId, "alice");

        transactionTemplate.executeWithoutResult(status -> {
            bookingService.confirm(slotId, "alice"); // joins this transaction and publishes the event
            status.setRollbackOnly();                // ...but the booking is never committed
        });

        verify(notifier, after(500).never()).bookingConfirmed(any());
        assertThat(slotRepository.findById(slotId).orElseThrow().getStatus()).isEqualTo(SlotStatus.HELD);
    }

    @Test
    void slowNotificationDoesNotDelayConfirm() {
        AtomicReference<String> notifierThread = new AtomicReference<>();
        doAnswer(invocation -> {
            notifierThread.set(Thread.currentThread().getName());
            Thread.sleep(2000);
            return null;
        }).when(notifier).bookingConfirmed(any());
        bookingService.hold(slotId, "alice");

        long startedAt = System.nanoTime();
        bookingService.confirm(slotId, "alice");
        long confirmMillis = (System.nanoTime() - startedAt) / 1_000_000;

        assertThat(confirmMillis).isLessThan(1000);
        verify(notifier, timeout(5000)).bookingConfirmed(any());
        assertThat(notifierThread.get()).startsWith("notify-");
    }
}
