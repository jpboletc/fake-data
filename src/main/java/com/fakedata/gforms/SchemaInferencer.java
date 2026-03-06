package com.fakedata.gforms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * A2: Schema inference from real submissions.
 *
 * Analyses multiple JSON submissions of the same form type and produces a union schema
 * showing all observed fields, which ones are optional, and any type variations.
 */
public class SchemaInferencer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Infer a union schema from multiple JSON submissions.
     *
     * @param submissions list of JSON strings
     * @return the inferred schema describing all observed fields
     */
    public InferredSchema infer(List<String> submissions) throws IOException {
        List<JsonNode> roots = new ArrayList<>();
        for (String json : submissions) {
            roots.add(MAPPER.readTree(json));
        }
        return inferFromNodes(roots);
    }

    /**
     * Infer from JSON files in a directory.
     */
    public InferredSchema inferFromDirectory(Path dir) throws IOException {
        List<JsonNode> roots = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> jsonFiles = files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
            for (Path file : jsonFiles) {
                roots.add(MAPPER.readTree(file.toFile()));
            }
        }
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("No JSON files found in " + dir);
        }
        return inferFromNodes(roots);
    }

    private InferredSchema inferFromNodes(List<JsonNode> roots) {
        int totalSubmissions = roots.size();
        SchemaNode schema = new SchemaNode("$", totalSubmissions);
        for (JsonNode root : roots) {
            mergeInto(schema, root);
        }
        return new InferredSchema(schema, totalSubmissions);
    }

    private void mergeInto(SchemaNode schema, JsonNode node) {
        if (node.isObject()) {
            schema.addObservedType("object");
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                SchemaNode child = schema.children.computeIfAbsent(
                        entry.getKey(),
                        k -> new SchemaNode(schema.path + "." + k, schema.totalSubmissions));
                child.occurrences++;
                mergeInto(child, entry.getValue());
            }
        } else if (node.isArray()) {
            schema.addObservedType("array");
            schema.maxArraySize = Math.max(schema.maxArraySize, node.size());
            // Merge all array elements into a single "[*]" child to represent element schema
            SchemaNode elemChild = schema.children.computeIfAbsent(
                    "[*]",
                    k -> new SchemaNode(schema.path + "[*]", schema.totalSubmissions));
            for (int i = 0; i < node.size(); i++) {
                elemChild.occurrences++;
                mergeInto(elemChild, node.get(i));
            }
        } else if (node.isTextual()) {
            schema.addObservedType("string");
            schema.addSampleValue(node.asText());
        } else if (node.isNumber()) {
            schema.addObservedType("number");
            schema.addSampleValue(node.asText());
        } else if (node.isBoolean()) {
            schema.addObservedType("boolean");
            schema.addSampleValue(node.asText());
        } else if (node.isNull()) {
            schema.addObservedType("null");
        }
    }

    // --- Data model ---

    public static class SchemaNode {
        public final String path;
        public final int totalSubmissions;
        public int occurrences = 0;
        public final Set<String> observedTypes = new LinkedHashSet<>();
        public final Map<String, SchemaNode> children = new LinkedHashMap<>();
        public int maxArraySize = 0;
        private final Set<String> sampleValues = new LinkedHashSet<>();
        private static final int MAX_SAMPLES = 5;

        public SchemaNode(String path, int totalSubmissions) {
            this.path = path;
            this.totalSubmissions = totalSubmissions;
        }

        void addObservedType(String type) {
            observedTypes.add(type);
        }

        void addSampleValue(String value) {
            if (sampleValues.size() < MAX_SAMPLES) {
                // Truncate long values
                sampleValues.add(value.length() > 80 ? value.substring(0, 77) + "..." : value);
            }
        }

        public boolean isOptional() {
            return occurrences < totalSubmissions;
        }

        public boolean hasTypeVariation() {
            return observedTypes.size() > 1;
        }

        public Set<String> getSampleValues() {
            return Collections.unmodifiableSet(sampleValues);
        }

        /**
         * Recursively collect all leaf paths with their metadata.
         */
        public List<FieldInfo> flattenFields() {
            List<FieldInfo> result = new ArrayList<>();
            flattenFields(result);
            return result;
        }

        private void flattenFields(List<FieldInfo> result) {
            if (children.isEmpty()) {
                result.add(new FieldInfo(path, observedTypes, occurrences, totalSubmissions, sampleValues));
            } else {
                // Include intermediate nodes that are optional or have type variation
                if (isOptional() || hasTypeVariation()) {
                    result.add(new FieldInfo(path, observedTypes, occurrences, totalSubmissions, sampleValues));
                }
                for (SchemaNode child : children.values()) {
                    child.flattenFields(result);
                }
            }
        }
    }

    public record FieldInfo(
            String path,
            Set<String> types,
            int occurrences,
            int totalSubmissions,
            Set<String> sampleValues
    ) {
        public boolean isOptional() {
            return occurrences < totalSubmissions;
        }
    }

    public record InferredSchema(SchemaNode root, int submissionCount) {

        /**
         * Get all leaf fields, flattened.
         */
        public List<FieldInfo> allFields() {
            return root.flattenFields();
        }

        /**
         * Get only optional fields (present in some but not all submissions).
         */
        public List<FieldInfo> optionalFields() {
            return allFields().stream().filter(FieldInfo::isOptional).toList();
        }

        /**
         * Get fields with type variation (e.g. sometimes string, sometimes array).
         */
        public List<FieldInfo> fieldsWithTypeVariation() {
            return allFields().stream().filter(f -> f.types().size() > 1).toList();
        }

        /**
         * Produce a human-readable summary.
         */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Schema inferred from %d submissions\n".formatted(submissionCount));
            sb.append("=".repeat(50)).append("\n\n");

            List<FieldInfo> fields = allFields();
            List<FieldInfo> optional = optionalFields();
            List<FieldInfo> typeVar = fieldsWithTypeVariation();

            sb.append("Total fields: %d\n".formatted(fields.size()));
            sb.append("Optional fields: %d\n".formatted(optional.size()));
            sb.append("Fields with type variation: %d\n\n".formatted(typeVar.size()));

            if (!optional.isEmpty()) {
                sb.append("OPTIONAL FIELDS (not present in all submissions):\n");
                for (FieldInfo f : optional) {
                    sb.append("  %s  (%d/%d submissions, types: %s)\n".formatted(
                            f.path(), f.occurrences(), f.totalSubmissions(), f.types()));
                }
                sb.append("\n");
            }

            if (!typeVar.isEmpty()) {
                sb.append("TYPE VARIATIONS:\n");
                for (FieldInfo f : typeVar) {
                    sb.append("  %s  types: %s\n".formatted(f.path(), f.types()));
                }
                sb.append("\n");
            }

            return sb.toString();
        }
    }
}
