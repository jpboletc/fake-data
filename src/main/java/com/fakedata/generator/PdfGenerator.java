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

import static com.fakedata.generator.GeneratorColors.*;

/**
 * Generates PDF files with realistic business report content.
 */
public class PdfGenerator extends AbstractFileGenerator {

    private static final String[] SECTION_TITLES = {
            "Detailed Analysis", "Market Assessment", "Operational Review",
            "Strategic Initiatives", "Performance Metrics", "Risk Analysis",
            "Growth Opportunities", "Competitive Landscape", "Resource Allocation",
            "Stakeholder Impact", "Process Improvements", "Future Outlook",
            "Regional Breakdown", "Department Performance", "Technology Assessment",
            "Compliance Review", "Quality Assurance", "Cost Analysis",
            "Revenue Drivers", "Implementation Progress"
    };

    @Override
    protected void doGenerate(Path filePath, ContentProvider contentProvider) throws IOException {
        doGenerate(filePath, contentProvider, 0);
    }

    @Override
    protected void doGenerate(Path filePath, ContentProvider contentProvider, int targetPages) throws IOException {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath.toFile()));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, TEXT_DARK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, ACCENT_BLUE);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font tableCellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // === Page 1: Title page & Executive Summary ===

            // Add title
            Paragraph title = new Paragraph(contentProvider.getReportTitle(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Add company info
            Paragraph companyInfo = new Paragraph();
            companyInfo.add(new Chunk("Prepared by: ", boldFont));
            companyInfo.add(new Chunk(contentProvider.getFullName() + "\n", normalFont));
            companyInfo.add(new Chunk("Department: ", boldFont));
            companyInfo.add(new Chunk(contentProvider.getDepartment() + "\n", normalFont));
            companyInfo.add(new Chunk("Date: ", boldFont));
            companyInfo.add(new Chunk(java.time.LocalDate.now().toString() + "\n\n", normalFont));
            document.add(companyInfo);

            // Executive Summary section
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
            addFinancialTable(document, contentProvider, sectionFont, tableHeaderFont, tableCellFont, "Financial Overview");

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

            // === Additional pages to reach target ===
            if (targetPages > 0) {
                int sectionIndex = 0;
                while (writer.getPageNumber() < targetPages) {
                    // Pick a section title (cycle through available titles)
                    String sectionTitle = SECTION_TITLES[sectionIndex % SECTION_TITLES.length];
                    sectionIndex++;

                    // Add section header
                    Paragraph secHeader = new Paragraph(sectionTitle, sectionFont);
                    secHeader.setSpacingBefore(25);
                    secHeader.setSpacingAfter(10);
                    document.add(secHeader);

                    // Add 2-4 paragraphs
                    int paraCount = 2 + (sectionIndex % 3);
                    for (int i = 0; i < paraCount; i++) {
                        Paragraph para = new Paragraph(contentProvider.getParagraph(), normalFont);
                        para.setSpacingAfter(10);
                        document.add(para);
                    }

                    // Every other section, add a table
                    if (sectionIndex % 2 == 0) {
                        addFinancialTable(document, contentProvider, sectionFont, tableHeaderFont, tableCellFont, null);
                    }

                    // Every third section, add bullet points
                    if (sectionIndex % 3 == 0) {
                        Paragraph subHeader = new Paragraph("Key Findings", sectionFont);
                        subHeader.setSpacingBefore(10);
                        subHeader.setSpacingAfter(8);
                        document.add(subHeader);

                        List<String> points = contentProvider.getBulletPoints(5);
                        com.lowagie.text.List bl = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
                        bl.setListSymbol("\u2022 ");
                        for (String point : points) {
                            bl.add(new ListItem(point, normalFont));
                        }
                        document.add(bl);
                    }
                }
            }

            // Footer
            Paragraph footer = new Paragraph("\n\nConfidential - " + contentProvider.getCompanyName(),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, TEXT_LIGHT));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

        } catch (DocumentException e) {
            throw new IOException("Failed to create PDF document", e);
        } finally {
            document.close();
        }
    }

    /**
     * Adds a styled financial table to the document.
     */
    private void addFinancialTable(Document document, ContentProvider contentProvider,
                                   Font sectionFont, Font tableHeaderFont, Font tableCellFont,
                                   String headerTitle) throws DocumentException {
        if (headerTitle != null) {
            Paragraph financialHeader = new Paragraph(headerTitle, sectionFont);
            financialHeader.setSpacingBefore(20);
            financialHeader.setSpacingAfter(10);
            document.add(financialHeader);
        }

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);

        float[] columnWidths = {2f, 1.5f, 1.5f, 1f};
        table.setWidths(columnWidths);

        String[] headers = {"Metric", "Current", "Previous", "Change"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
            cell.setBackgroundColor(ACCENT_BLUE);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        String[] metrics = {"Revenue", "Expenses", "Profit", "Growth Rate", "Market Share", "Customer Retention"};
        boolean altRow = false;
        for (String metric : metrics) {
            Color rowBg = altRow ? BG_ALT_ROW : Color.WHITE;

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
            Color changeColor = change >= 0 ? STATUS_POSITIVE : STATUS_NEGATIVE;
            Font changeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, changeColor);
            PdfPCell changeCell = new PdfPCell(new Phrase(String.format("%+.1f%%", change), changeFont));
            changeCell.setBackgroundColor(rowBg);
            changeCell.setPadding(6);
            changeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(changeCell);

            altRow = !altRow;
        }
        document.add(table);
    }

    @Override
    public String getExtension() {
        return "pdf";
    }

    @Override
    public String generateFilename(ContentProvider contentProvider) {
        return contentProvider.getPdfName();
    }
}
