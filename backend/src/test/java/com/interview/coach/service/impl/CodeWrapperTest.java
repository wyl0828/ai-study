package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodeWrapperTest {

    @Test
    void wrapsValidAnagramSolutionWithMain() {
        String userCode = """
                class Solution {
                    public boolean isAnagram(String s, String t) {
                        return true;
                    }
                }
                """;

        String wrapped = CodeWrapper.wrap(102L, userCode);

        assertThat(wrapped).contains("import java.util.*;");
        assertThat(wrapped).contains("public class Main");
        assertThat(wrapped).contains("new Solution().isAnagram(s, t)");
        assertThat(wrapped).contains(userCode);
    }

    @Test
    void wrapsReverseListSolutionWithListNodeAndMain() {
        String userCode = """
                class Solution {
                    public ListNode reverseList(ListNode head) {
                        return head;
                    }
                }
                """;

        String wrapped = CodeWrapper.wrap(103L, userCode);

        assertThat(wrapped).contains("import java.util.*;");
        assertThat(wrapped).contains("public class Main");
        assertThat(wrapped).contains("class ListNode");
        assertThat(wrapped).contains("new Solution().reverseList(head)");
        assertThat(wrapped).contains(userCode);
        assertThat(wrapped.indexOf("public class Main")).isLessThan(wrapped.indexOf("class ListNode"));
    }

    @Test
    void wrapsMergeTwoListsSolutionWithListNodeAndMain() {
        String userCode = """
                class Solution {
                    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
                        return list1;
                    }
                }
                """;

        String wrapped = CodeWrapper.wrap(104L, userCode);

        assertThat(wrapped).contains("import java.util.*;");
        assertThat(wrapped).contains("public class Main");
        assertThat(wrapped).contains("class ListNode");
        assertThat(wrapped).contains("new Solution().mergeTwoLists(list1, list2)");
        assertThat(wrapped).contains(userCode);
        assertThat(wrapped.indexOf("public class Main")).isLessThan(wrapped.indexOf("class ListNode"));
    }

    @Test
    void leavesOtherProblemsAndNullInputsUnchanged() {
        String acmCode = "public class Main {}";

        assertThat(CodeWrapper.wrap(101L, acmCode)).isSameAs(acmCode);
        assertThat(CodeWrapper.wrap(null, acmCode)).isSameAs(acmCode);
        assertThat(CodeWrapper.wrap(102L, null)).isNull();
    }
}
