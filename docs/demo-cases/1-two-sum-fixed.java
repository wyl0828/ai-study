import java.util.HashMap;
import java.util.Map;

class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> indexByValue = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int need = target - nums[i];
            if (indexByValue.containsKey(need)) {
                return new int[] {indexByValue.get(need), i};
            }
            indexByValue.put(nums[i], i);
        }
        return new int[0];
    }
}
