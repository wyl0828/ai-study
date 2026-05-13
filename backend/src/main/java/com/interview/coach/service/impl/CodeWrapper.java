package com.interview.coach.service.impl;

import com.interview.coach.entity.Problem;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class CodeWrapper {

    private static final Map<Long, SolutionProblemAdapter> ADAPTERS = adapters();

    private CodeWrapper() {
    }

    static Set<Long> supportedProblemIds() {
        return Collections.unmodifiableSet(ADAPTERS.keySet());
    }

    static String wrap(Problem problem, String userCode) {
        if (problem == null || problem.getId() == null || userCode == null) {
            return userCode;
        }
        SolutionProblemAdapter adapter = ADAPTERS.get(problem.getId());
        if (adapter == null) {
            return userCode;
        }
        return adapter.wrap(userCode);
    }

    private static Map<Long, SolutionProblemAdapter> adapters() {
        Map<Long, SolutionProblemAdapter> adapters = new LinkedHashMap<>();
        adapters.put(1L, CodeWrapper::wrapTwoSum);
        adapters.put(21L, CodeWrapper::wrapMergeTwoLists);
        adapters.put(49L, CodeWrapper::wrapGroupAnagrams);
        adapters.put(70L, CodeWrapper::wrapClimbStairs);
        adapters.put(102L, CodeWrapper::wrapLevelOrder);
        adapters.put(104L, CodeWrapper::wrapMaxDepth);
        adapters.put(121L, CodeWrapper::wrapBestTimeToBuyAndSellStock);
        adapters.put(128L, CodeWrapper::wrapLongestConsecutive);
        adapters.put(141L, CodeWrapper::wrapHasCycle);
        adapters.put(198L, CodeWrapper::wrapHouseRobber);
        adapters.put(206L, CodeWrapper::wrapReverseList);
        adapters.put(226L, CodeWrapper::wrapInvertTree);
        return adapters;
    }

    private static String wrapTwoSum(String userCode) {
        return appendUserCode(commonArrayHeader("""
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int[] nums = readIntArray(sc);
                        int target = sc.nextInt();
                        int[] result = new Solution().twoSum(nums, target);
                        printIntArray(result);
                    }
                """), userCode);
    }

    private static String wrapBestTimeToBuyAndSellStock(String userCode) {
        return appendUserCode(commonArrayHeader("""
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int[] prices = readIntArray(sc);
                        int result = new Solution().maxProfit(prices);
                        System.out.print(result);
                    }
                """), userCode);
    }

    private static String wrapLongestConsecutive(String userCode) {
        return appendUserCode(commonArrayHeader("""
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int[] nums = readIntArray(sc);
                        int result = new Solution().longestConsecutive(nums);
                        System.out.print(result);
                    }
                """), userCode);
    }

    private static String wrapClimbStairs(String userCode) {
        return appendUserCode("""
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int n = sc.nextInt();
                        int result = new Solution().climbStairs(n);
                        System.out.print(result);
                    }
                }

                """, userCode);
    }

    private static String wrapHouseRobber(String userCode) {
        return appendUserCode(commonArrayHeader("""
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int[] nums = readIntArray(sc);
                        int result = new Solution().rob(nums);
                        System.out.print(result);
                    }
                """), userCode);
    }

    private static String wrapGroupAnagrams(String userCode) {
        return appendUserCode("""
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int n = sc.nextInt();
                        String[] strs = new String[n];
                        for (int i = 0; i < n; i++) {
                            strs[i] = sc.next();
                        }
                        List<List<String>> result = new Solution().groupAnagrams(strs);
                        printStringGroups(result);
                    }

                    private static void printStringGroups(List<List<String>> groups) {
                        if (groups == null) {
                            System.out.print("[]");
                            return;
                        }
                        List<List<String>> normalized = new ArrayList<>();
                        for (List<String> group : groups) {
                            List<String> copy = new ArrayList<>(group);
                            Collections.sort(copy);
                            normalized.add(copy);
                        }
                        normalized.sort(Comparator.comparing(group -> String.join(",", group)));
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < normalized.size(); i++) {
                            if (i > 0) {
                                sb.append(",");
                            }
                            sb.append("[");
                            List<String> group = normalized.get(i);
                            for (int j = 0; j < group.size(); j++) {
                                if (j > 0) {
                                    sb.append(",");
                                }
                                sb.append('"').append(group.get(j)).append('"');
                            }
                            sb.append("]");
                        }
                        sb.append("]");
                        System.out.print(sb);
                    }
                }

                """, userCode);
    }

    private static String wrapReverseList(String userCode) {
        return appendUserCode(linkedListHeader("""
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        ListNode head = readList(sc);
                        ListNode result = new Solution().reverseList(head);
                        printList(result);
                    }
                """), userCode);
    }

    private static String wrapMergeTwoLists(String userCode) {
        return appendUserCode(linkedListHeader("""
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        ListNode list1 = readList(sc);
                        ListNode list2 = readList(sc);
                        ListNode result = new Solution().mergeTwoLists(list1, list2);
                        printList(result);
                    }
                """), userCode);
    }

    private static String wrapHasCycle(String userCode) {
        return appendUserCode("""
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int n = sc.nextInt();
                        ListNode[] nodes = new ListNode[n];
                        for (int i = 0; i < n; i++) {
                            nodes[i] = new ListNode(sc.nextInt());
                            if (i > 0) {
                                nodes[i - 1].next = nodes[i];
                            }
                        }
                        int pos = sc.nextInt();
                        if (n > 0 && pos >= 0) {
                            nodes[n - 1].next = nodes[pos];
                        }
                        ListNode head = n == 0 ? null : nodes[0];
                        boolean result = new Solution().hasCycle(head);
                        System.out.print(result);
                    }
                }

                class ListNode {
                    int val;
                    ListNode next;

                    ListNode(int val) {
                        this.val = val;
                    }
                }

                """, userCode);
    }

    private static String wrapLevelOrder(String userCode) {
        return appendUserCode(treeHeader("""
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        TreeNode root = readTree(sc);
                        List<List<Integer>> result = new Solution().levelOrder(root);
                        printIntegerGroups(result);
                    }
                """), userCode);
    }

    private static String wrapMaxDepth(String userCode) {
        return appendUserCode(treeHeader("""
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        TreeNode root = readTree(sc);
                        int result = new Solution().maxDepth(root);
                        System.out.print(result);
                    }
                """), userCode);
    }

    private static String wrapInvertTree(String userCode) {
        return appendUserCode(treeHeader("""
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        TreeNode root = readTree(sc);
                        TreeNode result = new Solution().invertTree(root);
                        printTree(result);
                    }
                """), userCode);
    }

    private static String commonArrayHeader(String mainMethod) {
        return """
                import java.util.*;

                public class Main {
                """ + mainMethod + """

                    private static int[] readIntArray(Scanner sc) {
                        int n = sc.hasNextInt() ? sc.nextInt() : 0;
                        int[] values = new int[n];
                        for (int i = 0; i < n; i++) {
                            values[i] = sc.nextInt();
                        }
                        return values;
                    }

                    private static void printIntArray(int[] values) {
                        if (values == null) {
                            System.out.print("[]");
                            return;
                        }
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < values.length; i++) {
                            if (i > 0) {
                                sb.append(",");
                            }
                            sb.append(values[i]);
                        }
                        sb.append("]");
                        System.out.print(sb);
                    }
                }

                """;
    }

    private static String linkedListHeader(String mainMethod) {
        return """
                import java.util.*;

                public class Main {
                """ + mainMethod + """

                    private static ListNode readList(Scanner sc) {
                        int n = sc.hasNextInt() ? sc.nextInt() : 0;
                        ListNode dummy = new ListNode(0);
                        ListNode tail = dummy;
                        for (int i = 0; i < n; i++) {
                            tail.next = new ListNode(sc.nextInt());
                            tail = tail.next;
                        }
                        return dummy.next;
                    }

                    private static void printList(ListNode head) {
                        StringBuilder sb = new StringBuilder("[");
                        ListNode cur = head;
                        while (cur != null) {
                            if (sb.length() > 1) {
                                sb.append(",");
                            }
                            sb.append(cur.val);
                            cur = cur.next;
                        }
                        sb.append("]");
                        System.out.print(sb);
                    }
                }

                class ListNode {
                    int val;
                    ListNode next;

                    ListNode(int val) {
                        this.val = val;
                    }
                }

                """;
    }

    private static String treeHeader(String mainMethod) {
        return """
                import java.util.*;

                public class Main {
                """ + mainMethod + """

                    private static TreeNode readTree(Scanner sc) {
                        int n = sc.hasNextInt() ? sc.nextInt() : 0;
                        if (n == 0) {
                            return null;
                        }
                        String[] values = new String[n];
                        for (int i = 0; i < n; i++) {
                            values[i] = sc.next();
                        }
                        if ("null".equals(values[0])) {
                            return null;
                        }
                        TreeNode root = new TreeNode(Integer.parseInt(values[0]));
                        Queue<TreeNode> queue = new ArrayDeque<>();
                        queue.offer(root);
                        int index = 1;
                        while (!queue.isEmpty() && index < n) {
                            TreeNode node = queue.poll();
                            if (index < n && !"null".equals(values[index])) {
                                node.left = new TreeNode(Integer.parseInt(values[index]));
                                queue.offer(node.left);
                            }
                            index++;
                            if (index < n && !"null".equals(values[index])) {
                                node.right = new TreeNode(Integer.parseInt(values[index]));
                                queue.offer(node.right);
                            }
                            index++;
                        }
                        return root;
                    }

                    private static void printIntegerGroups(List<List<Integer>> groups) {
                        if (groups == null) {
                            System.out.print("[]");
                            return;
                        }
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < groups.size(); i++) {
                            if (i > 0) {
                                sb.append(",");
                            }
                            sb.append("[");
                            List<Integer> group = groups.get(i);
                            for (int j = 0; j < group.size(); j++) {
                                if (j > 0) {
                                    sb.append(",");
                                }
                                sb.append(group.get(j));
                            }
                            sb.append("]");
                        }
                        sb.append("]");
                        System.out.print(sb);
                    }

                    private static void printTree(TreeNode root) {
                        if (root == null) {
                            System.out.print("[]");
                            return;
                        }
                        List<String> values = new ArrayList<>();
                        Queue<TreeNode> queue = new LinkedList<>();
                        queue.offer(root);
                        while (!queue.isEmpty()) {
                            TreeNode node = queue.poll();
                            if (node == null) {
                                values.add("null");
                            } else {
                                values.add(String.valueOf(node.val));
                                queue.offer(node.left);
                                queue.offer(node.right);
                            }
                        }
                        int end = values.size() - 1;
                        while (end >= 0 && "null".equals(values.get(end))) {
                            end--;
                        }
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i <= end; i++) {
                            if (i > 0) {
                                sb.append(",");
                            }
                            sb.append(values.get(i));
                        }
                        sb.append("]");
                        System.out.print(sb);
                    }
                }

                class TreeNode {
                    int val;
                    TreeNode left;
                    TreeNode right;

                    TreeNode(int val) {
                        this.val = val;
                    }
                }

                """;
    }

    private static String appendUserCode(String wrapper, String userCode) {
        StringBuilder imports = new StringBuilder();
        StringBuilder body = new StringBuilder();
        for (String line : userCode.split("\\R", -1)) {
            if (line.stripLeading().startsWith("import ")) {
                imports.append(line.strip()).append(System.lineSeparator());
            } else {
                body.append(line).append(System.lineSeparator());
            }
        }
        return imports + wrapper + body;
    }

    @FunctionalInterface
    private interface SolutionProblemAdapter {

        String wrap(String userCode);
    }
}
