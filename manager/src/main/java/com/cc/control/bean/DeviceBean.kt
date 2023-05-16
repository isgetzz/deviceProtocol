package com.cc.control.bean

/**
 * @Description 设备连接bean
 * @Author lk
 * @Date 2023/3/29 15:40
 */
data class DeviceBean(
    var productId: String = "",//产品Id
    var modelId: String = "",//型号id
    var modelName: String = "",//型号名称
    var cover: String = "",//型号封面
    var communicationProtocol: Int = 0,//通信协议
    var isOta: Int = 0,//是否支持ota
    var otaProtocol: Int = 0,//ota协议
    var versionEigenValue: Int = 0,//版本特征值
    var deviceUserRelId: String = "",//用户设备关联id
    var deviceAlias: String = "",//设备别名
    var bluetoothName: String = "",//蓝牙广播名
    var mac: String = "",//mac地址
)