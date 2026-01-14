package com.fakedata.generator;

import com.fakedata.content.ContentProvider;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Abstract base class for file generators providing common functionality.
 */
public abstract class AbstractFileGenerator implements FileGenerator {

    /**
     * Builds the full filename with extension.
     */
    protected String buildFullFilename(String filename) {
        return filename + "." + getExtension();
    }

    /**
     * Resolves the file path in the output directory.
     */
    protected Path resolveFilePath(Path outputDir, String fullFilename) {
        return outputDir.resolve(fullFilename);
    }

    /**
     * Creates the result object for a successfully generated file.
     */
    protected GeneratedFile createResult(Path filePath, String fullFilename) {
        return new GeneratedFile(filePath, fullFilename);
    }

    /**
     * Template method that handles common setup and result creation.
     * Subclasses implement doGenerate() for format-specific logic.
     */
    @Override
    public GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException {
        String fullFilename = buildFullFilename(filename);
        Path filePath = resolveFilePath(outputDir, fullFilename);

        doGenerate(filePath, contentProvider);

        return createResult(filePath, fullFilename);
    }

    /**
     * Performs the actual file generation. Implemented by subclasses.
     *
     * @param filePath        the full path where the file should be written
     * @param contentProvider the content provider for generating realistic data
     * @throws IOException if file generation fails
     */
    protected abstract void doGenerate(Path filePath, ContentProvider contentProvider) throws IOException;

    /**
     * Default implementation returns the extension as the format key.
     * Override if format key differs from extension.
     */
    @Override
    public String getFormatKey() {
        return getExtension();
    }
}
