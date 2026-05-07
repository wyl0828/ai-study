package com.interview.coach.enums;

import java.util.Locale;
import lombok.Getter;

@Getter
public enum LanguageEnum {
    JAVA("java");

    private final String code;

    LanguageEnum(String code) {
        this.code = code;
    }

    public static boolean isJava(String value) {
        return value != null && JAVA.code.equals(value.toLowerCase(Locale.ROOT));
    }
}
