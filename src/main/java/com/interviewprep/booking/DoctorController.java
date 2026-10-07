package com.interviewprep.booking;

import com.interviewprep.booking.BookingDtos.CreateDoctorRequest;
import com.interviewprep.booking.BookingDtos.CreateSlotsRequest;
import com.interviewprep.booking.BookingDtos.DoctorResponse;
import com.interviewprep.booking.BookingDtos.SlotResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final ScheduleService scheduleService;

    public DoctorController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<DoctorResponse> create(@Valid @RequestBody CreateDoctorRequest request) {
        Doctor doctor = scheduleService.createDoctor(request.name());
        return ResponseEntity.created(URI.create("/api/doctors/" + doctor.getId())).body(DoctorResponse.from(doctor));
    }

    @PostMapping("/{id}/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SlotResponse> createSlots(@PathVariable Long id, @Valid @RequestBody CreateSlotsRequest request) {
        return scheduleService.createSlots(id, request.date(), request.from(), request.to()).stream()
                .map(SlotResponse::from)
                .toList();
    }

    @GetMapping("/{id}/slots")
    public List<SlotResponse> availableSlots(
            @PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.availableSlots(id, date).stream().map(SlotResponse::from).toList();
    }
}
