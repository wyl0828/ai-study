package com.interview.coach.service.impl;

final class CodeWrapper {

    private static final Long VALID_ANAGRAM_PROBLEM_ID = 102L;
    private static final Long REVERSE_LIST_PROBLEM_ID = 103L;
    private static final Long MERGE_TWO_LISTS_PROBLEM_ID = 104L;

    private CodeWrapper() {
    }

    static String wrap(Long problemId, String userCode) {
        if (problemId == null || userCode == null) {
            return userCode;
        }
        if (VALID_ANAGRAM_PROBLEM_ID.equals(problemId)) {
            return wrapValidAnagram(userCode);
        }
        if (REVERSE_LIST_PROBLEM_ID.equals(problemId)) {
            return wrapReverseList(userCode);
        }
        if (MERGE_TWO_LISTS_PROBLEM_ID.equals(problemId)) {
            return wrapMergeTwoLists(userCode);
        }
        return userCode;
    }

    static boolean isSolutionModeProblem(Long problemId) {
        return VALID_ANAGRAM_PROBLEM_ID.equals(problemId)
                || REVERSE_LIST_PROBLEM_ID.equals(problemId)
                || MERGE_TWO_LISTS_PROBLEM_ID.equals(problemId);
    }

    private static String wrapValidAnagram(String userCode) {
        return """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        String s = sc.hasNext() ? sc.next() : "";
                        String t = sc.hasNext() ? sc.next() : "";
                        boolean result = new Solution().isAnagram(s, t);
                        System.out.print(result);
                    }
                }

                """ + userCode;
    }

    private static String wrapReverseList(String userCode) {
        return """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int n = sc.hasNextInt() ? sc.nextInt() : 0;
                        ListNode head = buildList(sc, n);
                        ListNode result = new Solution().reverseList(head);
                        printList(result);
                    }

                    private static ListNode buildList(Scanner sc, int n) {
                        ListNode dummy = new ListNode(0);
                        ListNode tail = dummy;
                        for (int i = 0; i < n; i++) {
                            tail.next = new ListNode(sc.nextInt());
                            tail = tail.next;
                        }
                        return dummy.next;
                    }

                    private static void printList(ListNode head) {
                        StringBuilder sb = new StringBuilder();
                        ListNode cur = head;
                        while (cur != null) {
                            if (sb.length() > 0) {
                                sb.append(' ');
                            }
                            sb.append(cur.val);
                            cur = cur.next;
                        }
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

                """ + userCode;
    }

    private static String wrapMergeTwoLists(String userCode) {
        return """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int n = sc.hasNextInt() ? sc.nextInt() : 0;
                        ListNode list1 = buildList(sc, n);
                        int m = sc.hasNextInt() ? sc.nextInt() : 0;
                        ListNode list2 = buildList(sc, m);
                        ListNode result = new Solution().mergeTwoLists(list1, list2);
                        printList(result);
                    }

                    private static ListNode buildList(Scanner sc, int n) {
                        ListNode dummy = new ListNode(0);
                        ListNode tail = dummy;
                        for (int i = 0; i < n; i++) {
                            tail.next = new ListNode(sc.nextInt());
                            tail = tail.next;
                        }
                        return dummy.next;
                    }

                    private static void printList(ListNode head) {
                        StringBuilder sb = new StringBuilder();
                        ListNode cur = head;
                        while (cur != null) {
                            if (sb.length() > 0) {
                                sb.append(' ');
                            }
                            sb.append(cur.val);
                            cur = cur.next;
                        }
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

                """ + userCode;
    }
}
