package com.fakedata.util;

import java.security.SecureRandom;

/**
 * Generates random alphanumeric IDs for mail_item_id and attached_id fields.
 */
public class IdGenerator {
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a random alphanumeric string of the specified length.
     *
     * @param length the length of the generated string
     * @return a random alphanumeric string
     */
    public static String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a 16-character alphanumeric ID.
     *
     * @return a 16-character alphanumeric string
     */
    public static String generate16() {
        return generate(16);
    }
}
