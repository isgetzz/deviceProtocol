package com.cc.control


import com.cc.control.protocol.*
import com.cc.control.protocol.DeviceConstants.D_SERVICE1826
import com.cc.control.protocol.DeviceConstants.D_SERVICE_BQ
import com.cc.control.protocol.DeviceConstants.D_SERVICE_FFFO
import com.cc.control.protocol.DeviceConstants.D_SERVICE_MRK
import com.inuker.bluetooth.library.beacon.BeaconParser
import com.inuker.bluetooth.library.utils.ByteUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 *  : cc
 *  : on 2022-02-20 12:54.
 *  :单车协议
 */
class DeviceBicycleFunction(device: String) : BaseDeviceFunction(device) {
    override fun startWrite(isCreate: Boolean) {
        /**
         *2022-11立聪确认，兼容Merach-MR636D运行状态再发开始会倒着走
         */
        if (propertyBean.serviceUUID == null) return
        if (propertyBean.name.contains("Merach-MR636D")) {
            mLifecycleScope?.launch {
                //解决设备清零之后开启运动获取状态是运行中，导致无法开始
                delay(1000)
                read(propertyBean.serviceUUID!!, string2UUID(DeviceConstants.D_SERVICE1826_2AD3)) {
                    if (it.size >= 2 && it[1].toInt() != 0x0D) {
                        start()
                    }
                }
            }
        } else {
            start()
        }
    }

    private fun start() {
        notifyBean.status = DEVICE_TREADMILL_RUNNING
        when (propertyBean.protocol) {
            DeviceConstants.D_SERVICE_TYPE_ZJ -> {
                if (dateArray.isEmpty()) {
                    dateArray.add(writeZJBicycleData())
                    dateArray.add(writeZJBicycleStatus())
                    write(writeZJInfo())
                }
                write(readZJModelId(), ::writeData)
            }
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                write(writeBQBicycleConnect())
            }
            DeviceConstants.D_SERVICE_TYPE_MRK -> {

            }
            else -> {  //开始指令华为部分设备用于结束训练之后恢复连接
                write(writeFTMSControl()) {
                    write(ByteUtils.stringToBytes("07"))
                }
            }
        }
    }

    override fun onControl(speed: Int, resistance: Int, slope: Int, isDelayed: Boolean) {
        deviceControl(when (propertyBean.protocol) {
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                if (propertyBean.type == DeviceConstants.D_ROW) {
                    writeBQBicycle5Resistance(resistance)
                } else {
                    writeBQBicycle6Resistance(resistance)
                }
            }
            DeviceConstants.D_SERVICE_TYPE_FTMS -> {
                write(writeFTMSControl())
                writeResistanceControl(resistance)
            }
            DeviceConstants.D_SERVICE_TYPE_MRK -> {
                writeMrkControl(resistance)
            }
            else -> {
                writeZJBicycleControl(resistance, slope)
            }
        }, isDelayed)
        writeToFile(TAG,
            "onControl ${propertyBean.name} $speed $resistance $readyConnect $isDelayed")
    }

    //目前用于游戏单车
    override fun setDeviceModel(model: Int, targetNum: Int, onSuccess: (() -> Unit)) {
        write(writeMrkModel(model))
    }

    override fun onBluetoothNotify(service: UUID, character: UUID, parser: BeaconParser) {
        when (service.toString()) {
            D_SERVICE_MRK -> {
                mrkProtocol(parser)
                mDataListener?.invoke(notifyBean)
            }
            D_SERVICE1826 -> {
                onFTMSProtocol(notifyBean, propertyBean.name, propertyBean.type, parser)
                mDataListener?.invoke(notifyBean)
            }
            D_SERVICE_FFFO, D_SERVICE_BQ -> {
                onFFF0Protocol(notifyBean, propertyBean.type, parser, dataLength) { start, array ->
                    if (propertyBean.protocol == DeviceConstants.D_SERVICE_TYPE_ZJ) {
                        mDataListener?.invoke(notifyBean)
                    } else if (start) {
                        if (readyConnect) {
                            mDataListener?.invoke(notifyBean)
                        } else if (dateArray.isEmpty()) {
                            array?.run { dateArray.add(this) }
                            writeData()
                            readyConnect = true
                        } else {
                            //柏群重连需要重新启动数据交互
                            writeData()
                            readyConnect = true
                        }
                    } else {
                        write(array)
                    }
                }
            }
        }
    }

    /**
     * mrk 协议
     */
    private fun mrkProtocol(beaconParser: BeaconParser) {
        //AA (01/02) 1数据 2 控制
        if (beaconParser.readByte() == 0xAA) {
            val length = beaconParser.readByte()
            val status = beaconParser.readByte()
            if (status == 2 && (beaconParser.readByte() == 1) && (beaconParser.readByte() and 0x7F == 6)) {
                return
            }
            if (status != 1) return
            if (dataLength - 2 == length) {
                onMrkProtocol(notifyBean, beaconParser, dataLength - 5)
            }
        }
    }

    override fun onDestroyWrite(): ByteArray {
        return when (propertyBean.protocol) {
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                writeBQBicycleClear()
            }
            DeviceConstants.D_SERVICE_TYPE_MRK -> {
                writeMrkStop()
            }
            else -> {
                writeZJBicycleClear()
            }
        }
    }
}