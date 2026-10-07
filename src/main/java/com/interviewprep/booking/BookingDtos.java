package com.interviewprep.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Request and response bodies for the booking API. */
final class BookingDtos {

    private BookingDtos() {
    }

    record CreateDoctorRequest(@NotBlank @Size(max = 255) String name) {
    }

    record DoctorResponse(Long id, String name) {

        static DoctorResponse from(Doctor doctor) {
            return new DoctorResponse(doctor.getId(), doctor.getName());
        }
    }

    /** Creates 30-minute slots from {@code from} (inclusive) to {@code to} (exclusive) on {@code date}. */
    record CreateSlotsRequest(@NotNull LocalDate date, @NotNull LocalTime from, @NotNull LocalTime to) {
    }

    record SlotResponse(Long id, LocalDateTime startsAt, LocalDateTime endsAt) {

        static SlotResponse from(Slot slot) {
            return new SlotResponse(slot.getId(), slot.getStartsAt(), slot.getEndsAt());
        }
    }

    record PatientRequest(@NotBlank @Size(max = 100) String patientId) {
    }

    record BookingResponse(Long slotId, Long doctorId, LocalDateTime startsAt, LocalDateTime endsAt,
                           String patientId, SlotStatus status, Instant holdExpiresAt) {

        static BookingResponse from(Slot slot) {
            return new BookingResponse(slot.getId(), slot.getDoctor().getId(), slot.getStartsAt(),
                    slot.getEndsAt(), slot.getPatientId(), slot.getStatus(), slot.getHoldExpiresAt());
        }
    }
}
