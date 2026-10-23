package com.musomi.desktop.controller.teacher;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes assessment data in a versioned JSON document.
 */
public class AssessmentRepository {

    public static final int CURRENT_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(AssessmentRepository.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<AssessmentsDocument> DOCUMENT_TYPE = new TypeReference<>() {};

    private final Path filePath;

    public AssessmentRepository() {
        this(Path.of(System.getProperty("user.home"), ".musomi", "assessments.json"));
    }

    public AssessmentRepository(Path filePath) {
        this.filePath = filePath;
    }

    public Optional<AssessmentsDocument> load() {
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        final AssessmentsDocument document;
        try {
            if (Files.readString(filePath).isBlank()) {
                log.warn("Assessment data file is empty: {}", filePath);
                return Optional.empty();
            }
            document = MAPPER.readValue(filePath.toFile(), DOCUMENT_TYPE);
        } catch (IOException ex) {
            log.warn("Could not read assessment data file {}; leaving it unchanged: {}",
                    filePath, ex.getMessage());
            return Optional.empty();
        }

        if (document == null) {
            log.warn("Assessment data file contains no document: {}", filePath);
            return Optional.empty();
        }
        if (document.version() != CURRENT_VERSION) {
            log.warn("Unsupported assessment data version {} in {}; expected {}; leaving file unchanged",
                    document.version(), filePath, CURRENT_VERSION);
            return Optional.empty();
        }
        if (document.assessments() == null) {
            log.warn("Assessment data file has no assessments list: {}", filePath);
            return Optional.empty();
        }

        log.info("Loaded {} assessments from {}", document.assessments().size(), filePath);
        return Optional.of(document);
    }

    public void save(AssessmentsDocument document) throws IOException {
        Path parent = filePath.getParent();
        Path temporaryPath = filePath.resolveSibling(filePath.getFileName() + ".tmp");

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(temporaryPath.toFile(), document);
            try {
                Files.move(temporaryPath, filePath,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryPath, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Saved {} assessments to {}", document.assessments().size(), filePath);
        } catch (IOException ex) {
            log.error("Failed to save assessment data to {}", filePath, ex);
            throw ex;
        }
    }
}
