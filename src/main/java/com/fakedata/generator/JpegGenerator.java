package com.fakedata.generator;

import com.fakedata.content.ContentProvider;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

/**
 * Generates JPEG files with business graphics (charts, diagrams).
 */
public class JpegGenerator implements FileGenerator {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final Color[] CHART_COLORS = {
            new Color(66, 133, 244),   // Blue
            new Color(219, 68, 55),    // Red
            new Color(244, 180, 0),    // Yellow
            new Color(15, 157, 88),    // Green
            new Color(171, 71, 188),   // Purple
            new Color(255, 112, 67)    // Orange
    };

    private final Random random = new Random();

    @Override
    public GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException {
        String fullFilename = filename + "." + getExtension();
        Path filePath = outputDir.resolve(fullFilename);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw title
        g2d.setColor(new Color(51, 51, 51));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 28));
        String title = contentProvider.getCompanyName() + " - " + contentProvider.getQuarter() + " " + contentProvider.getYear();
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, 50);

        // Subtitle
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String subtitle = contentProvider.getImageName().replace("_", " ");
        fm = g2d.getFontMetrics();
        g2d.setColor(new Color(102, 102, 102));
        g2d.drawString(subtitle, (WIDTH - fm.stringWidth(subtitle)) / 2, 80);

        // Decide chart type randomly
        int chartType = random.nextInt(3);
        switch (chartType) {
            case 0 -> drawBarChart(g2d, contentProvider);
            case 1 -> drawPieChart(g2d, contentProvider);
            case 2 -> drawLineChart(g2d, contentProvider);
        }

        // Draw footer
        g2d.setColor(new Color(153, 153, 153));
        g2d.setFont(new Font("SansSerif", Font.ITALIC, 12));
        String footer = "Confidential - " + contentProvider.getCompanyName();
        g2d.drawString(footer, 50, HEIGHT - 30);

        g2d.dispose();

        ImageIO.write(image, "jpeg", filePath.toFile());

        return new GeneratedFile(filePath, fullFilename);
    }

    private void drawBarChart(Graphics2D g2d, ContentProvider contentProvider) {
        int chartX = 150;
        int chartY = 120;
        int chartWidth = WIDTH - 300;
        int chartHeight = HEIGHT - 250;

        // Draw axes
        g2d.setColor(new Color(51, 51, 51));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(chartX, chartY + chartHeight, chartX + chartWidth, chartY + chartHeight); // X-axis
        g2d.drawLine(chartX, chartY, chartX, chartY + chartHeight); // Y-axis

        // Generate data
        String[] categories = contentProvider.getExpenseCategories();
        int numBars = Math.min(6, categories.length);
        double[] values = new double[numBars];
        double maxValue = 0;
        for (int i = 0; i < numBars; i++) {
            values[i] = contentProvider.getAmount(10000, 100000);
            maxValue = Math.max(maxValue, values[i]);
        }

        // Draw bars
        int barWidth = (chartWidth - 100) / numBars;
        int gap = 20;
        for (int i = 0; i < numBars; i++) {
            int barHeight = (int) ((values[i] / maxValue) * (chartHeight - 50));
            int x = chartX + 50 + i * (barWidth + gap);
            int y = chartY + chartHeight - barHeight;

            g2d.setColor(CHART_COLORS[i % CHART_COLORS.length]);
            g2d.fill(new Rectangle2D.Double(x, y, barWidth - gap, barHeight));

            // Draw value on top
            g2d.setColor(new Color(51, 51, 51));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
            String value = String.format("$%.0fK", values[i] / 1000);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(value, x + (barWidth - gap - fm.stringWidth(value)) / 2, y - 5);

            // Draw label below
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
            fm = g2d.getFontMetrics();
            String label = categories[i].length() > 10 ? categories[i].substring(0, 10) + "..." : categories[i];
            g2d.drawString(label, x + (barWidth - gap - fm.stringWidth(label)) / 2, chartY + chartHeight + 20);
        }

        // Y-axis labels
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        for (int i = 0; i <= 5; i++) {
            double val = maxValue * i / 5;
            int y = chartY + chartHeight - (int) ((val / maxValue) * (chartHeight - 50));
            g2d.drawString(String.format("$%.0fK", val / 1000), chartX - 60, y + 5);
            g2d.setColor(new Color(220, 220, 220));
            g2d.drawLine(chartX, y, chartX + chartWidth, y);
            g2d.setColor(new Color(51, 51, 51));
        }
    }

    private void drawPieChart(Graphics2D g2d, ContentProvider contentProvider) {
        int centerX = WIDTH / 2 - 100;
        int centerY = HEIGHT / 2 + 20;
        int radius = 200;

        // Generate data
        String[] categories = contentProvider.getRevenueStreams();
        int numSlices = Math.min(5, categories.length);
        double[] values = new double[numSlices];
        double total = 0;
        for (int i = 0; i < numSlices; i++) {
            values[i] = contentProvider.getAmount(10, 100);
            total += values[i];
        }

        // Draw pie slices
        double startAngle = 0;
        for (int i = 0; i < numSlices; i++) {
            double angle = (values[i] / total) * 360;
            g2d.setColor(CHART_COLORS[i % CHART_COLORS.length]);
            g2d.fill(new Arc2D.Double(centerX - radius, centerY - radius,
                    radius * 2, radius * 2, startAngle, angle, Arc2D.PIE));
            startAngle += angle;
        }

        // Draw legend
        int legendX = centerX + radius + 80;
        int legendY = centerY - (numSlices * 30) / 2;
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
        for (int i = 0; i < numSlices; i++) {
            g2d.setColor(CHART_COLORS[i % CHART_COLORS.length]);
            g2d.fillRect(legendX, legendY + i * 30, 20, 20);
            g2d.setColor(new Color(51, 51, 51));
            String label = String.format("%s (%.1f%%)", categories[i], (values[i] / total) * 100);
            g2d.drawString(label, legendX + 30, legendY + i * 30 + 15);
        }
    }

    private void drawLineChart(Graphics2D g2d, ContentProvider contentProvider) {
        int chartX = 150;
        int chartY = 120;
        int chartWidth = WIDTH - 300;
        int chartHeight = HEIGHT - 250;

        // Draw axes
        g2d.setColor(new Color(51, 51, 51));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(chartX, chartY + chartHeight, chartX + chartWidth, chartY + chartHeight);
        g2d.drawLine(chartX, chartY, chartX, chartY + chartHeight);

        // Generate data for 2 lines
        int numPoints = 12;
        double[] line1 = new double[numPoints];
        double[] line2 = new double[numPoints];
        double maxValue = 0;
        double base1 = contentProvider.getAmount(50, 100);
        double base2 = contentProvider.getAmount(30, 80);
        for (int i = 0; i < numPoints; i++) {
            line1[i] = base1 + contentProvider.getAmount(-10, 20) + i * 3;
            line2[i] = base2 + contentProvider.getAmount(-10, 15) + i * 2;
            maxValue = Math.max(maxValue, Math.max(line1[i], line2[i]));
        }

        // Draw grid lines
        g2d.setColor(new Color(220, 220, 220));
        g2d.setStroke(new BasicStroke(1));
        for (int i = 1; i <= 5; i++) {
            int y = chartY + chartHeight - (int) ((i / 5.0) * (chartHeight - 50));
            g2d.drawLine(chartX, y, chartX + chartWidth, y);
        }

        // Draw lines
        int pointGap = chartWidth / (numPoints - 1);
        g2d.setStroke(new BasicStroke(3));

        // Line 1
        g2d.setColor(CHART_COLORS[0]);
        int[] x1 = new int[numPoints];
        int[] y1 = new int[numPoints];
        for (int i = 0; i < numPoints; i++) {
            x1[i] = chartX + i * pointGap;
            y1[i] = chartY + chartHeight - (int) ((line1[i] / maxValue) * (chartHeight - 50));
        }
        g2d.drawPolyline(x1, y1, numPoints);

        // Line 2
        g2d.setColor(CHART_COLORS[1]);
        int[] x2 = new int[numPoints];
        int[] y2 = new int[numPoints];
        for (int i = 0; i < numPoints; i++) {
            x2[i] = chartX + i * pointGap;
            y2[i] = chartY + chartHeight - (int) ((line2[i] / maxValue) * (chartHeight - 50));
        }
        g2d.drawPolyline(x2, y2, numPoints);

        // Draw points
        for (int i = 0; i < numPoints; i++) {
            g2d.setColor(CHART_COLORS[0]);
            g2d.fillOval(x1[i] - 5, y1[i] - 5, 10, 10);
            g2d.setColor(CHART_COLORS[1]);
            g2d.fillOval(x2[i] - 5, y2[i] - 5, 10, 10);
        }

        // X-axis labels (months)
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        g2d.setColor(new Color(51, 51, 51));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
        for (int i = 0; i < numPoints; i++) {
            g2d.drawString(months[i], chartX + i * pointGap - 10, chartY + chartHeight + 20);
        }

        // Legend
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.setColor(CHART_COLORS[0]);
        g2d.fillRect(chartX + chartWidth - 200, chartY + 10, 15, 15);
        g2d.setColor(new Color(51, 51, 51));
        g2d.drawString("Revenue", chartX + chartWidth - 180, chartY + 22);

        g2d.setColor(CHART_COLORS[1]);
        g2d.fillRect(chartX + chartWidth - 200, chartY + 35, 15, 15);
        g2d.setColor(new Color(51, 51, 51));
        g2d.drawString("Expenses", chartX + chartWidth - 180, chartY + 47);
    }

    @Override
    public String getExtension() {
        return "jpeg";
    }

    @Override
    public String getFormatKey() {
        return "jpeg";
    }

    @Override
    public String generateFilename(ContentProvider contentProvider) {
        return contentProvider.getImageName();
    }
}
