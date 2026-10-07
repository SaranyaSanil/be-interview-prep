package com.interviewprep.files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileApiIntegrationTest {

    private static final int FIVE_MB = 5 * 1024 * 1024;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoredFileRepository repository;

    @Value("${app.files.storage-location}")
    private Path storageDir;

    @BeforeEach
    void clean() throws IOException {
        repository.deleteAll();
        try (Stream<Path> files = Files.list(storageDir)) {
            for (Path file : files.toList()) {
                Files.delete(file);
            }
        }
    }

    @Test
    void uploadedPngCanBeListedAndDownloadedWithOriginalName() throws Exception {
        String id = uploadId("holiday photo.png", "image/png", TestFiles.PNG);

        mockMvc.perform(get("/api/files"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].originalName").value("holiday photo.png"))
                .andExpect(jsonPath("$[0].contentType").value("image/png"))
                .andExpect(jsonPath("$[0].size").value(TestFiles.PNG.length))
                .andExpect(jsonPath("$[0].uploadedAt").exists());

        mockMvc.perform(get("/api/files/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(TestFiles.PNG))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"holiday photo.png\""))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void nonAsciiNameIsEncodedInDownloadHeader() throws Exception {
        String id = uploadId("résumé.pdf", "application/pdf", TestFiles.PDF);

        mockMvc.perform(get("/api/files/{id}/content", id))
                .andExpect(header().string("Content-Disposition", containsString(
                        "filename*=UTF-8''r%C3%A9sum%C3%A9.pdf")));
    }

    @Test
    void jpegAndPdfAreAccepted() throws Exception {
        upload("scan.jpg", "image/jpeg", TestFiles.JPEG).andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
        upload("doc.pdf", "application/pdf", TestFiles.PDF).andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("application/pdf"));
    }

    @Test
    void executableRenamedToPngIsRejected() throws Exception {
        upload("photo.png", "image/png", TestFiles.EXE)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.detail").value("Only JPEG, PNG and PDF files are allowed"));

        assertThat(repository.count()).isZero();
        assertThat(storedFileCount()).isZero();
    }

    @Test
    void validContentWithAMismatchedExtensionIsRejected() throws Exception {
        // PDF bytes that would be saved by the browser as an executable script or web page.
        upload("run.bat", "application/pdf", TestFiles.PDF)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.detail").value("File extension does not match its content (application/pdf)"));
        upload("invoice.html", "application/pdf", TestFiles.PDF).andExpect(status().isUnsupportedMediaType());
        upload("photo.pdf", "application/pdf", TestFiles.PNG).andExpect(status().isUnsupportedMediaType());
        upload("noextension", "image/png", TestFiles.PNG).andExpect(status().isUnsupportedMediaType());

        assertThat(repository.count()).isZero();
        assertThat(storedFileCount()).isZero();
    }

    @Test
    void extensionCheckIsCaseInsensitive() throws Exception {
        upload("PHOTO.JPEG", "image/jpeg", TestFiles.JPEG).andExpect(status().isCreated());
    }

    @Test
    void rightToLeftOverrideCannotDisguiseTheExtension() throws Exception {
        // "invoice<RLO>fdp.bat" displays as "invoicetab.pdf" but really ends in ".bat".
        upload("invoice‮fdp.bat", "application/pdf", TestFiles.PDF)
                .andExpect(status().isUnsupportedMediaType());
    }

    // MockMvc bypasses Spring's multipart size limit, so this exercises FileService's own check;
    // the multipart limit itself (also 413) was verified against the running server.
    @Test
    void fileOverFiveMegabytesIsRejected() throws Exception {
        upload("big.png", "image/png", TestFiles.ofSize(TestFiles.PNG, FIVE_MB + 1))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.detail").value("File exceeds the maximum size of 5 MB"));

        assertThat(storedFileCount()).isZero();
    }

    @Test
    void fileOfExactlyFiveMegabytesIsAccepted() throws Exception {
        upload("max.png", "image/png", TestFiles.ofSize(TestFiles.PNG, FIVE_MB)).andExpect(status().isCreated());
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        upload("empty.png", "image/png", new byte[0]).andExpect(status().isBadRequest());
    }

    @Test
    void traversalInFileNameNeverEscapesStorage() throws Exception {
        String id = uploadId("../../evil.png", "image/png", TestFiles.PNG);

        mockMvc.perform(get("/api/files/{id}", id))
                .andExpect(jsonPath("$.originalName").value("evil.png"));
        // The only file written is inside the storage directory, named by the id.
        assertThat(Files.exists(storageDir.resolve(id))).isTrue();
        assertThat(storedFileCount()).isEqualTo(1);
        assertThat(Files.exists(storageDir.resolve("../../evil.png").normalize())).isFalse();
    }

    @Test
    void deleteRemovesRecordAndStoredBytes() throws Exception {
        String id = uploadId("doc.pdf", "application/pdf", TestFiles.PDF);
        assertThat(Files.exists(storageDir.resolve(id))).isTrue();

        mockMvc.perform(delete("/api/files/{id}", id)).andExpect(status().isNoContent());

        assertThat(repository.existsById(UUID.fromString(id))).isFalse();
        assertThat(Files.exists(storageDir.resolve(id))).isFalse();
        mockMvc.perform(get("/api/files/{id}/content", id)).andExpect(status().isNotFound());
    }

    @Test
    void unknownOrMalformedIdIsHandled() throws Exception {
        mockMvc.perform(get("/api/files/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/files/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/files/{id}", "not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void missingFilePartIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/files"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private ResultActions upload(String name, String contentType, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/files").file(new MockMultipartFile("file", name, contentType, bytes)));
    }

    private String uploadId(String name, String contentType, byte[] bytes) throws Exception {
        String body = upload(name, contentType, bytes)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private long storedFileCount() throws IOException {
        try (Stream<Path> files = Files.list(storageDir)) {
            return files.count();
        }
    }
}
