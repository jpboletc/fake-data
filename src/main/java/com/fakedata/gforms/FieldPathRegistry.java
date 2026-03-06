package com.fakedata.gforms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/**
 * A3: Per-form-type registry of known JSON paths where fields of interest have been found.
 * Stored as YAML files in a configurable directory.
 */
public class FieldPathRegistry {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(
            new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    static {
        YAML_MAPPER.findAndRegisterModules();
    }

    private final Path registryDir;

    public FieldPathRegistry(Path registryDir) {
        this.registryDir = registryDir;
    }

    /**
     * Load the registry entry for a given form type. Returns empty optional if no registry exists.
     */
    public Optional<FormRegistry> load(String formId) throws IOException {
        Path file = registryDir.resolve(sanitizeFilename(formId) + ".yaml");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(YAML_MAPPER.readValue(file.toFile(), FormRegistry.class));
    }

    /**
     * Save or update the registry entry for a form type.
     */
    public void save(FormRegistry registry) throws IOException {
        Files.createDirectories(registryDir);
        Path file = registryDir.resolve(sanitizeFilename(registry.formId) + ".yaml");
        YAML_MAPPER.writeValue(file.toFile(), registry);
    }

    /**
     * Update registry with newly discovered paths from a finder run.
     * Returns the updated registry.
     */
    public FormRegistry updateFromMatches(String formId, List<FieldMatch> matches) throws IOException {
        FormRegistry registry = load(formId).orElseGet(() -> FormRegistry.empty(formId));

        for (FieldMatch match : matches) {
            // Add to known paths
            registry.knownPaths
                    .computeIfAbsent(match.fieldType(), k -> new LinkedHashSet<>())
                    .add(match.jsonPath());

            // Check if this is a newly discovered path
            boolean isNew = registry.autoDiscoveredPaths.stream()
                    .noneMatch(d -> d.path.equals(match.jsonPath()) && d.field.equals(match.fieldType()));

            if (isNew) {
                registry.autoDiscoveredPaths.add(new DiscoveredPath(
                        match.fieldType(), match.jsonPath(), LocalDate.now().toString()));
            }
        }

        registry.lastUpdated = LocalDate.now().toString();
        save(registry);
        return registry;
    }

    /**
     * Look up known paths for a given field type within a form's registry.
     */
    public List<String> getKnownPaths(String formId, String fieldType) throws IOException {
        return load(formId)
                .map(r -> r.knownPaths.getOrDefault(fieldType, Set.of()))
                .map(ArrayList::new)
                .orElse(new ArrayList<>());
    }

    private static String sanitizeFilename(String formId) {
        return formId.replaceAll("[^a-zA-Z0-9_-]", "-");
    }

    // --- Data model classes ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FormRegistry {
        @JsonProperty("form_id")
        public String formId;

        @JsonProperty("known_paths")
        public Map<String, Set<String>> knownPaths = new LinkedHashMap<>();

        @JsonProperty("last_updated")
        public String lastUpdated;

        @JsonProperty("auto_discovered_paths")
        public List<DiscoveredPath> autoDiscoveredPaths = new ArrayList<>();

        public FormRegistry() {}

        public static FormRegistry empty(String formId) {
            FormRegistry r = new FormRegistry();
            r.formId = formId;
            r.lastUpdated = LocalDate.now().toString();
            return r;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DiscoveredPath {
        public String field;
        public String path;

        @JsonProperty("first_seen")
        public String firstSeen;

        public DiscoveredPath() {}

        public DiscoveredPath(String field, String path, String firstSeen) {
            this.field = field;
            this.path = path;
            this.firstSeen = firstSeen;
        }
    }
}
