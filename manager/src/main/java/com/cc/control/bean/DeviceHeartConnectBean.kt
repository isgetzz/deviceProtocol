package com.cc.control.bean

/**
 * cc
 * 2022-09-26 11:08.
 * 心率设备状态
 */
data class DeviceHeartConnectBean(
    val isNotify: Boolean = false,
    val deviceType: String = "",
    val deviceAddress: String = "",
)