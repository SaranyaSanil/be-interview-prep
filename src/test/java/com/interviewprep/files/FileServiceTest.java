package com.interviewprep.files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private StoredFileRepository repository;

    @Mock
    private FileStorage storage;

    @Test
    void storedBytesAreRemovedWhenSavingTheRecordFails() {
        FileService service = new FileService(repository, storage, Clock.systemUTC(), DataSize.ofMegabytes(5));
        when(repository.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "a.png", "image/png", TestFiles.PNG)))
                .isInstanceOf(DataAccessResourceFailureException.class);

        ArgumentCaptor<UUID> saved = ArgumentCaptor.forClass(UUID.class);
        verify(storage).save(saved.capture(), any());
        verify(storage).delete(eq(saved.getValue()));
    }

    @Test
    void sizeLimitIsReportedInItsOwnUnit() {
        FileService service = new FileService(repository, storage, Clock.systemUTC(), DataSize.ofKilobytes(500));
        byte[] tooBig = TestFiles.ofSize(TestFiles.PNG, 500 * 1024 + 1);

        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "a.png", "image/png", tooBig)))
                .isInstanceOf(FileRejectedException.class)
                .hasMessageContaining("500 KB");
    }

    @Test
    void partiallyWrittenFileIsDeletedWhenTheWriteFails(@TempDir Path dir) throws IOException {
        FileStorage realStorage = new FileStorage(dir);
        InputStream failsHalfway = new InputStream() {
            private final InputStream data = new ByteArrayInputStream(new byte[1024]);

            @Override
            public int read() throws IOException {
                int next = data.read();
                if (next == -1) {
                    throw new IOException("client disconnected");
                }
                return next;
            }
        };

        assertThatThrownBy(() -> realStorage.save(UUID.randomUUID(), failsHalfway))
                .isInstanceOf(UncheckedIOException.class);

        try (var files = Files.list(dir)) {
            assertThat(files).isEmpty();
        }
    }
}
