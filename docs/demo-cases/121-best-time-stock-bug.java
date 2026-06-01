class Solution {
    public int maxProfit(int[] prices) {
        int minPrice = Integer.MAX_VALUE;
        int best = 0;

        for (int price : prices) {
            minPrice = Math.min(minPrice, price);
            best = Math.min(best, price - minPrice);
        }

        return best;
    }
}
