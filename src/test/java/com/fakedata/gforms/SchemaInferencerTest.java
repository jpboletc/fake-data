package com.fakedata.gforms;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaInferencerTest {

    @Test
    void infersSchemaFromMultipleSubmissions() throws IOException {
        List<String> submissions = List.of(
                """
                {"ref": "ABC-1234", "name": "Alice", "uploads": [{"file": "a.pdf"}]}
                """,
                """
                {"ref": "DEF-5678", "name": "Bob", "uploads": [{"file": "b.pdf"}, {"file": "c.pdf"}]}
                """,
                """
                {"ref": "GHI-9012", "name": "Charlie"}
                """
        );

        var inferencer = new SchemaInferencer();
        var schema = inferencer.infer(submissions);

        assertEquals(3, schema.submissionCount());

        // "uploads" should be optional (only in 2 of 3)
        List<SchemaInferencer.FieldInfo> optional = schema.optionalFields();
        assertTrue(optional.stream().anyMatch(f -> f.path().contains("uploads")));
    }

    @Test
    void detectsTypeVariation() throws IOException {
        List<String> submissions = List.of(
                """
                {"field": "string_value"}
                """,
                """
                {"field": 42}
                """
        );

        var schema = new SchemaInferencer().infer(submissions);
        List<SchemaInferencer.FieldInfo> typeVars = schema.fieldsWithTypeVariation();

        assertTrue(typeVars.stream().anyMatch(f -> f.path().contains("field")));
    }

    @Test
    void summaryIncludesStats() throws IOException {
        List<String> submissions = List.of(
                """
                {"a": 1, "b": 2}
                """,
                """
                {"a": 1, "c": 3}
                """
        );

        var schema = new SchemaInferencer().infer(submissions);
        String summary = schema.summary();

        assertTrue(summary.contains("2 submissions"));
        assertTrue(summary.contains("Optional fields"));
    }
}
