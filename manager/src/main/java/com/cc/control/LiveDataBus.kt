package com.cc.control

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/**
 *@author cc
 *@date 2023/2/8
 *@explain 解决在使用这个LiveDataBus的过程中，订阅者会收到订阅之前发布的消息 B站有相关LiveData详解
 *fragment订阅会有问题,进入之后会收到上一次的数据
 */
object LiveDataBus {
    const val TAG = "LiveDataBus"
    const val CONNECT_BEAN_KEY = "CONNECT_BEAN_KEY"//连接类状态
    const val NOTIFY_DATA_KEY = "NOTIFY_DATA_KEY"//数据通知
    const val BLUETOOTH_STATUS_KEY = "BLUETOOTH_STATUS_KEY"//蓝牙状态
    const val STOP_AUTO_KEY = "STOP_AUTO_KEY"//用户手动断开设备
    const val HEART_DISCONNECT_KEY = "HEART_DISCONNECT_KEY"//判断心率带主动被断开还是
    const val CONNECT_LISTENER_KEY = "CONNECT_LISTENER_KEY"//自动、手动连接状态回调，连接中、连接失败、连接成功
    const val DEVICE_NEED_OTA = "DEVICE_START_OTA"//需要ota

    /**
     * 因为只有当前页面homeFragment跟设备详情的时候才需要
     */
    var showAutoOta: Boolean = false

    /**
     *LiveData 集合
     */
    private val liveDataMap: MutableMap<String, BusMutableLiveData<Any>> = mutableMapOf()

    private val observerMap: MutableMap<String, Observer<*>> = mutableMapOf()

    /**
     * 不绑定生命周期订阅
     */
    fun <T> with(key: String): BusMutableLiveData<T> {
        if (!liveDataMap.containsKey(key)) {
            liveDataMap[key] = BusMutableLiveData()
        }
        return liveDataMap[key] as BusMutableLiveData<T>
    }

    /**
     * 发送
     */
    fun <T> postValue(key: String, value: T) {
        if (liveDataMap.containsKey(key)) {
            (liveDataMap[key] as BusMutableLiveData<T>).postValue(value)
        }
    }

    /**
     *清除liveData observer 用于设备连接状态跟数据监听
     *
     */
    fun removeObserver(liveDataKey: String, observerKey: String) {
        if (liveDataMap.containsKey(liveDataKey) && observerMap.containsKey(observerKey)) {
            liveDataMap[liveDataKey]?.removeObserver(observerMap[observerKey] as Observer<in Any>)
            //因为全局liveData对象绑定多个observer单个只清除observer避免其他key 无法接收
            // liveDataMap.remove(liveDataKey)
            observerMap.remove(observerKey)
        }
        Log.d(TAG,
            "removeObserver: ${liveDataMap.size} ${observerMap.size} $liveDataKey $observerKey")
    }

    fun removeObserver() {
        liveDataMap.forEach { entry ->
            observerMap.forEach {
                entry.value.removeObserver(it.value as Observer<in Any>)
            }
        }
        liveDataMap.clear()
        observerMap.clear()
    }

    /**
     * 数据订阅管理
     */
    class BusMutableLiveData<T> : MutableLiveData<T>() {
        fun observeForeverSticky(observer: Observer<in T>) {
            super.observeForever(observer)
        }

        fun observeForever(name: String, observer: Observer<in T>) {
            if (!observerMap.containsKey(name)) {
                observerMap[name] = ObserverWrapper(observer)
            }
            super.observeForever(observerMap[name] as Observer<in T>)
        }

        /**
         * 绑定生命周期，后台收不到数据
         */
        override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
            super.observe(owner, observer)
            try {
                hook(observer)
            } catch (e: Exception) {
                Log.d(TAG, "observe ${e.message}")
            }
        }

        /**
         * ObserverWrapper，把真正的回调给包装起来,把ObserverWrapper传给observeForever，
         * 那么在回调的时候我们去检查调用栈，如果回调是observeForever方法引起的，那么就不回调真正的订阅者。
         * 从而避免之前的回调页面被接收
         */
        private class ObserverWrapper<T>(val observer: Observer<T>) : Observer<T> {
            override fun onChanged(t: T) {
                if (isCallOnObserverForever()) {
                    return
                }
                observer.onChanged(t)
            }

            private fun isCallOnObserverForever(): Boolean {
                val stackTrace = Thread.currentThread().stackTrace
                for (element in stackTrace) {
                    if ("androidx.lifecycle.LiveData" == element.className && "observeForever" == element.methodName) {
                        return true
                    }
                }
                return false
            }
        }

        /**
         * 利用反射将 LiveData 的 mVersion 赋值给 ObserverWrapper 的 mLastVersion
         * 避免刚订阅就收到上一次数据状态
         */
        @Throws(Exception::class)
        private fun hook(observer: Observer<*>) {
            // Get wrapper's version.
            val liveDataClass = LiveData::class.java
            // SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers
            val observersField = liveDataClass.getDeclaredField("mObservers")
            observersField.isAccessible = true
            val observers = observersField[this]
            val observersClass: Class<*> = observers.javaClass
            // It's mObservers's get method.
            val methodGet = observersClass.getDeclaredMethod("get", Any::class.java)
            methodGet.isAccessible = true
            val observerWrapperEntry = methodGet.invoke(observers, observer)
            var observerWrapper: Any? = null
            if (observerWrapperEntry is Map.Entry<*, *>) {
                // Now we got observerWrapper.
                observerWrapper = observerWrapperEntry.value
            }
            if (observerWrapper == null) {
                throw NullPointerException("Wrapper can not be null!")
            }
            //1.getField 只能获取public的，包括从父类继承来的字段。
            //2.getDeclaredField 可以获取本类所有的字段，包括private的，但是不能获取继承来的字段。
            //(注： 这里只能获取到private的字段，但并不能访问该private字段的值,除非加上setAccessible(true))
            val observerWrapperParentClass: Class<*>? = observerWrapper.javaClass.superclass
            val lastVersionField = observerWrapperParentClass!!.getDeclaredField("mLastVersion")
            lastVersionField.isAccessible = true
            // Get livedata's version.
            val versionField = liveDataClass.getDeclaredField("mVersion")
            versionField.isAccessible = true
            val version = versionField[this]
            // Set wrapper's version.
            lastVersionField[observerWrapper] = version
        }
    }

}
