package com.interviewprep.booking;

import com.interviewprep.common.ConflictException;
import com.interviewprep.common.NotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Two-step booking: hold, then confirm before the hold expires. Every state change is one
 * conditional UPDATE (see {@link SlotRepository}); 0 rows updated means the slot was not in the
 * required state, which becomes a 404 or 409.
 */
@Service
public class BookingService {

    private final SlotRepository slotRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final Duration holdDuration;
    private final ZoneId clinicZone;

    public BookingService(SlotRepository slotRepository, ApplicationEventPublisher events, Clock clock,
                          BookingProperties properties) {
        this.slotRepository = slotRepository;
        this.events = events;
        this.clock = clock;
        this.holdDuration = properties.holdDuration();
        this.clinicZone = properties.zone();
    }

    @Transactional
    public Slot hold(Long slotId, String patientId) {
        Slot slot = getSlot(slotId);
        Instant now = clock.instant();
        Instant startsAt = slot.getStartsAt().atZone(clinicZone).toInstant();
        if (!startsAt.isAfter(now)) {
            throw new ConflictException("Slot " + slotId + " has already started");
        }
        // A hold never outlives the slot's start, so confirm's expiry check also stops confirming
        // an appointment that is already under way.
        Instant expiresAt = now.plus(holdDuration).isAfter(startsAt) ? startsAt : now.plus(holdDuration);
        if (slotRepository.hold(slotId, patientId, now, expiresAt) == 0) {
            throw new ConflictException("Slot " + slotId + " is not available");
        }
        return getSlot(slotId);
    }

    /**
     * Publishes BookingConfirmedEvent; the notification listener runs only after this
     * transaction commits, and asynchronously, so it neither delays nor outlives a failed save.
     */
    @Transactional
    public Slot confirm(Long slotId, String patientId) {
        getSlot(slotId);
        if (slotRepository.confirm(slotId, patientId, clock.instant()) == 0) {
            throw new ConflictException("No active hold on slot " + slotId + " for this patient; it may have expired");
        }
        Slot slot = getSlot(slotId);
        events.publishEvent(new BookingConfirmedEvent(
                slot.getId(), slot.getDoctor().getName(), slot.getStartsAt(), patientId));
        return slot;
    }

    @Transactional
    public Slot cancel(Long slotId, String patientId) {
        getSlot(slotId);
        if (slotRepository.cancel(slotId, patientId) == 0) {
            throw new ConflictException("No confirmed booking on slot " + slotId + " for this patient");
        }
        return getSlot(slotId);
    }

    private Slot getSlot(Long slotId) {
        return slotRepository.findById(slotId).orElseThrow(() -> new NotFoundException("Slot " + slotId + " not found"));
    }
}
