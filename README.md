# deviceManager

### remark  android 12  You can use bluetooth only after dynamically applying for permission  android.permission.BLUETOOTH_CONNECT android.permission.BLUETOOTH_SCAN

###  

前台进程（Foreground process） 前台进程是用户当前做的事所必须的进程，如果满足下面各种情况中的一种，一个进程被认为是在前台：

进程持有一个正在与用户交互的Activity。

进程持有一个Service，这个Service处于这几种状态:①Service与用户正在交互的Activity绑定。②Service是在前台运行的，即它调用了 startForeground()
。③Service正在执行它的生命周期回调函数（onCreate(), onStart(), or onDestroy()）。

进程持有一个broadcastReceiver，这个broadcastReceiver正在执行它的 onReceive() 方法。
