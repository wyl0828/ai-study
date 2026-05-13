package com.interview.coach.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodeModeEnumTest {

    @Test
    void javaV1OnlySupportsAcmAndSolutionModes() {
        assertThat(CodeModeEnum.values())
                .extracting(CodeModeEnum::name)
                .containsExactly("ACM", "SOLUTION");
    }

    @Test
    void solutionModeAcceptsInternalDbValuesCaseInsensitively() {
        assertThat(CodeModeEnum.isSolution("SOLUTION")).isTrue();
        assertThat(CodeModeEnum.isSolution("solution")).isTrue();
        assertThat(CodeModeEnum.isSolution("acm")).isFalse();
        assertThat(CodeModeEnum.isSolution(null)).isFalse();
    }
}
