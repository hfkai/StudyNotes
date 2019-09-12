### Android Q 行为变更总结

##### 总结
本次更新主要强调了隐私性，增加了应用限制但是没有过大的改变。
* 应用播放声音权限受到限制，限制条件为`暂停的应用`，但是目前没有准确测试什么情况下的应用处于暂停状态，无法判断对`无声音乐`保活的影响。
* 后台启动Activity受到限制，尽管启动途径很多，但是归结起来还是需要自身为前台，或者被前台应用启动。只保留前台服务的情况下，不保证能启动Activity。目前没有准确的测试此限制对`一像素保活`的影响。
* Go设备上，`SYSTEM_ALERT_WINDOW`权限受到禁止，对于悬浮窗功能，将会受到限制。
* 应用主目录下的文件将不再具有执行权限。不论是java代码还是Native代码，都受到限制。 


##### 全面屏手势
<details>
<summary>详细内容</summary>

##### 系统UI调整。
**可以通过主题设置系统UI的透明化**
```xml
<!-- values-29/themes.xml: -->

    <style name="AppTheme" parent="...">
        <item name="android:navigationBarColor">@android:color/transparent</item>

        <!-- Optional, but recommended for full edge-to-edge rendering -->
        <item name="android:statusBarColor">@android:color/transparent</item>
    </style>
    
```
或者使用代码动态设置
```java
    view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
```
> 注意：部分系统提供的组件，如 [CoordinatorLayout](https://developer.android.com/reference/androidx/coordinatorlayout/widget/CoordinatorLayout) 或 [DrawerLayout](https://developer.android.com/reference/androidx/drawerlayout/widget/DrawerLayout)会自动包含对系统UI可见性的`Flag`，因此上述设置可能会失效。需要重新设置来覆盖。

----------------

由于系统UI组件的存在，设备窗口的边缘存在一定范围的不可用。但是如果通过代码填充满了，那么可以通过回调方法来取得这些尺寸来规避交互的冲突。
```java
    view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
        @Override
        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
            // 1. Move views on top edge down by insets.getSystemWindowInsetTop()
            // 2. Move views on bottom edge up by insets.getSystemWindowInsetBottom()
            // 3. Also check getSystemWindowInsetLeft/Right()
            //    (i.e landscape orientations)
            return insets.consumeSystemWindowInsets();
        }
    });
```
> 注意：此方法会返回系统UI区域，手势交互区域的边缘宽度。但是对于侧面返回手势区域，将不会返回，因为侧面返回允许通过函数覆盖操作。返回的手势区域主要是屏幕下方的任务切换与返回桌面操作的区域。

--------------------------------

`Android Q`中，支持全面屏的全手势操作，其中，返回操作将会类似于`iOS`的侧面滑动，从屏幕左右向内滑动，都会触发返回事件，对于有侧面滑动需求的应用，需要使用函数来重置手势。
```java
    List<Rect> exclusionRects;

    public void onLayout(
            boolean changedCanvas, int left, int top, int right, int bottom) {
        // Update rect bounds and the exclusionRects list
        setSystemGestureExclusionRects(exclusionRects);
    }

    public void onDraw(Canvas canvas) {
        // Update rect bounds and the exclusionRects list
        setSystemGestureExclusionRects(exclusionRects);
    }
```
> 注意：View.setSystemGestureExclusionRects() 方法是 Android Q 引入的。但是从 androidx.core:core:1.1.0-dev01 开始，ViewCompat 中也提供这种方法。

对于任务切换和返回桌面的手势操作，是不允许被覆盖的，但是可以通过获取区域阈值来绕过冲突区域，或者使用全屏模式，来隐藏冲突区域，用户可以通过两次手势来恢复系统手势。
```java
 WindowInsets.getMandatorySystemGestureInsets()
```
> 注意：对于`Home`键的手势交互是不允许被覆盖的，系统的优先级高于应用，因此只有设置为全屏模式下，才能使手势区域可用，但是多次滑动，仍然会重新激活。因此，就算是设置为全屏模式，仍然只能放置点击按钮在手势区域。
</details>

##### 移除应用主目录下的执行权限
插件化可能受到影响
<details>
<summary>详细内容</summary>

以 Android Q 为目标平台的不受信任的应用无法再针对应用主目录中的文件调用 exec()。原因在于违反了 [W^X](https://en.wikipedia.org/wiki/W%5EX)。
此外，以 Android Q 为目标平台的应用无法针对已执行 dlopen() 的文件中的可执行代码进行内存中修改。这包括含有文本重定位的所有共享对象 (.so) 文件。
</details>

##### WiFi直连广播修改
<details>
<summary>详细内容</summary>

在 Android Q 中，以下与 [WLAN 直连](https://developer.android.com/training/connect-devices-wirelessly/wifi-direct)相关的广播不再具有粘性。
* [WIFI_P2P_CONNECTION_CHANGED_ACTION] (https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_CONNECTION_CHANGED_ACTION)
* [WIFI_P2P_THIS_DEVICE_CHANGED_ACTION] (https://developer.android.com/reference/android/net/wifi/p2p/WifiP2pManager#WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

但是可以通过`get()`方法获取信息。
</details>

##### WLAN 感知
WiFi下的Socket连接可能得到优化
<details>
<summary>详细内容</summary>

Android Q 扩大了支持范围，可以使用 WLAN 感知数据路径创建 TCP/UDP 套接字。要创建连接到 ServerSocket 的 TCP/UDP 套接字，客户端设备需要知道服务器的 IPv6 地址和端口。这在之前需要通过频外方式进行通信（例如使用 BT 或 WLAN 感知第 2 层消息传递），或者使用其他协议（例如 mDNS）通过频内方式发现。
服务器可以执行以下任一操作：
* 初始化 ServerSocket 并设置或获取要使用的端口。
* 将端口信息指定为 WLAN 感知网络请求的一部分。

```java
	// 将端口信息指定为网络请求的一部分
    ServerSocket ss = new ServerSocket();
    WifiAwareNetworkSpecifier ns = new WifiAwareNetworkSpecifier
      .Builder(discoverySession, peerHandle)
      .setPskPassphrase(“some-password”)
      .setPort(ss.getLocalPort())
      .build();

    NetworkRequest myNetworkRequest = new NetworkRequest.Builder()
      .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
      .setNetworkSpecifier(ns)
      .build();
    
```
客户端会执行 WLAN 感知网络请求来获取服务器提供的 IPv6 和端口：
```java
    callback = new ConnectivityManager.NetworkCallback() {
      @Override
      public void onAvailable(Network network) {
        ...
      }
      @Override
      public void onLinkPropertiesChanged(Network network,
          LinkProperties linkProperties) {
        ...
      }
      @Override
      Public void onCapabilitiesChanged(Network network,
          NetworkCapabilities networkCapabilities) {
        ...
        TransportInfo ti = networkCapabilities.getTransportInfo();
        if (ti instanceof WifiAwareNetworkInfo) {
           WifiAwareNetworkInfo info = (WifiAwareNetworkInfo) ti;
           Inet6Address peerAddress = info.getPeerIpv6Addr();
           int peerPort = info.getPort();
        }
      }
      @Override
      public void onLost(Network network) {
        ...
      }
    };

    connMgr.requestNetwork(networkRequest, callback);
    
```
</details>

##### Go 设备上的 SYSTEM_ALERT_WINDOW
Go 设备上的悬浮窗功能将被禁用
<details>
<summary>详细内容</summary>

对于使用`Android Q`(Go 版本) 的设备，将无法获取到`SYSTEM_ALERT_WINDOW`权限，除非在升级到 `Q` 之前已经获得了此权限，否则将无法申请或使用权限。使用 [Settings.canDrawOverlays()](https://developer.android.com/reference/android/provider/Settings#canDrawOverlays(android.content.Context))将会固定返回`false`。发送携带[ACTION_MANAGE_OVERLAY_PERMISSION](https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION)信息的`Intent`，也只会被拦截并且跳转到一个警示页面提醒用户。
</details>

##### 移除了 SHA-2 CBC 加密套件
<details>
<summary>详细内容</summary>

以下 SHA-2 CBC 加密套件已从平台中移除：

* TLS_RSA_WITH_AES_128_CBC_SHA256
* TLS_RSA_WITH_AES_256_CBC_SHA256
* TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256
* TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384
* TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256
* TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384

并且推荐使用`GCM`的加密套件。
</details>


##### 应用使用情况
应用通知可能被屏蔽、无声音乐保活可能受到限制
<details>
<summary>详细内容</summary>

Android Q 引入了与应用使用情况相关的以下行为变更：

* UsageStats 应用使用情况的改进 -- 当在分屏或画中画模式下使用应用时，Android Q 现在能够使用 [UsageStats](https://developer.android.com/reference/android/app/usage/UsageStats) 准确地跟踪应用使用情况。此外，Android Q 现在可以跟踪免安装应用的使用情况。

* 按应用开启灰度模式 -- Android Q 现在可以将应用设为灰度显示模式。

* 按应用开启干扰模式 -- Android Q 现在可以选择性地将应用设为“干扰模式”，此时系统会 **禁止显示其通知，并且不会将其显示为推荐的应用**。

* 暂停和播放 -- 在 Android Q 中，**暂停的应用无法再播放音频**。
</details>

##### HTTPS 连接变更
HTTPS的连接方式出现调整
<details>
<summary>详细内容</summary>

如果运行 Android Q 的应用将 null 传递给 [setSSLSocketFactory()](https://developer.android.com/reference/javax/net/ssl/HttpsURLConnection.html#setSSLSocketFactory(javax.net.ssl.SSLSocketFactory))，现在会出现 `IllegalArgumentException`。在以前的版本中，将 null 传递给 setSSLSocketFactory() 与传入当前的[默认 SSL 套接字工厂](https://developer.android.com/reference/javax/net/ssl/HttpsURLConnection#getDefaultSSLSocketFactory())效果相同。

</details>

##### android.preference 库弃用
推荐使用 AndroidX preference
<details>
<summary>详细内容</summary>

相关文档：
[Android Jetpack](https://developer.android.com/jetpack)
[设置指南](https://developer.android.com/guide/topics/ui/settings)
[公开示例应用](https://github.com/googlesamples/android-preferences)
[参考文档](https://developer.android.com/reference/androidx/preference/package-summary)
</details>

##### 摄像头变更
摄像头方向需要兼容
<details>
<summary>详细内容</summary>

受到折叠屏设备兼容性的影响，摄像头的方向不再是确定的，并且跟随屏幕方向的。需要在代码逻辑中判断摄像头方向并且按照需求旋转。[摄像头变更](https://source.android.com/compatibility/9/android-9-cdd.html#7_5_5_camera_orientation)
同时，API 级别 24 以上时需要明确设置 `android:resizeableActivity`，并提供必要的功能来处理多窗口操作。
</details>

##### 共享内存
dalvik 映射发生变化
<details>
<summary>详细内容</summary>
Ashmem 更改了 `/proc/pid/maps` 中的 dalvik 映射的格式，这会影响那些直接解析映射文件的应用。如果应用依赖于 dalvik 映射格式，则应用开发者应该在设备上测试新的 /proc/pid/maps 格式并相应地进行解析。

以 Android Q 为目标平台的应用无法再直接使用 ashmem (/dev/ashmem)，而必须通过 NDK 的 ASharedMemory 类访问共享内存。此外，应用无法直接对现有 ashmem 文件描述符进行 IOCTL，而必须改为使用 NDK 的 ASharedMemory 类或 Android Java API 创建共享内存区域。这项变更可以提高使用共享内存时的安全性和稳健性，从而提高 Android 的整体性能和安全性。
</details>

##### Android 运行时只接受系统生成的 OAT 文件
Android 运行时 (ART) 不再从应用进程调用 dex2oat。这项变更意味着 ART 将仅接受系统生成的 OAT 文件。

##### 在 ART 中强制要求 AOT 正确性
针对预编译的稳定性，可能需要针对自定义ClassLoader或者dex加载器做调整
<details>
<summary>详细内容</summary>

过去，如果编译时和运行时的类路径环境不同，则 Android 运行时 (ART) 执行的预先 (AOT) 编译可能会导致运行时崩溃。Android Q 现在始终要求这些环境上下文相同，因而导致出现了以下行为变更：
* 自定义类加载器（即应用编写的类加载器，与 dalvik.system 软件包中的类加载器不同）并非由 AOT 编译。这是因为 ART 无法在运行时了解自定义的类查找实现。
* 辅助 dex 文件（即由主 APK 外的应用手动加载的 dex 文件）现在由 AOT 在后台进行编译，由于首次使用编译可能代价过高，因此会导致在执行前出现意外的延迟。请注意，对于应用，建议您采用拆分方法，并弃用辅助 dex 文件。
* Android 中的共享库（Android 清单中的 <library> 和 <uses-library> 条目）现在具有新的类加载器层次结构。
</details>

##### 支持可折叠设备
Activity 生命周期发生变化
<details>
<summary>详细内容</summary>

Android Q 包含支持可折叠设备和大屏设备的变更。

当应用在 Android Q 上运行时，onResume() 和 onPause() 方法的工作原理是不同的。当多个应用同时在多窗口模式或多显示屏模式下显示时，可见堆栈中所有可设置为焦点的顶层 Activity 都处于“已恢复”状态，但实际上焦点仅位于其中一个 Activity 上，即“在最顶层处于已恢复状态”的 Activity。在 Android Q 之前的版本中运行时，一次只能恢复系统中的一个 Activity，而所有其他可见 Activity 都处于已暂停状态。

请不要将“焦点位于”的 Activity 与“在最顶层处于已恢复状态”的 Activity 混淆。系统会根据 Z-Order 来为 Activity 分配优先级，以便为用户最后进行互动的 Activity 提供更高的优先级。Activity 可能在顶层处于已恢复状态，但焦点却并不位于其上（例如，如果通知栏展开）。

在 Android Q 中，您可以订阅 [onTopResumedActivityChanged()](https://developer.android.com/reference/android/app/Activity#onTopResumedActivityChanged(boolean)) 回调，以便在 Activity 获取或失去在最顶层处于已恢复状态的位置后收到通知。这相当于 Android Q 之前版本中的已恢复状态；如果您的应用使用的专用或单一资源可能需要与其他应用共享，这可以作为有用的提示。

[resizeableActivity](https://developer.android.com/guide/topics/ui/multi-window#resizeableActivity) 清单属性的行为也发生了变化。如果某个应用在 Android Q 中设置 resizeableActivity=false，则当可用屏幕尺寸发生变化或者该应用从一个屏幕移到另一屏幕时，它可能处于兼容模式下。

应用可以使用新的 [android:minAspectRatio](https://developer.android.com/reference/android/R.attr.html#minAspectRatio) 属性来指示应用是否支持新的[屏幕宽高比](https://developer.android.com/preview/features/foldables#new_screen_ratios)。

从版本 3.5 开始，Android Studio 的模拟器工具将包含 7.3" 和 8" 的虚拟设备，以便您使用 Android Q 系统映像测试代码。

如需了解详情，请参阅[打造适用于可折叠设备的应用](https://developer.android.com/preview/features/foldables)。
</details>

##### java.io.FileChannel.map() 更改
非标准文件将会受到影响
<details>
<summary>详细内容</summary>
非标准文件（例如 /dev/zero）已不再支持 FileChannel.map()，其大小无法使用 [truncate()](http://man7.org/linux/man-pages/man2/truncate.2.html) 进行更改。之前的 Android 版本会生吞掉 truncate() 返回的错误，但 Android Q 会抛出 IOException。如果您需要旧行为，则必须使用原生代码。
</details>

##### 定位服务权限变更
后台定位权限需要单独申请
<details>
<summary>详细内容</summary>

[文档原址](https://developer.android.com/preview/privacy/device-location)
对于后台定位的需求，需要单独申请后台定位权限。但是在前台转到后台时，如果授予了前台位置服务，仍然可以开启一个位置信息获取的前台服务，但是服务需要做特殊标注。
```xml
    <service
        android:name="MyNavigationService"
        android:foregroundServiceType="location" ... >
        ...
    </service>    
```
 权限的申请
```xml
    <manifest>
      <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
      <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    </manifest>
    
```
> 注意：如果应用的目标版本如果低于 `Android Q` ，那么会自动添加后台定位服务的权限申请，并且位置权限请求将会同时请求后台定位权限，但是用户仍然可能会单独关闭后台定位权限。
> 如果目标版本基于 `Android Q`，那么在不申请后台服务权限的前提下，是无法使用后台服务的。

</details>

##### Activity启动受限
后台启动Activity受到约束，需要是可见应用，或者受到可见应用的拉起。
<details>
<summary>详细内容</summary>

在 Android Q 上运行的应用只有在满足以下一个或多个条件时才能启动 Activity：

* 该应用具有可见窗口，例如在前台运行的 Activity。

> 注意：为了启动 Activity，前台服务**不会**将应用限定为在前台运行。
该应用在前台任务的返回栈中具有一项 Activity。

* 该应用具有最近启动的 Activity。

* 该应用对最近的一项 Activity 调用了 finish()。这仅适用于在调用 finish() 时，应用在前台中具有一项 Activity，或在前台任务的返回栈中具有一项 Activity 的情况。

* 该应用的一项服务被系统绑定。该条件仅适用于以下服务（可能需要启动界面）：AccessibilityService、AutofillService、CallRedirectionService、HostApduService、InCallService、TileService、VoiceInteractionService 以及 VrListenerService。

* 该应用的某一项服务被其他可见应用绑定。请注意，绑定到该服务的应用必须在后台对该应用保持可见，才能成功启动 Activity。

* 该应用会从系统收到通知 PendingIntent。如果存在针对服务和广播接收器的待定 intent，则该应用可以在待定 intent 发送后启动 Activity 几秒钟时间。

* 该应用会收到从其他可见应用发送的 PendingIntent。

* 该应用会收到系统广播，其中要求应用启动界面。示例包括 ACTION_NEW_OUTGOING_CALL 和 SECRET_CODE_ACTION。该应用可以在广播发送后启动 Activity 并持续几秒钟时间。

* 该应用已通过 CompanionDeviceManager API 与配套硬件设备相关联。借助此 API，该应用可以启动 Activity 以响应用户在配对设备上执行的操作。

* 该应用是在设备所有者模式下运行的设备政策控制器。示例用例包括完全托管的企业设备，以及数字标识牌和自助服务终端等专属设备。

* 该应用已获得用户授予的 SYSTEM_ALERT_WINDOW 权限。

> 注意：在 Android Q（Go 版本）设备上运行的应用无法获得 SYSTEM_ALERT_WINDOW 权限。
如果应用不满足以上任何条件，但最近屏幕上有一个现有任务，则系统仍会为启动 Activity 提供支持。当此类应用尝试启动新的 Activity 时，系统会将该 Activity 置于应用的现有任务上方，但不会离开目前可见的任务。当用户之后返回该应用的任务时，系统会启动新的 Activity，而不是之前在应用任务上方的 Activity。
</details>