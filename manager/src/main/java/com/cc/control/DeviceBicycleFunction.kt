package com.cc.control

import com.cc.control.protocol.*
import com.cc.control.protocol.DeviceConstants.D_SERVICE1826
import com.cc.control.protocol.DeviceConstants.D_SERVICE_BQ
import com.cc.control.protocol.DeviceConstants.D_SERVICE_FFFO
import com.cc.control.protocol.DeviceConstants.D_SERVICE_MRK
import com.inuker.bluetooth.library.beacon.BeaconParser
import com.inuker.bluetooth.library.utils.ByteUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.experimental.and

/**
 * @Author      : cc
 * @Date        : on 2022-02-20 12:54.
 * @Description :单车协议
 */
class DeviceBicycleFunction : BaseDeviceFunction() {

    override fun onDeviceWrite(isCreate: Boolean) {
        when (deviceDateBean.deviceProtocol) {
            DeviceConstants.D_SERVICE_TYPE_ZJ -> {
                if (dateArray.isEmpty()) {
                    dateArray.add(onWriteZJBicycleData())
                    dateArray.add(onWriteZJBicycleStatus())
                }
                write(onWriteZJModelId(), ::onDeviceCmd)
            }
            DeviceConstants.D_SERVICE_TYPE_OTHER -> {
                //   serviceUUId.toString().equals(D_SERVICE_FFFO, ignoreCase = true)
            }
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                write(onWriteBQBicycleConnect())
            }
            else -> {
                //开始指令华为部分设备用于结束训练之后恢复连接
                write(onFTMSControl()) {
                    write(ByteUtils.stringToBytes("07")) { }
                }
            }
        }
    }

    override fun onDeviceControl(
        speed: Int,
        resistance: Int,
        slope: Int,
    ) {
        writeToFile("onDeviceControl 单车",
            "${deviceDateBean.deviceType} speed: $speed resistance: $resistance slope $slope ${deviceDateBean.deviceProtocol}")
        GlobalScope.launch {
            logI(TAG, "write:单车 控制延时")
            writeData = false
            clearAllRequest()
            delay(300)
            writeData = true
        }
        write(when (deviceDateBean.deviceProtocol) {
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                if (deviceDateBean.deviceType == DeviceConstants.D_ROW) {
                    onWriteBQBicycle5Resistance(resistance)
                } else {
                    onWriteBQBicycle6Resistance(resistance)
                }
            }
            DeviceConstants.D_SERVICE_TYPE_FTMS -> {
                write(onFTMSControl())
                onBicycleControl(resistance)
            }
            else -> {
                onWriteZJBicycleControl(resistance, slope)
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
                onFTMSProtocol(deviceNotifyBean,
                    deviceDateBean.deviceName,
                    deviceDateBean.deviceType,
                    beaconParser)
                deviceDataListener?.invoke(deviceNotifyBean)
            }
            D_SERVICE_FFFO, D_SERVICE_BQ -> {
                onFFF0Protocol(deviceNotifyBean,
                    deviceDateBean.deviceType,
                    beaconParser,
                    value.size) { startNotify, byteArray: ByteArray? ->
                    if (deviceDateBean.deviceProtocol == DeviceConstants.D_SERVICE_TYPE_ZJ) {
                        deviceDataListener?.invoke(deviceNotifyBean)
                    } else if (startNotify) {
                        if (readyConnect) {
                            deviceDataListener?.let { it(deviceNotifyBean) }
                        } else if (dateArray.isEmpty()) {
                            byteArray?.run {
                                dateArray.add(this)
                            }
                            onDeviceCmd()
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
            if (deviceStatus != 1) //数据位1
                return
        }
        if (value.size - 2 == len) {
            onMrkProtocol(deviceNotifyBean, beaconParser, value.size - 5)
        }
    }

    override fun onDestroyWrite(): ByteArray {
        return when (deviceDateBean.deviceProtocol) {
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                onWriteBQBicycleClear()
            }
            else -> {
                //华为base处理了,只需要处理ZJ
                onWriteZJBicycleClear()
            }
        }
    }
}