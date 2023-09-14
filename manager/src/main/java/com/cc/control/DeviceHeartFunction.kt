package com.cc.control

import com.cc.control.protocol.DeviceConstants
import com.inuker.bluetooth.library.beacon.BeaconParser
import java.util.*

/**
 * Author      : cc
 * Date        : on 2022-02-18 16:53.
 * Description :心率带
 */
class DeviceHeartFunction(device: String) : BaseDeviceFunction(device) {
    override fun startWrite(isCreate: Boolean) {
        read(UUID.fromString(DeviceConstants.D_SERVICE_ELECTRIC_HEART),
            UUID.fromString(DeviceConstants.D_CHARACTER_ELECTRIC_HEART)) {
            notifyBean.electric = (it[0].toInt() and 0xff)
            mDataListener?.invoke(notifyBean)
        }
    }

    override fun onControl(speed: Int, resistance: Int, slope: Int, isDelayed: Boolean,isSlope: Boolean) {
    }

    override fun onBluetoothNotify(service: UUID, character: UUID, parser: BeaconParser) {
        val flag = parser.readByte()
        val rat = if (flag == 0) parser.readByte() else parser.readShort()
        notifyBean.rate = rat.coerceAtMost(200)
        mDataListener?.invoke(notifyBean)
    }


    override fun onDestroyWrite(): ByteArray? {
        return null
    }
}