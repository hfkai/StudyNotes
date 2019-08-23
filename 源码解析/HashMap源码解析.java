  #jdk8的Hashmap
  #
	/**
    *
	*HashMap 源码解析 
	*@autor hfk
	*/
/**
*put过程
	 1、对key求Hash值(先求key的hasCode()，再根据这个值对其进行前
		16位和后16位的一个异或扰动，尽量使其index均匀分布)，
		然后根据hash&(n-1) (PS:n必须为2的n次幂(2^n 对后面哈希函数
		和扩容的判断有影响 if ((e.hash & oldCap) == 0)，
		扩容之后的index要么是原来的，要么是加上+length)，即二进制是
		10000...的形式，所以n-1才能是0111...的形式，所以&之后才能均匀的分布到
		0-15的index里面) 计算下标(相当于hash%n)
*	 2、如果没有碰撞就直接放在桶中（碰撞了就以链表形式放在后面），节点存在就替换旧值
	3、桶满了（容量*加载因子）0.75(实践证明，0.75比较满足泊松分布,)
	(在理想情况下，使用随机哈希吗，
	节点出现的频率在hash桶中遵循泊松分布，
	同时给出了桶中元素的个数和概率的对照表)
	* 0:    0.60653066
	* 1:    0.30326533
	* 2:    0.07581633
	* 3:    0.01263606
	* 4:    0.00157952
	* 5:    0.00015795
	* 6:    0.00001316
	* 7:    0.00000094
	* 8:    0.00000006
	从上表可以看出当桶中元素到达8个的时候，概率已经变得非常小，
	也就是说用0.75作为负载因子，每个碰撞位置的链表长度超过8个是几乎不可能的。
	hash容器指定初始容量必须为2的幂次方。
	HashMap负载因子为0.75是空间和时间成本的一种折中
	如果取值0.5的话，会增加未使用的空间，如果取1的话，会增加结构的复杂度
	0.75设置为一个经验值（由于长度是2的整次幂，所以*0.75一般都可以得到一个整数的结果）
	
*/    
	public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }
	 static final int hash(Object key) {
        int h;
        //将前面的16位和后面的16位进行异或，使扰动之后的index结果尽可能的不同
		//这么做也能在保证table的length比较小的时候，也能
		//保证到高低位的bit都参与到hash的计算中来，同时也不会有太大的开销
		return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
		
    }
  /**
  @param evict 如果为false，则表处于创建模式。
  如果为true的话，就删除最久没使用的
  */
  final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {

        Node<K,V>[] tab; Node<K,V> p; int n, i;
		//判断当前的table是否为空，为空的话初始化table
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((p = tab[i = (n - 1) & hash]) == null)//(n-1)&hash 想当于hash%n 随机分布到0-15
            tab[i] = newNode(hash, key, value, null);//当前的table[i]里面没有链表，初始化
        else {
			//当前table[i]存在节点
            Node<K,V> e; K k;
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k)))){
			//如果要插入的节点已经存在，且key相等，赋值给e
                e = p;
			}else if (p instanceof TreeNode){
				//如果table[i]里面已经有红黑树，则插入红黑树，并进行旋转的操作
				//如果有相同hash和相同key的话，就返回值
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            }else {
				//单链表，往下查找并插入节点
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
						//找到null的节点，进行插入
                        p.next = newNode(hash, key, value, null);
						//如果这个大于等于8，则把单链表变成红黑树
						//这里你可能有个疑问，8-1不是等于7吗？
						//那是因为(binCount是从下标0开始的！！！！)
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))){
                        //往下找，找到相同的key节点,则break结束循环
						break;
						}
                    p = e;
                }
            }
            if (e != null) { // existing mapping for key
			//e不为null，则说明有相同的节点，则修改value的值
                V oldValue = e.value;
				//由于put操作的时候，onlyIfAbsent传的是false
				//则旧的值终会被替换
				/**@param onlyIfAbsent 代表着是否保留旧的value
				**/
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
				//afterNodeAccess 是一个回调方法，回调相同key的节点
				/**@link LinkedHashMap有用到这个**/
                afterNodeAccess(e);
                return oldValue;
            }
        }
        ++modCount;//结构修改的次数加1   迭代器 failfast
		//size 加1之后判断是否超出预期的容量值
        if (++size > threshold){
			//进行扩容
            resize();
		}
        afterNodeInsertion(evict);//也是LinkedHashMap用到的.
		//根据Lru算法来删除最久没使用的
		//节点
        return null;
    }
	//初始化和扩容
	 final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;//旧的数组长度
        int oldThr = threshold;//旧的预期值
        int newCap, newThr = 0;
        if (oldCap > 0) {
			//已经初始化过，现在要进行扩容
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY){
				//对旧的数组长度和旧的预期值进行2倍的扩容
						 
                newThr = oldThr << 1; // double threshold
			}
        } else if (oldThr > 0){ // initial capacity was placed in threshold
		 //进行数组的初始化
            newCap = oldThr;
		}else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;//默认的初始化长度 1<<4 16  
			//初始化预期值
		   newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        
		}
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
				//对旧的数组进行一个数据迁移
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
					//如果oldTab[j] 里面有东西(红黑树或者链表)
                    oldTab[j] = null;//清除旧的格子,对节点进行重新分布
                    if (e.next == null){//只有一个节点
                        newTab[e.hash & (newCap - 1)] = e;//用哈希函数重新计算index
                    }else if (e instanceof TreeNode){
						//格子里面装着红黑树，对其进行拆解...待弄清
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    }else { // preserve order
					//格子里面装着多个节点,先初始化4个节点
					//分别是头尾高低节点
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;//下一个节点
                        do {
                            next = e.next;
							//oldCap是2的整次幂 (因此二进制是
							//10000....)，以16为例子，结果只可能
							//是0或者16
                            if ((e.hash & oldCap) == 0) {
								//代表是在0-16里面
								//所以新的数组还在里面，就不用迁移
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }else {
								//节点的hash是可以在16之外的
								//进行迁移
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
							//hash&16判断比较小的数据
							//位置不变
                            loTail.next = null;
                            newTab[j] = loHead;//loHead存储着hash…&16之后比较小
							//节点集合
                        }
                        if (hiTail != null) {
							//hash&16判断比较小的数据
							//位置加上旧的数组长度
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;//hiHead存储着hash&16之后比较大
							//节点集合
                        }
                    }
                }
            }
        }
        return newTab;
    }

	/**
     * 得到一个节点
     */
     final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
                //根据hash值找到的这个index下标不为null
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k)))){
                    //找到相同的节点，返回first这个节点
                return first;
            }
            if ((e = first.next) != null) {
<<<<<<< HEAD
                //下一个节点
                if (first instanceof TreeNode){
                    //如果这个节点是红黑树
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
=======
                if (first instanceof TreeNode){//first节点是红黑树
                //红黑树获取节点
                    return ((TreeNode<K,V>)first)
                    .getTreeNode(hash, key);
>>>>>>> a8d78e1e7dd35c34323411f62166e4fcde03708c
                }
                do {
                    //不是红黑树,也就是说是链表，因此对其进行遍历循环查询
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }
    	//Q：1、HashMap的核心数据结构是什么？数据+链表+红黑树 ，因为红黑树的查找性呢过
	//比链表高
	//HashMap存储Element为什么不用取模？取模有哪些弊端？
	//
	//JDK8HashMap性能
	//jdk8比jdk7性能要高15%-20%
	//---------------------------LinkedHashMap 基于map的双向链表-----------------------
	
	//删除最久的节点,也就是head节点
	 void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K,V> first;
		//
        if (evict && (first = head) != null && removeEldestEntry(first)) {
			//移除第一个节点
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }
	//这个是LinkedHashMap的一个接口，如果实现这个，可以增加一些删除的判断条件
	/**
	*@link SizedMap 就实现了这个接口，并用  return this.size() > 10;
	*来进行是否删除的判断
	**/
	protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }
	//移除一个节点
	final Node<K,V> removeNode(int hash, Object key, Object value,
                               boolean matchValue, boolean movable) {
        Node<K,V>[] tab; Node<K,V> p; int n, index;
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (p = tab[index = (n - 1) & hash]) != null) {
				//table数组不为null且要删除这个table[index]有这个节点
				
            Node<K,V> node = null, e; K k; V v;
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k)))){
					//直接把这个节点赋值给node
                node = p;
			}else if ((e = p.next) != null) {
                if (p instanceof TreeNode)
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    do {
                        if (e.hash == hash &&
                            ((k = e.key) == key ||
                             (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            if (node != null && (!matchValue || (v = node.value) == value ||
                                 (value != null && value.equals(v)))) {
									 //移除相同节点
                if (node instanceof TreeNode){
					//红黑树的移除，并进行旋转，重新着色
                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                }else if (node == p){
					//链表第一个就是p，就直接把node.next 赋值给格子
                    tab[index] = node.next;
                }else{
					//此时p是node节点的上一个节点
					//把找到node.next 关联到p.next上
                    p.next = node.next;
				}
                ++modCount;//移除也算是修改了一次结构
                --size;//占用的大小减一
                afterNodeRemoval(node);//LinkedHashMap 用到，删除双向链表用的
				//至于是在哪里关联双向链表
				//是在LinkedHashMap的Entry里面
				/**
				*static class Entry<K,V> extends HashMap.Node<K,V> {
				*		Entry<K,V> before, after;//初始化前后两个节点
				*		Entry(int hash, K key, V value, Node<K,V> next) {
				*			super(hash, key, value, next);
				*		}
				*	}
				*
				*/
                return node;
            }
        }
        return null;
    }
	
	  void afterNodeRemoval(Node<K,V> e) { // unlink
	 // 关联p的前后两个节点
        LinkedHashMap.Entry<K,V> p =
            (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.before = p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a == null)
            tail = b;
        else
            a.before = b;
    }

	/**
	**@param accessOrder   false： 基于插入顺序     true：  基于访问顺序 
	*LRU 最近最少被使用的调度算法
	**/
	//回调相同key的节点  把相同的节点移动到最后一个节点tail上
	void afterNodeAccess(Node<K,V> e) { // move node to last
        LinkedHashMap.Entry<K,V> last;
		//accessOrder 默认为false,也就是默认不会进入
		//当accessOrder为true且last最后一个节点不是key相同的节点的时候
        if (accessOrder && (last = tail) != e) {
			//把e的前面和后面的节点分别赋值给b和a
            LinkedHashMap.Entry<K,V> p =
                (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
            p.after = null;
            if (b == null){
				//前面的节点为null，把e后面的节点a赋值给头结点
                head = a;
            }else{
				//前面的节点不为null,把e后面的节点a赋值在b后面，即b->a
                b.after = a;
			}
            if (a != null){
				//后面的节点不为null，把e前面的节点b赋值在a后面,即a->b
                a.before = b;
			}else{
				//后面的节点为null，把e前面的节点b赋值给尾节点
                last = b;
			}
            if (last == null){
				//尾节点是null，则把p给头结点(a和b都为null)
                head = p;
            }else {
				//尾节点不为null，则最后一个节点，放在p前面，并last的后一个节点指向p
				//也就是last<->p;
                p.before = last;
                last.after = p;
            }
			//把p赋值给尾节点,并modcount++(结构修改的次数)
            tail = p;
            ++modCount;
        }
    }