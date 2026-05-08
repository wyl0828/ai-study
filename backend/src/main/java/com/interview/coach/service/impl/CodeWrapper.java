package com.interview.coach.service.impl;

public final class CodeWrapper {

    private static final Long REVERSE_LIST_PROBLEM_ID = 103L;

    private CodeWrapper() {
    }

    public static String wrap(Long problemId, String userCode) {
        if (problemId == null || userCode == null) {
            return userCode;
        }
        if (REVERSE_LIST_PROBLEM_ID.equals(problemId)) {
            return wrapReverseList(userCode);
        }
        return userCode;
    }

    private static String wrapReverseList(String solutionCode) {
        return """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int n = sc.nextInt();

                        ListNode dummy = new ListNode(0);
                        ListNode tail = dummy;
                        for (int i = 0; i < n; i++) {
                            tail.next = new ListNode(sc.nextInt());
                            tail = tail.next;
                        }

                        Solution solution = new Solution();
                        ListNode result = solution.reverseList(dummy.next);
                        while (result != null) {
                            System.out.print(result.val);
                            if (result.next != null) {
                                System.out.print(" ");
                            }
                            result = result.next;
                        }
                    }
                }

                class ListNode {
                    int val;
                    ListNode next;

                    ListNode(int val) {
                        this.val = val;
                    }
                }

                """ + solutionCode;
    }
}
