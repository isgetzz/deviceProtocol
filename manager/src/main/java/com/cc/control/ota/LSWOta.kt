package com.cc.control.ota

import com.cc.control.logD
import com.cc.control.protocol.*
import com.cc.control.protocol.DeviceConvert.*
import com.inuker.bluetooth.library.utils.ByteUtils.stringToBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.ceil

/**
 * @Author      : cc
 * @Date        : on 2022-02-22 16:40.
 * @Description : 凌思维-J003
 */
class LSWOta : BaseDeviceOta() {
    //数据包
    private var writeByteArrayList = ArrayList<ByteArray>()
    private var writeTotalSize = 0//总包长进度条形式

    private var totalLength = 0//多少4096扇区
    private var writeProgress = 0 //当前进度条
    private var writeLength = 4096 //每个扇区
    private var writeLittlePosition = 0 //当前扇区index
    private var writeLittleLength = 0 //当前扇区长度
    private var writeTotalPosition = 0//当前包index
    private var writeLittleByteArrayList = ArrayList<ByteArray>()//当前扇区数据集合


    private companion object {
        const val D_OTA_DIGEST1_LSW = "0100"//前区标识01 cmd 00、01 位置
        const val D_OTA_DIGEST2_LSW = "0101"
        const val D_OTA_SECOND_LSW = "0200500518" //数据包标识02 ota地址 18055000
        const val D_OTA_THIRD_LSW = "0300"//第二包接收成功回调
        const val D_OTA_INDEX_LSW = "04"//当前总包数 cmd
        const val D_OTA_SUCCESS_LSW = "05"//当前总包数 cmd
    }

    override fun initFilePath(filePath: String) {
        if (!filePath.isFileExist()) {
            return
        }
        filePath.readFileToByteArray().run {
            val sha32 = getSHA256()
            //源文件用sha256获得32字节、这里需要分成两包
            totalLength = ceil((size * 1.0 / writeLength)).toInt()
            val littleLength = ceil(writeLength / (deviceConnectBean.mtu - 4.0)).toInt()//4096多少mtu
            val residueLength = size % writeLength //取最后剩余byte
            writeTotalSize =
                (size / writeLength * littleLength + ceil(residueLength / (deviceConnectBean.mtu - 4.0))).toInt()
            dvSplitByteArr(writeLength).run {
                writeByteArrayList = this
            }
            write(stringToBytes(D_OTA_DIGEST1_LSW + sha32.substring(0, sha32.length / 2)),
                true) {//成功
                write(stringToBytes(D_OTA_DIGEST2_LSW + sha32.substring(sha32.length / 2,
                    sha32.length)),
                    true) {//成功
                    write(stringToBytes(D_OTA_SECOND_LSW + intTo4HexString(size) + intTo4HexString(
                        deviceConnectBean.mtu - 4)),
                        true)
                }
            }
        }
    }

    /**
     * 每个扇区 length=4096/(mtu-4)
     */
    private fun otaFormat() {
        job?.cancel()
        job = null
        if (isFinish) {
            return
        }
        job = GlobalScope.launch(context = Dispatchers.IO) {
            writeByteArrayList.let {
                //向上取整扇区/mtu-4
                logD(TAG, "otaFormat-1:当前扇区 $writeLittlePosition  扇区总长度$writeLittleLength")
                if (writeLittlePosition < writeLittleLength) {
                    writeNoRsp(writeLittleByteArrayList[writeLittlePosition],
                        writeTotalSize,
                        writeProgress,
                        writeLittleLength,
                        writeLittlePosition) {
                        writeProgress++
                        writeLittlePosition++
                        otaFormat()
                    }
                } else {
                    read {
                        if (ackCheck(it, writeLength, deviceConnectBean.mtu - 4)) {
                            writeLittlePosition = checkPosition(it, writeLittlePosition)
                            otaFormat()
                        } else {
                            writeTotalPosition++
                            if (writeTotalPosition < totalLength) {
                                writePosition()
                            } else {
                                write(stringToBytes(D_OTA_SUCCESS_LSW), true) {
                                    deviceOtaListener?.invoke(D_OTA_SUCCESS, 0)
                                    resetUpdate()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onBluetoothNotify(service: UUID?, character: UUID?, value: ByteArray) {
        logD(TAG, "otaFormat32: ${bytesToHexString(value)}")
        if (bytesToHexString(value).startsWith(D_OTA_THIRD_LSW)) {
            writePosition()
        }
    }

    /**
     * 当前index
     */
    private fun writePosition() {
        writeLittlePosition = 0
        write(stringToBytes(D_OTA_INDEX_LSW + intTo2HexString(writeTotalPosition)), true) {
            writeByteArrayList[writeTotalPosition].dvPositionSplitByte(deviceConnectBean.mtu - 4)
                .run {
                    writeLittleLength = size
                    writeLittleByteArrayList = this
                    logD(TAG, "otaFormat33: $writeLittleLength")
                    otaFormat()
                }
        }
    }
}