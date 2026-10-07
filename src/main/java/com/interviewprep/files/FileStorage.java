package com.interviewprep.files;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Stores file bytes on the local filesystem. Files are named by a server-generated UUID,
 * so user input never becomes part of a path.
 */
@Component
public class FileStorage {

    private final Path root;

    public FileStorage(@Value("${app.files.storage-location}") Path location) throws IOException {
        this.root = location.toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public void save(UUID id, InputStream content) {
        try {
            Files.copy(content, resolve(id));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file " + id, e);
        }
    }

    public Resource load(UUID id) {
        Path path = resolve(id);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("File content missing for " + id);
        }
        return new PathResource(path);
    }

    public void delete(UUID id) {
        try {
            Files.deleteIfExists(resolve(id));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete file " + id, e);
        }
    }

    // Defence in depth: even though names are UUIDs, never touch anything outside the root.
    Path resolve(UUID id) {
        Path path = root.resolve(id.toString()).normalize();
        if (!path.getParent().equals(root)) {
            throw new IllegalArgumentException("Resolved path is outside the storage location");
        }
        return path;
    }
}
