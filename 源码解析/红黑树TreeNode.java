    static final int MIN_TREEIFY_CAPACITY = 64;//红黑树所需要的最小长度

    //该方法用于比较红黑树val的值
    //identityHashCode和hashCode的区别是，
    //identityHashCode会返回对象的hashCode，而不管对象是否重写了hashCode方法。
    public static native int identityHashCode(Object x);
    //假如hashCode一样的话，而且类也一样(也就是类重写了hashCode
    //,并且返回了一样的值)，key又不相同，HashMap就会
    //去比较两者对象的HashCode

    /** 
    *
     * 当单链表大于等于8的时候，转化为红黑树
     * Replaces all linked nodes in bin at index for given hash unless
     * table is too small, in which case resizes instead.
     */
    final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;//
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY){
            //当tab数组为null或者少于红黑树所需要的最小长度，进行扩容，让链表均匀分配
            resize();
        }else if ((e = tab[index = (n - 1) & hash]) != null) {
            //当前tab[index]已经有元素
            TreeNode<K,V> hd = null, tl = null;//初始化两个TreeNode 的节点
            //
            do {
                //把TreeNode的节点装进hd
                TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null){
                    //t1为null表示是第一次进来
                    hd = p;
                }else {
                    //一个一个关联起来
                    p.prev = tl;//tl<-p
                    tl.next = p;//tl->p
                    //tl<=>p
                }
                tl = p;//替换tl为下一个节点
            } while ((e = e.next) != null);
            if ((tab[index] = hd) != null){
                //调用TreeNode转化红黑树的方法
                hd.treeify(tab);
            }
        }
    }

     // For treeifyBin
    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }
// ----------------------------TreeNode内部类----------------------------------
        TreeNode<K,V> parent;  // red-black tree links 红黑关联
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        boolean red;
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }

        /**
         * Forms tree of the nodes linked from this node.
         * @return root of tree
         */
        final void treeify(Node<K,V>[] tab) {
            TreeNode<K,V> root = null;
            for (TreeNode<K,V> x = this, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;
                x.left = x.right = null;
                if (root == null) {
                    x.parent = null;
                    x.red = false;
                    root = x;
                }else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = root;;) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h){
                            dir = -1;
                        }else if (ph < h){
                            dir = 1;
                        }else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0){
                            dir = tieBreakOrder(k, pk);
                        }
                        TreeNode<K,V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            //红黑平衡二叉树可以做到，插入一个值的时候，
                            // 这个值在最底的那个节点，即左右子树都为null
                            //所以这个判断就是找到最下面的那个节点（叶子）
                            //表示p节点的子节点为null，此时就要做一个插入动作
                            x.parent = xp;//x的父节点是p
                            if (dir <= 0){
                                //在左边
                                xp.left = x;
                            }else{
                                //在右子树
                                xp.right = x;
                            }
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            moveRootToFront(tab, root);
        }


        /**
         * Ensures that the given root is the first node of its bin.
         */
        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                int index = (n - 1) & root.hash;
                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];
                if (root != first) {
                    Node<K,V> rn;
                    tab[index] = root;
                    TreeNode<K,V> rp = root.prev;
                    if ((rn = root.next) != null)
                        ((TreeNode<K,V>)rn).prev = rp;
                    if (rp != null)
                        rp.next = rn;
                    if (first != null)
                        first.prev = root;
                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
            }
        }
        
        /**
         *  红黑树的五大特征
         *  1、每个结点要么是红的要么是黑的。  
         *  2、根结点是黑的。  
         *  3、每个叶结点（叶结点即指树尾端NIL指针或NULL结点）都是黑的。  
         *  4、如果一个结点是红的，那么它的两个儿子都是黑的。  
         *  5、对于任意结点而言，其到叶结点树尾端NIL指针的每条路径都包含相同数目的黑结点。 
         */
          //修复红黑树的平衡插入  包括左右旋、重新着色等等
          /**
           *  (1)如果插入的结点的父结点是黑色，由于此不会违反性质2和性质4，红黑树没有被破坏，所以此时什么也不做
           * （2）如果插入的节点的父节点是root节点，则直接返回root
           *● 插入修复情况1：如果当前结点的父结点是红色且祖父结点的另一个子结点（叔叔结点）是红色
           *● 插入修复情况2：当前节点的父节点是红色,叔叔节点是黑色，当前节点是其父节点的右子树
           *● 插入修复情况3：当前节点的父节点是红色,叔叔节点是黑色，当前节点是其父节点的左子树
           *
           */
          static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                    TreeNode<K,V> x) {
            x.red = true;//默认值是红色
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {//初始化4个节点
 //FIXME 推测 :xp是x父节点,xpp是xp的父节点，xppl是xpp的左节点，xppr是xpp的右节点
                if ((xp = x.parent) == null) {//这是根节点，
                //所以返回x,并把red变成false
                    x.red = false;
                    return x;
                }else if (!xp.red || (xpp = xp.parent) == null){
                    //满足不用修复的(1)(2)情况
                    return root;
                }
                if (xp == (xppl = xpp.left)) {
                    //x的父节点是祖父节点的左子树
                    if ((xppr = xpp.right) != null && xppr.red) {
                        // x的祖父节点的右子树不为null且是红色 
                        
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                }else {
                    //x的父节点是祖父节点的右子树
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }
                    else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }