## 阻塞队列 
接口表示一个线程安放如和提取实例的队列。
// -----------------------------ArrayBlockingQueue----------
 /** Main lock guarding all access */
    final ReentrantLock lock;
  public ArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }
   public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull =  lock.newCondition();
    }

  public boolean add(E e) {
        if (offer(e))
            return true;
        else
            throw new IllegalStateException("Queue full");
    }
    public boolean offer(E e) {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lock();//锁住
        try {
            if (count == items.length)
                return false;
            else {
                insert(e);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }
   private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException();
    }

    // ---ReentrantLock---
// 支持重入性，表示能够对共享资源能够重复加锁，即当前线程获取该锁再次获取不会被阻塞
// 是否是公平锁
 public ReentrantLock(boolean fair) {
    //  ReentrantLock的lock最终会回调到这个sync的lock()
        sync = fair ? new FairSync() //公平锁
        : new NonfairSync();//非公平锁
    }
    public void lock() {
       sync.lock();
    }
 static final class FairSync extends Sync {
    // 公平锁
       private static final long serialVersionUID = -3000897897090466540L;
        final void lock() {
            //
            acquire(1);
        }

        /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }
 static final class NonfairSync extends Sync {
     //非公平锁
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * Performs lock.  Try immediate barge, backing up to normal
         * acquire on failure.
         */
        final void lock() {
            // 非公平锁同步
            if (compareAndSetState(0, 1))//
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

// -----------------------------AbstractQueuedSynchronizer-------------------------------
// public abstract class AbstractQueuedSynchronizer

    Unsafe unsafe = Unsafe.getUnsafe();
       private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;
     static {
        try {
            stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));//获取AbstractQueuedSynchronizer
                // 类中state变量的值
            headOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

     /**
     * The synchronization state.
     */
    private volatile int state;

    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a <tt>volatile</tt> read.
     * @return current state value
     */
    protected final int getState() {
        return state;
    }

    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a <tt>volatile</tt> write.
     * @param newState the new state value
     */
    protected final void setState(int newState) {
        state = newState;
    }


    protected final boolean compareAndSetState(int expect, int update) {
            // See below for intrinsics setup to support this
            // CAS操作是原子性的，所以多线程并发使用CAS更新数据时，可以不使用锁，JDK中大量使用了CAS来更新数据而防止加锁来保持原子更新
            // 更新status的值，如果expect==status的话
       return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

// -----------------------------------------------------------

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }



 // --------------------------------