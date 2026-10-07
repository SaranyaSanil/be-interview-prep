package com.interviewprep.library;

import com.interviewprep.common.BadRequestException;
import com.interviewprep.common.ConflictException;
import com.interviewprep.common.NotFoundException;
import java.time.Clock;
import java.time.Year;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final Clock clock;

    public BookService(BookRepository bookRepository, Clock clock) {
        this.bookRepository = bookRepository;
        this.clock = clock;
    }

    public List<Book> search(String title, String author) {
        return bookRepository.search(blankToNull(title), blankToNull(author));
    }

    public Book get(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book " + id + " not found"));
    }

    @Transactional
    public Book create(BookRequest request) {
        validatePublishedYear(request.publishedYear());
        String isbn = request.isbn().trim();
        if (bookRepository.existsByIsbn(isbn)) {
            throw new ConflictException("A book with ISBN " + isbn + " already exists");
        }
        return bookRepository.save(
                new Book(request.title().trim(), request.author().trim(), isbn, request.publishedYear()));
    }

    @Transactional
    public Book update(Long id, BookRequest request) {
        Book book = get(id);
        validatePublishedYear(request.publishedYear());
        String isbn = request.isbn().trim();
        if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new ConflictException("A book with ISBN " + isbn + " already exists");
        }
        book.updateDetails(request.title().trim(), request.author().trim(), isbn, request.publishedYear());
        return book;
    }

    @Transactional
    public void delete(Long id) {
        bookRepository.delete(get(id));
    }

    /**
     * Two concurrent borrows can both see the book as available; the @Version check makes
     * the second commit fail with an optimistic-locking error, which is returned as 409.
     */
    @Transactional
    public Book borrow(Long id, String memberId) {
        Book book = get(id);
        if (book.isBorrowed()) {
            throw new ConflictException("Book " + id + " is already borrowed");
        }
        book.borrow(memberId.trim(), clock.instant());
        return book;
    }

    @Transactional
    public Book giveBack(Long id) {
        Book book = get(id);
        if (!book.isBorrowed()) {
            throw new ConflictException("Book " + id + " is not currently borrowed");
        }
        book.giveBack();
        return book;
    }

    private void validatePublishedYear(Integer publishedYear) {
        if (publishedYear != null && publishedYear > Year.now(clock).getValue()) {
            throw new BadRequestException("Published year cannot be in the future");
        }
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
