package com.fakedata.gforms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * A4: Structural comparison of two or more JSON submissions of the same form type.
 *
 * Highlights keys present in one but not the other, type differences,
 * and structural depth differences.
 */
public class StructuralDiffTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Compare two JSON strings and return structural differences.
     */
    public DiffResult diff(String jsonA, String jsonB) throws IOException {
        JsonNode a = MAPPER.readTree(jsonA);
        JsonNode b = MAPPER.readTree(jsonB);
        return diff(a, b);
    }

    /**
     * Compare two JSON files.
     */
    public DiffResult diff(Path fileA, Path fileB) throws IOException {
        JsonNode a = MAPPER.readTree(fileA.toFile());
        JsonNode b = MAPPER.readTree(fileB.toFile());
        return diff(a, b);
    }

    /**
     * Compare two parsed JSON trees.
     */
    public DiffResult diff(JsonNode a, JsonNode b) {
        List<Difference> diffs = new ArrayList<>();
        compare(a, b, "$", diffs);
        return new DiffResult(diffs);
    }

    private void compare(JsonNode a, JsonNode b, String path, List<Difference> diffs) {
        if (a == null && b == null) return;

        if (a == null) {
            diffs.add(new Difference(path, DiffType.ONLY_IN_B, null, describeTypeDetailed(b)));
            return;
        }
        if (b == null) {
            diffs.add(new Difference(path, DiffType.ONLY_IN_A, describeTypeDetailed(a), null));
            return;
        }

        String typeA = describeType(a);
        String typeB = describeType(b);

        if (!typeA.equals(typeB)) {
            diffs.add(new Difference(path, DiffType.TYPE_MISMATCH, describeTypeDetailed(a), describeTypeDetailed(b)));
            return;
        }

        if (a.isObject() && b.isObject()) {
            Set<String> allKeys = new TreeSet<>();
            a.fieldNames().forEachRemaining(allKeys::add);
            b.fieldNames().forEachRemaining(allKeys::add);

            for (String key : allKeys) {
                String childPath = path + "." + key;
                JsonNode childA = a.get(key);
                JsonNode childB = b.get(key);
                compare(childA, childB, childPath, diffs);
            }
        } else if (a.isArray() && b.isArray()) {
            if (a.size() != b.size()) {
                diffs.add(new Difference(path, DiffType.ARRAY_SIZE_DIFF,
                        String.valueOf(a.size()), String.valueOf(b.size())));
            }
            int maxLen = Math.max(a.size(), b.size());
            for (int i = 0; i < maxLen; i++) {
                String childPath = path + "[" + i + "]";
                JsonNode elemA = i < a.size() ? a.get(i) : null;
                JsonNode elemB = i < b.size() ? b.get(i) : null;
                compare(elemA, elemB, childPath, diffs);
            }
        }
        // Leaf value differences are not tracked — we care about structure, not values
    }

    private String describeType(JsonNode node) {
        if (node.isObject()) return "object";
        if (node.isArray()) return "array";
        if (node.isTextual()) return "string";
        if (node.isNumber()) return "number";
        if (node.isBoolean()) return "boolean";
        if (node.isNull()) return "null";
        return "unknown";
    }

    private String describeTypeDetailed(JsonNode node) {
        if (node.isObject()) return "object(" + node.size() + " keys)";
        if (node.isArray()) return "array(" + node.size() + " elements)";
        return describeType(node);
    }

    // --- Data model ---

    public enum DiffType {
        ONLY_IN_A,       // Key exists only in first document
        ONLY_IN_B,       // Key exists only in second document
        TYPE_MISMATCH,   // Same path has different types
        ARRAY_SIZE_DIFF  // Arrays at same path have different lengths
    }

    public record Difference(String path, DiffType type, String detailA, String detailB) {
        @Override
        public String toString() {
            return switch (type) {
                case ONLY_IN_A -> "%s: only in A (%s)".formatted(path, detailA);
                case ONLY_IN_B -> "%s: only in B (%s)".formatted(path, detailB);
                case TYPE_MISMATCH -> "%s: type mismatch (A=%s, B=%s)".formatted(path, detailA, detailB);
                case ARRAY_SIZE_DIFF -> "%s: array size differs (A=%s, B=%s)".formatted(path, detailA, detailB);
            };
        }
    }

    public record DiffResult(List<Difference> differences) {

        public boolean hasDifferences() {
            return !differences.isEmpty();
        }

        public List<Difference> onlyInA() {
            return differences.stream().filter(d -> d.type() == DiffType.ONLY_IN_A).toList();
        }

        public List<Difference> onlyInB() {
            return differences.stream().filter(d -> d.type() == DiffType.ONLY_IN_B).toList();
        }

        public List<Difference> typeMismatches() {
            return differences.stream().filter(d -> d.type() == DiffType.TYPE_MISMATCH).toList();
        }

        public String summary() {
            if (!hasDifferences()) return "No structural differences found.";

            StringBuilder sb = new StringBuilder();
            sb.append("Structural differences: %d total\n".formatted(differences.size()));
            sb.append("=".repeat(50)).append("\n\n");

            var onlyA = onlyInA();
            var onlyB = onlyInB();
            var typeMm = typeMismatches();

            if (!onlyA.isEmpty()) {
                sb.append("Only in A (%d):\n".formatted(onlyA.size()));
                onlyA.forEach(d -> sb.append("  ").append(d.path()).append(" (").append(d.detailA()).append(")\n"));
                sb.append("\n");
            }
            if (!onlyB.isEmpty()) {
                sb.append("Only in B (%d):\n".formatted(onlyB.size()));
                onlyB.forEach(d -> sb.append("  ").append(d.path()).append(" (").append(d.detailB()).append(")\n"));
                sb.append("\n");
            }
            if (!typeMm.isEmpty()) {
                sb.append("Type mismatches (%d):\n".formatted(typeMm.size()));
                typeMm.forEach(d -> sb.append("  ").append(d).append("\n"));
                sb.append("\n");
            }

            return sb.toString();
        }
    }
}
