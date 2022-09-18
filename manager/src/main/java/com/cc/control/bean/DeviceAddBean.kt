package com.cc.control.bean

/**
 * @Author : cc
 * @Date : on 2022-06-28 18:11.
 * @Description :绑定设备接口反馈
 */
data class DeviceAddBean(
    val oneLevelTypeId: String = "",//一级设备类型id
    val id: String = "",
    val twoLevelTypeId: String = "",//二级设备类型id
    val communicationProtocol: Int = 0, // 通信类型
    val otaType: Int = 0,//ota 类型
    val eigenValue: Int = 0, //1：2a26 ,2:2a28
)