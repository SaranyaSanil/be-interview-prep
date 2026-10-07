package com.interviewprep.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A 30-minute slot for one doctor. Holding, confirming and cancelling change it only through
 * conditional UPDATEs in {@link SlotRepository}, never by loading and saving the entity.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "starts_at"}))
public class Slot {

    public static final Duration LENGTH = Duration.ofMinutes(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    // Clinic-local wall-clock time.
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotStatus status;

    private String patientId;

    private Instant holdExpiresAt;

    protected Slot() {
    }

    public Slot(Doctor doctor, LocalDateTime startsAt) {
        this.doctor = doctor;
        this.startsAt = startsAt;
        this.status = SlotStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return startsAt.plus(LENGTH);
    }

    public SlotStatus getStatus() {
        return status;
    }

    public String getPatientId() {
        return patientId;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }
}
