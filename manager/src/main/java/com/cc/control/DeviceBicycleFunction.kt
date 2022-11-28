package com.cc.control

import com.cc.control.protocol.*
import com.cc.control.protocol.DeviceConstants.D_SERVICE1826
import com.cc.control.protocol.DeviceConstants.D_SERVICE_BQ
import com.cc.control.protocol.DeviceConstants.D_SERVICE_FFFO
import com.cc.control.protocol.DeviceConstants.D_SERVICE_MRK
import com.inuker.bluetooth.library.beacon.BeaconParser
import com.inuker.bluetooth.library.utils.ByteUtils
import java.util.*
import kotlin.experimental.and

/**
 *  : cc
 *  : on 2022-02-20 12:54.
 *  :单车协议
 */
class DeviceBicycleFunction : BaseDeviceFunction() {
    override fun onDeviceWrite(isCreate: Boolean) {
        //2022-11 立聪确认 初始化Merach-MR636D不做操作,根据设备当前状态来发送指令
        if (deviceConnectInfoBean.deviceName.contains("Merach-MR636D") && isCreate) {
            return
        }
        when (deviceConnectInfoBean.deviceProtocol) {
            DeviceConstants.D_SERVICE_TYPE_ZJ -> {
                if (dateArray.isEmpty()) {
                    dateArray.add(writeZJBicycleData())
                    dateArray.add(writeZJBicycleStatus())
                }
                write(readZJModelId(), ::writeData)
            }
            DeviceConstants.D_SERVICE_TYPE_OTHER -> {
                //   serviceUUId.toString().equals(D_SERVICE_FFFO, ignoreCase = true)
            }
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                write(writeBQBicycleConnect())
            }
            else -> {
                //开始指令华为部分设备用于结束训练之后恢复连接
                write(writeFTMSControl()) {
                    write(ByteUtils.stringToBytes("07"))
                }
            }
        }
    }

    override fun onDeviceControl(
        speed: Int,
        resistance: Int,
        slope: Int,
    ) {
        writeToFile(TAG,
            "onDeviceControl ${deviceConnectInfoBean.deviceName} $speed $resistance $slope $readyConnect")
        deviceControlDelayed(when (deviceConnectInfoBean.deviceProtocol) {
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                if (deviceConnectInfoBean.deviceType == DeviceConstants.D_ROW) {
                    writeBQBicycle5Resistance(resistance)
                } else {
                    writeBQBicycle6Resistance(resistance)
                }
            }
            DeviceConstants.D_SERVICE_TYPE_FTMS -> {
                write(writeFTMSControl())
                writeBicycleControl(resistance)
            }
            else -> {
                writeZJBicycleControl(resistance, slope)
            }
        })
    }

    override fun onBluetoothNotify(
        service: UUID,
        character: UUID,
        value: ByteArray,
        beaconParser: BeaconParser,
    ) {
        when (service.toString()) {
            D_SERVICE_MRK -> {
                mrkProtocol(value, beaconParser)
                deviceDataListener?.invoke(deviceNotifyBean)
            }
            D_SERVICE1826 -> {
                onFTMSProtocol(deviceNotifyBean, deviceConnectInfoBean.deviceName, deviceConnectInfoBean.deviceType, beaconParser)
                deviceDataListener?.invoke(deviceNotifyBean)
            }
            D_SERVICE_FFFO, D_SERVICE_BQ -> {
                onFFF0Protocol(deviceNotifyBean, deviceConnectInfoBean.deviceType, beaconParser, value.size) { startNotify, byteArray: ByteArray? ->
                    if (deviceConnectInfoBean.deviceProtocol == DeviceConstants.D_SERVICE_TYPE_ZJ) {
                        deviceDataListener?.invoke(deviceNotifyBean)
                    } else if (startNotify) {
                        if (readyConnect) {
                            deviceDataListener?.invoke(deviceNotifyBean)
                        } else if (dateArray.isEmpty()) {
                            byteArray?.run {
                                dateArray.add(this)
                            }
                            writeData()
                            readyConnect = true
                        }
                    } else {
                        write(byteArray)
                    }
                }
            }
        }
    }

    /**
     * mrk 协议
     */
    private fun mrkProtocol(value: ByteArray, beaconParser: BeaconParser) {
        val response = (value[3] and 0xff.toByte()).toInt()
        val isSuccess = (value[4] and 0xff.toByte()).toInt()
        if (adr == 0xAA) {
            //控制状态02 并且设置成功 结果是复位
            if (deviceStatus == 2 && (isSuccess == 1) && (response and 0x7F == 6)) {
                return
            }
            //数据位1
            if (deviceStatus != 1)
                return
        }
        if (value.size - 2 == len) {
            onMrkProtocol(deviceNotifyBean, beaconParser, value.size - 5)
        }
    }

    override fun onDestroyWrite(): ByteArray {
        return when (deviceConnectInfoBean.deviceProtocol) {
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                writeBQBicycleClear()
            }
            else -> {
                writeZJBicycleClear()
            }
        }
    }
}