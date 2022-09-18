package com.cc.control

import com.inuker.bluetooth.library.beacon.BeaconParser
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-02-18 16:53.
 * @Description :小件不需要控制
 */
class DeviceOtherFunction: BaseDeviceFunction() {
    override fun onDeviceWrite(isCreate: Boolean) {


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

    }

    override fun onDestroyWrite(): ByteArray {
        return byteArrayOf()
    }
}