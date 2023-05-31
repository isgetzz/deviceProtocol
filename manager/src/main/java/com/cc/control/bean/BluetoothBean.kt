package com.cc.control.bean

import android.bluetooth.BluetoothDevice
import com.inuker.bluetooth.library.search.SearchResult
import java.io.Serializable

/**
 *@author cc
 * @date 2022-7-31
 * @explain 搜索设备bean
 */
 class BluetoothBean(device: BluetoothDevice) : SearchResult(device) {
    class DeviceDetails(
        var id: String = "",
        var twoTypeId: String = "",
        var isBinding: Int = 0,
        var image: String = "",
        var equipName: String = "",
        var isMerach: Int = 0,
        var twoTypeName: String = "",
        var oneLevelTypeId: String = "",//大类Id,
        var oneLevelTypeName: String = "", //大类名称
        var mac: String = "",
        var communicationProtocol: Int = 0,
        var otaType: Int = 0,
        var eigenValue: Int = 0, // 1：2a26 ,2:2a28
    ) : Serializable
}