package com.cc.control


import com.cc.control.protocol.*
import com.inuker.bluetooth.library.beacon.BeaconParser
import java.util.*

/**
 *  : cc
 *  : on 2022-02-20 12:54.
 *  :力量站
 */
class DevicePowerFunction(device: String) : BaseDeviceFunction(device) {
    /**
     * 开始下发指令 isCreate 目前用于跑步机
     */
    override fun startWrite(isCreate: Boolean) {
        notifyBean.status = DEVICE_TREADMILL_RUNNING
        if (dateArray.isEmpty()) {
            dateArray.add(writeZJBicycleData())
            dateArray.add(writeZJBicycleStatus())
            write(writeZJInfo())
        }
        write(readZJModelId(), ::writeData)
    }

    /**
     * 目前智健跑步机速度跟坡度一条指令
     * speed 速度 resistance 阻力 slope 坡度
     */
    override fun onControl(
        speed: Int,
        resistance: Int,
        slope: Int,
        isDelayed: Boolean,
        isSlope: Boolean,
    ) {
        deviceControl(writeZJBicycleControl(resistance, slope), false)
        writeToFile(TAG, "onDeviceControl ${propertyBean.name} $speed $resistance $readyConnect")
    }

    override fun setDeviceModel(model: Int, targetNum: Int, onSuccess: () -> Unit) {
    }

    override fun onBluetoothNotify(service: UUID, character: UUID, parser: BeaconParser) {
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


    override fun onDestroyWrite(): ByteArray {
        return writeZJBicycleClear()
    }
}