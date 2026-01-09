package com.fakedata.generator;

import com.fakedata.content.ContentProvider;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates PDF files with realistic business report content.
 */
public class PdfGenerator implements FileGenerator {

    private static final Color HEADER_BG = new Color(66, 133, 244);
    private static final Color HEADER_TEXT = Color.WHITE;
    private static final Color ALT_ROW_BG = new Color(245, 245, 245);

    @Override
    public GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException {
        String fullFilename = filename + "." + getExtension();
        Path filePath = outputDir.resolve(fullFilename);

        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath.toFile()));
            document.open();

            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(51, 51, 51));
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
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(66, 133, 244));
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

            // Financial Overview section with table
            Paragraph financialHeader = new Paragraph("Financial Overview", sectionFont);
            financialHeader.setSpacingBefore(20);
            financialHeader.setSpacingAfter(10);
            document.add(financialHeader);

            // Create a styled table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(15);

            // Set column widths
            float[] columnWidths = {2f, 1.5f, 1.5f, 1f};
            table.setWidths(columnWidths);

            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, HEADER_TEXT);
            Font tableCellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Header row
            String[] headers = {"Metric", "Current", "Previous", "Change"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
                cell.setBackgroundColor(HEADER_BG);
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data rows
            String[] metrics = {"Revenue", "Expenses", "Profit", "Growth Rate", "Market Share", "Customer Retention"};
            boolean altRow = false;
            for (String metric : metrics) {
                Color rowBg = altRow ? ALT_ROW_BG : Color.WHITE;

                PdfPCell metricCell = new PdfPCell(new Phrase(metric, tableCellFont));
                metricCell.setBackgroundColor(rowBg);
                metricCell.setPadding(6);
                table.addCell(metricCell);

                PdfPCell currentCell = new PdfPCell(new Phrase(String.format("$%.2fM", contentProvider.getAmount(1, 10)), tableCellFont));
                currentCell.setBackgroundColor(rowBg);
                currentCell.setPadding(6);
                currentCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(currentCell);

                PdfPCell prevCell = new PdfPCell(new Phrase(String.format("$%.2fM", contentProvider.getAmount(1, 10)), tableCellFont));
                prevCell.setBackgroundColor(rowBg);
                prevCell.setPadding(6);
                prevCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(prevCell);

                double change = contentProvider.getAmount(-15, 25);
                Color changeColor = change >= 0 ? new Color(15, 157, 88) : new Color(219, 68, 55);
                Font changeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, changeColor);
                PdfPCell changeCell = new PdfPCell(new Phrase(String.format("%+.1f%%", change), changeFont));
                changeCell.setBackgroundColor(rowBg);
                changeCell.setPadding(6);
                changeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(changeCell);

                altRow = !altRow;
            }
            document.add(table);

            // Recommendations section
            Paragraph recsHeader = new Paragraph("Recommendations", sectionFont);
            recsHeader.setSpacingBefore(10);
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
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, new Color(153, 153, 153)));
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
