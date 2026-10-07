package com.interviewprep.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookService bookService;

    @BeforeEach
    void cleanDatabase() {
        bookRepository.deleteAll();
    }

    @Test
    void borrowingAnUnavailableBookReturnsConflictWithErrorBody() throws Exception {
        long id = createBook("Dune", "Frank Herbert", "978-0441013593");

        borrow(id, "alice")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.borrowedBy").value("alice"));

        borrow(id, "bob")
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Book " + id + " is already borrowed"));
    }

    @Test
    void returnedBookCanBeBorrowedAgain() throws Exception {
        long id = createBook("Dune", "Frank Herbert", "978-0441013593");
        borrow(id, "alice").andExpect(status().isOk());

        mockMvc.perform(post("/api/books/{id}/return", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        borrow(id, "bob").andExpect(status().isOk());
    }

    @Test
    void deletingABorrowedBookIsRejected() throws Exception {
        long id = createBook("Dune", "Frank Herbert", "978-0441013593");
        borrow(id, "alice").andExpect(status().isOk());

        mockMvc.perform(delete("/api/books/{id}", id)).andExpect(status().isConflict());

        assertThat(bookRepository.existsById(id)).isTrue();
    }

    @Test
    void invalidBookReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"isbn\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.author").exists());
    }

    @Test
    void malformedJsonReturnsBadRequestInSameFormat() throws Exception {
        mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
    }

    @Test
    void duplicateIsbnIsRejected() throws Exception {
        createBook("Dune", "Frank Herbert", "978-0441013593");

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("Other", "Someone", "978-0441013593")))
                .andExpect(status().isConflict());
    }

    @Test
    void searchMatchesTitleOrAuthorCaseInsensitively() throws Exception {
        createBook("Dune", "Frank Herbert", "isbn-1");
        createBook("Emma", "Jane Austen", "isbn-2");

        mockMvc.perform(get("/api/books").param("title", "dUn"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Dune"));
        mockMvc.perform(get("/api/books").param("author", "austen"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Emma"));
        mockMvc.perform(get("/api/books"))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void unknownBookReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/books/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void concurrentBorrowsOfTheSameBookOnlyOneSucceeds() throws Exception {
        long id = createBook("Dune", "Frank Herbert", "978-0441013593");
        int attempts = 10;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            String member = "member-" + i;
            results.add(pool.submit(() -> {
                start.await();
                try {
                    bookService.borrow(id, member);
                    return true;
                } catch (RuntimeException rejected) {
                    return false;
                }
            }));
        }
        start.countDown();

        int successes = 0;
        for (Future<Boolean> result : results) {
            successes += result.get() ? 1 : 0;
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
    }

    private long createBook(String title, String author, String isbn) throws Exception {
        String body = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson(title, author, isbn)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private ResultActions borrow(long id, String memberId) throws Exception {
        return mockMvc.perform(post("/api/books/{id}/borrow", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"" + memberId + "\"}"));
    }

    private static String bookJson(String title, String author, String isbn) {
        return "{\"title\":\"" + title + "\",\"author\":\"" + author
                + "\",\"isbn\":\"" + isbn + "\",\"publishedYear\":1965}";
    }
}
