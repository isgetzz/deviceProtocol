package com.cc.control.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author : cc
 * @Date : on 2022-05-18 10:08.
 * @Description :设备列表
 */
data class DeviceListBean(val records: List<Records> = listOf()) {
    @Parcelize
    data class Records(
        var deviceUserRelId: String = "0",
        var productId: String = "",//设备类型
        var modelId: String = "",//型号Id
        var deviceAlias: String = "",//别名
        var communicationProtocol: Int = 0, // 1:麦瑞克,2:FTMS,3:智健,4:柏群,5:FTMS+智健
        var deviceId: String = "",
        var productName: String = "",//划船机
        var mac: String = "",
        var isOta: Int = 0,//是否支持ota 1是
        var cover: String = "",//图片
        var helpCenter: String = "",
        var productManual: String = "",
        var modelName: String = "",//型号名称902
        var bluetoothName: String = "",//蓝牙名称
        var firmwareVersion: String = "",//蓝牙名称
        var productType: Int = 0,//产品分类 1.运动设备，3健康设备
        var versionEigenValue: Int = 0,// 1：2a26 ,2:2a28
        var isMerit: Int = 0, // 1是0否
        var otaProtocol: Int = 0,//ota协议
        var showMedal: Boolean = false,//显示勋章
        var featureDescription: List<FeatureDescription>? = listOf(),
    ) : Parcelable {
        @Parcelize
        data class FeatureDescription(
            val code: Int = 0,
            val backgroundColor: String = "#ffffff",
            val fontColor: String = "#ffffff",
            val desc: String = "",
        ) : Parcelable
    }
}