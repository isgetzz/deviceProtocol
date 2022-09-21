package com.cc.control.ota

import com.cc.control.BluetoothClientManager
import com.cc.control.protocol.CRC16
import com.cc.control.protocol.dvSplitByteArrEndSeamProtection
import com.cc.control.protocol.isFileExist
import com.cc.control.protocol.readFileToByteArray
import com.inuker.bluetooth.library.Code
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-02-23 10:48.
 * @Description :描述 博通
 */
class BTOta : BaseDeviceOta() {
    private var writeLength = 16 //每包长度
    private var dataWriteBuffer: ByteBuffer = ByteBuffer.allocate(18)
    private var writeByteArrayList = ArrayList<ByteArray>()
    private var writeTotalSize = 0//总包长
    private var writePosition = 0 //当前包index

    override fun onFile(filePath: String) {
        if (!filePath.isFileExist()) {
            return
        }
        filePath.readFileToByteArray().dvSplitByteArrEndSeamProtection(writeLength).run {
            writeByteArrayList = this
            writeTotalSize = size
            deviceConnectBean.run {
                BluetoothClientManager.client.write(address,
                    otaService,
                    otaControlCharacter,
                    writeByteArrayList[writePosition]) { code ->
                    if (code == Code.REQUEST_SUCCESS) {
                        otaFormat()
                    }
                }
            }
        }
    }

    private fun otaFormat() {
        job?.cancel()
        job = null
        if (!isFinish) {
            job = GlobalScope.launch(context = Dispatchers.IO) {
                writeByteArrayList.let {
                    if (writePosition < writeTotalSize) {
                        dataWriteBuffer.clear()
                        dataWriteBuffer.putShort(CRC16.shortTransposition(writePosition))
                        dataWriteBuffer.put(writeByteArrayList[writePosition])
                        writeNoRsp(
                            dataWriteBuffer.array(),
                            writeTotalSize,
                            writePosition,
                            onSuccess = {
                                writePosition++
                                otaFormat()
                            })
                    } else {
                        deviceOtaListener?.invoke(D_OTA_SUCCESS, 100)
                        job?.cancel()
                        job = null
                    }
                }
            }
        }
    }

    override fun onBluetoothNotify(service: UUID?, character: UUID?, value: ByteArray) {

    }
}