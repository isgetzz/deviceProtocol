package com.cc.control.ota

import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-09-21 16:22.
 * @Description :其他
 */
class OtherOta :BaseDeviceOta() {
    override fun onFile(filePath: String) {
        TODO("Not yet implemented")
    }

    override fun onBluetoothNotify(service: UUID?, character: UUID?, value: ByteArray) {
        TODO("Not yet implemented")
    }
}