package com.fakedata.config;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the file generation process.
 */
public class GeneratorConfig {
    private final List<String> submissionRefs;
    private final Map<String, Integer> formatCounts;
    private final Path outputDir;
    private final String validationPattern;

    public GeneratorConfig(List<String> submissionRefs, Map<String, Integer> formatCounts,
                           Path outputDir, String validationPattern) {
        this.submissionRefs = submissionRefs;
        this.formatCounts = formatCounts;
        this.outputDir = outputDir;
        this.validationPattern = validationPattern;
    }

    public List<String> getSubmissionRefs() {
        return submissionRefs;
    }

    public Map<String, Integer> getFormatCounts() {
        return formatCounts;
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public String getValidationPattern() {
        return validationPattern;
    }
}
