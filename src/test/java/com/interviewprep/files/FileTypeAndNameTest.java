package com.interviewprep.files;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FileTypeAndNameTest {

    @Test
    void detectsAllowedTypesBySignature() {
        assertThat(FileType.detect(TestFiles.JPEG)).contains(FileType.JPEG);
        assertThat(FileType.detect(TestFiles.PNG)).contains(FileType.PNG);
        assertThat(FileType.detect(TestFiles.PDF)).contains(FileType.PDF);
    }

    @Test
    void rejectsExecutableAndTooShortContent() {
        assertThat(FileType.detect(TestFiles.EXE)).isEmpty();
        assertThat(FileType.detect(new byte[] {(byte) 0xFF, (byte) 0xD8})).isEmpty();
        assertThat(FileType.detect("hello".getBytes(StandardCharsets.US_ASCII))).isEmpty();
    }

    @Test
    void originalNameKeepsOnlyTheLastPathSegment() {
        assertThat(FileService.safeOriginalName("../../etc/passwd.png")).isEqualTo("passwd.png");
        assertThat(FileService.safeOriginalName("..\\..\\windows\\evil.pdf")).isEqualTo("evil.pdf");
        assertThat(FileService.safeOriginalName("/abs/path/photo.jpg")).isEqualTo("photo.jpg");
    }

    @Test
    void unusableNamesFallBackToDefault() {
        assertThat(FileService.safeOriginalName(null)).isEqualTo("file");
        assertThat(FileService.safeOriginalName("..")).isEqualTo("file");
        assertThat(FileService.safeOriginalName("dir/")).isEqualTo("file");
        assertThat(FileService.safeOriginalName("bad\r\nname.png")).isEqualTo("badname.png");
        assertThat(FileService.safeOriginalName("invoice‮fdp.png")).isEqualTo("invoicefdp.png");
    }
}
