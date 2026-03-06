package com.fakedata.gforms;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StructuralDiffToolTest {

    @Test
    void identicalDocumentsHaveNoDifferences() throws IOException {
        String json = """
                {"a": 1, "b": {"c": "hello"}}
                """;
        var result = new StructuralDiffTool().diff(json, json);
        assertFalse(result.hasDifferences());
    }

    @Test
    void detectsFieldOnlyInA() throws IOException {
        String a = """
                {"shared": 1, "onlyInA": "value"}
                """;
        String b = """
                {"shared": 1}
                """;
        var result = new StructuralDiffTool().diff(a, b);
        assertEquals(1, result.onlyInA().size());
        assertEquals("$.onlyInA", result.onlyInA().getFirst().path());
    }

    @Test
    void detectsFieldOnlyInB() throws IOException {
        String a = """
                {"shared": 1}
                """;
        String b = """
                {"shared": 1, "onlyInB": "value"}
                """;
        var result = new StructuralDiffTool().diff(a, b);
        assertEquals(1, result.onlyInB().size());
    }

    @Test
    void detectsTypeMismatch() throws IOException {
        String a = """
                {"field": "string_value"}
                """;
        String b = """
                {"field": ["array", "value"]}
                """;
        var result = new StructuralDiffTool().diff(a, b);
        assertEquals(1, result.typeMismatches().size());
        assertEquals("$.field", result.typeMismatches().getFirst().path());
    }

    @Test
    void detectsNestedDifferences() throws IOException {
        String a = """
                {"outer": {"inner1": "a", "inner2": "b"}}
                """;
        String b = """
                {"outer": {"inner1": "a", "inner3": "c"}}
                """;
        var result = new StructuralDiffTool().diff(a, b);
        assertTrue(result.hasDifferences());
        assertTrue(result.onlyInA().stream().anyMatch(d -> d.path().equals("$.outer.inner2")));
        assertTrue(result.onlyInB().stream().anyMatch(d -> d.path().equals("$.outer.inner3")));
    }

    @Test
    void detectsArraySizeDifferences() throws IOException {
        String a = """
                {"items": [1, 2, 3]}
                """;
        String b = """
                {"items": [1, 2]}
                """;
        var result = new StructuralDiffTool().diff(a, b);
        assertTrue(result.hasDifferences());
    }

    @Test
    void summaryIsReadable() throws IOException {
        String a = """
                {"shared": 1, "onlyA": "x", "typeDiff": "string"}
                """;
        String b = """
                {"shared": 1, "onlyB": "y", "typeDiff": 42}
                """;
        var result = new StructuralDiffTool().diff(a, b);
        String summary = result.summary();
        assertTrue(summary.contains("Only in A"));
        assertTrue(summary.contains("Only in B"));
        assertTrue(summary.contains("Type mismatches"));
    }
}
