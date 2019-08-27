
//# 面试题: 
//# 1 注解原理是什么？
//# 2 为什么注解元素不能是基本类型包装类？
//# 3 为什么编译时注解比运行时注解效率高？核心原理是什么？
//# 4 控制反转和依赖注入有什么区别？

/**
 * Member is an interface that reflects identifying information about
 * a single member (a field or a method) or a constructor.
  
 *
 * @see java.lang.Class
 * @see Field
 * @see Method
 * @see Constructor
 *
 * @author Nakul Saraiya
 */
public
interface Member {

    /**
        所有公共的成员()
     * Identifies the set of all public members of a class or interface,
     * including inherited members.
     * @see java.lang.SecurityManager#checkMemberAccess
     */
    public static final int PUBLIC = 0;

    /**

     * Identifies the set of declared members of a class or interface.
     * Inherited members are not included.
     * @see java.lang.SecurityManager#checkMemberAccess
     */
    public static final int DECLARED = 1;

    /**
     * Returns the Class object representing the class or interface
     * that declares the member or constructor represented by this Member.
     *
     * @return an object representing the declaring class of the
     * underlying member
     */
    public Class<?> getDeclaringClass();

    /**
     * Returns the simple name of the underlying member or constructor
     * represented by this Member.
     *
     * @return the simple name of the underlying member
     */
    public String getName();

    /**
     * Returns the Java language modifiers for the member or
     * constructor represented by this Member, as an integer.  The
     * Modifier class should be used to decode the modifiers in
     * the integer.
     *
     * @return the Java language modifiers for the underlying member
     * @see Modifier
     */
    public int getModifiers();

    /**
     * Returns {@code true} if this member was introduced by
     * the compiler; returns {@code false} otherwise.
     *
     * @return true if and only if this member was introduced by
     * the compiler.
     * @since 1.5
     */
    public boolean isSynthetic();
}

// 在某一个线程的调用栈中，当 AccessController 的 checkPermission 方法被最近的调用程序（例如 A 类中的方法）调用时，
// 对于程序要求的所有访问权限，ACC 决定是否授权的基本算法如下：
// 1. 如果调用链中的某个调用程序没有所需的权限，将抛出 AccessControlException；
// 2. 若是满足以下情况即被授予权限：
// a. 调用程序访问另一个有该权限域里程序的方法，并且此方法标记为有访问“特权”；
// b. 调用程序所调用（直接或间接）的后续对象都有上述权限。
 public static void checkPermission(Permission perm)
                 throws AccessControlException
    {
        //System.err.println("checkPermission "+perm);
        //Thread.currentThread().dumpStack();

        if (perm == null) {//perm不能为null
            throw new NullPointerException("permission can't be null");
        }

        AccessControlContext stack = getStackAccessControlContext();
        // if context is null, we had privileged system code on the stack.
        if (stack == null) {
            Debug debug = AccessControlContext.getDebug();
            boolean dumpDebug = false;
            if (debug != null) {
                dumpDebug = !Debug.isOn("codebase=");
                dumpDebug &= !Debug.isOn("permission=") ||
                    Debug.isOn("permission=" + perm.getClass().getCanonicalName());
            }

            if (dumpDebug && Debug.isOn("stack")) {
                Thread.currentThread().dumpStack();
            }

            if (dumpDebug && Debug.isOn("domain")) {
                debug.println("domain (context is null)");
            }

            if (dumpDebug) {
                debug.println("access allowed "+perm);
            }
            return;
        }

        AccessControlContext acc = stack.optimize();
        acc.checkPermission(perm);
    }

   /**
    * 得到了堆栈的所有保护域
     * Returns the AccessControl context. i.e., it gets
     * the protection domains of all the callers on the stack,
     * starting at the first class with a non-null
     * ProtectionDomain.
     *
     * @return the access control context based on the current stack or
     *         null if there was only privileged system code.
     */

    private static native AccessControlContext getStackAccessControlContext();

   AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    // Tests to ensure the system properties table is fully
                    // initialized. This is needed because reflection code is
                    // called very early in the initialization process (before
                    // command-line arguments have been parsed and therefore
                    // these user-settable properties installed.) We assume that
                    // if System.out is non-null then the System class has been
                    // fully initialized and that the bulk of the startup code
                    // has been run.

                    if (System.out == null) {
                        // java.lang.System not yet fully initialized
                        return null;
                    }

                    String val =
                        System.getProperty("sun.reflect.noCaches");
                    if (val != null && val.equals("true")) {
                        useCaches = false;
                    }

                    initted = true;
                    return null;
                }
            });



  /**
     * Performs the specified <code>PrivilegedAction</code> with privileges
     * enabled. The action is performed with <i>all</i> of the permissions
     * possessed by the caller's protection domain.
     *
     * <p> If the action's <code>run</code> method throws an (unchecked)
     * exception, it will propagate through this method.
     *
     * <p> Note that any DomainCombiner associated with the current
     * AccessControlContext will be ignored while the action is performed.
     *
     * @param action the action to be performed.
     *
     * @return the value returned by the action's <code>run</code> method.
     *
     * @exception NullPointerException if the action is <code>null</code>
     *
     * @see #doPrivileged(PrivilegedAction,AccessControlContext)
     * @see #doPrivileged(PrivilegedExceptionAction)
     * @see #doPrivilegedWithCombiner(PrivilegedAction)
     * @see java.security.DomainCombiner
     */
    //使用特权
    @CallerSensitive
    public static native <T> T doPrivileged(PrivilegedAction<T> action);



