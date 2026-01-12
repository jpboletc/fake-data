package com.fakedata.generator;

import com.fakedata.content.ContentProvider;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Generates DOCX files with realistic business document content.
 */
public class DocxGenerator implements FileGenerator {

    @Override
    public GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException {
        String fullFilename = filename + "." + getExtension();
        Path filePath = outputDir.resolve(fullFilename);

        try (XWPFDocument document = new XWPFDocument()) {
            // Title
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(24);
            titleRun.setText(contentProvider.getDocumentName().replace("_", " "));
            titleRun.addBreak();

            // Subtitle with company name
            XWPFParagraph subtitlePara = document.createParagraph();
            subtitlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subtitleRun = subtitlePara.createRun();
            subtitleRun.setFontSize(14);
            subtitleRun.setColor("666666");
            subtitleRun.setText(contentProvider.getCompanyName());
            subtitleRun.addBreak();

            // Document metadata
            XWPFParagraph metaPara = document.createParagraph();
            metaPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun metaRun = metaPara.createRun();
            metaRun.setFontSize(11);
            metaRun.setColor("999999");
            metaRun.setText("Prepared by: " + contentProvider.getFullName());
            metaRun.addBreak();
            metaRun.setText("Department: " + contentProvider.getDepartment());
            metaRun.addBreak();
            metaRun.setText("Date: " + LocalDate.now().toString());
            metaRun.addBreak();
            metaRun.addBreak();

            // Executive Summary section
            addSectionHeader(document, "Executive Summary");
            addParagraph(document, contentProvider.getExecutiveSummary());

            // Background section
            addSectionHeader(document, "Background");
            addParagraph(document, contentProvider.getParagraph());
            addParagraph(document, contentProvider.getParagraph());

            // Key Points section
            addSectionHeader(document, "Key Points");
            List<String> keyPoints = contentProvider.getBulletPoints(5);
            for (String point : keyPoints) {
                addBulletPoint(document, point);
            }

            // Analysis section
            addSectionHeader(document, "Analysis");
            addParagraph(document, contentProvider.getParagraph());

            // Create a simple table
            addSectionHeader(document, "Summary Data");
            XWPFTable table = document.createTable(5, 3);
            table.setWidth("100%");

            // Header row
            XWPFTableRow headerRow = table.getRow(0);
            setCellText(headerRow.getCell(0), "Item", true);
            setCellText(headerRow.getCell(1), "Status", true);
            setCellText(headerRow.getCell(2), "Priority", true);

            // Data rows
            String[] items = {"Project Timeline", "Resource Allocation", "Budget Review", "Risk Assessment"};
            String[] statuses = {"Complete", "In Progress", "Pending", "Under Review"};
            String[] priorities = {"High", "Medium", "Low", "Critical"};

            for (int i = 0; i < 4; i++) {
                XWPFTableRow row = table.getRow(i + 1);
                setCellText(row.getCell(0), items[i], false);
                setCellText(row.getCell(1), statuses[(int) (Math.random() * statuses.length)], false);
                setCellText(row.getCell(2), priorities[(int) (Math.random() * priorities.length)], false);
            }

            // Recommendations section
            addSectionHeader(document, "Recommendations");
            List<String> recommendations = contentProvider.getBulletPoints(4);
            int recNum = 1;
            for (String rec : recommendations) {
                addNumberedPoint(document, recNum++, rec);
            }

            // Next Steps section
            addSectionHeader(document, "Next Steps");
            addParagraph(document, contentProvider.getParagraph());

            // Footer
            XWPFParagraph footerPara = document.createParagraph();
            footerPara.setAlignment(ParagraphAlignment.CENTER);
            footerPara.setSpacingBefore(400);
            XWPFRun footerRun = footerPara.createRun();
            footerRun.setItalic(true);
            footerRun.setFontSize(10);
            footerRun.setColor("999999");
            footerRun.setText("Confidential - " + contentProvider.getCompanyName() + " - " + LocalDate.now().getYear());

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                document.write(fos);
            }
        }

        return new GeneratedFile(filePath, fullFilename);
    }

    private void addSectionHeader(XWPFDocument document, String text) {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingBefore(200);
        para.setSpacingAfter(100);
        XWPFRun run = para.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setColor("333333");
        run.setText(text);
    }

    private void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingAfter(100);
        XWPFRun run = para.createRun();
        run.setFontSize(11);
        run.setText(text);
    }

    private void addBulletPoint(XWPFDocument document, String text) {
        XWPFParagraph para = document.createParagraph();
        para.setIndentationLeft(720); // 0.5 inch
        XWPFRun bulletRun = para.createRun();
        bulletRun.setText("\u2022 ");
        XWPFRun textRun = para.createRun();
        textRun.setFontSize(11);
        textRun.setText(text);
    }

    private void addNumberedPoint(XWPFDocument document, int number, String text) {
        XWPFParagraph para = document.createParagraph();
        para.setIndentationLeft(720);
        XWPFRun numRun = para.createRun();
        numRun.setBold(true);
        numRun.setText(number + ". ");
        XWPFRun textRun = para.createRun();
        textRun.setFontSize(11);
        textRun.setText(text);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean isHeader) {
        XWPFParagraph para = cell.getParagraphs().get(0);
        XWPFRun run = para.createRun();
        run.setText(text);
        if (isHeader) {
            run.setBold(true);
            cell.setColor("E0E0E0");
        }
    }

    @Override
    public String getExtension() {
        return "docx";
    }

    @Override
    public String getFormatKey() {
        return "docx";
    }

    @Override
    public String generateFilename(ContentProvider contentProvider) {
        return contentProvider.getDocumentName();
    }
}
