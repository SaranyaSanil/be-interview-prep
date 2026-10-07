package com.interviewprep.booking;

import com.interviewprep.booking.BookingDtos.BookingResponse;
import com.interviewprep.booking.BookingDtos.PatientRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final BookingService bookingService;

    public SlotController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/{id}/hold")
    public BookingResponse hold(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return BookingResponse.from(bookingService.hold(id, request.patientId().trim()));
    }

    @PostMapping("/{id}/confirm")
    public BookingResponse confirm(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return BookingResponse.from(bookingService.confirm(id, request.patientId().trim()));
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return BookingResponse.from(bookingService.cancel(id, request.patientId().trim()));
    }
}
