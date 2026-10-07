package com.interviewprep.booking;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * State changes are single conditional UPDATEs. PostgreSQL locks the row while updating it and
 * re-checks the WHERE clause, so when many requests race for one slot exactly one sees
 * "1 row updated"; the rest see 0 and are rejected. A HELD slot whose hold has expired counts
 * as available, so expiry needs no background job.
 */
public interface SlotRepository extends JpaRepository<Slot, Long> {

    @Query("""
            SELECT s FROM Slot s
            WHERE s.doctor.id = :doctorId
              AND s.startsAt >= :from AND s.startsAt < :to
              AND (s.status = com.interviewprep.booking.SlotStatus.AVAILABLE
                   OR (s.status = com.interviewprep.booking.SlotStatus.HELD AND s.holdExpiresAt <= :now))
            ORDER BY s.startsAt
            """)
    List<Slot> findAvailable(@Param("doctorId") Long doctorId, @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to, @Param("now") Instant now);

    @Query("SELECT s.startsAt FROM Slot s WHERE s.doctor.id = :doctorId AND s.startsAt IN :startTimes")
    List<LocalDateTime> findExistingStartTimes(@Param("doctorId") Long doctorId,
                                               @Param("startTimes") List<LocalDateTime> startTimes);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Slot s
            SET s.status = com.interviewprep.booking.SlotStatus.HELD, s.patientId = :patientId,
                s.holdExpiresAt = :expiresAt
            WHERE s.id = :id
              AND (s.status = com.interviewprep.booking.SlotStatus.AVAILABLE
                   OR (s.status = com.interviewprep.booking.SlotStatus.HELD AND s.holdExpiresAt <= :now))
            """)
    int hold(@Param("id") Long id, @Param("patientId") String patientId,
             @Param("now") Instant now, @Param("expiresAt") Instant expiresAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Slot s
            SET s.status = com.interviewprep.booking.SlotStatus.BOOKED, s.holdExpiresAt = null
            WHERE s.id = :id
              AND s.status = com.interviewprep.booking.SlotStatus.HELD
              AND s.patientId = :patientId
              AND s.holdExpiresAt > :now
            """)
    int confirm(@Param("id") Long id, @Param("patientId") String patientId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Slot s
            SET s.status = com.interviewprep.booking.SlotStatus.AVAILABLE, s.patientId = null
            WHERE s.id = :id
              AND s.status = com.interviewprep.booking.SlotStatus.BOOKED
              AND s.patientId = :patientId
            """)
    int cancel(@Param("id") Long id, @Param("patientId") String patientId);
}
