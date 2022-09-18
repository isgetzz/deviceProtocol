package com.cc.control

import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener
import com.cc.control.bean.DeviceConnectObserverBean

/**
 * @Author      : cc
 * @Date        : on 2022-07-22 14:51.
 * @Description :断开回调，如果连接的话需要先获取完后台配置才成功
 */
class DeviceConnectStatusListener : BleConnectStatusListener() {

    override fun onConnectStatusChanged(mac: String, status: Int) {
        if (status != Constants.STATUS_CONNECTED) {
            //断开的设备解绑订阅，防止连接设备失败还会走回调
            BluetoothClientManager.client.unregisterConnectStatusListener(mac, this)
            BluetoothClientManager.deviceConnectObserverBean.postValue(DeviceConnectObserverBean(mac))
            logD("BluetoothClientManager0", "$mac $status")
            BluetoothClientManager.deviceManagerMap.value?.forEach { it ->
                if (it.value.address == mac) {
                    it.value.isDeviceConnect = false
                }
            }
        }
    }
}