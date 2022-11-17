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
 *  : cc
 *  : on 2022-02-20 12:54.
 *  :单车协议
 */
class DeviceBicycleFunction : BaseDeviceFunction() {
    override fun onDeviceWrite(isCreate: Boolean) {
        when (deviceConnectInfoBean.deviceProtocol) {
            DeviceConstants.D_SERVICE_TYPE_ZJ -> {
                if (dateArray.isEmpty()) {
                    dateArray.add(writeZJBicycleData())
                    dateArray.add(writeZJBicycleStatus())
                }
                write(readZJModelId(), ::writeDeviceCmd)
            }
            DeviceConstants.D_SERVICE_TYPE_OTHER -> {
                //   serviceUUId.toString().equals(D_SERVICE_FFFO, ignoreCase = true)
            }
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                write(writeBQBicycleConnect())
            }
            else -> {
                //开始指令华为部分设备用于结束训练之后恢复连接
                //单车636D create 发送完恢复然后短时间又发一条会导致设备时间倒计时并暂停
                //02 - 0x01 停止 0x02暂停
                //04  Started or Resumed
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
        writeToFile("onDeviceControl 单车",
            "${deviceConnectInfoBean.deviceType} speed: $speed resistance: $resistance slope $slope ${deviceConnectInfoBean.deviceProtocol}")
        GlobalScope.launch {
            logI(TAG, "write:单车 控制延时")
            writeData = false
            delay(300)
            write(when (deviceConnectInfoBean.deviceProtocol) {
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
            delay(300)
            writeData = true
        }
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
                    deviceConnectInfoBean.deviceName,
                    deviceConnectInfoBean.deviceType,
                    beaconParser)
                deviceDataListener?.invoke(deviceNotifyBean)
            }
            D_SERVICE_FFFO, D_SERVICE_BQ -> {
                onFFF0Protocol(deviceNotifyBean,
                    deviceConnectInfoBean.deviceType,
                    beaconParser,
                    value.size) { startNotify, byteArray: ByteArray? ->
                    if (deviceConnectInfoBean.deviceProtocol == DeviceConstants.D_SERVICE_TYPE_ZJ) {
                        deviceDataListener?.invoke(deviceNotifyBean)
                    } else if (startNotify) {
                        if (readyConnect) {
                            deviceDataListener?.let { it(deviceNotifyBean) }
                        } else if (dateArray.isEmpty()) {
                            byteArray?.run {
                                dateArray.add(this)
                            }
                            writeDeviceCmd()
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
        return when (deviceConnectInfoBean.deviceProtocol) {
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                writeBQBicycleClear()
            }
            else -> {
                //华为base处理了,只需要处理ZJ
                writeZJBicycleClear()
            }
        }
    }
}