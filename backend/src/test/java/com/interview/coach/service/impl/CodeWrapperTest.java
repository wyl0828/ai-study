package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.interview.coach.entity.Problem;
import org.junit.jupiter.api.Test;

class CodeWrapperTest {

    @Test
    void registersHot100SolutionAdapters() {
        assertThat(CodeWrapper.supportedProblemIds())
                .containsExactlyInAnyOrder(
                        1L, 3L, 15L, 20L, 21L, 49L, 53L, 56L, 70L, 94L,
                        102L, 104L, 121L, 128L, 141L, 198L, 200L, 206L, 226L, 704L);
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
    void wrapsLongestSubstringSolutionWithStringInput() {
        String userCode = """
                class Solution {
                    public int lengthOfLongestSubstring(String s) {
                        return s.length();
                    }
                }
                """;

        String wrapped = CodeWrapper.wrap(problem(3L), userCode);

        assertThat(wrapped).contains("String s = sc.hasNext() ? sc.next() : \"\"");
        assertThat(wrapped).contains("new Solution().lengthOfLongestSubstring(s)");
        assertThat(wrapped).contains("System.out.print(result)");
    }

    @Test
    void wrapsThreeSumSolutionWithNormalizedGroups() {
        String userCode = """
                class Solution {
                    public List<List<Integer>> threeSum(int[] nums) {
                        return new ArrayList<>();
                    }
                }
                """;

        String wrapped = CodeWrapper.wrap(problem(15L), userCode);

        assertThat(wrapped).contains("new Solution().threeSum(nums)");
        assertThat(wrapped).contains("printIntegerGroups(result)");
        assertThat(wrapped).contains("normalized.sort");
    }

    @Test
    void wrapsNumberOfIslandsSolutionWithGridInput() {
        String userCode = """
                class Solution {
                    public int numIslands(char[][] grid) {
                        return 0;
                    }
                }
                """;

        String wrapped = CodeWrapper.wrap(problem(200L), userCode);

        assertThat(wrapped).contains("char[][] grid = readCharGrid(sc)");
        assertThat(wrapped).contains("new Solution().numIslands(grid)");
        assertThat(wrapped).contains("grid[i] = sc.next().toCharArray()");
    }

    @Test
    void wrapsBinarySearchSolutionWithArrayAndTarget() {
        String userCode = """
                class Solution {
                    public int search(int[] nums, int target) {
                        return -1;
                    }
                }
                """;

        String wrapped = CodeWrapper.wrap(problem(704L), userCode);

        assertThat(wrapped).contains("int[] nums = readIntArray(sc)");
        assertThat(wrapped).contains("int target = sc.nextInt()");
        assertThat(wrapped).contains("new Solution().search(nums, target)");
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
