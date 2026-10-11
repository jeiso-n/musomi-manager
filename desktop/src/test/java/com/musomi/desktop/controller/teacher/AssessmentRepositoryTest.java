package com.musomi.desktop.controller.teacher;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssessmentRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldReturnEmptyWhenFileDoesNotExist() {
        AssessmentRepository repository = new AssessmentRepository(temporaryDirectory.resolve("assessments.json"));

        assertTrue(repository.load().isEmpty());
    }

    @Test
    void shouldRoundTripSingleAssessmentWithMarks() throws IOException {
        Path filePath = temporaryDirectory.resolve("assessments.json");
        AssessmentRepository repository = new AssessmentRepository(filePath);
        AssessmentsDocument document = new AssessmentsDocument(1, List.of(
                new AssessmentDto("Math Test", "S3 Blue", "Mathematics", "Algebra",
                        "15 Sep 2026", 40, "Draft", List.of(
                        new StudentMarkDto("1", "John Mugisha", "35"),
                        new StudentMarkDto("2", "Sarah Namukasa", "")
                ))
        ));

        repository.save(document);

        assertEquals(Optional.of(document), repository.load());
    }

    @Test
    void shouldRoundTripEmptyAssessmentList() throws IOException {
        Path filePath = temporaryDirectory.resolve("assessments.json");
        AssessmentRepository repository = new AssessmentRepository(filePath);

        repository.save(new AssessmentsDocument(1, List.of()));

        AssessmentsDocument loaded = repository.load().orElseThrow();
        assertTrue(loaded.assessments().isEmpty());
    }

    @Test
    void shouldReturnEmptyForCorruptJsonAndPreserveFile() throws IOException {
        Path filePath = temporaryDirectory.resolve("assessments.json");
        byte[] contents = "not json at all".getBytes(StandardCharsets.UTF_8);
        Files.write(filePath, contents);
        AssessmentRepository repository = new AssessmentRepository(filePath);

        assertTrue(repository.load().isEmpty());
        assertArrayEquals(contents, Files.readAllBytes(filePath));
    }

    @Test
    void shouldReturnEmptyForEmptyFileAndPreserveFile() throws IOException {
        Path filePath = temporaryDirectory.resolve("assessments.json");
        Files.createFile(filePath);
        AssessmentRepository repository = new AssessmentRepository(filePath);

        assertTrue(repository.load().isEmpty());
        assertTrue(Files.exists(filePath));
        assertEquals(0, Files.size(filePath));
    }

    @Test
    void shouldReturnEmptyForVersionMismatchAndPreserveFile() throws IOException {
        Path filePath = temporaryDirectory.resolve("assessments.json");
        AssessmentRepository repository = new AssessmentRepository(filePath);
        repository.save(new AssessmentsDocument(999, List.of()));
        byte[] contents = Files.readAllBytes(filePath);

        assertTrue(repository.load().isEmpty());
        assertArrayEquals(contents, Files.readAllBytes(filePath));
    }

    @Test
    void shouldReturnEmptyForNullAssessmentsList() throws IOException {
        Path filePath = temporaryDirectory.resolve("assessments.json");
        Files.writeString(filePath, "{\"version\":1,\"assessments\":null}");
        AssessmentRepository repository = new AssessmentRepository(filePath);

        assertTrue(repository.load().isEmpty());
    }

    @Test
    void shouldCreateParentDirectoryOnSave() throws IOException {
        Path filePath = temporaryDirectory.resolve("nested").resolve("path").resolve("assessments.json");
        AssessmentRepository repository = new AssessmentRepository(filePath);

        repository.save(new AssessmentsDocument(1, List.of()));

        assertTrue(Files.exists(filePath));
    }

    @Test
    void shouldNotLeaveTempFileAfterSuccessfulSave() throws IOException {
        Path filePath = temporaryDirectory.resolve("assessments.json");
        AssessmentRepository repository = new AssessmentRepository(filePath);

        repository.save(new AssessmentsDocument(1, List.of()));

        assertFalse(Files.exists(filePath.resolveSibling("assessments.json.tmp")));
    }
}
