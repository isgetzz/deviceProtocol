package com.cc.control.ota

import com.inuker.bluetooth.library.utils.ByteUtils
import com.cc.control.protocol.CRC16
import com.cc.control.protocol.CRC16.GeneralCRCFun
import com.cc.control.protocol.dvSplitByteArr
import com.cc.control.protocol.isFileExist
import com.cc.control.protocol.readFileToByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-02-22 16:40.
 * @Description :跑步机、swan单车、凯信达667
 */
class TLWOta : BaseDeviceOta() {
    //数据包
    private var writeByteArrayList = ArrayList<ByteArray>()
    private var writeTotalSize = 0//总包长
    private var writePosition = 0 //当前包index
    private var writeLength = 16 //每包长度
    private val writeBuffer = ByteBuffer.allocate(20)

    companion object {
        const val D_OTA_START_TLW = "01ff"
    }

    override fun initFilePath(filePath: String) {
        if (!filePath.isFileExist()) {
            return
        }
        filePath.readFileToByteArray().dvSplitByteArr(writeLength).run {
            writeByteArrayList = this
            writeTotalSize = this.size
            write(onStartTeLink(), onSuccess = {
                otaFormat()
            })
        }
    }

    private fun otaFormat() {
        if (isFinish) {
            job?.cancel()
            job = null
            return
        }
        job = GlobalScope.launch(context = Dispatchers.IO) {
            writeByteArrayList.let {
                //如果是最后一包则要把长度写进去 最后一把的数据长度写进去
                job?.cancel()
                job = null
                if (writePosition < writeTotalSize) {
                    writeBuffer.clear()
                    writeBuffer.putShort(CRC16.shortTransposition(writePosition))
                    writeBuffer.put(writeByteArrayList[writePosition])
                    //最后一包不足包长补齐
                    if (writePosition == writeTotalSize - 1) {
                        for (index in writeByteArrayList[writePosition].size until 16) {
                            writeBuffer.put(0xFF.toByte())
                        }
                    }
                    //校验码
                    writeBuffer.putShort(GeneralCRCFun(writeBuffer, 18))
                    writeNoRsp(
                        writeBuffer.array(),
                        writeTotalSize,
                        writePosition,
                        onSuccess = {
                            writePosition++
                            otaFormat()
                        })
                } else {
                    write(writeOtaFinish(writeTotalSize - 1), onSuccess = {
                        deviceOtaListener?.invoke(D_OTA_SUCCESS, 100)
                    })
                    job?.cancel()
                    job = null
                }
            }
        }

    }

    override fun onBluetoothNotify(service: UUID?, character: UUID?, value: ByteArray) {

    }

    private fun onStartTeLink(): ByteArray { //智建开始更新
        return ByteUtils.stringToBytes(D_OTA_START_TLW)
    }
}