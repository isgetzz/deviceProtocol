package com.cc.control

import com.cc.control.bean.DeviceConnectObserverBean
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener

/**
 * @author      : cc
 * on 2022-07-22 14:51.
 * 断开回调，如果连接的话需要先获取完后台配置才成功
 */
class DeviceConnectStatusListener : BleConnectStatusListener() {
    private val deviceConnectObserverBean = DeviceConnectObserverBean()
    override fun onConnectStatusChanged(mac: String, status: Int) {
        if (status != Constants.STATUS_CONNECTED) {
            //断开的设备解绑订阅，防止连接设备失败还会走回调
            BluetoothClientManager.client.unregisterConnectStatusListener(mac, this)
            BluetoothClientManager.deviceConnectBean(mac).let {
                it.isDeviceConnect = false
                BluetoothClientManager.deviceConnectObserverBean.postValue(deviceConnectObserverBean.apply {
                    deviceAddress = mac
                    deviceConnectStatus = false
                    deviceType = it.deviceType
                    deviceName = it.deviceName
                })
                writeToFile("BaseDeviceFunction",
                    " DeviceConnectStatusListener:${it.deviceType} ${it.deviceName} $mac $status")
            }
        }

    }
}