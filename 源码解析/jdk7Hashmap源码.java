JDK7.0
#1、HashMap 
	
	 public V put(K key, V value) {
			if (table == EMPTY_TABLE) {
				inflateTable(threshold);//初始化链表
			}
			if (key == null)
				return putForNullKey(value);//插入key为null的节点
			int hash = hash(key);//计算hash值
			int i = indexFor(hash, table.length);//由hash计算index
			for (Entry<K,V> e = table[i]; e != null; e = e.next) {
				
				Object k;
				//遍历找到key相同的节点
				if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
					V oldValue = e.value;
					e.value = value;
					e.recordAccess(this);
					return oldValue;
				}
			}

			modCount++;//修改次数+1，用于迭代器
			addEntry(hash, key, value, i);
			return null;
		}
		void addEntry(int hash, K key, V value, int bucketIndex) {
        if ((size >= threshold) && (null != table[bucketIndex])) {
            resize(2 * table.length);
            hash = (null != key) ? hash(key) : 0;
            bucketIndex = indexFor(hash, table.length);
        }

        createEntry(hash, key, value, bucketIndex);
    }
	
	   private void inflateTable(int toSize) {
        // Find a power of 2 >= toSize
        int capacity = roundUpToPowerOf2(toSize);//找到大于toSize的最近的2次幂

        threshold = (int) Math.min(capacity * loadFactor, MAXIMUM_CAPACITY + 1);
        table = new Entry[capacity];
        initHashSeedAsNeeded(capacity);
    }
	
	/**
     * Initialize the hashing mask value. We defer initialization until we
     * really need it.
     */
    final boolean initHashSeedAsNeeded(int capacity) {
		//刚进来的时候hashSeed = 0，所以currentAltHashing=false
        boolean currentAltHashing = hashSeed != 0;
        boolean useAltHashing = sun.misc.VM.isBooted() &&
                (capacity >= Holder.ALTERNATIVE_HASHING_THRESHOLD);
        boolean switching = currentAltHashing ^ useAltHashing;//current false
		//相当于switching=useAltHashing
        if (switching) {
            hashSeed = useAltHashing
                ? sun.misc.Hashing.randomHashSeed(this)
                : 0;
        }
        return switching;
    }
	
	 final int hash(Object k) {
        int h = hashSeed;//hashSeed
        if (0 != h && k instanceof String) {
            return sun.misc.Hashing.stringHash32((String) k);//如果capacity
			//超过了ALTERNATIVE_HASHING_THRESHOLD(即"jdk.map.althashing.threshold"设置的值,
			//一般不会超过，因为是Integer的max值，只有设置了这个，才有可能超过)
			//超过的话，则启用Hashing.murmur3_32();哈希函数来执行散列任务(一般只执行一次，下次就返回
			//执行后的值)
        }

        h ^= k.hashCode();

        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }
	
	private static volatile boolean booted = false;
	 public static boolean isBooted() {
        return booted;//返回的是true，不知道是在哪里被赋值了
    }
	//扩容
	void resize(int newCapacity) {
        Entry[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        Entry[] newTable = new Entry[newCapacity];
        transfer(newTable, initHashSeedAsNeeded(newCapacity));
        table = newTable;
        threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);
    }
	/**
	扩容移动数组的时候，多线程容易产生死循环
     * Transfers all entries from current table to newTable.
     */
    void transfer(Entry[] newTable, boolean rehash) {
        int newCapacity = newTable.length;
        for (Entry<K,V> e : table) {//遍历table数组
            while(null != e) {
                Entry<K,V> next = e.next;//线程2跑到这里被阻塞
				//等到线程1扩容完毕，(会产生一个位置的倒置)
				//线程2的e和next是
				//指向线程1扩容之后的新指针
				//从而会使得线程2有可能出现环，致使造成死循环的情况
                if (rehash) {//一般是false，具体请看@initHashSeedAsNeeded的解析
                    e.hash = null == e.key ? 0 : hash(e.key);
                }
                int i = indexFor(e.hash, newCapacity);
                e.next = newTable[i];//线程2
                newTable[i] = e;
                e = next;
            }
        }
    }

		