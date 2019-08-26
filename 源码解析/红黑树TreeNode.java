    ## 红黑树讲解 本文最好对照着此文中来看，尤其是修复的情况与源码中一一对应，你会
    #  豁然开朗许多,如果不能，请告知，我就再改改咯(O^O)
    # 当然，鉴于本人水平有限，文中如果有差错，请指正
    ## https://blog.csdn.net/v_july_v/article/details/6105630
    ## https://www.jianshu.com/p/d780ed60874a 左右旋动画化gif（写博客的时候，可以贴图上去）

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
                //hd<->p1<->p2<->p3.....
                TreeNode<K,V> p = replacementTreeNode(e, null);//把链表的节点
                // 转化成红黑树的一个节点
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
            if ((tab[index] = hd) != null){//把hd重新赋值给table[index]
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
        TreeNode extends LinkedHashMap.LinkedHashMapEntry<K,V> 
        LinkedHashMapEntry<K,V> extends HashMap.Node<K,V> 
        //这两句说明了TreeNode 集成自LinkedHashMap.LinkedHashMapEntry，
        // 也就是拥有linke...的双链表和单链表的next指针
 
        TreeNode<K,V> parent;  // red-black tree links 红黑关联
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // needed to unlink next upon deletion  用于最后的调整tab链表的位置
        boolean red;
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }

        /**
         *将table[index]的节点转换成红黑树
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
                    root = x;//把第一个节点初始化给root
                }else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K,V> p = root;;) {//
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h){
                            dir = -1;
                        }else if (ph < h){
                            dir = 1;
                        }else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0){
                            //ph = h
                            //kc == null且经过查找x.key的class也是空
                            //或者@link compareComparables()返回0
                            //就只能去比较两者对象的identityHashCode值了
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
                            //对插入的节点x进行整个红黑树的插入平衡(包括着色，旋转等)
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }
            //保证root根节点是在tab数组元素上的第一个节点 
            moveRootToFront(tab, root);
        }
        /**
            比较两者对象的原始hashCode值
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                (d = a.getClass().getName().
                 compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                     -1 : 1);
            return d;
        }
    /**
      如果x是null或者x的类不是和k的类是一样的话就返回0，否则就用compareTo比较两者的大小
     * Returns k.compareTo(x) if x matches kc (k's screened comparable
     * class), else 0.
     **/
    @SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable)k).compareTo(x));
    }
    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;//如果是string 直接返回
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                        ((p = (ParameterizedType)t).getRawType() ==
                         Comparable.class) &&
                        (as = p.getActualTypeArguments()) != null &&
                        as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

        /**
         *
         *   确保给定的根是其table[index]的第一个节点。
         * 思路：因为TreeNode是一个双链表+红黑树的结构
         * 所以要替换成首个节点，原有的节点就应该放在root的后面，并用
         * 双链表关联起来
         * 也就是root<->first,并且要把原先的结构rootprev<->root<->rootnext
         * 替换成rootprev<->rootnext
         *举个例子，table[index]存的是1<->2<->3<->4
         * 经过红黑树之后root节点变成2，first节点还是1
         *所以需要把2移动到前面来，即变成2<->1<->3<->4
         *注意节点2包括红黑树是(left 和 right)，但是前后节点还是要跟之前是一样的
         * Ensures that the given root is the first node of its bin.
         */
        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
            int n;
            if (root != null && tab != null && (n = tab.length) > 0) {
                //如果root和tab不为null
                int index = (n - 1) & root.hash;//计算下标的值
                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];//找到这个index所在的第一个节点
                if (root != first) {//如果不等，则进入
                    Node<K,V> rn;
                    tab[index] = root;//将root（调整好的红黑树赋值给tab）
                    TreeNode<K,V> rp = root.prev;//找到root的前一个节点rp
                    if ((rn = root.next) != null){
                        //root的下一个节点rn不为null的话，就把root的前一个节点rp赋值给
                        //rn的前一个节点也就是之前是rp<-root->rn
                        // 现在变成是rp<-rn
                        ((TreeNode<K,V>)rn).prev = rp;
                    }
                    if (rp != null){
                        //rp不为null，就讲rp和rn关联起来
                        //也就是变成了rp<->rn
                        rp.next = rn;
                    }
                    if (first != null){
                        //将root赋值给first的前一个节点root<-first
                        first.prev = root;
                    }
                    //将root和first关联起来,变成root<->first
                    // 并将root的前一个节点变成null,也就是 null<-root<->first
                    root.next = first;
                    root.prev = null;
                }
                assert checkInvariants(root);
                //上面的代码你可能看不懂，举个栗子eg:
                System.out.println("测试1");
                  assert false;
                System.out.println("测试2");
                // 这个的输出结果是
                   测试1
                    Exception in thread "main" Disconnected from the target VM, 
                    address: '127.0.0.1:61407', transport: 'socket'
                    java.lang.AssertionError
                        at com.example.Jianji.main(Jianji.java:29)
                // 以上结果并没有测试2的输出
                // ******************
                //assert true 正常运行 false 会抛出一个异常AssertionError，程序结束退出.
               // 注意是错误，这就使得后面的代码直接不执行了
               //当然，这要在Vm Options(在Run->Configurations)里面设置-ea才会生效
               //否则无效，当然，这种方式存在很大的不确定性，所以仅仅用于调试之用
            }
        }
        //检查t节点是否有效
        static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
            //为了方便观察，我调整了源码的顺序
            TreeNode<K,V> tp = t.parent, //t的父节点
            tb = t.prev, //t的前一个节点
            tn = (TreeNode<K,V>)t.next,//t的后一个节点
            tl = t.left,//t的左子树 
            tr = t.right;//t的右子树
           
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }
        /**
          红黑树演示代码下载
          ：http://files.cnblogs.com/files/bbvi/RedBlackBinaryTree.rar
          以1<->2<->3<->4为例子 代入下面代码，你会豁然开朗，
          在这就不做特别说明了。
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
           解决方案：
           1、 将当前节点的父节点和叔叔节点涂黑，祖父结点涂红，把当前结点指向祖父节点，
               从新的当前节点重新开始算法。
           2、（1）当前节点的父节点做为新的当前节点，以新当前节点为支点左旋。
               (2）父节点变为黑色，祖父节点变为红色，在祖父节点为支点右旋。
           3、（1）当前节点的父节点做为新的当前节点，以新当前节点为支点右旋。
              （2）父节点变为黑色，祖父节点变为红色，在祖父节点为支点左旋。
           *
           */
          static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                    TreeNode<K,V> x) {
            x.red = true;//默认值是红色
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {//初始化4个节点
        //说明:xp是x父节点,xpp是xp的父节点，xppl是xpp的左节点，xppr是xpp的右节点
                if ((xp = x.parent) == null) {//这是根节点，
                //所以返回x,并把red变成false（根部节点必须是黑色的）
                    x.red = false;
                    return x;
                }else if (!xp.red || (xpp = xp.parent) == null){
                    //父节点是黑色，或者祖父节点是null，也就是说
                    //xp是root 满足不用修复的(1)(2)情况
                    return root;
                }
                 //这里请注意，下面都是父节点xp颜色是红色的情况，
                //  xp是黑色的情况在上面已经都判断完毕了！！！
                if (xp == (xppl = xpp.left)) {
                    //x的父节点是祖父节点的左子树
                    if ((xppr = xpp.right) != null && xppr.red) {
                        //case 1: 参考解决方案1
                        // x的祖父节点的右子树不为null且是红色 (叔叔节点是红色)
                        //满足修复情况1，则用重新着色修复
                        // 一个红色节点拥有两个黑色节点
                        xppr.red = false;//把叔叔节点变成黑色
                        xp.red = false;//把x的父节点变成黑色
                        xpp.red = true;// 把根部节点变红
                        x = xpp;//把x变成是x的祖父节点 
                        //这个着色完毕，进入下一个循环（就是往上寻找遍历）
                    }else {
                         // else 代表如果x的祖父节点的右子树节点为null或者是黑色
                        //case 2 
                        // 当前节点的父节点是红色,叔叔节点是黑色，当前节点是其父节点的右子节点
                        //  参考解决方案2(1)
                        if (x == xp.right) {
                            //如果x是父节点的右子树,则以x的父节点xp进行左旋，并将x赋值成xp。
                            root = rotateLeft(root, x = xp);
                            //对xp和xpp重新赋值
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        // case 2（2） 参考解决方案2(2)
                        //如果父节点不为空，设置为黑色black
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                //祖父节点不为null，设置为red,并对其祖父节点
                                //这里注意了，是祖父节点进行右旋("上面" 加重点 是讲父亲节点当成
                                // 新节点，再对其进行左旋,参考解决方案2 case2)
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                }else {
                    //x的父节点是祖父节点的右子树 与上面相反，不过原理一样
                    if (xppl != null && xppl.red) {
                        //case 1: 参考解决方案1
                        // x的祖父节点的左子树不为null且是红色 (叔叔节点是红色)
                        //满足修复情况1，则用重新着色修复
                        // 一个红色节点拥有两个黑色节点
                        xppl.red = false;//把叔叔节点变成黑色
                        xp.red = false;//把x的父节点变成黑色
                        xpp.red = true;// 把根部节点变红
                        x = xpp;//把x变成是x的祖父节点
                        //这个着色完毕，进入下一个循环（就是往上寻找遍历）
                    }else {
                          // else 代表如果x的祖父节点的左子树节点为null或者是黑色
                        //case 3 
                        // 当前节点的父节点是红色,叔叔节点是黑色，当前节点是其父节点的左子树
                        //  参考解决方案3(1)
                        if (x == xp.left) {
                        //如果x是父节点的左子树,则以x的父节点xp进行右旋，并将x赋值成xp。
                            root = rotateRight(root, x = xp);
                              //对xp和xpp重新赋值
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                 //祖父节点不为null，设置为red,并对其祖父节点
                                //这里注意了，是祖父节点进行左旋("上面" 加重点 是讲父亲节点当成
                                // 新节点，再对其进行右旋,参考解决方案3(2) case3(2))
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

         /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR
        // 对p进行左旋
        
        /*************对红黑树节点x进行左旋操作 ******************/
        /*
        *   @param p是x的父节点
        * 左旋示意图：对节点x进行左旋  
        * 旋转的例子（图中的字母与下面代码的字母无关，只是为了说明下情况）
        *     p'                       p'
        *    /                       /
        *   x                       y
        *  / \                     / \
        * lx  y      ----->       x  ry
        *    / \                 / \
        *   ly ry               lx ly
        * 左旋做了三件事：
        * 1. 将y的左子节点赋给x的右子节点,并将x赋给y左子节点的父节点(y左子节点非空时)
        * 2. 将x的父节点p'(非空时)赋给y的父节点，同时更新p'的子节点为y(左或右)
        * 3. 将y的左子节点设为x，将x的父节点设为y
        */
        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                              TreeNode<K,V> p) {
            TreeNode<K,V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null){ // 是进行第一种变形
        //        p                       
        // *    / | \
        // *   O  |   x  (r)                
        // *  / \ |  /  \                 
        // * O1 O2 ->rl  O3   (--->这个虚线是指的(rl = p.right = r.left)这句代码)
                    rl.parent = p;//将rl和p关联
                }
                if ((pp = r.parent = p.parent) == null){
                //(pp = r.parent = p.parent)这句代码相当于
                //第二种情况，也就是将x父节点变成p的父节点
        //         
        //          fu (pp) null 也就是说到头了 这时候就可以将r赋值给root
        //         / |
        //        p  x(r)                     
        // *    /  \  / \
        // *   O    rl   O3         
        // *  / \                      
        // * O1 O2     并把  r赋值给root根节点,并将颜色设置为黑色
                    (root = r).red = false;
                }else if (pp.left == p){
                    //如果p是左边的节点，就把新的节点x(r)赋值到相同的位置
                    pp.left = r;
                }else{
                    //同理，如果p是右边的节点，就把新的节点x(r)赋值到相同的位置
                    pp.right = r;
                }
                //最后，把p赋值给r的左边节点，并更改p的父节点为r
                //(你可能有个疑问，更改左节点不就是更改父节点了吗，其实不是，
                //因为parent和left是两个不同的指针对象，所以要一个一个对其进行操作!)
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        /*
         * @param p是x的祖父节点
        * 右旋示意图：对节点y进行右旋
        * 旋转的例子（图中的字母与下面代码的字母无关，只是为了说明下情况）
        *        p’                   p‘
        *       /                   /
        *      y                   x
        *     / \                 / \
        *    x  ry   ----->      lx  y
        *   / \                     / \
        * lx  rx                   rx ry
        * 右旋做了三件事：
        * 1. 将x的右子节点赋给y的左子节点,并将y赋给x右子节点的父节点(x右子节点非空时)
        * 2. 将y的父节点p'(非空时)赋给x的父节点，同时更新p的子节点为x(左或右)
        * 3. 将x的右子节点设为y，将y的父节点设为x
        */
        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
                                               TreeNode<K,V> p) {
            TreeNode<K,V> l, pp, lr;
            if (p != null && (l = p.left) != null) {//
                if ((lr = p.left = l.right) != null){//跟左旋类似，进行第一种变形
        //        
        //         xpp (p)                      
        // *     /  | \
        // *   xp(l)|   xppr               
        // *  / \   |  /  \                 
        // * x   lr<- O2  O3   (--->这个虚线是指的(lr = p.left = l.right)这句代码)
        //参考步骤1   将p和lr关联
                    lr.parent = p;
                }
                if ((pp = l.parent = p.parent) == null){
        //(pp = l.parent = p.parent)这句代码相当于第二种情况
        //         
        //          fu (pp) null 也就是说到头了 这时候就可以将r赋值给root
        //         / |
        //        xp(l)
        //       /  \
        //      x  xpp (p)                      
        // *     /   \
        // *    lr     xpp               
        // *          /  \                 
        // *         O2  O3 并把  r赋值给root根节点,并将颜色设置为黑色
                    (root = l).red = false;
                }else if (pp.right == p){
                    pp.right = l;
                }else{
                    pp.left = l;
                }
                ////最后，把p赋值给l的右边节点，并更改p的父节点为r
                l.right = p;
                p.parent = l;
            }
            return root;
        }
        ##红黑树的插入 
       /**
         * 插入的步骤和上面的类似
         *有一点不同，就是增加了一个查找,如果存在相同的key，则改变旧的value.
         * Tree version of putVal.
         */
        final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab,
                                       int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            TreeNode<K,V> root = (parent != null) ? root() : this;//this不是root的话，就找.
            for (TreeNode<K,V> p = root;;) {
                int dir, ph; K pk;
                if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))//找到返回p节点
                    return p;
                else if ((kc == null &&
                          (kc = comparableClassFor(k)) == null) ||
                         (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        //找红黑树里面有没有这个节点  注find是红黑树的查找方法
                        // 这里不做特别说明，具体做法就是对比节点的值
                        //<中间节点的值就查左，＞就右边，等于就返回
                        TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                             (q = ch.find(h, k, kc)) != null) ||
                            ((ch = p.right) != null &&
                             (q = ch.find(h, k, kc)) != null))
                            return q;
                    }
                    //找不到,就计算dir的值，为下面的插入做准备
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) { //找到最底层，这里和上面
                // 差不多，就不做过多说明
                    Node<K,V> xpn = xp.next;
                    TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    xp.next = x;
                    x.parent = x.prev = xp;
                    if (xpn != null)
                        ((TreeNode<K,V>)xpn).prev = x;
                    moveRootToFront(tab, balanceInsertion(root, x));//平衡后将其设置为table[index]
                    //的第一个节点
                    return null;
                }
            }
        }

#红黑树的移除
       /**
         这个比普通的红黑树删除要复杂，因为要是删除节点到了2-6之间的话，会
         由红黑树转化成链表
         * Removes the given node, that must be present before this call.
         * This is messier than typical red-black deletion code because we
         * cannot swap the contents of an interior node with a leaf
         * successor that is pinned by "next" pointers that are accessible
         * independently during traversal. So instead we swap the tree
         * linkages. If the current tree appears to have too few nodes,
         * the bin is converted back to a plain bin. (The test triggers
         * somewhere between 2 and 6 nodes, depending on tree structure).
         */
        final void removeTreeNode(HashMap<K,V> map, Node<K,V>[] tab,
                                  boolean movable) {
            int n;
            if (tab == null || (n = tab.length) == 0)
                return;
            int index = (n - 1) & hash;
            TreeNode<K,V> first = (TreeNode<K,V>)tab[index], root = first, rl;
            // succ是next节点，pred是上一个节点 
            TreeNode<K,V> succ = (TreeNode<K,V>)next, pred = prev;
            if (pred == null){
                tab[index] = first = succ;//直接把first替换成next
            }else{
                pred.next = succ;//pred->succ
            }
            if (succ != null){
                succ.prev = pred;//pred<->succ把前后节点关联起来，相当于删除了这个节点
            }
            if (first == null){
                //不用移除了，已经没有了
                return;
            }
            if (root.parent != null){ 
                root = root.root();//找到first最顶端的root节点
            }
            if (root == null || root.right == null ||
                (rl = root.left) == null || rl.left == null) {
            //这个的意思是<=6就转化成红黑树(这里说下，不得不佩服
            // 设计者的功底，由于红黑树的特点，执行到最后的是到rl.left，此时红黑树
            // 中最多有6个节点)
                tab[index] = first.untreeify(map);  // too small 把TreeNode重新弄成Node
                return;
            }
            /***
           1、 没有儿子，即为叶结点。直接把父结点的对应儿子指针设为NULL，删除儿子结点就OK了。
            2、只有一个儿子。那么把父结点的相应儿子指针指向儿子的独生子，删除儿子结点也OK了。
           3、 有两个儿子。这是最麻烦的情况，因为你删除节点之后，还要保证满足搜索二叉树的结构。
            其实也比较容易，我们可以选择左儿子中的最大元素或者右儿子中的最小元素放到待删除节点的位置，
            就可以保证结构的不变。当然，你要记得调整子树，毕竟又出现了节点删除。习惯上大家选择左儿子中的
            最大元素，其实选择右儿子的最小元素也一样，没有任何差别，只是人们习惯从左向右。
            这里咱们也选择左儿子的最大元素，将它放到待删结点的位置。左儿子的最大元素其实很好找，
            只要顺着左儿子不断的去搜索右子树就可以了，直到找到一个没有右子树的结点。那就是最大的了。

             */
            //下面是二叉搜索树的删除  pl代表左边，pr右边
            /**
            @link https://blog.csdn.net/qq_37169817/article/details/78880110
             */

            TreeNode<K,V> p = this, pl = left, pr = right, replacement;
            if (pl != null && pr != null) {
                //有两个儿子情况
                TreeNode<K,V> s = pr, sl;
                //找到左边最大的元素
                while ((sl = s.left) != null){ // find successor
                    s = sl;
                }
                boolean c = s.red; s.red = p.red; p.red = c; // swap colors 交换p和s的颜色
                TreeNode<K,V> sr = s.right;//找到左边最大的元素，放到待删除的节点
                TreeNode<K,V> pp = p.parent;//p的父节点
                if (s == pr) { // p was s's direct parent
                //改变p父亲的方向
                    p.parent = s;
                    s.right = p;
                }else {

                    TreeNode<K,V> sp = s.parent;
                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }
                    if ((s.right = pr) != null)
                        pr.parent = s;
                }
                p.left = null;
                if ((p.right = sr) != null)
                    sr.parent = p;
                if ((s.left = pl) != null)
                    pl.parent = s;
                if ((s.parent = pp) == null){
                    root = s;
                }else if (p == pp.left){
                    pp.left = s;
                }else{
                    pp.right = s;
                }
                if (sr != null){
                    replacement = sr;
                }else{
                    replacement = p;
                }
            }else if (pl != null){//只有左边
                replacement = pl;
            }else if (pr != null){//只有右边
                replacement = pr;
            }else{
                replacement = p;//左右都没有
            }
            if (replacement != p) {
                //把replacement替换到p的位置上
                TreeNode<K,V> pp = replacement.parent = p.parent;
                if (pp == null)
                    root = replacement;
                else if (p == pp.left)
                    pp.left = replacement;
                else
                    pp.right = replacement;
                p.left = p.right = p.parent = null;
            }
            //注意，p的颜色和p最左边的节点的颜色交换过，也就是说
            //当最左边的节点是红色的话就不用修改红黑树的颜色结构
            // (具体去下载下那个html演示，就知道为什么了)
            //如果p是红色就返回root，否则就要做红黑树删除的修复,具体参考博客（有4种情况）
            TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);
            if (replacement == p) {  // detach 
            //也就说不需要修改二叉搜索树结构，那么直接把p的左右子树置null
                TreeNode<K,V> pp = p.parent;
                p.parent = null;
                if (pp != null) {
                    if (p == pp.left)
                        pp.left = null;
                    else if (p == pp.right)
                        pp.right = null;
                }
            }
            if (movable)//false的话就不root当做是首个节点
                moveRootToFront(tab, r);
        }

    