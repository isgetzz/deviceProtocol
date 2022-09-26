package com.cc.control.bean

/**
 * cc
 * 2022-09-26 11:08.
 * 设备数据订阅bean
 */
data class DeviceNotifyBean(
    val isNotify: Boolean = false,
    val deviceType: String = "",
    val deviceAddress: String = "",
)