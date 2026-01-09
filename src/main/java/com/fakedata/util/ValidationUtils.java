package com.fakedata.util;

import java.util.regex.Pattern;

/**
 * Utility class for validating submission references.
 */
public class ValidationUtils {
    public static final String DEFAULT_PATTERN = "^[A-Za-z0-9]{12}$";

    /**
     * Validates a submission reference against the given pattern.
     *
     * @param submissionRef the submission reference to validate
     * @param pattern       the regex pattern to match against
     * @return true if valid, false otherwise
     */
    public static boolean isValidSubmissionRef(String submissionRef, String pattern) {
        if (submissionRef == null || submissionRef.isBlank()) {
            return false;
        }
        return Pattern.matches(pattern, submissionRef.trim());
    }

    /**
     * Validates a submission reference using the default pattern (12 alphanumeric characters).
     *
     * @param submissionRef the submission reference to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidSubmissionRef(String submissionRef) {
        return isValidSubmissionRef(submissionRef, DEFAULT_PATTERN);
    }

    /**
     * Validates a regex pattern is compilable.
     *
     * @param pattern the pattern to validate
     * @return true if the pattern compiles, false otherwise
     */
    public static boolean isValidPattern(String pattern) {
        try {
            Pattern.compile(pattern);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
