package com.interviewprep.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.interviewprep.common.BadRequestException;
import com.interviewprep.common.ConflictException;
import com.interviewprep.common.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    @Mock
    private BookRepository bookRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createRejectsPublishedYearInTheFuture() {
        BookRequest request = new BookRequest("Title", "Author", "isbn-1", 2027);

        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
        verify(bookRepository, never()).save(any());
    }

    @Test
    void createAcceptsCurrentYear() {
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book book = bookService.create(new BookRequest(" Title ", "Author", "isbn-1", 2026));

        assertThat(book.getTitle()).isEqualTo("Title");
        assertThat(book.getPublishedYear()).isEqualTo(2026);
    }

    @Test
    void createRejectsDuplicateIsbn() {
        when(bookRepository.existsByIsbn("isbn-1")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(new BookRequest("Title", "Author", "isbn-1", 2000)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void borrowMarksBookAsBorrowedByMember() {
        Book book = new Book("Title", "Author", "isbn-1", 2000);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.borrow(1L, "member-7");

        assertThat(book.isBorrowed()).isTrue();
        assertThat(book.getBorrowedBy()).isEqualTo("member-7");
        assertThat(book.getBorrowedAt()).isEqualTo(NOW);
    }

    @Test
    void borrowRejectsBookThatIsAlreadyBorrowed() {
        Book book = new Book("Title", "Author", "isbn-1", 2000);
        book.borrow("member-1", NOW);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.borrow(1L, "member-2"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already borrowed");
        assertThat(book.getBorrowedBy()).isEqualTo("member-1");
    }

    @Test
    void returnRejectsBookThatIsNotBorrowed() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(new Book("Title", "Author", "isbn-1", 2000)));

        assertThatThrownBy(() -> bookService.giveBack(1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteRejectsBorrowedBook() {
        Book book = new Book("Title", "Author", "isbn-1", 2000);
        book.borrow("member-1", NOW);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> bookService.delete(1L)).isInstanceOf(ConflictException.class);
        verify(bookRepository, never()).delete(any());
    }

    @Test
    void getUnknownBookThrowsNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.get(99L)).isInstanceOf(NotFoundException.class);
    }
}
