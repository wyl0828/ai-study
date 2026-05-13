package com.interview.coach.enums;

public enum CodeModeEnum {

    ACM("acm"),
    SOLUTION("solution");

    private final String value;

    CodeModeEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static boolean isSolution(String codeMode) {
        return SOLUTION.value.equalsIgnoreCase(normalize(codeMode));
    }

    private static String normalize(String codeMode) {
        if (codeMode == null || codeMode.isBlank()) {
            return ACM.value;
        }
        return codeMode.trim();
    }
}
