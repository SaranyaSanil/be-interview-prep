package com.interviewprep.library;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String author,
        @NotBlank @Size(max = 20) String isbn,
        // Optional; "not in the future" depends on today's date, so it is checked in BookService.
        @Positive Integer publishedYear) {
}
