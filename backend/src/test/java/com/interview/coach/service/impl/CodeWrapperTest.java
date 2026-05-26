package com.interview.coach.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.interview.coach.entity.Problem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    void wrappedThreeSumOutputMatchesExpectedFormat(@TempDir Path tempDir) throws Exception {
        String userCode = """
                class Solution {
                    public List<List<Integer>> threeSum(int[] nums) {
                        Arrays.sort(nums);
                        List<List<Integer>> result = new ArrayList<>();
                        for (int i = 0; i < nums.length - 2; i++) {
                            if (i > 0 && nums[i] == nums[i - 1]) {
                                continue;
                            }
                            int left = i + 1;
                            int right = nums.length - 1;
                            while (left < right) {
                                int sum = nums[i] + nums[left] + nums[right];
                                if (sum == 0) {
                                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                                    while (left < right && nums[left] == nums[left + 1]) {
                                        left++;
                                    }
                                    while (left < right && nums[right] == nums[right - 1]) {
                                        right--;
                                    }
                                    left++;
                                    right--;
                                } else if (sum < 0) {
                                    left++;
                                } else {
                                    right--;
                                }
                            }
                        }
                        return result;
                    }
                }
                """;

        String output = compileAndRunWrapped(tempDir, 15L, userCode, "6 -1 0 1 2 -1 -4");

        assertThat(output).isEqualTo("[[-1,-1,2],[-1,0,1]]");
    }

    @Test
    void wrappedMergeIntervalsOutputMatchesExpectedFormat(@TempDir Path tempDir) throws Exception {
        String userCode = """
                class Solution {
                    public int[][] merge(int[][] intervals) {
                        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));
                        List<int[]> merged = new ArrayList<>();
                        for (int[] interval : intervals) {
                            if (merged.isEmpty() || merged.get(merged.size() - 1)[1] < interval[0]) {
                                merged.add(interval);
                            } else {
                                int[] last = merged.get(merged.size() - 1);
                                last[1] = Math.max(last[1], interval[1]);
                            }
                        }
                        return merged.toArray(new int[merged.size()][]);
                    }
                }
                """;

        String output = compileAndRunWrapped(tempDir, 56L, userCode, "4 1 3 2 6 8 10 15 18");

        assertThat(output).isEqualTo("[[1,6],[8,10],[15,18]]");
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
    void wrappedNumberOfIslandsOutputMatchesExpectedFormat(@TempDir Path tempDir) throws Exception {
        String userCode = """
                class Solution {
                    public int numIslands(char[][] grid) {
                        int count = 0;
                        for (int r = 0; r < grid.length; r++) {
                            for (int c = 0; c < grid[r].length; c++) {
                                if (grid[r][c] == '1') {
                                    count++;
                                    sink(grid, r, c);
                                }
                            }
                        }
                        return count;
                    }

                    private void sink(char[][] grid, int r, int c) {
                        if (r < 0 || c < 0 || r >= grid.length || c >= grid[r].length || grid[r][c] != '1') {
                            return;
                        }
                        grid[r][c] = '0';
                        sink(grid, r + 1, c);
                        sink(grid, r - 1, c);
                        sink(grid, r, c + 1);
                        sink(grid, r, c - 1);
                    }
                }
                """;

        String output = compileAndRunWrapped(tempDir, 200L, userCode, "4 5 11110 11010 11000 00000");

        assertThat(output).isEqualTo("1");
    }

    @Test
    void wrappedInorderTraversalOutputMatchesExpectedFormat(@TempDir Path tempDir) throws Exception {
        String userCode = """
                class Solution {
                    public List<Integer> inorderTraversal(TreeNode root) {
                        List<Integer> result = new ArrayList<>();
                        traverse(root, result);
                        return result;
                    }

                    private void traverse(TreeNode node, List<Integer> result) {
                        if (node == null) {
                            return;
                        }
                        traverse(node.left, result);
                        result.add(node.val);
                        traverse(node.right, result);
                    }
                }
                """;

        String output = compileAndRunWrapped(tempDir, 94L, userCode, "4 1 null 2 3");

        assertThat(output).isEqualTo("[1,3,2]");
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

    private String compileAndRunWrapped(Path tempDir, Long problemId, String userCode, String input) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("CodeWrapper output tests require a JDK, not a JRE").isNotNull();

        Path sourceFile = tempDir.resolve("Main.java");
        Files.writeString(sourceFile, CodeWrapper.wrap(problem(problemId), userCode), StandardCharsets.UTF_8);

        DiagnosticCollector<javax.tools.JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            Iterable<? extends javax.tools.JavaFileObject> files =
                    fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()));
            Boolean success = compiler.getTask(
                    null, fileManager, diagnostics, List.of("-encoding", "UTF-8", "-d", tempDir.toString()),
                    null, files).call();
            assertThat(success)
                    .as(() -> diagnostics.getDiagnostics().stream()
                            .map(this::formatDiagnostic)
                            .collect(Collectors.joining(System.lineSeparator())))
                    .isTrue();
        }

        ByteArrayInputStream stdin = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        java.io.InputStream originalIn = System.in;
        try (PrintStream capture = new PrintStream(stdout, true, StandardCharsets.UTF_8);
                URLClassLoader classLoader = new URLClassLoader(new URL[] {tempDir.toUri().toURL()})) {
            System.setIn(stdin);
            System.setOut(capture);
            Class<?> mainClass = classLoader.loadClass("Main");
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[0]);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
        return stdout.toString(StandardCharsets.UTF_8);
    }

    private String formatDiagnostic(Diagnostic<?> diagnostic) {
        return "line " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null);
    }

    private Problem problem(Long id) {
        Problem problem = new Problem();
        problem.setId(id);
        problem.setCodeMode("solution");
        return problem;
    }
}
