package com.fakedata.gforms;

import java.util.List;

/**
 * Pre-configured extraction rules for common gForms field types.
 */
public final class DefaultExtractionRules {

    private DefaultExtractionRules() {}

    /** Attachment filenames: files with common document extensions */
    public static ExtractionRule attachmentByValue() {
        return ExtractionRule.byValue("attachment",
                "(?i)\\S+\\.(pdf|xlsx?|docx?|csv|txt|jpg|jpeg|png|odt|ods|odp|pptx?|zip)$");
    }

    /** Attachment filenames: keys that suggest file upload fields */
    public static ExtractionRule attachmentByKey() {
        return ExtractionRule.byKey("attachment",
                "(?i)(file\\s?name|upload|attachment|document|supporting.?doc)");
    }

    /** Submission reference: HMRC-style XXXX-XXXX-XXXX pattern */
    public static ExtractionRule submissionRefDashed() {
        return ExtractionRule.byValue("submission_reference",
                "^[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$");
    }

    /** Submission reference: key name patterns */
    public static ExtractionRule submissionRefByKey() {
        return ExtractionRule.byKey("submission_reference",
                "(?i)(submission.?ref|correlation.?id|envelope.?id|case.?ref)");
    }

    /** UTR (Unique Taxpayer Reference): 10-digit number */
    public static ExtractionRule utr() {
        return ExtractionRule.byKeyAndValue("utr",
                "(?i)(utr|taxpayer.?ref|tax.?ref)",
                "^\\d{10}$");
    }

    /** Email addresses */
    public static ExtractionRule email() {
        return ExtractionRule.byValue("email",
                "^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    /** Dates in common UK formats: DD/MM/YYYY or YYYY-MM-DD */
    public static ExtractionRule date() {
        return ExtractionRule.byValue("date",
                "^(\\d{2}/\\d{2}/\\d{4}|\\d{4}-\\d{2}-\\d{2})$");
    }

    /** Monetary amounts: optional currency symbol + digits with optional decimals */
    public static ExtractionRule monetaryAmount() {
        return ExtractionRule.byKeyAndValue("monetary_amount",
                "(?i)(amount|total|value|cost|price|fee|payment|turnover|revenue)",
                "^[£$€]?\\s?\\d[\\d,]*(\\.\\d{1,2})?$");
    }

    /** NINO (National Insurance Number) */
    public static ExtractionRule nino() {
        return ExtractionRule.byValue("nino",
                "^[A-CEGHJ-PR-TW-Z]{2}\\d{6}[A-D]$");
    }

    /** Company Registration Number */
    public static ExtractionRule companyRegNumber() {
        return ExtractionRule.byKeyAndValue("company_registration",
                "(?i)(company.?reg|crn|company.?number|registration.?number)",
                "^[A-Z0-9]{6,8}$");
    }

    /** Returns all default extraction rules. */
    public static List<ExtractionRule> all() {
        return List.of(
                attachmentByValue(),
                attachmentByKey(),
                submissionRefDashed(),
                submissionRefByKey(),
                utr(),
                email(),
                date(),
                monetaryAmount(),
                nino(),
                companyRegNumber()
        );
    }
}
