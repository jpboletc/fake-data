package com.fakedata.generator;

import com.fakedata.content.ContentProvider;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates PDF files with realistic business report content.
 */
public class PdfGenerator implements FileGenerator {

    @Override
    public GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException {
        String fullFilename = filename + "." + getExtension();
        Path filePath = outputDir.resolve(fullFilename);

        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath.toFile()));
            document.open();

            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24);
            Paragraph title = new Paragraph(contentProvider.getReportTitle(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Add company info
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            Paragraph companyInfo = new Paragraph();
            companyInfo.add(new Chunk("Prepared by: ", boldFont));
            companyInfo.add(new Chunk(contentProvider.getFullName() + "\n", normalFont));
            companyInfo.add(new Chunk("Department: ", boldFont));
            companyInfo.add(new Chunk(contentProvider.getDepartment() + "\n", normalFont));
            companyInfo.add(new Chunk("Date: ", boldFont));
            companyInfo.add(new Chunk(java.time.LocalDate.now().toString() + "\n\n", normalFont));
            document.add(companyInfo);

            // Executive Summary section
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph summaryHeader = new Paragraph("Executive Summary", sectionFont);
            summaryHeader.setSpacingBefore(15);
            summaryHeader.setSpacingAfter(10);
            document.add(summaryHeader);

            Paragraph summary = new Paragraph(contentProvider.getExecutiveSummary(), normalFont);
            summary.setSpacingAfter(15);
            document.add(summary);

            // Key Highlights section
            Paragraph highlightsHeader = new Paragraph("Key Highlights", sectionFont);
            highlightsHeader.setSpacingBefore(15);
            highlightsHeader.setSpacingAfter(10);
            document.add(highlightsHeader);

            List<String> highlights = contentProvider.getBulletPoints(5);
            com.lowagie.text.List bulletList = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
            bulletList.setListSymbol("\u2022 ");
            for (String highlight : highlights) {
                bulletList.add(new ListItem(highlight, normalFont));
            }
            document.add(bulletList);

            // Analysis section
            Paragraph analysisHeader = new Paragraph("Detailed Analysis", sectionFont);
            analysisHeader.setSpacingBefore(20);
            analysisHeader.setSpacingAfter(10);
            document.add(analysisHeader);

            for (int i = 0; i < 3; i++) {
                Paragraph para = new Paragraph(contentProvider.getParagraph(), normalFont);
                para.setSpacingAfter(10);
                document.add(para);
            }

            // Financial Overview section
            Paragraph financialHeader = new Paragraph("Financial Overview", sectionFont);
            financialHeader.setSpacingBefore(20);
            financialHeader.setSpacingAfter(10);
            document.add(financialHeader);

            // Create a simple table
            Table table = new Table(4);
            table.setWidth(100);
            table.setPadding(5);

            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font tableCellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            String[] headers = {"Metric", "Current", "Previous", "Change"};
            for (String header : headers) {
                Cell cell = new Cell(new Phrase(header, tableHeaderFont));
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            String[] metrics = {"Revenue", "Expenses", "Profit", "Growth Rate"};
            for (String metric : metrics) {
                table.addCell(new Phrase(metric, tableCellFont));
                table.addCell(new Phrase(String.format("$%.2fM", contentProvider.getAmount(1, 10)), tableCellFont));
                table.addCell(new Phrase(String.format("$%.2fM", contentProvider.getAmount(1, 10)), tableCellFont));
                double change = contentProvider.getAmount(-15, 25);
                table.addCell(new Phrase(String.format("%+.1f%%", change), tableCellFont));
            }
            document.add(table);

            // Recommendations section
            Paragraph recsHeader = new Paragraph("Recommendations", sectionFont);
            recsHeader.setSpacingBefore(20);
            recsHeader.setSpacingAfter(10);
            document.add(recsHeader);

            List<String> recommendations = contentProvider.getBulletPoints(4);
            com.lowagie.text.List recsList = new com.lowagie.text.List(com.lowagie.text.List.ORDERED);
            for (String rec : recommendations) {
                recsList.add(new ListItem(rec, normalFont));
            }
            document.add(recsList);

            // Footer
            Paragraph footer = new Paragraph("\n\nConfidential - " + contentProvider.getCompanyName(),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

        } catch (DocumentException e) {
            throw new IOException("Failed to create PDF document", e);
        } finally {
            document.close();
        }

        return new GeneratedFile(filePath, fullFilename);
    }

    @Override
    public String getExtension() {
        return "pdf";
    }

    @Override
    public String getFormatKey() {
        return "pdf";
    }

    @Override
    public String generateFilename(ContentProvider contentProvider) {
        return contentProvider.getPdfName();
    }
}
