package com.interviewprep.booking;

import com.interviewprep.common.BadRequestException;
import com.interviewprep.common.ConflictException;
import com.interviewprep.common.NotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Doctors and their slots: creating them and listing what is still available. */
@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final DoctorRepository doctorRepository;
    private final SlotRepository slotRepository;
    private final Clock clock;
    private final ZoneId clinicZone;

    public ScheduleService(DoctorRepository doctorRepository, SlotRepository slotRepository, Clock clock,
                           BookingProperties properties) {
        this.doctorRepository = doctorRepository;
        this.slotRepository = slotRepository;
        this.clock = clock;
        this.clinicZone = properties.zone();
    }

    @Transactional
    public Doctor createDoctor(String name) {
        return doctorRepository.save(new Doctor(name.trim()));
    }

    public Doctor getDoctor(Long id) {
        return doctorRepository.findById(id).orElseThrow(() -> new NotFoundException("Doctor " + id + " not found"));
    }

    @Transactional
    public List<Slot> createSlots(Long doctorId, LocalDate date, LocalTime from, LocalTime to) {
        Doctor doctor = getDoctor(doctorId);
        if (!isOnHalfHour(from) || !isOnHalfHour(to)) {
            throw new BadRequestException("Slot times must start on the hour or half hour");
        }
        if (!from.isBefore(to)) {
            throw new BadRequestException("'from' must be before 'to'");
        }

        List<LocalDateTime> startTimes = new ArrayList<>();
        for (LocalTime start = from; start.isBefore(to); start = start.plus(Slot.LENGTH)) {
            startTimes.add(date.atTime(start));
        }
        if (!slotRepository.findExistingStartTimes(doctorId, startTimes).isEmpty()) {
            throw new ConflictException("Some of these slots already exist for doctor " + doctorId);
        }
        // The unique (doctor, start) constraint still guards against two concurrent requests.
        return slotRepository.saveAll(startTimes.stream().map(start -> new Slot(doctor, start)).toList());
    }

    /** Free slots (or slots whose hold has expired) on that day that haven't started yet. */
    public List<Slot> availableSlots(Long doctorId, LocalDate date) {
        getDoctor(doctorId);
        LocalDateTime nowAtClinic = LocalDateTime.ofInstant(clock.instant(), clinicZone);
        return slotRepository.findAvailable(
                        doctorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay(), clock.instant())
                .stream()
                .filter(slot -> slot.getStartsAt().isAfter(nowAtClinic))
                .toList();
    }

    private static boolean isOnHalfHour(LocalTime time) {
        return time.getMinute() % 30 == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }
}
