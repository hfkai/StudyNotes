#Java 中的安全模型
<h4>&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbspJava的执行程序分成本地和远程两种,本地是可信任的(可随便访问本地的资源)，远程是不受到信任的（限制访问本地资源），因此，就出现了沙箱的机制，以便用于隔离远程的代码。<br>
&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp在后续的 Java1.1 版本中，针对安全机制做了改进，增加了安全策略，允许用户指定代码对本地资源的访问权限。如图 2 所示，
<p>&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp在 Java1.2 版本中，再次改进了安全机制，增加了代码签名。不论本地代码或是远程代码，都会按照用户的安全策略设定，由类加载器加载到虚拟机中权限不同的运行空间，来实现差异化的代码执行权限控制。如图 3 所示</p>
<p>
&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp当前最新的安全机制实现，则引入了域 (Domain) 的概念。虚拟机会把所有代码加载到不同的系统域和应用域，系统域部分专门负责与关键资源进行交互，而各个应用域部分则通过系统域的部分代理来对各种需要的资源进行访问。虚拟机中不同的受保护域 (Protected Domain)，对应不一样的权限 (Permission)。存在于不同域中的类文件就具有了当前域的全部权限。以域来区分代码，不同域的代码拥有不同域的访问权限
doPrivileged @link 注解.java，换句话说这是一个“特权”，可以获得更大的权限，甚至比调用它的应用程序还要多，可以做到临时访问更多的资源。</p>

<p>&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp&nbsp例如，应用程序可能无法直接访问某些系统资源，但这样的应用程序必须得到这些资源才能够完成功能。针对这种情况，Java SDK 给域提供了 doPrivileged 方法，让程序突破当前域权限限制，临时扩大访问权限。下面内容会详细讲解一下安全相关的方法使用</p>
#Java安全控制实现




#java内存模型
