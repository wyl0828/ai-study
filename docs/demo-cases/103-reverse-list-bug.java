// Demo case: problemId=103 反转链表
// Mode: Solution
// Expected: WRONG_ANSWER because the original head is returned after pointer reversal.

class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode cur = head;

        while (cur != null) {
            ListNode next = cur.next;
            cur.next = prev;
            prev = cur;
            cur = next;
        }

        return head;
    }
}
