package com.fakedata.gforms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GFormFieldExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void extractFindsFieldsAndUpdatesRegistry() throws IOException {
        Path registryDir = tempDir.resolve("registry");
        var extractor = GFormFieldExtractor.withDefaults(registryDir);

        String json = """
                {
                  "metaData": {"submission-reference": "ABCD-1234-EFGH"},
                  "gform": {
                    "uploads": [
                      {"filename": "report.pdf"}
                    ]
                  }
                }
                """;

        var result = extractor.extract("test-form", json);

        assertTrue(result.allMatches().size() > 0);
        assertTrue(result.hasNewPaths(), "First run should discover new paths");

        // Registry should be created
        var registry = new FieldPathRegistry(registryDir);
        var loaded = registry.load("test-form");
        assertTrue(loaded.isPresent());
        assertFalse(loaded.get().knownPaths.isEmpty());
    }

    @Test
    void secondRunUsesRegistryFastPath() throws IOException {
        Path registryDir = tempDir.resolve("registry");
        var extractor = GFormFieldExtractor.withDefaults(registryDir);

        String json = """
                {
                  "metaData": {"submission-reference": "ABCD-1234-EFGH"},
                  "data": {"email": "user@example.com"}
                }
                """;

        // First run — populates registry
        extractor.extract("test-form", json);

        // Second run — should use fast path
        var result = extractor.extract("test-form", json);
        assertFalse(result.fastPathMatches().isEmpty(), "Second run should have fast-path matches");
        assertFalse(result.hasNewPaths(), "No new paths on second run with same structure");
    }

    @Test
    void firstValueConvenience() throws IOException {
        Path registryDir = tempDir.resolve("registry");
        var extractor = GFormFieldExtractor.withDefaults(registryDir);

        String json = """
                {"metaData": {"submission-reference": "WXYZ-9876-ABCD"}}
                """;

        var result = extractor.extract("test-form", json);
        var ref = result.firstValue("submission_reference");
        assertTrue(ref.isPresent());
        assertEquals("WXYZ-9876-ABCD", ref.get());
    }

    @Test
    void resolveJsonPathWorks() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "a": {"b": [{"c": "found"}]},
                  "simple": "value"
                }
                """);

        assertEquals("value", GFormFieldExtractor.resolveJsonPath(root, "$.simple").asText());
        assertEquals("found", GFormFieldExtractor.resolveJsonPath(root, "$.a.b[0].c").asText());
        assertNull(GFormFieldExtractor.resolveJsonPath(root, "$.nonexistent"));
        assertNull(GFormFieldExtractor.resolveJsonPath(root, "$.a.b[5].c"));
    }
}
