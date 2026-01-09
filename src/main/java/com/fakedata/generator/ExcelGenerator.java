package com.fakedata.generator;

import com.fakedata.content.ContentProvider;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates XLSX files with realistic financial data.
 */
public class ExcelGenerator implements FileGenerator {

    private final boolean useLegacyFormat;

    public ExcelGenerator() {
        this(false);
    }

    public ExcelGenerator(boolean useLegacyFormat) {
        this.useLegacyFormat = useLegacyFormat;
    }

    @Override
    public GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException {
        String fullFilename = filename + "." + getExtension();
        Path filePath = outputDir.resolve(fullFilename);

        try (Workbook workbook = new XSSFWorkbook()) {
            createSummarySheet(workbook, contentProvider);
            createRevenueSheet(workbook, contentProvider);
            createExpenseSheet(workbook, contentProvider);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                workbook.write(fos);
            }
        }

        return new GeneratedFile(filePath, fullFilename);
    }

    private void createSummarySheet(Workbook workbook, ContentProvider contentProvider) {
        Sheet sheet = workbook.createSheet("Summary");

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        CellStyle percentStyle = createPercentStyle(workbook);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(contentProvider.getCompanyName() + " - Financial Summary " + contentProvider.getYear());
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        rowNum++;

        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = contentProvider.getFinancialHeaders();
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Revenue data
        String[] categories = contentProvider.getRevenueStreams();
        double[][] data = contentProvider.generateFinancialData(categories.length);
        for (int i = 0; i < categories.length; i++) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(categories[i]);
            for (int j = 0; j < 5; j++) {
                Cell cell = row.createCell(j + 1);
                cell.setCellValue(data[i][j]);
                cell.setCellStyle(currencyStyle);
            }
            Cell growthCell = row.createCell(6);
            growthCell.setCellValue(data[i][5] / 100);
            growthCell.setCellStyle(percentStyle);
        }

        // Total row
        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL");
        totalLabel.setCellStyle(headerStyle);

        // Sum formulas
        int dataStartRow = 4; // 0-indexed
        int dataEndRow = dataStartRow + categories.length - 1;
        for (int col = 1; col <= 5; col++) {
            Cell cell = totalRow.createCell(col);
            String colLetter = String.valueOf((char) ('A' + col));
            cell.setCellFormula(String.format("SUM(%s%d:%s%d)", colLetter, dataStartRow, colLetter, dataEndRow));
            cell.setCellStyle(currencyStyle);
        }

        // Auto-size columns
        for (int i = 0; i < 7; i++) {
            sheet.setColumnWidth(i, 4000);
        }
    }

    private void createRevenueSheet(Workbook workbook, ContentProvider contentProvider) {
        Sheet sheet = workbook.createSheet("Revenue Details");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // Headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Product/Service", "Region", "Q1", "Q2", "Q3", "Q4", "Total"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        String[] products = {"Enterprise License", "Professional License", "Basic License", "Support Contract", "Consulting", "Training"};
        String[] regions = {"North America", "Europe", "Asia Pacific", "Latin America"};

        for (String product : products) {
            for (String region : regions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product);
                row.createCell(1).setCellValue(region);

                double q1 = contentProvider.getAmount(10000, 50000);
                double q2 = contentProvider.getAmount(10000, 50000);
                double q3 = contentProvider.getAmount(10000, 50000);
                double q4 = contentProvider.getAmount(10000, 50000);

                Cell c2 = row.createCell(2);
                c2.setCellValue(q1);
                c2.setCellStyle(currencyStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(q2);
                c3.setCellStyle(currencyStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(q3);
                c4.setCellStyle(currencyStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(q4);
                c5.setCellStyle(currencyStyle);

                Cell totalCell = row.createCell(6);
                totalCell.setCellFormula(String.format("SUM(C%d:F%d)", rowNum, rowNum));
                totalCell.setCellStyle(currencyStyle);
            }
        }

        for (int i = 0; i < 7; i++) {
            sheet.setColumnWidth(i, 4500);
        }
    }

    private void createExpenseSheet(Workbook workbook, ContentProvider contentProvider) {
        Sheet sheet = workbook.createSheet("Expenses");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Category", "Department", "Budget", "Actual", "Variance", "Status"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        String[] categories = contentProvider.getExpenseCategories();
        String[] departments = {"Finance", "Marketing", "Sales", "Operations", "IT"};

        for (String category : categories) {
            for (int i = 0; i < 2; i++) { // 2 departments per category
                String dept = departments[(int) (Math.random() * departments.length)];
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(category);
                row.createCell(1).setCellValue(dept);

                double budget = contentProvider.getAmount(20000, 80000);
                double actual = budget * (0.85 + Math.random() * 0.3);

                Cell budgetCell = row.createCell(2);
                budgetCell.setCellValue(budget);
                budgetCell.setCellStyle(currencyStyle);

                Cell actualCell = row.createCell(3);
                actualCell.setCellValue(actual);
                actualCell.setCellStyle(currencyStyle);

                Cell varianceCell = row.createCell(4);
                varianceCell.setCellFormula(String.format("C%d-D%d", rowNum, rowNum));
                varianceCell.setCellStyle(currencyStyle);

                Cell statusCell = row.createCell(5);
                statusCell.setCellFormula(String.format("IF(E%d>=0,\"Under Budget\",\"Over Budget\")", rowNum));
            }
        }

        for (int i = 0; i < 6; i++) {
            sheet.setColumnWidth(i, 4000);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.0%"));
        return style;
    }

    @Override
    public String getExtension() {
        return useLegacyFormat ? "xls" : "xlsx";
    }

    @Override
    public String getFormatKey() {
        return useLegacyFormat ? "xls" : "xlsx";
    }

    @Override
    public String generateFilename(ContentProvider contentProvider) {
        return contentProvider.getSpreadsheetName();
    }
}
