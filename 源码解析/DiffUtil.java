
Diff 差分算法
## http://www.xmailserver.org/diff2.pdf

/**
        final int max = oldSize + newSize + Math.abs(oldSize - newSize);
        final int[] forward = new int[max * 2];
        final int[] backward = new int[max * 2];
      final Snake snake = diffPartial(cb, range.oldListStart, range.oldListEnd,
                    range.newListStart, range.newListEnd, forward, backward, max);

 */
 private static Snake diffPartial(Callback cb, int startOld, int endOld,
            int startNew, int endNew, int[] forward, int[] backward, int kOffset) {
        final int oldSize = endOld - startOld;//旧的数组大小
        final int newSize = endNew - startNew;//新的数组大小

        if (endOld - startOld < 1 || endNew - startNew < 1) {//任何一个为0都返回null
            return null;
        }

        final int delta = oldSize - newSize;//两个的差值
        final int dLimit = (oldSize + newSize + 1) / 2;//限制值
        Arrays.fill(forward, kOffset - dLimit - 1, kOffset + dLimit + 1, 0);//初始化 kOffset - dLimit - 1 至  kOffset + dLimit + 1
        // 的下标为90
        Arrays.fill(backward, kOffset - dLimit - 1 + delta, kOffset + dLimit + 1 + delta, oldSize);
        //初始化 kOffset - dLimit - 1 + delta 至   kOffset + dLimit + 1 + delta 的下标的值的oldSize
        final boolean checkInFwd = delta % 2 != 0;//奇数
        for (int d = 0; d <= dLimit; d++) {
            for (int k = -d; k <= d; k += 2) {
                // find forward path
                // we can reach k from k - 1 or k + 1. Check which one is further in the graph
                int x;
                final boolean removal;
                if (k == -d || (k != d && forward[kOffset + k - 1] < forward[kOffset + k + 1])) {
                    x = forward[kOffset + k + 1];
                    removal = false;
                } else {
                    x = forward[kOffset + k - 1] + 1;
                    removal = true;
                }
                // set y based on x
                int y = x - k;
                // move diagonal as long as items match
                while (x < oldSize && y < newSize
                        && cb.areItemsTheSame(startOld + x, startNew + y)) {
                    x++;
                    y++;
                }
                forward[kOffset + k] = x;
                if (checkInFwd && k >= delta - d + 1 && k <= delta + d - 1) {
                    if (forward[kOffset + k] >= backward[kOffset + k]) {
                        Snake outSnake = new Snake();
                        outSnake.x = backward[kOffset + k];
                        outSnake.y = outSnake.x - k;
                        outSnake.size = forward[kOffset + k] - backward[kOffset + k];
                        outSnake.removal = removal;
                        outSnake.reverse = false;
                        return outSnake;
                    }
                }
            }
            for (int k = -d; k <= d; k += 2) {
                // find reverse path at k + delta, in reverse
                final int backwardK = k + delta;
                int x;
                final boolean removal;
                if (backwardK == d + delta || (backwardK != -d + delta
                        && backward[kOffset + backwardK - 1] < backward[kOffset + backwardK + 1])) {
                    x = backward[kOffset + backwardK - 1];
                    removal = false;
                } else {
                    x = backward[kOffset + backwardK + 1] - 1;
                    removal = true;
                }

                // set y based on x
                int y = x - backwardK;
                // move diagonal as long as items match
                while (x > 0 && y > 0
                        && cb.areItemsTheSame(startOld + x - 1, startNew + y - 1)) {
                    x--;
                    y--;
                }
                backward[kOffset + backwardK] = x;
                if (!checkInFwd && k + delta >= -d && k + delta <= d) {
                    if (forward[kOffset + backwardK] >= backward[kOffset + backwardK]) {
                        Snake outSnake = new Snake();
                        outSnake.x = backward[kOffset + backwardK];
                        outSnake.y = outSnake.x - backwardK;
                        outSnake.size =
                                forward[kOffset + backwardK] - backward[kOffset + backwardK];
                        outSnake.removal = removal;
                        outSnake.reverse = true;
                        return outSnake;
                    }
                }
            }
        }
        throw new IllegalStateException("DiffUtil hit an unexpected case while trying to calculate"
                + " the optimal path. Please make sure your data is not changing during the"
                + " diff calculation.");
    }
