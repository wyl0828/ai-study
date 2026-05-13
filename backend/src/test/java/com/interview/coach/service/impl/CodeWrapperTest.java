package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.interview.coach.entity.Problem;
import org.junit.jupiter.api.Test;

class CodeWrapperTest {

    @Test
    void registersHot100SolutionAdapters() {
        assertThat(CodeWrapper.supportedProblemIds())
                .containsExactlyInAnyOrder(1L, 21L, 49L, 70L, 102L, 104L, 121L, 128L, 141L, 198L, 206L, 226L);
    }

    @Test
    void wrapsTwoSumSolutionWithMain() {
        String userCode = """
                import java.util.HashMap;
                import java.util.Map;

                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        return new int[] {0, 1};
                    }
                }
                """;

        String wrapped = CodeWrapper.wrap(problem(1L), userCode);

        assertThat(wrapped).contains("import java.util.*;");
        assertThat(wrapped).contains("public class Main");
        assertThat(wrapped).contains("new Solution().twoSum(nums, target)");
        assertThat(wrapped).contains("printIntArray(result)");
        assertThat(wrapped.indexOf("import java.util.HashMap;")).isLessThan(wrapped.indexOf("public class Main"));
        assertThat(wrapped.indexOf("public class Main")).isLessThan(wrapped.indexOf("class Solution"));
    }

    @Test
    void wrapsBestTimeToBuyAndSellStockSolutionWithMain() {
        String userCode = """
                class Solution {
                    public int maxProfit(int[] prices) {
                        return 5;
                    }
                }
                """;

        String wrapped = CodeWrapper.wrap(problem(121L), userCode);

        assertThat(wrapped).contains("import java.util.*;");
        assertThat(wrapped).contains("public class Main");
        assertThat(wrapped).contains("new Solution().maxProfit(prices)");
        assertThat(wrapped).contains("System.out.print(result)");
        assertThat(wrapped).contains("class Solution");
        assertThat(wrapped).contains("public int maxProfit(int[] prices)");
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

        String wrapped = CodeWrapper.wrap(problem(206L), userCode);

        assertThat(wrapped).contains("import java.util.*;");
        assertThat(wrapped).contains("public class Main");
        assertThat(wrapped).contains("class ListNode");
        assertThat(wrapped).contains("new Solution().reverseList(head)");
        assertThat(wrapped).contains("class Solution");
        assertThat(wrapped).contains("public ListNode reverseList(ListNode head)");
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

        String wrapped = CodeWrapper.wrap(problem(21L), userCode);

        assertThat(wrapped).contains("import java.util.*;");
        assertThat(wrapped).contains("public class Main");
        assertThat(wrapped).contains("class ListNode");
        assertThat(wrapped).contains("new Solution().mergeTwoLists(list1, list2)");
        assertThat(wrapped).contains("class Solution");
        assertThat(wrapped).contains("public ListNode mergeTwoLists(ListNode list1, ListNode list2)");
        assertThat(wrapped.indexOf("public class Main")).isLessThan(wrapped.indexOf("class ListNode"));
    }

    @Test
    void leavesOtherProblemsAndNullInputsUnchanged() {
        String acmCode = "public class Main {}";

        assertThat(CodeWrapper.wrap(problem(999L), acmCode)).isSameAs(acmCode);
        assertThat(CodeWrapper.wrap(null, acmCode)).isSameAs(acmCode);
        assertThat(CodeWrapper.wrap(problem(1L), null)).isNull();
    }

    private Problem problem(Long id) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setCodeMode("solution");
        return problem;
    }
}
