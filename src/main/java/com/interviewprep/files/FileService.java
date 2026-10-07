package com.interviewprep.files;

import com.interviewprep.common.BadRequestException;
import com.interviewprep.common.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class FileService {

    private static final int MAX_NAME_LENGTH = 255;

    private final StoredFileRepository repository;
    private final FileStorage storage;
    private final Clock clock;
    private final DataSize maxFileSize;

    public FileService(StoredFileRepository repository, FileStorage storage, Clock clock,
                       @Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize) {
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
        this.maxFileSize = maxFileSize;
    }

    public List<StoredFile> list() {
        return repository.findAllByOrderByUploadedAtDesc();
    }

    public StoredFile get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("File " + id + " not found"));
    }

    public Resource content(UUID id) {
        get(id);
        return storage.load(id);
    }

    @Transactional
    public StoredFile upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        // Spring's multipart limit normally rejects this first; re-checked here so the rule
        // holds even if that limit is misconfigured.
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new FileRejectedException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File exceeds the maximum size of " + maxFileSize.toMegabytes() + " MB");
        }
        FileType type = FileType.detect(readHeader(file)).orElseThrow(() -> new FileRejectedException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG and PDF files are allowed"));

        UUID id = UUID.randomUUID();
        try (InputStream content = file.getInputStream()) {
            storage.save(id, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read upload", e);
        }
        try {
            return repository.saveAndFlush(new StoredFile(
                    id, safeOriginalName(file.getOriginalFilename()), type.mediaType(), file.getSize(),
                    clock.instant()));
        } catch (RuntimeException e) {
            storage.delete(id); // don't leave an orphaned file without a record
            throw e;
        }
    }

    /**
     * The record is deleted and flushed first; if removing the bytes then fails, the exception
     * rolls the record back, so a record never points at a missing file.
     */
    @Transactional
    public void delete(UUID id) {
        repository.delete(get(id));
        repository.flush();
        storage.delete(id);
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(FileType.HEADER_LENGTH);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read upload", e);
        }
    }

    /**
     * Keeps only the last path segment ("../../etc/passwd.png" -> "passwd.png") and drops
     * control characters. The name is only ever stored and echoed in the download header,
     * never used to build a path.
     */
    static String safeOriginalName(String originalName) {
        if (originalName == null) {
            return "file";
        }
        String name = originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").strip();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            return "file";
        }
        return name.length() > MAX_NAME_LENGTH ? name.substring(name.length() - MAX_NAME_LENGTH) : name;
    }
}
