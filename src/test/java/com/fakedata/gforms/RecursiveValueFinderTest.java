package com.fakedata.gforms;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecursiveValueFinderTest {

    @Test
    void findsAttachmentFilenames() throws IOException {
        String json = """
                {
                  "gform": {
                    "section1": {
                      "uploads": [
                        {"filename": "REF123_1_report.pdf", "size": 1024},
                        {"filename": "REF123_2_data.xlsx", "size": 2048}
                      ]
                    }
                  }
                }
                """;

        var finder = RecursiveValueFinder.withDefaultRules();
        List<FieldMatch> matches = finder.find(json);

        List<FieldMatch> attachments = matches.stream()
                .filter(m -> m.fieldType().equals("attachment"))
                .toList();

        assertTrue(attachments.size() >= 2, "Should find at least 2 attachments");
        assertTrue(attachments.stream().anyMatch(m -> m.value().equals("REF123_1_report.pdf")));
        assertTrue(attachments.stream().anyMatch(m -> m.value().equals("REF123_2_data.xlsx")));
    }

    @Test
    void findsSubmissionReference() throws IOException {
        String json = """
                {
                  "metaData": {
                    "submission-reference": "ABCD-1234-EFGH"
                  },
                  "data": {
                    "nested": {
                      "correlationId": "some-other-ref"
                    }
                  }
                }
                """;

        var finder = RecursiveValueFinder.withDefaultRules();
        List<FieldMatch> matches = finder.find(json);

        List<FieldMatch> refs = matches.stream()
                .filter(m -> m.fieldType().equals("submission_reference"))
                .toList();

        assertTrue(refs.size() >= 1);
        // The dashed pattern should match
        assertTrue(refs.stream().anyMatch(m -> m.value().equals("ABCD-1234-EFGH")));
        // The key-based rule should also match correlationId
        assertTrue(refs.stream().anyMatch(m -> m.key().equals("correlationId")));
    }

    @Test
    void findsEmailAddresses() throws IOException {
        String json = """
                {
                  "contact": {
                    "email": "test@example.com",
                    "altEmail": "other@company.co.uk"
                  }
                }
                """;

        var finder = RecursiveValueFinder.withDefaultRules();
        List<FieldMatch> emails = finder.find(json).stream()
                .filter(m -> m.fieldType().equals("email"))
                .toList();

        assertEquals(2, emails.size());
    }

    @Test
    void findsDates() throws IOException {
        String json = """
                {"submitted": "2026-03-01", "dob": "15/06/1990"}
                """;

        var finder = RecursiveValueFinder.withDefaultRules();
        List<FieldMatch> dates = finder.find(json).stream()
                .filter(m -> m.fieldType().equals("date"))
                .toList();

        assertEquals(2, dates.size());
    }

    @Test
    void findsNINO() throws IOException {
        String json = """
                {"nino": "AB123456C"}
                """;

        var finder = RecursiveValueFinder.withDefaultRules();
        List<FieldMatch> matches = finder.find(json).stream()
                .filter(m -> m.fieldType().equals("nino"))
                .toList();

        assertEquals(1, matches.size());
        assertEquals("AB123456C", matches.getFirst().value());
    }

    @Test
    void handlesDeepNesting() throws IOException {
        String json = """
                {
                  "a": {"b": {"c": {"d": {"e": {"filename": "deep.pdf"}}}}}
                }
                """;

        var finder = RecursiveValueFinder.withDefaultRules();
        List<FieldMatch> matches = finder.find(json);

        assertTrue(matches.stream().anyMatch(m -> m.value().equals("deep.pdf")));
        assertTrue(matches.stream().anyMatch(m ->
                m.jsonPath().equals("$.a.b.c.d.e.filename")));
    }

    @Test
    void customRulesWork() throws IOException {
        String json = """
                {"customField": "CUSTOM_VALUE_42"}
                """;

        var finder = new RecursiveValueFinder(List.of(
                ExtractionRule.byValue("custom", "^CUSTOM_VALUE_\\d+$")
        ));

        List<FieldMatch> matches = finder.find(json);
        assertEquals(1, matches.size());
        assertEquals("custom", matches.getFirst().fieldType());
    }

    @Test
    void keyAndValueBothRequired() throws IOException {
        String json = """
                {
                  "amount": "£1,500.00",
                  "description": "£1,500.00",
                  "unrelatedAmount": "not-money"
                }
                """;

        var finder = RecursiveValueFinder.withDefaultRules();
        List<FieldMatch> amounts = finder.find(json).stream()
                .filter(m -> m.fieldType().equals("monetary_amount"))
                .toList();

        // Only "amount" key with monetary value pattern should match
        assertEquals(1, amounts.size());
        assertEquals("amount", amounts.getFirst().key());
    }
}
