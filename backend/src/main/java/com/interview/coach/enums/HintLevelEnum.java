package com.interview.coach.enums;

public enum HintLevelEnum {
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3);

    private final int value;

    HintLevelEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
