package com.fakedata.gforms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A5: Two-tier field extraction strategy.
 *
 * 1. Fast path: check known paths from the registry (O(1) lookups)
 * 2. Fallback: run the recursive value finder across the full document
 * 3. Auto-update: if fallback finds fields at new paths, log and update the registry
 */
public class GFormFieldExtractor {

    private static final Logger LOG = Logger.getLogger(GFormFieldExtractor.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RecursiveValueFinder finder;
    private final FieldPathRegistry registry;

    public GFormFieldExtractor(RecursiveValueFinder finder, FieldPathRegistry registry) {
        this.finder = finder;
        this.registry = registry;
    }

    /**
     * Create an extractor with default rules and a registry directory.
     */
    public static GFormFieldExtractor withDefaults(Path registryDir) {
        return new GFormFieldExtractor(
                RecursiveValueFinder.withDefaultRules(),
                new FieldPathRegistry(registryDir));
    }

    /**
     * Extract all fields of interest from a JSON submission.
     *
     * @param formId  the form type identifier (used for registry lookup)
     * @param json    the JSON submission content
     * @return extraction result with matches and metadata
     */
    public ExtractionResult extract(String formId, String json) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        return extract(formId, root);
    }

    /**
     * Extract from a file path.
     */
    public ExtractionResult extract(String formId, Path jsonFile) throws IOException {
        JsonNode root = MAPPER.readTree(jsonFile.toFile());
        return extract(formId, root);
    }

    /**
     * Extract from a pre-parsed JsonNode.
     */
    public ExtractionResult extract(String formId, JsonNode root) throws IOException {
        // Tier 1: Fast path — check known paths from registry
        List<FieldMatch> fastPathMatches = fastPathLookup(formId, root);

        // Tier 2: Fallback — recursive search
        List<FieldMatch> allMatches = finder.find(root);

        // Identify newly discovered paths
        List<FieldMatch> newDiscoveries = findNewPaths(formId, allMatches);

        if (!newDiscoveries.isEmpty()) {
            LOG.warning("New field paths discovered for form '%s': %s".formatted(
                    formId, newDiscoveries.stream().map(m -> m.jsonPath()).toList()));
            registry.updateFromMatches(formId, newDiscoveries);
        }

        // Also update registry with all matches to keep it current
        if (!allMatches.isEmpty()) {
            registry.updateFromMatches(formId, allMatches);
        }

        return new ExtractionResult(
                allMatches,
                fastPathMatches,
                newDiscoveries,
                !newDiscoveries.isEmpty()
        );
    }

    /**
     * Fast path: traverse only known paths from the registry.
     */
    private List<FieldMatch> fastPathLookup(String formId, JsonNode root) throws IOException {
        List<FieldMatch> matches = new ArrayList<>();
        var registryOpt = registry.load(formId);
        if (registryOpt.isEmpty()) return matches;

        var formRegistry = registryOpt.get();
        for (var entry : formRegistry.knownPaths.entrySet()) {
            String fieldType = entry.getKey();
            for (String path : entry.getValue()) {
                JsonNode resolved = resolveJsonPath(root, path);
                if (resolved != null && !resolved.isMissingNode() && !resolved.isNull()) {
                    matches.add(new FieldMatch(
                            fieldType, path, extractLastKey(path),
                            resolved.asText(), "registry-fast-path"));
                }
            }
        }
        return matches;
    }

    /**
     * Find matches that exist in allMatches but not yet in the registry.
     */
    private List<FieldMatch> findNewPaths(String formId, List<FieldMatch> allMatches) throws IOException {
        var registryOpt = registry.load(formId);
        if (registryOpt.isEmpty()) return new ArrayList<>(allMatches);

        var formRegistry = registryOpt.get();
        List<FieldMatch> newOnes = new ArrayList<>();
        for (FieldMatch match : allMatches) {
            var knownForType = formRegistry.knownPaths.getOrDefault(match.fieldType(), java.util.Set.of());
            if (!knownForType.contains(match.jsonPath())) {
                newOnes.add(match);
            }
        }
        return newOnes;
    }

    /**
     * Resolve a simple dot-notation JSON path like "$.gform.section.field" against a tree.
     * Supports array index notation like [0].
     */
    static JsonNode resolveJsonPath(JsonNode root, String path) {
        if (path == null || root == null) return null;

        // Strip leading "$."
        String normalized = path.startsWith("$.") ? path.substring(2) : path;
        if (normalized.equals("$")) return root;

        JsonNode current = root;
        String[] segments = normalized.split("\\.");

        for (String segment : segments) {
            if (current == null || current.isMissingNode()) return null;

            // Handle array index: fieldName[0]
            int bracketStart = segment.indexOf('[');
            if (bracketStart >= 0) {
                String fieldName = segment.substring(0, bracketStart);
                if (!fieldName.isEmpty()) {
                    current = current.get(fieldName);
                    if (current == null) return null;
                }

                // Parse all array indices in this segment
                String remaining = segment.substring(bracketStart);
                while (remaining.startsWith("[")) {
                    int bracketEnd = remaining.indexOf(']');
                    if (bracketEnd < 0) return null;
                    String indexStr = remaining.substring(1, bracketEnd);
                    try {
                        int index = Integer.parseInt(indexStr);
                        if (!current.isArray() || index >= current.size()) return null;
                        current = current.get(index);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                    remaining = remaining.substring(bracketEnd + 1);
                }
            } else {
                current = current.get(segment);
            }
        }
        return current;
    }

    private static String extractLastKey(String path) {
        if (path == null) return null;
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0) {
            String last = path.substring(lastDot + 1);
            int bracket = last.indexOf('[');
            return bracket >= 0 ? last.substring(0, bracket) : last;
        }
        return path;
    }

    /**
     * Result of a two-tier extraction.
     */
    public record ExtractionResult(
            List<FieldMatch> allMatches,
            List<FieldMatch> fastPathMatches,
            List<FieldMatch> newDiscoveries,
            boolean hasNewPaths
    ) {
        /**
         * Get all matches for a specific field type.
         */
        public List<FieldMatch> matchesForType(String fieldType) {
            return allMatches.stream()
                    .filter(m -> m.fieldType().equals(fieldType))
                    .toList();
        }

        /**
         * Get the first match value for a field type, or empty if none found.
         */
        public java.util.Optional<String> firstValue(String fieldType) {
            return allMatches.stream()
                    .filter(m -> m.fieldType().equals(fieldType))
                    .map(FieldMatch::value)
                    .findFirst();
        }
    }
}
