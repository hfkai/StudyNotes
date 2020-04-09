## 参考链接
https://www.sohu.com/a/211287207_684445 请先看这一篇
http://www.hollischuang.com/archives/2509
https://juejin.im/post/5b7d69e4e51d4538ca5730cb#heading-22


## 课程内容
* CPU多核并发缓存架构剖析
* java线程内存模型底层实现原理
* CPU缓存一致性协议详解
* 深入汇编语言底层理解volatile关键字
* 并发编程的可见性，原子性与有序性详解

1、java线程模型JMM
线程A->工作内存（共享变量副本）  <=> 主内存共享变量
线程B->工作内存（共享变量副本）  <=> 主内存共享变量
底层的数据操作都是通过java原子操作来实现的：（硬件级别实现，汇编语言）
read，load，use，assign，store，write，lock，unlock

总线加锁：（被out）
多个线程同时访问主内存共享变量的时候，加一个lock，必须等操作
解决完成之后，才可以unlock，也就是相当于把并行线路，变成串行
MESI缓存一致性机制
多个cpu从主内存读取同一个数据到各自的高速缓存，当其中某个cpu修改
了缓存里面的数据，该数据会马上同步回主内存，其他cpu通过总线嗅探机制可以
感知到数据的变化从而将自己的缓存里的数据失效。

# Volatile的汇编查看 ==> 汇编手册 指令 手册
* 查看运行代码的汇编指令.zip 解压后放到../jre/bin   dill  lib
* java程序汇编代码查看
VM options: -server -Xcomp -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly -XX:CompileCommand = compileonly,*VolatileVisibilityTest.prepareData
并选择有汇编zip的那个jre
# Volatile缓存可见性实现原理（C语言实现）

* 底层实现主要是通过汇编lock前缀 指令 ，它就会锁定这块内存区域的缓存 （缓存行锁定) 并回写到主内存

### IA-32架构软件开发者手册对lock指令的解释 
* 会将当前处理器缓存行的数据立即写回到系统内存
* 这个写回内存的操作会引起在其他cpu里缓存了该内存地址的数据无效(MESI协议)

# 并发编程三大特性
* 可见，原子，有序
* volatile保证可见与有序，但是不保证原子性，保证原子性需要借助synchronize
这样子的锁机制


# 总结
* 我们知道线程读取主内存共享变量的时候，会在自己的线程下面拷贝一个副本，以提高读取的效率，那么这个便会导致信息更新不及时
那么这就需要一个总线嗅探机制来解决，java设计者通过构建一个MESI缓存一致性协议，使得在总线上，如果发现有主内存变量的改变，便逐个去通知各个线程，以此来更新自己线程下的主内存变量副本