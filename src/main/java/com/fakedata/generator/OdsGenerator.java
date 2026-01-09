package com.fakedata.generator;

import com.fakedata.content.ContentProvider;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Table;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates ODS (OpenDocument Spreadsheet) files with realistic financial data.
 */
public class OdsGenerator implements FileGenerator {

    @Override
    public GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException {
        String fullFilename = filename + "." + getExtension();
        Path filePath = outputDir.resolve(fullFilename);

        try {
            SpreadsheetDocument document = SpreadsheetDocument.newSpreadsheetDocument();

            // Remove default sheet and create our own
            Table sheet = document.getSheetByIndex(0);
            sheet.setTableName("Financial Summary");

            // Title
            Cell titleCell = sheet.getCellByPosition(0, 0);
            titleCell.setStringValue(contentProvider.getCompanyName() + " - Financial Summary " + contentProvider.getYear());
            titleCell.setFont(new org.odftoolkit.simple.style.Font("Arial", org.odftoolkit.simple.style.StyleTypeDefinitions.FontStyle.BOLD, 16));

            // Headers
            String[] headers = contentProvider.getFinancialHeaders();
            for (int i = 0; i < headers.length; i++) {
                Cell cell = sheet.getCellByPosition(i, 2);
                cell.setStringValue(headers[i]);
                cell.setFont(new org.odftoolkit.simple.style.Font("Arial", org.odftoolkit.simple.style.StyleTypeDefinitions.FontStyle.BOLD, 11));
            }

            // Revenue data
            String[] categories = contentProvider.getRevenueStreams();
            double[][] data = contentProvider.generateFinancialData(categories.length);

            for (int i = 0; i < categories.length; i++) {
                int row = 3 + i;
                sheet.getCellByPosition(0, row).setStringValue(categories[i]);

                for (int j = 0; j < 5; j++) {
                    Cell cell = sheet.getCellByPosition(j + 1, row);
                    cell.setDoubleValue(data[i][j]);
                    cell.setFormatString("$#,##0.00");
                }

                Cell growthCell = sheet.getCellByPosition(6, row);
                growthCell.setPercentageValue(data[i][5] / 100);
            }

            // Add totals row
            int totalRow = 3 + categories.length + 1;
            Cell totalLabel = sheet.getCellByPosition(0, totalRow);
            totalLabel.setStringValue("TOTAL");
            totalLabel.setFont(new org.odftoolkit.simple.style.Font("Arial", org.odftoolkit.simple.style.StyleTypeDefinitions.FontStyle.BOLD, 11));

            // Sum formulas
            for (int col = 1; col <= 5; col++) {
                Cell cell = sheet.getCellByPosition(col, totalRow);
                String colLetter = String.valueOf((char) ('A' + col));
                cell.setFormula("=SUM(" + colLetter + "4:" + colLetter + (3 + categories.length) + ")");
                cell.setFormatString("$#,##0.00");
            }

            // Create expense sheet
            Table expenseSheet = document.appendSheet("Expenses");
            String[] expenseHeaders = {"Category", "Budget", "Actual", "Variance"};
            for (int i = 0; i < expenseHeaders.length; i++) {
                Cell cell = expenseSheet.getCellByPosition(i, 0);
                cell.setStringValue(expenseHeaders[i]);
                cell.setFont(new org.odftoolkit.simple.style.Font("Arial", org.odftoolkit.simple.style.StyleTypeDefinitions.FontStyle.BOLD, 11));
            }

            String[] expenseCategories = contentProvider.getExpenseCategories();
            for (int i = 0; i < expenseCategories.length; i++) {
                int row = 1 + i;
                expenseSheet.getCellByPosition(0, row).setStringValue(expenseCategories[i]);

                double budget = contentProvider.getAmount(20000, 80000);
                double actual = budget * (0.85 + Math.random() * 0.3);

                Cell budgetCell = expenseSheet.getCellByPosition(1, row);
                budgetCell.setDoubleValue(budget);
                budgetCell.setFormatString("$#,##0.00");

                Cell actualCell = expenseSheet.getCellByPosition(2, row);
                actualCell.setDoubleValue(actual);
                actualCell.setFormatString("$#,##0.00");

                Cell varianceCell = expenseSheet.getCellByPosition(3, row);
                varianceCell.setFormula("=B" + (row + 1) + "-C" + (row + 1));
                varianceCell.setFormatString("$#,##0.00");
            }

            document.save(filePath.toFile());
            document.close();

        } catch (Exception e) {
            throw new IOException("Failed to create ODS document", e);
        }

        return new GeneratedFile(filePath, fullFilename);
    }

    @Override
    public String getExtension() {
        return "ods";
    }

    @Override
    public String getFormatKey() {
        return "ods";
    }

    @Override
    public String generateFilename(ContentProvider contentProvider) {
        return contentProvider.getSpreadsheetName();
    }
}
