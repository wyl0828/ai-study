USE ai_interview_coach;

UPDATE problem
SET title = '有效字母异位词',
    description = '给定两个只包含小写字母的字符串 s 和 t，判断 t 是否是 s 的字母异位词。',
    input_format = '给定参数 s 和 t，无需处理标准输入。',
    output_format = '返回 true 或 false，无需自行打印结果。',
    template_code = 'class Solution {
    public boolean isAnagram(String s, String t) {
        // 请在这里实现判断逻辑
        return false;
    }
}',
    updated_at = NOW()
WHERE id = 102;

UPDATE problem
SET description = '给定一个单链表的头节点 head，请反转链表，并返回反转后的头节点。',
    input_format = '给定参数 head，无需处理标准输入。',
    output_format = '返回反转后的链表头节点，无需自行打印结果。',
    template_code = 'class Solution {
    public ListNode reverseList(ListNode head) {
        // 请在这里实现反转链表逻辑
        return null;
    }
}',
    updated_at = NOW()
WHERE id = 103;

UPDATE problem
SET title = '合并两个有序链表',
    description = '给定两个升序链表 list1 和 list2，请合并为一个升序链表并返回合并后的头节点。',
    input_format = '给定参数 list1 和 list2，无需处理标准输入。',
    output_format = '返回合并后的链表头节点，无需自行打印结果。',
    template_code = 'class Solution {
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        // 请在这里实现合并逻辑
        return null;
    }
}',
    updated_at = NOW()
WHERE id = 104;
