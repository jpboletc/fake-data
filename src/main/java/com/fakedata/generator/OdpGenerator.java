package com.fakedata.generator;

import com.fakedata.content.ContentProvider;
import org.odftoolkit.simple.PresentationDocument;
import org.odftoolkit.simple.presentation.Slide;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates ODP (OpenDocument Presentation) files.
 * Creates a multi-slide presentation structure.
 */
public class OdpGenerator implements FileGenerator {

    @Override
    public GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException {
        String fullFilename = filename + "." + getExtension();
        Path filePath = outputDir.resolve(fullFilename);

        try {
            PresentationDocument document = PresentationDocument.newPresentationDocument();

            // The document starts with one blank slide
            // Add additional slides for a complete presentation

            // Agenda slide
            document.newSlide(document.getSlideCount(), "Agenda", Slide.SlideLayout.TITLE_ONLY);

            // Content slides
            for (int i = 0; i < 4; i++) {
                document.newSlide(document.getSlideCount(), contentProvider.getSlideTitle(),
                        Slide.SlideLayout.TITLE_ONLY);
            }

            // Summary slide
            document.newSlide(document.getSlideCount(), "Summary", Slide.SlideLayout.TITLE_ONLY);

            // Closing slide
            document.newSlide(document.getSlideCount(), "Thank You", Slide.SlideLayout.TITLE_ONLY);

            document.save(filePath.toFile());
            document.close();

        } catch (Exception e) {
            throw new IOException("Failed to create ODP document: " + e.getMessage(), e);
        }

        return new GeneratedFile(filePath, fullFilename);
    }

    @Override
    public String getExtension() {
        return "odp";
    }

    @Override
    public String getFormatKey() {
        return "odp";
    }

    @Override
    public String generateFilename(ContentProvider contentProvider) {
        return contentProvider.getPresentationName();
    }
}
