package com.fakedata.gforms;

import java.util.regex.Pattern;

/**
 * Defines how to identify a field of interest by key name pattern, value pattern, or both.
 */
public record ExtractionRule(
        String fieldType,
        Pattern keyPattern,
        Pattern valuePattern
) {
    public ExtractionRule {
        if (keyPattern == null && valuePattern == null) {
            throw new IllegalArgumentException("At least one of keyPattern or valuePattern must be provided");
        }
    }

    public static ExtractionRule byKey(String fieldType, String keyRegex) {
        return new ExtractionRule(fieldType, Pattern.compile(keyRegex, Pattern.CASE_INSENSITIVE), null);
    }

    public static ExtractionRule byValue(String fieldType, String valueRegex) {
        return new ExtractionRule(fieldType, null, Pattern.compile(valueRegex));
    }

    public static ExtractionRule byKeyAndValue(String fieldType, String keyRegex, String valueRegex) {
        return new ExtractionRule(
                fieldType,
                Pattern.compile(keyRegex, Pattern.CASE_INSENSITIVE),
                Pattern.compile(valueRegex)
        );
    }

    public boolean matchesKey(String key) {
        return keyPattern != null && keyPattern.matcher(key).find();
    }

    public boolean matchesValue(String value) {
        return valuePattern != null && valuePattern.matcher(value).find();
    }
}
