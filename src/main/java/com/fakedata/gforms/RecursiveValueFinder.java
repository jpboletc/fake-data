package com.fakedata.gforms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * A1: Recursive JSON walker that finds fields of interest by pattern matching
 * on key names and/or values, regardless of where they appear in the tree.
 */
public class RecursiveValueFinder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<ExtractionRule> rules;

    public RecursiveValueFinder(List<ExtractionRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Search a JSON string for all fields matching the configured rules.
     */
    public List<FieldMatch> find(String json) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        List<FieldMatch> matches = new ArrayList<>();
        walk(root, "$", null, matches);
        return matches;
    }

    /**
     * Search a JSON file for all fields matching the configured rules.
     */
    public List<FieldMatch> find(Path jsonFile) throws IOException {
        return find(Files.readString(jsonFile));
    }

    /**
     * Search a pre-parsed JsonNode tree.
     */
    public List<FieldMatch> find(JsonNode root) {
        List<FieldMatch> matches = new ArrayList<>();
        walk(root, "$", null, matches);
        return matches;
    }

    private void walk(JsonNode node, String path, String currentKey, List<FieldMatch> matches) {
        if (node.isObject()) {
            walkObject((ObjectNode) node, path, matches);
        } else if (node.isArray()) {
            walkArray((ArrayNode) node, path, matches);
        } else if (node.isValueNode()) {
            checkValue(node, path, currentKey, matches);
        }
    }

    private void walkObject(ObjectNode node, String path, List<FieldMatch> matches) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            String childPath = path + "." + key;
            walk(entry.getValue(), childPath, key, matches);
        }
    }

    private void walkArray(ArrayNode node, String path, List<FieldMatch> matches) {
        for (int i = 0; i < node.size(); i++) {
            String childPath = path + "[" + i + "]";
            walk(node.get(i), childPath, null, matches);
        }
    }

    private void checkValue(JsonNode node, String path, String currentKey, List<FieldMatch> matches) {
        String valueStr = node.asText();
        // Track which field types we've already matched at this path to avoid duplicates
        Set<String> matchedTypes = new HashSet<>();

        for (ExtractionRule rule : rules) {
            if (matchedTypes.contains(rule.fieldType())) continue;

            boolean keyMatch = currentKey != null && rule.matchesKey(currentKey);
            boolean valueMatch = rule.matchesValue(valueStr);

            boolean matched = false;
            if (rule.keyPattern() != null && rule.valuePattern() != null) {
                matched = keyMatch && valueMatch;
            } else if (rule.keyPattern() != null) {
                matched = keyMatch;
            } else {
                matched = valueMatch;
            }

            if (matched) {
                matches.add(new FieldMatch(rule.fieldType(), path, currentKey, valueStr, rule.fieldType()));
                matchedTypes.add(rule.fieldType());
            }
        }
    }

    /**
     * Returns a pre-configured finder with common gForms extraction rules.
     */
    public static RecursiveValueFinder withDefaultRules() {
        return new RecursiveValueFinder(DefaultExtractionRules.all());
    }
}
