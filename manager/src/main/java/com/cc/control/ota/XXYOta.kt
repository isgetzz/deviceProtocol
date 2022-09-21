package com.cc.control.ota

import android.util.Log
import com.inuker.bluetooth.library.utils.ByteUtils
import com.cc.control.protocol.DeviceConvert.bytesToHexSum
import com.cc.control.protocol.dvSplitByteArrEndSeamProtection
import com.cc.control.protocol.isFileExist
import com.cc.control.protocol.readFileToByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.floor

/**
 * @Author      : cc
 * @Date        : on 2022-02-22 16:40.
 * @Description : 新向远JOO2
 */
class XXYOta : BaseDeviceOta() {
    private var writeByteArrayList = ArrayList<ByteArray>()
    private var totalLength = 0//总包
    private var writeProgress = 0 //当前进度条
    private var writeLength = 256 //每个扇区
    private var endWrite: ByteArray? = null

    override fun onFile(filePath: String) {
        if (!filePath.isFileExist()) {
            return
        }
        val fileByte = filePath.readFileToByteArray()
        endWrite = ByteUtils.stringToBytes(bytesToHexSum(fileByte))
        fileByte.dvSplitByteArrEndSeamProtection(writeLength).run {
            writeByteArrayList = this
            totalLength = this.size
            otaFormat()
        }
    }

    /**
     * 开始更新
     *
     */
    private fun otaFormat() {
        job?.cancel()
        job = null
        if (!isFinish) {
            job = GlobalScope.launch(context = Dispatchers.IO) {
                writeByteArrayList.let {
                    Log.d(TAG, "otaFormat: $writeProgress $totalLength ")
                    if (writeProgress < totalLength) {
                        write(writeByteArrayList[writeProgress],
                            onSuccess = {
                                writeProgress++
                                deviceOtaListener?.invoke(D_OTA_UPDATE,
                                    floor(writeProgress * 1.0 / totalLength * 100).toInt())
                                otaFormat()
                            })
                    } else {
                        //结束校验
                        endWrite?.run {
                            write(this, onSuccess = {
                                deviceOtaListener?.invoke(D_OTA_SUCCESS, 0)
                            })
                        }
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