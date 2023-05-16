package com.cc.control.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author      : cc
 * @Date        : on 2022-02-15 14:05.
 * @Description :设备状态监听
 */
@Parcelize
data class DeviceConnectBean(
    var address: String = "",//连接地址
    var type: String = "",//心率带用到了
    var name: String = "",//设备名称如果接口获取mac为空需要用蓝牙名称判断是否同一个设备
    var isConnect: Boolean = false,//连接状态 true  requestDevice 成功 才为true
    var startAuto: Boolean = true,//是否需要自动重连
    var deviceRelId: String = "",//后台的Id标识用于设备绑定解绑
    var modelId: String = "",//设备二级Id
    var connecting: Boolean = false,//标记设备是否连接中
    var isAuto: Boolean = false,//false 手动连接
    var requestDevice: Boolean = false,//是否请求后台设备数据
    var characteristic: List<CharacteristicBean> = listOf(),//请求连接接口需要 所有特征值数据
    var uniqueModelIdentify: List<CharacteristicBean>? = listOf(),//请求连接接口需要 唯一确定的特征值数据
) : Parcelable
