package com.fakedata.generator;

import com.fakedata.content.ContentProvider;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.style.Font;
import org.odftoolkit.simple.style.StyleTypeDefinitions;
import org.odftoolkit.simple.table.Table;
import org.odftoolkit.simple.text.list.List;
import org.odftoolkit.simple.text.Paragraph;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Generates ODT (OpenDocument Text) files with realistic business document content.
 */
public class OdtGenerator implements FileGenerator {

    @Override
    public GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException {
        String fullFilename = filename + "." + getExtension();
        Path filePath = outputDir.resolve(fullFilename);

        try {
            TextDocument document = TextDocument.newTextDocument();

            // Title
            Paragraph titlePara = document.addParagraph(contentProvider.getDocumentName().replace("_", " "));
            titlePara.setFont(new Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 24));
            titlePara.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.CENTER);

            // Subtitle
            Paragraph subtitlePara = document.addParagraph(contentProvider.getCompanyName());
            subtitlePara.setFont(new Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 14));
            subtitlePara.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.CENTER);

            // Document metadata
            Paragraph metaPara = document.addParagraph(
                    "Prepared by: " + contentProvider.getFullName() + "\n" +
                            "Department: " + contentProvider.getDepartment() + "\n" +
                            "Date: " + LocalDate.now().toString()
            );
            metaPara.setFont(new Font("Arial", StyleTypeDefinitions.FontStyle.ITALIC, 11));
            metaPara.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.CENTER);

            // Add some space
            document.addParagraph("");

            // Executive Summary section
            addSectionHeader(document, "Executive Summary");
            addParagraph(document, contentProvider.getExecutiveSummary());

            // Background section
            addSectionHeader(document, "Background");
            addParagraph(document, contentProvider.getParagraph());
            addParagraph(document, contentProvider.getParagraph());

            // Key Points section
            addSectionHeader(document, "Key Points");
            java.util.List<String> keyPoints = contentProvider.getBulletPoints(5);
            List bulletList = document.addList();
            for (String point : keyPoints) {
                bulletList.addItem(point);
            }

            // Analysis section
            addSectionHeader(document, "Analysis");
            addParagraph(document, contentProvider.getParagraph());

            // Summary table
            addSectionHeader(document, "Summary Data");
            Table table = document.addTable(5, 3);

            // Header row
            table.getCellByPosition(0, 0).setStringValue("Item");
            table.getCellByPosition(1, 0).setStringValue("Status");
            table.getCellByPosition(2, 0).setStringValue("Priority");

            String[] items = {"Project Timeline", "Resource Allocation", "Budget Review", "Risk Assessment"};
            String[] statuses = {"Complete", "In Progress", "Pending", "Under Review"};
            String[] priorities = {"High", "Medium", "Low", "Critical"};

            for (int i = 0; i < 4; i++) {
                table.getCellByPosition(0, i + 1).setStringValue(items[i]);
                table.getCellByPosition(1, i + 1).setStringValue(statuses[(int) (Math.random() * statuses.length)]);
                table.getCellByPosition(2, i + 1).setStringValue(priorities[(int) (Math.random() * priorities.length)]);
            }

            // Recommendations section
            addSectionHeader(document, "Recommendations");
            java.util.List<String> recommendations = contentProvider.getBulletPoints(4);
            List numberedList = document.addList();
            for (String rec : recommendations) {
                numberedList.addItem(rec);
            }

            // Next Steps
            addSectionHeader(document, "Next Steps");
            addParagraph(document, contentProvider.getParagraph());

            // Footer
            document.addParagraph("");
            Paragraph footerPara = document.addParagraph("Confidential - " + contentProvider.getCompanyName() + " - " + LocalDate.now().getYear());
            footerPara.setFont(new Font("Arial", StyleTypeDefinitions.FontStyle.ITALIC, 10));
            footerPara.setHorizontalAlignment(StyleTypeDefinitions.HorizontalAlignmentType.CENTER);

            document.save(filePath.toFile());
            document.close();

        } catch (Exception e) {
            throw new IOException("Failed to create ODT document", e);
        }

        return new GeneratedFile(filePath, fullFilename);
    }

    private void addSectionHeader(TextDocument document, String text) {
        document.addParagraph("");
        Paragraph para = document.addParagraph(text);
        para.setFont(new Font("Arial", StyleTypeDefinitions.FontStyle.BOLD, 14));
    }

    private void addParagraph(TextDocument document, String text) {
        Paragraph para = document.addParagraph(text);
        para.setFont(new Font("Arial", StyleTypeDefinitions.FontStyle.REGULAR, 11));
    }

    @Override
    public String getExtension() {
        return "odt";
    }

    @Override
    public String getFormatKey() {
        return "odt";
    }

    @Override
    public String generateFilename(ContentProvider contentProvider) {
        return contentProvider.getDocumentName();
    }
}
