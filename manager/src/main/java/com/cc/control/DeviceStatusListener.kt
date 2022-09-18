package com.cc.control

/**
 * 跑步状态回调
 *@property onDeviceFinish 跑步机暂停
 *@property  onDeviceSpeedCut 减速
 *@property onDeviceCountTime 倒计时
 *@property onDevicePause 暂停
 */
interface DeviceStatusListener {
    fun onDeviceRunning()

    fun onDevicePause()

    fun onDeviceSpeedCut()

    fun onDeviceFinish()

    fun onDeviceCountTime(time: Int)
}
