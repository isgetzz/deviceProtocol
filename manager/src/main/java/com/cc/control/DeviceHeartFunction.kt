package com.cc.control

import com.cc.control.protocol.DeviceConstants
import com.inuker.bluetooth.library.beacon.BeaconParser
import java.util.*

/**
 * Author      : cc
 * Date        : on 2022-02-18 16:53.
 * Description :心率带
 */
class DeviceHeartFunction : BaseDeviceFunction() {
    override fun onDeviceWrite(isCreate: Boolean) {
        read(UUID.fromString(DeviceConstants.D_SERVICE_ELECTRIC_HEART),
            UUID.fromString(DeviceConstants.D_CHARACTER_ELECTRIC_HEART)) {
            deviceNotifyBean.electric = (it[0].toInt() and 0xff)
            deviceDataListener?.invoke(deviceNotifyBean)
        }
    }

    override fun onDeviceControl(
        speed: Int,
        resistance: Int,
        slope: Int,
    ) {

    }

    override fun onBluetoothNotify(
        service: UUID,
        character: UUID,
        value: ByteArray,
        beaconParser: BeaconParser,
    ) {
        val flag = beaconParser.readByte()
        val rat = if (flag == 0)
            beaconParser.readByte()
        else beaconParser.readShort()
        deviceNotifyBean.rate = rat.coerceAtMost(200)
        deviceDataListener?.invoke(deviceNotifyBean)
    }

    override fun onDestroyWrite(): ByteArray? {
        return null
    }
}