USE ai_interview_coach;

INSERT INTO `user` (id, username, password_hash, email, created_at, updated_at) VALUES
(1, 'demo', 'demo-only-not-for-production', 'demo@example.com', NOW(), NOW());

INSERT INTO knowledge_point (id, name, category, description) VALUES
(1, 'Array Traversal', 'Array', 'Traverse arrays and handle index boundaries.'),
(2, 'HashMap Lookup', 'HashMap', 'Use hash maps for complement and membership lookup.'),
(3, 'String Counting', 'HashMap', 'Count characters or tokens with fixed arrays or maps.'),
(4, 'Linked List Pointer', 'LinkedList', 'Move and reconnect linked list pointers safely.'),
(5, 'Binary Tree Recursion', 'Tree', 'Use recursion with correct base cases on binary trees.'),
(6, 'Binary Tree BFS', 'Tree', 'Use queues for level-order traversal.'),
(7, 'Dynamic Programming State', 'DynamicProgramming', 'Define states, initialization, and transitions.'),
(8, 'Binary Search Patience', 'DynamicProgramming', 'Use binary search over tails for LIS.');

INSERT INTO problem
(id, title, description, difficulty, category, input_format, output_format, template_code, solution_outline, enabled, created_at, updated_at)
VALUES
(101, 'Two Sum',
'Given n integers and a target, return the indices of two numbers whose sum is target. Return -1 -1 if no pair exists.',
'EASY', 'HashMap',
'Line 1: n. Line 2: n integers. Line 3: target.',
'Print two indices separated by one space, or -1 -1.',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) {
            nums[i] = sc.nextInt();
        }
        int target = sc.nextInt();

        // TODO: print two indices, for example: System.out.println(i + " " + j);
        System.out.println("-1 -1");
    }
}',
'Use a HashMap from value to index. For each number, check target - nums[i] before inserting nums[i].',
1, NOW(), NOW()),
(102, 'Valid Anagram',
'Given two lowercase strings s and t, determine whether t is an anagram of s.',
'EASY', 'HashMap',
'Line 1: s. Line 2: t.',
'Print true or false.',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.next();
        String t = sc.next();

        // TODO: compare character counts.
        System.out.println(false);
    }
}',
'Count characters for both strings, or increment for s and decrement for t.',
1, NOW(), NOW()),
(103, 'Reverse Linked List',
'Given a linked list, reverse it and print node values from new head to tail.',
'EASY', 'LinkedList',
'Line 1: n. Line 2: n integers.',
'Print reversed values separated by one space. Print an empty line for n = 0.',
'import java.util.*;

public class Main {
    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        ListNode dummy = new ListNode(0);
        ListNode tail = dummy;
        for (int i = 0; i < n; i++) {
            tail.next = new ListNode(sc.nextInt());
            tail = tail.next;
        }

        // TODO: reverse dummy.next and print the result.
    }
}',
'Use prev, cur, and next pointers. Move cur forward while reversing cur.next.',
1, NOW(), NOW()),
(104, 'Merge Two Sorted Lists',
'Given two sorted integer lists, merge them into one sorted list and print the values.',
'EASY', 'LinkedList',
'Line 1: n. Line 2: n integers. Line 3: m. Line 4: m integers.',
'Print merged values separated by one space.',
'import java.util.*;

public class Main {
    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = sc.nextInt();
        int m = sc.nextInt();
        int[] b = new int[m];
        for (int i = 0; i < m; i++) b[i] = sc.nextInt();

        // TODO: merge the two sorted arrays or build linked lists first.
    }
}',
'Use a dummy node and advance the pointer with the smaller current value.',
1, NOW(), NOW()),
(105, 'Maximum Depth of Binary Tree',
'Given a binary tree in level-order form, compute its maximum depth. The token null means an empty child.',
'EASY', 'Tree',
'Line 1: n. Line 2: n level-order tokens such as 3 9 20 null null 15 7.',
'Print the maximum depth as an integer.',
'import java.util.*;

public class Main {
    static class TreeNode {
        String val;
        TreeNode left;
        TreeNode right;
        TreeNode(String val) { this.val = val; }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        String[] values = new String[n];
        for (int i = 0; i < n; i++) values[i] = sc.next();

        // TODO: build the tree and print max depth.
    }
}',
'Build the tree level by level, then use max(leftDepth, rightDepth) + 1 with null base case.',
1, NOW(), NOW()),
(106, 'Binary Tree Level Order',
'Given a binary tree in level-order form, print each level from top to bottom.',
'MEDIUM', 'Tree',
'Line 1: n. Line 2: n level-order tokens. The token null means an empty child.',
'Print levels separated by semicolon. Values inside a level are separated by one space.',
'import java.util.*;

public class Main {
    static class TreeNode {
        String val;
        TreeNode left;
        TreeNode right;
        TreeNode(String val) { this.val = val; }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        String[] values = new String[n];
        for (int i = 0; i < n; i++) values[i] = sc.next();

        // TODO: build the tree, BFS by level, and print levels.
    }
}',
'Use a queue. For each level, process exactly queue.size() nodes before adding a semicolon.',
1, NOW(), NOW()),
(107, 'Climbing Stairs',
'You can climb 1 or 2 steps each move. Count how many distinct ways can reach the nth step.',
'EASY', 'DynamicProgramming',
'Line 1: n.',
'Print the number of ways.',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();

        // TODO: dynamic programming.
        System.out.println(0);
    }
}',
'Let dp[i] be ways to reach step i. Initialize dp[0] = 1 and dp[1] = 1.',
1, NOW(), NOW()),
(108, 'Longest Increasing Subsequence',
'Given an integer array, return the length of the longest strictly increasing subsequence.',
'MEDIUM', 'DynamicProgramming',
'Line 1: n. Line 2: n integers.',
'Print the LIS length.',
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();

        // TODO: O(n^2) DP is enough for the MVP cases.
        System.out.println(0);
    }
}',
'Use dp[i] as the LIS length ending at i, or maintain tails with binary search.',
1, NOW(), NOW());

INSERT INTO problem_knowledge_point (problem_id, knowledge_point_id) VALUES
(101, 1), (101, 2),
(102, 3),
(103, 4),
(104, 4),
(105, 5),
(106, 6),
(107, 7),
(108, 7), (108, 8);

INSERT INTO test_case (problem_id, input_data, expected_output, is_sample, weight) VALUES
(101, '4
2 7 11 15
9
', '0 1', 1, 1),
(101, '3
3 2 4
6
', '1 2', 0, 1),
(101, '2
3 3
6
', '0 1', 0, 1),
(102, 'anagram
nagaram
', 'true', 1, 1),
(102, 'rat
car
', 'false', 0, 1),
(102, 'aacc
ccac
', 'false', 0, 1),
(103, '5
1 2 3 4 5
', '5 4 3 2 1', 1, 1),
(103, '1
7
', '7', 0, 1),
(103, '0
', '', 0, 1),
(104, '3
1 2 4
3
1 3 4
', '1 1 2 3 4 4', 1, 1),
(104, '0
0
', '', 0, 1),
(104, '2
2 5
3
1 3 4
', '1 2 3 4 5', 0, 1),
(105, '7
3 9 20 null null 15 7
', '3', 1, 1),
(105, '2
1 null
', '1', 0, 1),
(105, '0
', '0', 0, 1),
(106, '7
3 9 20 null null 15 7
', '3;9 20;15 7', 1, 1),
(106, '1
1
', '1', 0, 1),
(106, '0
', '', 0, 1),
(107, '2
', '2', 1, 1),
(107, '3
', '3', 0, 1),
(107, '5
', '8', 0, 1),
(108, '8
10 9 2 5 3 7 101 18
', '4', 1, 1),
(108, '6
0 1 0 3 2 3
', '4', 0, 1),
(108, '7
7 7 7 7 7 7 7
', '1', 0, 1);
