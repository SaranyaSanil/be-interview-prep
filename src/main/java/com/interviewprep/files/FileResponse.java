package com.interviewprep.files;

import java.time.Instant;
import java.util.UUID;

public record FileResponse(UUID id, String originalName, String contentType, long size, Instant uploadedAt) {

    static FileResponse from(StoredFile file) {
        return new FileResponse(
                file.getId(), file.getOriginalName(), file.getContentType(), file.getSize(), file.getUploadedAt());
    }
}
