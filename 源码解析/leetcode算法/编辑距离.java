

// 输入: word1 = "horse", word2 = "ros"
// 输出: 3
// 解释: 
// horse -> rorse (将 'h' 替换为 'r')
// rorse -> rose (删除 'r')
// rose -> ros (删除 'e')
// 动态规划

链接：https://leetcode-cn.com/problems/edit-distance


 public int minDistance(String word1, String word2) {
        int row = word1.length() + 1;
        int col = word2.length() + 1;
        int[][] dp = new int[row][col];
        for(int i = 0;i<row;i++){
            dp[i][0] = i;
        }
        for(int i = 1;i<col;i++){
            dp[0][i] = i;
        }
        for(int i = 1;i<row;i++){
            for(int j = 1;j<col;j++){
                if(word1.charAt(i - 1) == word2.charAt(j - 1)){
                    dp[i][j] = dp[i - 1][j - 1];
                }else{
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j - 1],dp[i - 1][j]),dp[i][j - 1]) + 1;
                }
            }
        }
        return dp[row - 1][col - 1];
    }

