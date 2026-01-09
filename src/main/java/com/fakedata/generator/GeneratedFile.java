package com.fakedata.generator;

import java.nio.file.Path;

/**
 * Represents a generated file with its metadata.
 */
public record GeneratedFile(
        Path path,
        String filename
) {
}
