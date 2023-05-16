package com.cc.control

import com.cc.control.BluetoothManager.TAG
import com.cc.control.LiveDataBus.CONNECT_BEAN_KEY
import com.cc.control.bean.DeviceConnectBean
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener

/**
 * author      : cc
 * on 2022-07-22 14:51.
 * 断开回调，如果连接的话需要先获取完后台配置才成功
 */
class BleStatusListener : BleConnectStatusListener() {
    override fun onConnectStatusChanged(mac: String, status: Int) {
        //断开的设备解绑订阅，防止连接设备失败还会走回调
        if (status != Constants.STATUS_CONNECTED) {
            BluetoothManager.client.unregisterConnectStatusListener(mac, this)
            val it = BluetoothManager.getConnectBean(mac, false)
            it.isConnect = false
            val bean = DeviceConnectBean(mac, it.type, it.name, startAuto = it.autoConnect)
            LiveDataBus.postValue(CONNECT_BEAN_KEY, bean)
            writeToFile(TAG, "BleStatusListener:$bean")
        }
    }
}