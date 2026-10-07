package com.interviewprep.files;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Allowed file types, recognised by their leading "magic bytes" rather than by the
 * file name or the client's Content-Type, both of which the client controls.
 */
public enum FileType {

    JPEG("image/jpeg", Set.of("jpg", "jpeg"), new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", Set.of("png"), new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}),
    PDF("application/pdf", Set.of("pdf"), new byte[] {'%', 'P', 'D', 'F', '-'});

    /** Enough bytes to check the longest signature. */
    static final int HEADER_LENGTH = 8;

    private final String mediaType;
    private final Set<String> extensions;
    private final byte[] signature;

    FileType(String mediaType, Set<String> extensions, byte[] signature) {
        this.mediaType = mediaType;
        this.extensions = extensions;
        this.signature = signature;
    }

    public String mediaType() {
        return mediaType;
    }

    /** True if the file name ends with an extension for this type (case-insensitive). */
    boolean matchesExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && extensions.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    static Optional<FileType> detect(byte[] header) {
        return Arrays.stream(values()).filter(type -> type.matches(header)).findFirst();
    }

    private boolean matches(byte[] header) {
        return header.length >= signature.length
                && Arrays.equals(header, 0, signature.length, signature, 0, signature.length);
    }
}
