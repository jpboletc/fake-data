package com.fakedata;

import com.fakedata.content.ContentProvider;
import com.fakedata.content.ContentProvider.Theme;
import com.fakedata.generator.*;
import com.fakedata.manifest.ManifestWriter;
import com.fakedata.util.ValidationUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;

@Command(
        name = "fake-data",
        mixinStandardHelpOptions = true,
        version = "fake-data 1.0.0",
        description = "Generates dummy attachment files with realistic content and a CSV manifest."
)
public class FakeDataApp implements Callable<Integer> {

    @Option(names = {"-r", "--refs"},
            description = "Comma-separated submission references")
    private String refs;

    @Option(names = {"-f", "--file"},
            description = "File with submission references (one per line, optional // comment for theme)")
    private Path inputFile;

    @Option(names = {"-F", "--formats"},
            description = "Format specification (e.g., 'pdf:2,xlsx:1,pptx:1'). Supported: pdf, jpeg, xlsx, xls, ods, docx, odt, pptx, odp",
            defaultValue = "pdf:1,xlsx:1,docx:1,pptx:1")
    private String formats;

    @Option(names = {"-o", "--output"},
            description = "Output directory",
            defaultValue = "./output")
    private Path outputDir;

    @Option(names = {"-p", "--pattern"},
            description = "Regex pattern for submission reference validation",
            defaultValue = "^[A-Za-z0-9]{12}$")
    private String validationPattern;

    @Option(names = {"-t", "--theme"},
            description = "Global theme for content generation (financial, entertainment, healthcare, technology, legal, education, retail)")
    private String globalTheme;

    @Option(names = {"-m", "--manifest"},
            description = "Manifest filename (e.g., 'manifest12012611.csv' for DDMMYYHH format)")
    private String manifestFilename;

    // Registry of available generators
    private static final Map<String, FileGenerator> GENERATORS = new LinkedHashMap<>();

    static {
        GENERATORS.put("pdf", new PdfGenerator());
        GENERATORS.put("jpeg", new JpegGenerator());
        GENERATORS.put("xlsx", new ExcelGenerator(false));
        GENERATORS.put("xls", new ExcelGenerator(true));
        GENERATORS.put("ods", new OdsGenerator());
        GENERATORS.put("docx", new DocxGenerator());
        GENERATORS.put("odt", new OdtGenerator());
        GENERATORS.put("pptx", new PptxGenerator());
        GENERATORS.put("odp", new OdpGenerator());
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new FakeDataApp()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // Validate inputs
        if (refs == null && inputFile == null) {
            System.err.println("Error: Must specify either --refs or --file");
            return 1;
        }

        if (!ValidationUtils.isValidPattern(validationPattern)) {
            System.err.println("Error: Invalid validation pattern: " + validationPattern);
            return 1;
        }

        // Parse submission references (with optional themes)
        Map<String, Theme> submissionRefs = parseSubmissionRefs();
        if (submissionRefs.isEmpty()) {
            System.err.println("Error: No valid submission references provided");
            return 1;
        }

        // Parse format specification
        Map<String, Integer> formatCounts = parseFormats();
        if (formatCounts.isEmpty()) {
            System.err.println("Error: No valid formats specified");
            return 1;
        }

        // Create output directory
        Files.createDirectories(outputDir);

        // Generate files
        ManifestWriter manifest = new ManifestWriter();
        Theme defaultTheme = ContentProvider.parseTheme(globalTheme);

        System.out.println("Generating files...");
        System.out.println("Output directory: " + outputDir.toAbsolutePath());
        System.out.println("Formats: " + formatCounts);
        System.out.println("Submission references: " + submissionRefs.size());
        if (defaultTheme != Theme.DEFAULT) {
            System.out.println("Global theme: " + defaultTheme);
        }
        System.out.println();

        int totalFiles = 0;
        for (Map.Entry<String, Theme> entry : submissionRefs.entrySet()) {
            String submissionRef = entry.getKey();
            Theme theme = entry.getValue() != null ? entry.getValue() : defaultTheme;

            System.out.println("Processing: " + submissionRef + (theme != Theme.DEFAULT ? " [" + theme + "]" : ""));

            ContentProvider contentProvider = new ContentProvider(theme);
            int fileNumber = 1;

            for (Map.Entry<String, Integer> formatEntry : formatCounts.entrySet()) {
                String format = formatEntry.getKey();
                int count = formatEntry.getValue();

                FileGenerator generator = GENERATORS.get(format);
                if (generator == null) {
                    System.err.println("  Warning: Unknown format '" + format + "', skipping");
                    continue;
                }

                for (int i = 0; i < count; i++) {
                    try {
                        String baseName = generator.generateFilename(contentProvider);
                        manifest.addEntry(submissionRef, fileNumber, baseName + "." + generator.getExtension());
                        String fullFilename = manifest.getLastFilename();

                        // Extract just the base filename (without submission prefix) for generator
                        String filenameForGenerator = submissionRef + "_" + fileNumber + "_" + baseName;

                        GeneratedFile generated = generator.generate(outputDir, filenameForGenerator, contentProvider);
                        System.out.println("  Created: " + generated.filename());
                        fileNumber++;
                        totalFiles++;
                    } catch (IOException e) {
                        System.err.println("  Error generating " + format + " file: " + e.getMessage());
                    }
                }
            }
        }

        // Write manifest (default: manifestDDMMYYHH.csv)
        String actualManifestName = manifestFilename;
        if (actualManifestName == null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyHH"));
            actualManifestName = "manifest" + timestamp + ".csv";
        }
        try {
            manifest.write(outputDir, actualManifestName);
            System.out.println("\nGenerated " + actualManifestName + " with " + manifest.getEntryCount() + " entries");
        } catch (IOException e) {
            System.err.println("Error writing manifest: " + e.getMessage());
            return 1;
        }

        System.out.println("\nComplete! Generated " + totalFiles + " files.");
        return 0;
    }

    /**
     * Parses submission references from CLI args or file.
     * File format supports comments for per-submission themes:
     * ABCD1234EFGH // financial
     * WXYZ9876MNOP // entertainment & media
     *
     * @return map of submission reference to theme (null if no theme specified)
     */
    private Map<String, Theme> parseSubmissionRefs() throws IOException {
        Map<String, Theme> result = new LinkedHashMap<>();

        // Parse from --refs argument
        if (refs != null && !refs.isBlank()) {
            for (String ref : refs.split(",")) {
                String trimmed = ref.trim();
                if (!trimmed.isEmpty()) {
                    if (ValidationUtils.isValidSubmissionRef(trimmed, validationPattern)) {
                        result.put(trimmed, null); // No theme from CLI refs
                    } else {
                        System.err.println("Warning: Invalid submission reference '" + trimmed + "', skipping");
                    }
                }
            }
        }

        // Parse from file
        if (inputFile != null) {
            if (!Files.exists(inputFile)) {
                System.err.println("Error: Input file not found: " + inputFile);
            } else {
                try (BufferedReader reader = new BufferedReader(new FileReader(inputFile.toFile()))) {
                    String line;
                    int lineNum = 0;
                    while ((line = reader.readLine()) != null) {
                        lineNum++;
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        // Parse optional theme comment: REF // theme
                        String ref;
                        Theme theme = null;
                        int commentIdx = line.indexOf("//");
                        if (commentIdx >= 0) {
                            ref = line.substring(0, commentIdx).trim();
                            String themeStr = line.substring(commentIdx + 2).trim();
                            theme = ContentProvider.parseTheme(themeStr);
                        } else {
                            ref = line;
                        }

                        if (!ref.isEmpty()) {
                            if (ValidationUtils.isValidSubmissionRef(ref, validationPattern)) {
                                result.put(ref, theme);
                            } else {
                                System.err.println("Warning: Line " + lineNum + ": Invalid submission reference '" + ref + "', skipping");
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Parses format specification string.
     * Format: "pdf:2,xlsx:1,pptx:1" or just "pdf,xlsx" (count defaults to 1)
     *
     * @return map of format to count
     */
    private Map<String, Integer> parseFormats() {
        Map<String, Integer> result = new LinkedHashMap<>();

        if (formats == null || formats.isBlank()) {
            return result;
        }

        for (String spec : formats.split(",")) {
            spec = spec.trim().toLowerCase();
            if (spec.isEmpty()) continue;

            String format;
            int count = 1;

            int colonIdx = spec.indexOf(':');
            if (colonIdx >= 0) {
                format = spec.substring(0, colonIdx).trim();
                try {
                    count = Integer.parseInt(spec.substring(colonIdx + 1).trim());
                    if (count < 1) count = 1;
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid count in '" + spec + "', using 1");
                }
            } else {
                format = spec;
            }

            if (GENERATORS.containsKey(format)) {
                result.put(format, result.getOrDefault(format, 0) + count);
            } else {
                System.err.println("Warning: Unknown format '" + format + "', skipping. Valid formats: " + GENERATORS.keySet());
            }
        }

        return result;
    }
}
