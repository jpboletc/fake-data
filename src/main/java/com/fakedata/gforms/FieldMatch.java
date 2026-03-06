package com.fakedata.gforms;

/**
 * A single match found by the recursive value finder.
 *
 * @param fieldType  the type of field matched (e.g. "attachment", "submission_reference")
 * @param jsonPath   the JSON path where the match was found (e.g. "$.gform.uploads[0].filename")
 * @param key        the JSON key name at the match location
 * @param value      the matched value (as string)
 * @param ruleName   the extraction rule that matched
 */
public record FieldMatch(
        String fieldType,
        String jsonPath,
        String key,
        String value,
        String ruleName
) {
    @Override
    public String toString() {
        return "%s: %s = \"%s\" (at %s)".formatted(fieldType, jsonPath, value, ruleName);
    }
}
