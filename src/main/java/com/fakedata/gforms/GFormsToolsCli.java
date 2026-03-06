package com.fakedata.gforms;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI entry point for the gForms schema tools.
 * Can be run standalone or integrated into the main FakeDataApp.
 */
@Command(name = "gforms-tools", mixinStandardHelpOptions = true,
        description = "Tools for analysing variable gForms JSON submissions",
        subcommands = {
                GFormsToolsCli.ExtractCommand.class,
                GFormsToolsCli.InferSchemaCommand.class,
                GFormsToolsCli.DiffCommand.class,
                GFormsToolsCli.FindCommand.class
        })
public class GFormsToolsCli implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new GFormsToolsCli()).execute(args);
        System.exit(exitCode);
    }

    // --- Subcommands ---

    @Command(name = "extract", description = "Extract fields of interest using two-tier strategy (A5)")
    static class ExtractCommand implements Callable<Integer> {

        @Option(names = {"--form-id", "-f"}, required = true, description = "Form type identifier")
        String formId;

        @Option(names = {"--input", "-i"}, required = true, description = "Input JSON file")
        Path inputFile;

        @Option(names = {"--registry", "-r"}, defaultValue = "form-schemas",
                description = "Registry directory (default: form-schemas)")
        Path registryDir;

        @Override
        public Integer call() throws IOException {
            var extractor = GFormFieldExtractor.withDefaults(registryDir);
            var result = extractor.extract(formId, inputFile);

            System.out.println("Extraction results for form: " + formId);
            System.out.println("=".repeat(50));
            System.out.println("Total matches: " + result.allMatches().size());
            System.out.println("Fast-path matches: " + result.fastPathMatches().size());
            System.out.println("New discoveries: " + result.newDiscoveries().size());

            if (result.hasNewPaths()) {
                System.out.println("\nNEW PATHS DISCOVERED:");
                result.newDiscoveries().forEach(m ->
                        System.out.println("  " + m));
            }

            System.out.println("\nAll matches:");
            result.allMatches().forEach(m ->
                    System.out.println("  " + m));

            return 0;
        }
    }

    @Command(name = "find", description = "Recursively find fields of interest in a JSON file (A1)")
    static class FindCommand implements Callable<Integer> {

        @Option(names = {"--input", "-i"}, required = true, description = "Input JSON file")
        Path inputFile;

        @Override
        public Integer call() throws IOException {
            var finder = RecursiveValueFinder.withDefaultRules();
            List<FieldMatch> matches = finder.find(inputFile);

            System.out.println("Found %d matches:".formatted(matches.size()));
            matches.forEach(m -> System.out.println("  " + m));

            return 0;
        }
    }

    @Command(name = "infer-schema", description = "Infer union schema from multiple submissions (A2)")
    static class InferSchemaCommand implements Callable<Integer> {

        @Option(names = {"--dir", "-d"}, required = true,
                description = "Directory containing JSON submissions of the same form type")
        Path inputDir;

        @Override
        public Integer call() throws IOException {
            var inferencer = new SchemaInferencer();
            var schema = inferencer.inferFromDirectory(inputDir);
            System.out.print(schema.summary());
            return 0;
        }
    }

    @Command(name = "diff", description = "Compare structure of two JSON submissions (A4)")
    static class DiffCommand implements Callable<Integer> {

        @Option(names = {"--a"}, required = true, description = "First JSON file")
        Path fileA;

        @Option(names = {"--b"}, required = true, description = "Second JSON file")
        Path fileB;

        @Override
        public Integer call() throws IOException {
            var tool = new StructuralDiffTool();
            var result = tool.diff(fileA, fileB);
            System.out.print(result.summary());
            return 0;
        }
    }
}
