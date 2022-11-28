package com.cc.control

/**
 * 跑步状态回调
 * onDeviceFinish 跑步机暂停
 * onDeviceSpeedCut 减速
 * onDeviceCountTime 倒计时
 * onDevicePause 暂停
 * onDeviceConnectStatus 设备状态
 */
interface DeviceStatusListener {
    fun onDeviceRunning()

    fun onDevicePause()

    fun onDeviceSpeedCut()

    fun onDeviceFinish()

    fun onDeviceCountTime(time: Int)

}
