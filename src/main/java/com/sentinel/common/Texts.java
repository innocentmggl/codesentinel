package com.sentinel.common;

public final class Texts {

    private Texts() {
    }

    public static String truncate(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, maxChars);
    }
}
