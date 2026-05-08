package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodeWrapperTest {

    @Test
    void wrapsReverseListSolutionAsExecutableMainProgram() {
        String solutionCode = """
                class Solution {
                    public ListNode reverseList(ListNode head) {
                        return head;
                    }
                }
                """;

        String wrappedCode = CodeWrapper.wrap(103L, solutionCode);

        assertThat(wrappedCode).contains("import java.util.*;");
        assertThat(wrappedCode).contains("class ListNode {");
        assertThat(wrappedCode).contains("public class Main {");
        assertThat(wrappedCode).contains("Solution solution = new Solution();");
        assertThat(wrappedCode).contains("solution.reverseList(dummy.next)");
        assertThat(wrappedCode).contains(solutionCode);
    }

    @Test
    void putsMainBeforeListNodeSoPistonRunsMainClass() {
        String wrappedCode = CodeWrapper.wrap(103L, "class Solution {}");

        assertThat(wrappedCode.indexOf("public class Main"))
                .isLessThan(wrappedCode.indexOf("class ListNode"));
    }

    @Test
    void leavesOtherProblemsUnchanged() {
        String mainCode = "public class Main {}";

        assertThat(CodeWrapper.wrap(101L, mainCode)).isSameAs(mainCode);
    }

    @Test
    void leavesNullInputsUnchanged() {
        String code = "class Solution {}";

        assertThat(CodeWrapper.wrap(null, code)).isSameAs(code);
        assertThat(CodeWrapper.wrap(103L, null)).isNull();
    }
}
