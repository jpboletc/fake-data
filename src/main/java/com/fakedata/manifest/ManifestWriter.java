package com.fakedata.manifest;

import com.fakedata.util.IdGenerator;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the manifest CSV file containing metadata for all generated files.
 */
public class ManifestWriter {

    private final List<ManifestEntry> entries = new ArrayList<>();

    /**
     * Represents a single entry in the manifest.
     */
    public record ManifestEntry(
            String mailItemId,
            String attachedId,
            String filename
    ) {
    }

    /**
     * Adds a new entry to the manifest.
     *
     * @param submissionRef the submission reference
     * @param fileNumber    the file number within this submission
     * @param filename      the generated filename (without submission prefix)
     */
    public void addEntry(String submissionRef, int fileNumber, String filename) {
        String mailItemId = IdGenerator.generate16();
        // String attachedId = IdGenerator.generate16();
        String attachedId = mailItemId;
        String fullFilename = submissionRef + "_" + fileNumber + "_" + filename;

        entries.add(new ManifestEntry(mailItemId, attachedId, fullFilename));
    }

    /**
     * Returns the full filename for the last added entry.
     *
     * @return the full filename with submission prefix
     */
    public String getLastFilename() {
        if (entries.isEmpty()) {
            return null;
        }
        return entries.get(entries.size() - 1).filename();
    }

    /**
     * Returns all entries in the manifest.
     *
     * @return list of manifest entries
     */
    public List<ManifestEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    /**
     * Writes the manifest to a CSV file.
     *
     * @param outputDir the directory to write the manifest to
     * @param filename  the manifest filename
     * @throws IOException if writing fails
     */
    public void write(Path outputDir, String filename) throws IOException {
        Path manifestPath = outputDir.resolve(filename);

        try (FileWriter fw = new FileWriter(manifestPath.toFile())) {
            // Write blank line at start
            fw.write(System.lineSeparator());

            try (CSVWriter writer = new CSVWriter(fw,
                    CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END)) {

                // Write entries (no header, no quotes)
                for (ManifestEntry entry : entries) {
                    writer.writeNext(new String[]{
                            entry.mailItemId(),
                            entry.attachedId(),
                            entry.filename()
                    });
                }
            }
        }
    }

    /**
     * Returns the number of entries in the manifest.
     *
     * @return the entry count
     */
    public int getEntryCount() {
        return entries.size();
    }

    /**
     * Clears all entries from the manifest.
     */
    public void clear() {
        entries.clear();
    }
}
