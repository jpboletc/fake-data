package com.fakedata.generator;

import com.fakedata.content.ContentProvider;
import org.apache.poi.xslf.usermodel.*;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Generates PPTX files with realistic presentation content.
 */
public class PptxGenerator implements FileGenerator {

    private static final Color TITLE_COLOR = new Color(51, 51, 51);
    private static final Color SUBTITLE_COLOR = new Color(102, 102, 102);
    private static final Color ACCENT_COLOR = new Color(66, 133, 244);

    @Override
    public GeneratedFile generate(Path outputDir, String filename, ContentProvider contentProvider) throws IOException {
        String fullFilename = filename + "." + getExtension();
        Path filePath = outputDir.resolve(fullFilename);

        try (XMLSlideShow ppt = new XMLSlideShow()) {
            // Title slide
            createTitleSlide(ppt, contentProvider);

            // Agenda slide
            createAgendaSlide(ppt, contentProvider);

            // Content slides
            for (int i = 0; i < 4; i++) {
                createContentSlide(ppt, contentProvider);
            }

            // Summary slide
            createSummarySlide(ppt, contentProvider);

            // Thank you slide
            createClosingSlide(ppt, contentProvider);

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                ppt.write(fos);
            }
        }

        return new GeneratedFile(filePath, fullFilename);
    }

    private void createTitleSlide(XMLSlideShow ppt, ContentProvider contentProvider) {
        XSLFSlide slide = ppt.createSlide();

        // Title
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(50, 150, 620, 100));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        titlePara.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText(contentProvider.getPresentationName().replace("_", " "));
        titleRun.setFontSize(40.0);
        titleRun.setBold(true);
        titleRun.setFontColor(TITLE_COLOR);

        // Subtitle (company name)
        XSLFTextBox subtitleBox = slide.createTextBox();
        subtitleBox.setAnchor(new Rectangle(50, 260, 620, 50));
        XSLFTextParagraph subtitlePara = subtitleBox.addNewTextParagraph();
        subtitlePara.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun subtitleRun = subtitlePara.addNewTextRun();
        subtitleRun.setText(contentProvider.getCompanyName());
        subtitleRun.setFontSize(24.0);
        subtitleRun.setFontColor(SUBTITLE_COLOR);

        // Date and presenter
        XSLFTextBox infoBox = slide.createTextBox();
        infoBox.setAnchor(new Rectangle(50, 350, 620, 80));
        XSLFTextParagraph infoPara = infoBox.addNewTextParagraph();
        infoPara.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun infoRun = infoPara.addNewTextRun();
        infoRun.setText(contentProvider.getFullName() + "\n" + contentProvider.getQuarter() + " " + contentProvider.getYear());
        infoRun.setFontSize(16.0);
        infoRun.setFontColor(SUBTITLE_COLOR);
    }

    private void createAgendaSlide(XMLSlideShow ppt, ContentProvider contentProvider) {
        XSLFSlide slide = ppt.createSlide();

        // Title
        addSlideTitle(slide, "Agenda");

        // Agenda items
        XSLFTextBox contentBox = slide.createTextBox();
        contentBox.setAnchor(new Rectangle(50, 100, 620, 350));

        String[] agendaItems = {"Executive Overview", "Market Analysis", "Key Metrics", "Strategic Initiatives", "Q&A"};
        for (int i = 0; i < agendaItems.length; i++) {
            XSLFTextParagraph para = contentBox.addNewTextParagraph();
            para.setIndentLevel(0);
            para.setBullet(true);
            XSLFTextRun run = para.addNewTextRun();
            run.setText(agendaItems[i]);
            run.setFontSize(20.0);
            run.setFontColor(TITLE_COLOR);
        }
    }

    private void createContentSlide(XMLSlideShow ppt, ContentProvider contentProvider) {
        XSLFSlide slide = ppt.createSlide();

        // Title
        String slideTitle = contentProvider.getSlideTitle();
        addSlideTitle(slide, slideTitle);

        // Content
        XSLFTextBox contentBox = slide.createTextBox();
        contentBox.setAnchor(new Rectangle(50, 100, 620, 350));

        List<String> bulletPoints = contentProvider.getSlideContent();
        for (String point : bulletPoints) {
            XSLFTextParagraph para = contentBox.addNewTextParagraph();
            para.setIndentLevel(0);
            para.setBullet(true);
            para.setSpaceBefore(10.0);
            XSLFTextRun run = para.addNewTextRun();
            run.setText(point);
            run.setFontSize(18.0);
            run.setFontColor(TITLE_COLOR);
        }

        // Add a sub-bullet for some items
        if (bulletPoints.size() > 2) {
            XSLFTextParagraph subPara = contentBox.addNewTextParagraph();
            subPara.setIndentLevel(1);
            subPara.setBullet(true);
            XSLFTextRun subRun = subPara.addNewTextRun();
            subRun.setText(contentProvider.getSentence());
            subRun.setFontSize(14.0);
            subRun.setFontColor(SUBTITLE_COLOR);
        }
    }

    private void createSummarySlide(XMLSlideShow ppt, ContentProvider contentProvider) {
        XSLFSlide slide = ppt.createSlide();

        addSlideTitle(slide, "Key Takeaways");

        XSLFTextBox contentBox = slide.createTextBox();
        contentBox.setAnchor(new Rectangle(50, 100, 620, 350));

        List<String> takeaways = contentProvider.getBulletPoints(4);
        int num = 1;
        for (String takeaway : takeaways) {
            XSLFTextParagraph para = contentBox.addNewTextParagraph();
            para.setSpaceBefore(15.0);
            XSLFTextRun numRun = para.addNewTextRun();
            numRun.setText(num++ + ". ");
            numRun.setFontSize(18.0);
            numRun.setBold(true);
            numRun.setFontColor(ACCENT_COLOR);

            XSLFTextRun textRun = para.addNewTextRun();
            textRun.setText(takeaway);
            textRun.setFontSize(18.0);
            textRun.setFontColor(TITLE_COLOR);
        }
    }

    private void createClosingSlide(XMLSlideShow ppt, ContentProvider contentProvider) {
        XSLFSlide slide = ppt.createSlide();

        // Thank you text
        XSLFTextBox thankYouBox = slide.createTextBox();
        thankYouBox.setAnchor(new Rectangle(50, 150, 620, 80));
        XSLFTextParagraph thankYouPara = thankYouBox.addNewTextParagraph();
        thankYouPara.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun thankYouRun = thankYouPara.addNewTextRun();
        thankYouRun.setText("Thank You");
        thankYouRun.setFontSize(44.0);
        thankYouRun.setBold(true);
        thankYouRun.setFontColor(ACCENT_COLOR);

        // Questions text
        XSLFTextBox questionsBox = slide.createTextBox();
        questionsBox.setAnchor(new Rectangle(50, 250, 620, 50));
        XSLFTextParagraph questionsPara = questionsBox.addNewTextParagraph();
        questionsPara.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun questionsRun = questionsPara.addNewTextRun();
        questionsRun.setText("Questions?");
        questionsRun.setFontSize(28.0);
        questionsRun.setFontColor(SUBTITLE_COLOR);

        // Contact info
        XSLFTextBox contactBox = slide.createTextBox();
        contactBox.setAnchor(new Rectangle(50, 350, 620, 100));
        XSLFTextParagraph contactPara = contactBox.addNewTextParagraph();
        contactPara.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun contactRun = contactPara.addNewTextRun();
        contactRun.setText(contentProvider.getFullName() + "\n" + contentProvider.getEmail());
        contactRun.setFontSize(14.0);
        contactRun.setFontColor(SUBTITLE_COLOR);
    }

    private void addSlideTitle(XSLFSlide slide, String title) {
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(50, 20, 620, 60));
        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText(title);
        titleRun.setFontSize(32.0);
        titleRun.setBold(true);
        titleRun.setFontColor(TITLE_COLOR);
    }

    @Override
    public String getExtension() {
        return "pptx";
    }

    @Override
    public String getFormatKey() {
        return "pptx";
    }

    @Override
    public String generateFilename(ContentProvider contentProvider) {
        return contentProvider.getPresentationName();
    }
}
