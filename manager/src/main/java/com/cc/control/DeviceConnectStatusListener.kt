package com.cc.control

import com.cc.control.bean.DeviceConnectObserverBean
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener

/**
 * @Author      : cc
 * @Date        : on 2022-07-22 14:51.
 * @Description :断开回调，如果连接的话需要先获取完后台配置才成功
 */
class DeviceConnectStatusListener : BleConnectStatusListener() {
    private val deviceConnectObserverBean = DeviceConnectObserverBean()
    override fun onConnectStatusChanged(mac: String, status: Int) {
        if (status != Constants.STATUS_CONNECTED) {
            //断开的设备解绑订阅，防止连接设备失败还会走回调
            BluetoothClientManager.client.unregisterConnectStatusListener(mac, this)
            val connectBean = BluetoothClientManager.deviceConnectBean(mac)
            BluetoothClientManager.deviceConnectObserverBean.postValue(deviceConnectObserverBean.apply {
                deviceAddress = mac
                deviceConnectStatus = false
                deviceType = connectBean.deviceType
                deviceName = connectBean.deviceName
            })
            logD("BluetoothClientManager0",
                "${connectBean.deviceType} ${connectBean.deviceName} $mac $status")
            BluetoothClientManager.deviceManagerMap.value?.forEach { it ->
                if (it.value.address == mac) {
                    it.value.isDeviceConnect = false
                }
            }
        }
    }
}