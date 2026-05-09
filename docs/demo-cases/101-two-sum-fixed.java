// Demo case: problemId=101 两数之和
// Mode: ACM
// Expected: ACCEPTED by checking complement before inserting the current number.

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) {
            nums[i] = sc.nextInt();
        }
        int target = sc.nextInt();

        Map<Integer, Integer> indexByValue = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int need = target - nums[i];
            if (indexByValue.containsKey(need)) {
                System.out.println(indexByValue.get(need) + " " + i);
                return;
            }
            indexByValue.put(nums[i], i);
        }


    }
}
