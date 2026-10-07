package com.interviewprep.files;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Minimal byte payloads that start with each format's signature. */
final class TestFiles {

    static final byte[] JPEG = withBody(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
    static final byte[] PNG = withBody(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
    static final byte[] PDF = "%PDF-1.7\n%test document\n".getBytes(StandardCharsets.US_ASCII);
    // Windows executables start with "MZ".
    static final byte[] EXE = withBody(new byte[] {'M', 'Z', (byte) 0x90, 0});

    private TestFiles() {
    }

    static byte[] ofSize(byte[] signature, int size) {
        byte[] content = Arrays.copyOf(signature, size);
        Arrays.fill(content, signature.length, size, (byte) 'x');
        return content;
    }

    private static byte[] withBody(byte[] signature) {
        return ofSize(signature, signature.length + 32);
    }
}
