package com.cc.control.bean

/**
 * @Author      : cc
 * @Date        : on 2022-02-15 14:05.
 * @Description :设备状态监听
 */
class DeviceConnectObserverBean(
    var deviceAddress: String = "",//连接地址
    var deviceConnectStatus: Boolean = false,//绑定状态 true 连接
    var deviceType: String = "",//心率带用到了
    var deviceName: String = "",//设备名称如果接口获取mac为空需要用蓝牙名称判断是否同一个设备
)
