package com.interviewprep.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BookingApiIntegrationTest.TestClockConfig.class)
class BookingApiIntegrationTest {

    // 08:00 UTC (clinic zone is UTC in tests); slots are created from 09:00 the same day.
    private static final Instant START = Instant.parse("2026-10-08T08:00:00Z");
    private static final String DATE = "2026-10-08";

    @TestConfiguration
    static class TestClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(START);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MutableClock clock;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private long doctorId;
    private long nineOClock;

    @BeforeEach
    void setUp() throws Exception {
        clock.set(START);
        slotRepository.deleteAll();
        doctorRepository.deleteAll();
        doctorId = createDoctor("Dr. Rao");
        List<Long> slots = createSlots(doctorId, "09:00", "10:30");
        nineOClock = slots.get(0);
    }

    @Test
    void twentyPatientsBookingTheSameSlotAtOnceOnlyOneSucceeds() throws Exception {
        int patients = 20;
        ExecutorService pool = Executors.newFixedThreadPool(patients);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        for (int i = 0; i < patients; i++) {
            String patient = "patient-" + i;
            statuses.add(pool.submit(() -> {
                start.await();
                return action(nineOClock, "hold", patient).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();

        List<Integer> results = new ArrayList<>();
        for (Future<Integer> status : statuses) {
            results.add(status.get());
        }
        pool.shutdown();

        assertThat(results).filteredOn(code -> code == 200).hasSize(1);
        assertThat(results).filteredOn(code -> code == 409).hasSize(patients - 1);
        Slot slot = slotRepository.findById(nineOClock).orElseThrow();
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.HELD);
        assertThat(slot.getPatientId()).startsWith("patient-");
    }

    @Test
    void expiredHoldMakesTheSlotAvailableAgain() throws Exception {
        action(nineOClock, "hold", "alice")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.holdExpiresAt").value("2026-10-08T08:05:00Z"));
        availableSlots().andExpect(jsonPath("$", hasSize(2)));

        clock.advance(Duration.ofMinutes(5).minusSeconds(1));
        action(nineOClock, "hold", "bob").andExpect(status().isConflict());

        clock.advance(Duration.ofSeconds(1)); // hold has now expired
        availableSlots()
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(nineOClock));
        action(nineOClock, "hold", "bob").andExpect(status().isOk());
        action(nineOClock, "confirm", "alice").andExpect(status().isConflict());
    }

    @Test
    void confirmingWithinTheHoldBooksTheSlotPermanently() throws Exception {
        action(nineOClock, "hold", "alice").andExpect(status().isOk());
        clock.advance(Duration.ofMinutes(4));

        action(nineOClock, "confirm", "alice")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.patientId").value("alice"))
                .andExpect(jsonPath("$.holdExpiresAt").doesNotExist());

        clock.advance(Duration.ofMinutes(30)); // a confirmed booking never expires
        action(nineOClock, "hold", "bob").andExpect(status().isConflict());
        availableSlots().andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void onlyTheHoldingPatientCanConfirm() throws Exception {
        action(nineOClock, "hold", "alice").andExpect(status().isOk());

        action(nineOClock, "confirm", "bob").andExpect(status().isConflict());
        action(nineOClock, "confirm", "alice").andExpect(status().isOk());
        action(nineOClock, "confirm", "alice").andExpect(status().isConflict()); // already booked
    }

    @Test
    void cancellingAConfirmedBookingFreesTheSlot() throws Exception {
        action(nineOClock, "hold", "alice").andExpect(status().isOk());
        action(nineOClock, "confirm", "alice").andExpect(status().isOk());

        action(nineOClock, "cancel", "bob").andExpect(status().isConflict());
        action(nineOClock, "cancel", "alice")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.patientId").doesNotExist());

        availableSlots().andExpect(jsonPath("$", hasSize(3)));
        action(nineOClock, "hold", "bob").andExpect(status().isOk());
    }

    @Test
    void heldButUnconfirmedSlotCannotBeCancelled() throws Exception {
        action(nineOClock, "hold", "alice").andExpect(status().isOk());

        action(nineOClock, "cancel", "alice").andExpect(status().isConflict());
    }

    @Test
    void slotsThatHaveStartedCannotBeHeldOrListed() throws Exception {
        clock.set(Instant.parse("2026-10-08T09:00:00Z"));

        action(nineOClock, "hold", "alice")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Slot " + nineOClock + " has already started"));
        availableSlots().andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void slotCreationValidatesTimesAndRejectsDuplicates() throws Exception {
        long other = createDoctor("Dr. Mehta");

        slotsRequest(other, "09:15", "10:00").andExpect(status().isBadRequest());
        slotsRequest(other, "10:00", "09:00").andExpect(status().isBadRequest());
        slotsRequest(doctorId, "10:00", "11:00").andExpect(status().isConflict()); // 10:00 already exists
        slotsRequest(999_999, "09:00", "10:00").andExpect(status().isNotFound());
    }

    @Test
    void unknownSlotAndMissingPatientAreRejected() throws Exception {
        action(999_999, "hold", "alice").andExpect(status().isNotFound());
        mockMvc.perform(post("/api/slots/{id}/hold", nineOClock)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.patientId").exists());
    }

    private ResultActions action(long slotId, String action, String patientId) throws Exception {
        return mockMvc.perform(post("/api/slots/{id}/" + action, slotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientId\":\"" + patientId + "\"}"));
    }

    private ResultActions availableSlots() throws Exception {
        return mockMvc.perform(get("/api/doctors/{id}/slots", doctorId).param("date", DATE))
                .andExpect(status().isOk());
    }

    private long createDoctor(String name) throws Exception {
        String body = mockMvc.perform(post("/api/doctors")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions slotsRequest(long doctor, String from, String to) throws Exception {
        return mockMvc.perform(post("/api/doctors/{id}/slots", doctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"" + DATE + "\",\"from\":\"" + from + "\",\"to\":\"" + to + "\"}"));
    }

    private List<Long> createSlots(long doctor, String from, String to) throws Exception {
        String body = slotsRequest(doctor, from, to)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        List<Long> ids = new ArrayList<>();
        for (JsonNode slot : objectMapper.readTree(body)) {
            ids.add(slot.get("id").asLong());
        }
        return ids;
    }
}
