package com.cc.control.ota

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.os.Build
import com.cc.control.BluetoothClientManager
import com.cc.control.logD
import com.cc.control.protocol.DeviceConstants.D_MTU_LENGTH
import com.inuker.bluetooth.library.utils.BluetoothUtils

/**
 * @Author      : cc
 * @Date        : on 2022-04-08 15:43
 * @Description : mtu 设置回调
 */
class MtuGattCallback(
    address: String,
    private val mtuBackListener: (Int) -> Unit,
) : BluetoothGattCallback() {
    init {
        val bluetoothDevice = BluetoothUtils.getRemoteDevice(address)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                bluetoothDevice.connectGatt(BluetoothClientManager.app,
                    false,
                    this,
                    BluetoothDevice.TRANSPORT_LE,
                    BluetoothDevice.PHY_LE_1M_MASK)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                bluetoothDevice.connectGatt(BluetoothClientManager.app,
                    false,
                    this,
                    BluetoothDevice.TRANSPORT_LE)
            }
            else -> {
                bluetoothDevice.connectGatt(BluetoothClientManager.app,
                    false,
                    this
                )
            }
        }

    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.requestMtu(D_MTU_LENGTH)
            logD("MtuGattCallback", "$status==$newState")
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
    }

    /**
     * 设置完成之后需要关闭通道,不然无法断开连接
     */
    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
        mtuBackListener.invoke(mtu)
        gatt.disconnect()
        logD("MtuGattCallback", "$status==$mtu")
    }
}