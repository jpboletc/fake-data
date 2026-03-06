package com.fakedata.gforms;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Generates a PDF version of the gForms variable schema strategy document.
 */
public class StrategyDocumentPdf {

    // Colours
    private static final Color BLUE = new Color(66, 133, 244);
    private static final Color DARK = new Color(51, 51, 51);
    private static final Color MEDIUM = new Color(102, 102, 102);
    private static final Color LIGHT_BG = new Color(245, 245, 245);
    private static final Color GREEN = new Color(15, 157, 88);
    private static final Color AMBER = new Color(244, 180, 0);
    private static final Color RED = new Color(219, 68, 55);

    // Fonts
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, BLUE);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 14, MEDIUM);
    private static final Font H1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BLUE);
    private static final Font H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, DARK);
    private static final Font H3 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, DARK);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 11, DARK);
    private static final Font BODY_ITALIC = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11, MEDIUM);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DARK);
    private static final Font CODE = FontFactory.getFont(FontFactory.COURIER, 10, DARK);
    private static final Font TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 10, DARK);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 9, MEDIUM);
    private static final Font WARNING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, RED);

    public static void generate(Path outputPath) throws IOException {
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(outputPath.toFile()));
            doc.open();

            addTitlePage(doc);
            addRecommendationSummary(doc);
            addTechnicalRecommendations(doc);
            addProcessRecommendations(doc);
            addWhatNotToDo(doc);
            addImplementationOrder(doc);
            addArchitectureOverview(doc);

            doc.close();
        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF", e);
        }
    }

    private static void addTitlePage(Document doc) throws DocumentException {
        doc.add(spacer(80));

        Paragraph title = new Paragraph("Handling Variable gForms\nJSON Schemas", TITLE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        doc.add(title);

        Paragraph subtitle = new Paragraph("Strategy & Technical Recommendations", SUBTITLE);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(30);
        doc.add(subtitle);

        doc.add(new LineSeparator(1f, 40, BLUE, Element.ALIGN_CENTER, 0));
        doc.add(spacer(30));

        Paragraph context = new Paragraph();
        context.setAlignment(Element.ALIGN_CENTER);
        context.add(new Chunk("Prepared: " + LocalDate.now() + "\n\n", BODY));
        context.add(new Chunk(
                "A combined technical and process strategy for reliably extracting data from " +
                "variably-structured UK Government gForms JSON submissions.", BODY_ITALIC));
        doc.add(context);

        doc.add(spacer(40));

        // Key context box
        PdfPTable contextBox = new PdfPTable(1);
        contextBox.setWidthPercentage(85);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_BG);
        cell.setPadding(15);
        cell.setBorderColor(BLUE);
        cell.setBorderWidth(1);

        Paragraph boxContent = new Paragraph();
        boxContent.add(new Chunk("Problem: ", BOLD));
        boxContent.add(new Chunk(
                "gForms produce JSON with variable structures due to optional questions and conditional branching. " +
                "The same form type can produce structurally different JSON depending on which questions were answered. " +
                "Form originators have not provided guaranteed schemas.\n\n", BODY));
        boxContent.add(new Chunk("Approach: ", BOLD));
        boxContent.add(new Chunk(
                "Combine technical resilience (schema-agnostic field finding) with schema discovery " +
                "(infer and maintain schemas from real data).", BODY));
        cell.addElement(boxContent);
        contextBox.addCell(cell);
        doc.add(contextBox);

        doc.newPage();
    }

    private static void addRecommendationSummary(Document doc) throws DocumentException {
        doc.add(heading1("Recommendation Summary"));

        doc.add(para("Two complementary strategies:"));
        doc.add(spacer(5));

        doc.add(numberedItem("1", "Technical resilience",
                "Build the parser to find fields of interest regardless of where they appear (schema-agnostic), " +
                "removing dependency on knowing exact structure."));
        doc.add(numberedItem("2", "Schema discovery process",
                "Use our own tooling to infer and maintain schemas from real data, reducing reliance on " +
                "externally-provided documentation."));

        doc.add(spacer(10));

        // Overview table
        PdfPTable table = new PdfPTable(new float[]{0.08f, 0.25f, 0.12f, 0.55f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        addTableHeader(table, "Ref", "Component", "Priority", "Purpose");
        addTableRow(table, "A1", "Recursive Value Finder", "High", "Find fields by pattern matching on values/keys, not by path");
        addTableRow(table, "A2", "Schema Inferencer", "Medium", "Learn union schemas from real submission data");
        addTableRow(table, "A3", "Field Path Registry", "Medium", "Track known paths per form type, auto-updated");
        addTableRow(table, "A4", "Structural Diff Tool", "Low", "Compare submissions to find structural variations");
        addTableRow(table, "A5", "Two-Tier Extractor", "High", "Fast-path registry lookup with recursive fallback");
        addTableRow(table, "B1", "Reframe the Question", "High", "Ask customer targeted questions, not full schema");
        addTableRow(table, "B2", "Form Definition Export", "Medium", "Request existing config rather than new docs");
        addTableRow(table, "B3", "Change Notification", "Medium", "Lightweight agreement for structural changes");
        addTableRow(table, "B4", "Sample Submissions", "Medium", "One test submission per branch path");
        addTableRow(table, "B5", "Confidence Rating", "Low", "Green/Amber/Red per form type for risk visibility");

        doc.add(table);
        doc.newPage();
    }

    private static void addTechnicalRecommendations(Document doc) throws DocumentException {
        doc.add(heading1("A. Technical Recommendations"));
        doc.add(para("Things we can build and do ourselves, without external dependencies."));
        doc.add(spacer(10));

        // A1
        doc.add(heading2("A1. Recursive Value Finder (High Priority)"));
        calloutBox(doc, "Stop trying to parse by known paths. Search the entire document instead.");

        doc.add(para("Instead of coding json[\"gform\"][\"section3\"][\"uploads\"][0][\"filename\"], " +
                "implement a recursive JSON walker that:"));
        doc.add(bulletItem("Traverses the entire JSON tree regardless of structure"));
        doc.add(bulletItem("Identifies fields of interest by pattern matching on values and/or key names"));
        doc.add(bulletItem("Returns all matches with their JSON paths for logging/debugging"));
        doc.add(bulletItem("Can be configured with a set of \"extraction rules\" per field type"));

        doc.add(spacer(5));
        doc.add(para("Pattern examples built into the implementation:"));

        PdfPTable patternTable = new PdfPTable(new float[]{0.25f, 0.35f, 0.40f});
        patternTable.setWidthPercentage(100);
        patternTable.setSpacingBefore(5);
        addTableHeader(patternTable, "Field Type", "Key Pattern", "Value Pattern");
        addTableRow(patternTable, "Attachment", "filename, upload, attachment", "*.pdf, *.xlsx, *.docx, ...");
        addTableRow(patternTable, "Submission Ref", "submission-ref, correlation-id", "XXXX-XXXX-XXXX");
        addTableRow(patternTable, "UTR", "utr, taxpayer-ref", "10-digit number");
        addTableRow(patternTable, "Email", "(any key)", "standard email regex");
        addTableRow(patternTable, "Date", "(any key)", "DD/MM/YYYY or YYYY-MM-DD");
        addTableRow(patternTable, "Monetary Amount", "amount, total, value, cost", "optional currency + digits");
        addTableRow(patternTable, "NINO", "(any key)", "AA123456A format");
        addTableRow(patternTable, "Company Reg", "company-reg, crn", "6-8 alphanumeric");
        doc.add(patternTable);

        doc.add(spacer(5));
        Paragraph rationale = new Paragraph();
        rationale.add(new Chunk("Why this works: ", BOLD));
        rationale.add(new Chunk(
                "The values you're looking for often have predictable formats even if their locations are unpredictable. " +
                "This inverts the problem \u2014 instead of \"where is field X?\", you ask \"does this value match a pattern I care about?\"",
                BODY));
        doc.add(rationale);
        doc.add(spacer(10));

        // A2
        doc.add(heading2("A2. Schema Inference from Real Submissions (Medium Priority)"));
        calloutBox(doc, "Build a tool that learns the schema from data you already have.");

        doc.add(para("Collect multiple real submissions per form type and run a schema inferencer that:"));
        doc.add(bulletItem("Produces a union schema showing all observed fields across all submissions of that form type"));
        doc.add(bulletItem("Flags fields that are optional (present in some submissions but not others)"));
        doc.add(bulletItem("Highlights structural variations (e.g. field is a string in one submission, an array in another)"));
        doc.add(bulletItem("Tracks where fields of interest have been observed"));
        doc.add(spacer(10));

        // A3
        doc.add(heading2("A3. Per-Form-Type Field Path Registry (Medium Priority)"));
        doc.add(para("Maintain a YAML configuration file per form type that records known JSON paths " +
                "where fields of interest have been found. The recursive finder (A1) populates this automatically. " +
                "Over time, you build a high-confidence map of where important fields appear per form type."));
        doc.add(spacer(5));

        // YAML example
        PdfPTable yamlBox = new PdfPTable(1);
        yamlBox.setWidthPercentage(90);
        PdfPCell yamlCell = new PdfPCell();
        yamlCell.setBackgroundColor(LIGHT_BG);
        yamlCell.setPadding(10);
        yamlCell.setBorderColor(new Color(200, 200, 200));
        yamlCell.addElement(new Paragraph("form_id: \"creative-industry-tax-relief\"\n" +
                "known_paths:\n" +
                "  attachments:\n" +
                "    - \"$.gform.uploads[*].filename\"\n" +
                "    - \"$.gform.section3.supportingDocs[*].name\"\n" +
                "  submission_reference:\n" +
                "    - \"$.metaData.submission-reference\"\n" +
                "last_updated: \"2026-03-01\"\n" +
                "auto_discovered_paths:\n" +
                "  - field: \"attachment\"\n" +
                "    path: \"$.gform.additionalEvidence.document\"\n" +
                "    first_seen: \"2026-02-15\"", CODE));
        yamlBox.addCell(yamlCell);
        doc.add(yamlBox);
        doc.add(spacer(10));

        // A4
        doc.add(heading2("A4. Structural Diff Tool (Low Priority, High Diagnostic Value)"));
        doc.add(para("Compares two or more JSON submissions of the same form type and highlights:"));
        doc.add(bulletItem("Keys present in one but not the other (optional fields / conditional branches)"));
        doc.add(bulletItem("Type differences (string vs array vs object)"));
        doc.add(bulletItem("Structural depth differences"));
        doc.add(para("Useful for onboarding new form types and diagnosing parsing failures."));
        doc.add(spacer(10));

        // A5
        doc.add(heading2("A5. Defensive Parsing Pattern (High Priority)"));
        doc.add(para("For production code, implement a two-tier extraction strategy:"));

        PdfPTable tierTable = new PdfPTable(new float[]{0.10f, 0.20f, 0.70f});
        tierTable.setWidthPercentage(100);
        tierTable.setSpacingBefore(5);
        addTableHeader(tierTable, "Tier", "Strategy", "Description");
        addTableRow(tierTable, "1", "Fast path", "Check the known path registry (A3) first \u2014 O(1) lookup at expected locations");
        addTableRow(tierTable, "2", "Fallback", "If fast path finds nothing, run the recursive finder (A1) across the full document");
        addTableRow(tierTable, "3", "Alert", "If fallback finds fields at new paths, log a warning and auto-update the registry");
        doc.add(tierTable);

        doc.add(spacer(5));
        doc.add(para("This gives speed in the common case and resilience in the edge cases, plus automatic schema evolution."));

        doc.newPage();
    }

    private static void addProcessRecommendations(Document doc) throws DocumentException {
        doc.add(heading1("B. Process & Business Recommendations"));
        doc.add(para("Things to propose upstream \u2014 low cost, high leverage."));
        doc.add(spacer(10));

        // B1
        doc.add(heading2("B1. Reframe the Question to the Customer (High Priority)"));
        doc.add(para("Asking \"what is the schema?\" is hard to answer because the schema is inherently variable. " +
                "A more targeted question:"));
        calloutBox(doc,
                "\"We don't need the full schema. We need to know: what are all the possible question nodes " +
                "in the form that accept file uploads, and what are the key names used for references/identifiers? " +
                "A list of field names and types is sufficient.\"");
        doc.add(spacer(10));

        // B2
        doc.add(heading2("B2. Request Form Definition Export (Medium Priority)"));
        doc.add(para("gForms is built on a configuration system. The form definitions already exist. Ask for:"));
        doc.add(bulletItem("The form template/definition files"));
        doc.add(bulletItem("A list of all fields marked as \"file upload\" type"));
        doc.add(bulletItem("The branching logic diagram or decision tree"));
        doc.add(para("This reframes the request from \"write us documentation\" to \"share configuration that already exists.\""));
        doc.add(spacer(10));

        // B3
        doc.add(heading2("B3. Change Notification Agreement (Medium Priority)"));
        doc.add(para("Propose a lightweight agreement:"));
        calloutBox(doc,
                "\"When a form definition changes in a way that adds, moves, or removes file upload fields " +
                "or key identifiers, provide us with the form ID and a sample submission showing the new structure.\"");
        doc.add(spacer(10));

        // B4
        doc.add(heading2("B4. Sample Submissions Per Branch (Medium Priority)"));
        doc.add(para("Request one test submission for each major question branch. This gives raw material for " +
                "schema inference (A2) without requiring anyone to write documentation."));
        doc.add(spacer(10));

        // B5
        doc.add(heading2("B5. Establish a \"Schema Confidence\" Rating (Low Priority)"));

        PdfPTable confTable = new PdfPTable(new float[]{0.12f, 0.45f, 0.43f});
        confTable.setWidthPercentage(100);
        confTable.setSpacingBefore(5);
        addTableHeader(confTable, "Rating", "Meaning", "Action");
        addConfidenceRow(confTable, "Green", GREEN, "All field paths known, validated against 10+ real submissions", "Use fast-path only");
        addConfidenceRow(confTable, "Amber", AMBER, "Some paths known, <10 real submissions seen", "Use fast-path + fallback");
        addConfidenceRow(confTable, "Red", RED, "New form type, no real submissions yet", "Use recursive finder only");
        doc.add(confTable);

        doc.newPage();
    }

    private static void addWhatNotToDo(Document doc) throws DocumentException {
        doc.add(heading1("C. What NOT to Do"));
        doc.add(spacer(5));

        String[][] warnings = {
                {"Don't write rigid JSON Schemas per form type and validate against them",
                        "The variability is a feature of gForms, not a bug. Rigid schemas will break constantly."},
                {"Don't block on getting \"complete schema documentation\"",
                        "Reframe the ask (B1) or infer it from real data (A2). Waiting will not resolve the underlying issue."},
                {"Don't parse by hardcoded paths without a fallback",
                        "This is the current fragility. Always have the recursive finder as a safety net."},
                {"Don't treat each structural variation as a defect or blocker",
                        "Design the system to expect and handle variation from day one."}
        };

        for (String[] w : warnings) {
            PdfPTable warningBox = new PdfPTable(1);
            warningBox.setWidthPercentage(100);
            warningBox.setSpacingBefore(5);
            warningBox.setSpacingAfter(5);
            PdfPCell cell = new PdfPCell();
            cell.setPadding(10);
            cell.setBackgroundColor(new Color(255, 243, 243));
            cell.setBorderColor(RED);
            cell.setBorderWidth(1);
            Paragraph p = new Paragraph();
            p.add(new Chunk("\u2717 " + w[0] + "\n", WARNING));
            p.add(new Chunk(w[1], BODY));
            cell.addElement(p);
            warningBox.addCell(cell);
            doc.add(warningBox);
        }

        doc.newPage();
    }

    private static void addImplementationOrder(Document doc) throws DocumentException {
        doc.add(heading1("Suggested Implementation Order"));
        doc.add(spacer(5));

        PdfPTable table = new PdfPTable(new float[]{0.06f, 0.10f, 0.35f, 0.49f});
        table.setWidthPercentage(100);
        addTableHeader(table, "#", "Ref", "Component", "Rationale");
        addTableRow(table, "1", "A1", "Recursive Value Finder", "Immediate resilience, small effort, addresses the core vulnerability");
        addTableRow(table, "2", "B1", "Reframe the Question", "Zero cost, provides the customer with a more answerable question");
        addTableRow(table, "3", "A3", "Path Registry", "Builds naturally on top of A1, captures institutional knowledge");
        addTableRow(table, "4", "A5", "Two-tier parsing", "Production-grade pattern combining A1 + A3");
        addTableRow(table, "5", "A2", "Schema Inference", "When you have enough real submissions to make it valuable");
        addTableRow(table, "6", "B3", "Change Notification", "Propose once you have your own tooling working, from a position of strength");
        addTableRow(table, "7", "A4", "Diff Tool", "Build when debugging or onboarding new form types becomes a pain point");
        doc.add(table);

        doc.add(spacer(15));

        PdfPTable statusBox = new PdfPTable(1);
        statusBox.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setPadding(15);
        cell.setBackgroundColor(new Color(232, 245, 233));
        cell.setBorderColor(GREEN);
        cell.setBorderWidth(1);
        Paragraph p = new Paragraph();
        p.add(new Chunk("Implementation Status\n\n", H3));
        p.add(new Chunk("All five technical components (A1\u2013A5) have been implemented as Java tooling in the ", BODY));
        p.add(new Chunk("com.fakedata.gforms", CODE));
        p.add(new Chunk(" package. The tools are available via CLI:\n\n", BODY));
        p.add(new Chunk("java -cp fake-data-all.jar com.fakedata.gforms.GFormsToolsCli <command>\n\n", CODE));
        p.add(new Chunk("Commands: ", BOLD));
        p.add(new Chunk("find, extract, infer-schema, diff", CODE));
        cell.addElement(p);
        statusBox.addCell(cell);
        doc.add(statusBox);

        doc.newPage();
    }

    private static void addArchitectureOverview(Document doc) throws DocumentException {
        doc.add(heading1("Architecture Overview"));
        doc.add(spacer(5));

        doc.add(heading2("Component Diagram"));
        doc.add(spacer(5));

        // Text-based architecture diagram
        PdfPTable diagramBox = new PdfPTable(1);
        diagramBox.setWidthPercentage(95);
        PdfPCell cell = new PdfPCell();
        cell.setPadding(15);
        cell.setBackgroundColor(LIGHT_BG);
        cell.setBorderColor(new Color(200, 200, 200));

        Font diagramFont = FontFactory.getFont(FontFactory.COURIER, 9, DARK);
        cell.addElement(new Paragraph(
                "  JSON Submission\n" +
                "        |\n" +
                "        v\n" +
                "  +---------------------+\n" +
                "  | GFormFieldExtractor  |  (A5: Two-Tier Strategy)\n" +
                "  +---------------------+\n" +
                "        |           |\n" +
                "   Tier 1          Tier 2\n" +
                "   (fast)        (fallback)\n" +
                "        |           |\n" +
                "        v           v\n" +
                "  +-----------+  +----------------------+\n" +
                "  | Field Path|  | RecursiveValueFinder |  (A1)\n" +
                "  | Registry  |  +----------------------+\n" +
                "  |   (A3)    |     |  Uses ExtractionRules\n" +
                "  +-----------+     |  (DefaultExtractionRules)\n" +
                "        ^          |\n" +
                "        |          |\n" +
                "        +----------+\n" +
                "     Auto-update new paths\n" +
                "\n" +
                "  Supporting Tools:\n" +
                "  +-------------------+    +--------------------+\n" +
                "  | SchemaInferencer  |    | StructuralDiffTool |\n" +
                "  |       (A2)        |    |        (A4)        |\n" +
                "  +-------------------+    +--------------------+\n" +
                "  Learns schemas from       Compares structure of\n" +
                "  multiple submissions      two submissions",
                diagramFont));
        diagramBox.addCell(cell);
        doc.add(diagramBox);
        doc.add(spacer(15));

        doc.add(heading2("Key Design Decisions"));
        doc.add(bulletItem("Pattern matching on values, not path-based parsing \u2014 inverts the fragility"));
        doc.add(bulletItem("Per-field-type deduplication prevents duplicate matches from overlapping rules"));
        doc.add(bulletItem("Registry auto-evolves: new paths logged as warnings and persisted to YAML"));
        doc.add(bulletItem("Separate CLI entry point (GFormsToolsCli) \u2014 shares the fat JAR, independent of FakeDataApp"));
        doc.add(bulletItem("Jackson for JSON + YAML processing; OpenPDF for document generation"));
        doc.add(spacer(15));

        doc.add(heading2("File Inventory"));

        PdfPTable fileTable = new PdfPTable(new float[]{0.45f, 0.10f, 0.45f});
        fileTable.setWidthPercentage(100);
        addTableHeader(fileTable, "File", "Ref", "Purpose");
        addTableRow(fileTable, "ExtractionRule.java", "A1", "Configurable field matching rule");
        addTableRow(fileTable, "FieldMatch.java", "A1", "Match result record");
        addTableRow(fileTable, "DefaultExtractionRules.java", "A1", "Pre-built rules for common gForms fields");
        addTableRow(fileTable, "RecursiveValueFinder.java", "A1", "Recursive JSON walker");
        addTableRow(fileTable, "FieldPathRegistry.java", "A3", "YAML-based path registry per form type");
        addTableRow(fileTable, "GFormFieldExtractor.java", "A5", "Two-tier extraction entry point");
        addTableRow(fileTable, "SchemaInferencer.java", "A2", "Union schema builder");
        addTableRow(fileTable, "StructuralDiffTool.java", "A4", "JSON structural comparison");
        addTableRow(fileTable, "GFormsToolsCli.java", "CLI", "Picocli CLI with 4 subcommands");
        addTableRow(fileTable, "StrategyDocumentPdf.java", "\u2014", "This document generator");
        doc.add(fileTable);

        doc.add(spacer(20));
        Paragraph footer = new Paragraph(
                "Document generated from source code. " +
                "For the latest version, rebuild with: java -cp fake-data-all.jar com.fakedata.gforms.StrategyDocumentPdf",
                SMALL);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // --- Helpers ---

    private static Paragraph heading1(String text) throws DocumentException {
        Paragraph p = new Paragraph(text, H1);
        p.setSpacingBefore(5);
        p.setSpacingAfter(10);
        return p;
    }

    private static Paragraph heading2(String text) {
        Paragraph p = new Paragraph(text, H2);
        p.setSpacingBefore(10);
        p.setSpacingAfter(5);
        return p;
    }

    private static Paragraph para(String text) {
        Paragraph p = new Paragraph(text, BODY);
        p.setSpacingAfter(5);
        return p;
    }

    private static Paragraph bulletItem(String text) {
        Paragraph p = new Paragraph("\u2022  " + text, BODY);
        p.setIndentationLeft(15);
        p.setSpacingAfter(3);
        return p;
    }

    private static Paragraph numberedItem(String num, String title, String description) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(num + ". " + title + " \u2014 ", BOLD));
        p.add(new Chunk(description, BODY));
        p.setSpacingAfter(8);
        p.setIndentationLeft(10);
        return p;
    }

    private static Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(height);
        return p;
    }

    private static PdfPTable calloutBox(Document doc, String text) throws DocumentException {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(95);
        box.setSpacingBefore(5);
        box.setSpacingAfter(10);
        PdfPCell cell = new PdfPCell(new Paragraph(text, BODY_ITALIC));
        cell.setPadding(12);
        cell.setBackgroundColor(new Color(232, 240, 254));
        cell.setBorderColor(BLUE);
        cell.setBorderWidth(1);
        box.addCell(cell);
        doc.add(box);
        return box;
    }

    private static void addTableHeader(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, TABLE_HEADER));
            cell.setBackgroundColor(BLUE);
            cell.setPadding(6);
            cell.setBorderWidth(0);
            table.addCell(cell);
        }
    }

    private static void addTableRow(PdfPTable table, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, TABLE_CELL));
            cell.setPadding(5);
            cell.setBorderColor(new Color(220, 220, 220));
            cell.setBorderWidth(0.5f);
            table.addCell(cell);
        }
    }

    private static void addConfidenceRow(PdfPTable table, String rating, Color color, String meaning, String action) {
        PdfPCell ratingCell = new PdfPCell(new Phrase(rating, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, color)));
        ratingCell.setPadding(5);
        ratingCell.setBorderColor(new Color(220, 220, 220));
        ratingCell.setBorderWidth(0.5f);
        table.addCell(ratingCell);

        addTableRow(table, meaning, action);
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length > 0 ? Path.of(args[0]) : Path.of("gforms-strategy.pdf");
        generate(output);
        System.out.println("Generated: " + output.toAbsolutePath());
    }
}
