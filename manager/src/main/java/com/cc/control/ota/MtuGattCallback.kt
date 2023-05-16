package com.cc.control.ota

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.os.Build
import com.cc.control.BluetoothManager
import com.cc.control.logD
import com.cc.control.protocol.DeviceConstants.D_MTU_LENGTH
import com.inuker.bluetooth.library.utils.BluetoothUtils

/**
 * @Author      : cc
 * @Date        : on 2022-04-08 15:43
 * @Description : mtu 设置回调
 */
@SuppressLint("MissingPermission")
/**
 * J003每次连接都需要重新配置，避免重启重连重置
 */
class MtuGattCallback(private val address: String) : BluetoothGattCallback() {
    init {
        val bluetoothDevice = BluetoothUtils.getRemoteDevice(address)
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                bluetoothDevice.connectGatt(BluetoothManager.mApplication,
                    false,
                    this,
                    BluetoothDevice.TRANSPORT_LE,
                    BluetoothDevice.PHY_LE_1M_MASK)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                bluetoothDevice.connectGatt(BluetoothManager.mApplication,
                    false,
                    this,
                    BluetoothDevice.TRANSPORT_LE)
            }
            else -> {
                bluetoothDevice.connectGatt(BluetoothManager.mApplication,
                    false,
                    this
                )
            }
        }

    }

    @SuppressLint("MissingPermission")
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
     * status BluetoothDevice.PHY_LE_1M、BluetoothDevice.PHY_LE_CODED
     */
    override fun onMtuChanged(gatt: BluetoothGatt, mtuSize: Int, status: Int) {
        super.onMtuChanged(gatt, mtuSize, status)
        if (BluetoothGatt.GATT_SUCCESS == status) {
            BluetoothManager.getConnectBean(address, false).run {
                mtu = mtuSize
            }
        }
        gatt.disconnect()
        logD("MtuGattCallback", "$status==$mtuSize")
    }
}