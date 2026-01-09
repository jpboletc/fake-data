package com.fakedata.generator;

import com.fakedata.content.ContentProvider;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for file generators. Each implementation generates a specific file format.
 */
public interface FileGenerator {

    /**
     * Generates a file with realistic content.
     *
     * @param outputDir       the directory to write the file to
     * @param filename        the filename (without extension)
     * @param contentProvider the content provider for generating realistic data
     * @return the generated file result containing the full filename
     * @throws IOException if file generation fails
     */
    GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException;

    /**
     * Returns the file extension for this generator (e.g., "pdf", "xlsx").
     *
     * @return the file extension without the dot
     */
    String getExtension();

    /**
     * Returns the format key used in CLI (e.g., "pdf", "xlsx").
     *
     * @return the format key
     */
    String getFormatKey();

    /**
     * Generates a contextually appropriate filename for this file type.
     *
     * @param contentProvider the content provider
     * @return the generated filename without extension
     */
    String generateFilename(ContentProvider contentProvider);
}
