package com.cc.control.ota

import com.cc.control.bean.UnitEnum
import com.cc.control.logD
import com.cc.control.protocol.*
import com.cc.control.protocol.DeviceConvert.intTo4HexString
import com.cc.control.protocol.DeviceConvert.stringToBytes
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.floor

/**
 * @Author      : cc
 * @Date        : on 2022-04-02 10:03.
 * @Description :富芮坤协-心率跳绳J001
 */
class FRKOta : BaseDeviceOta() {
    private var writeByteArrayList = ArrayList<ByteArray>()
    private val dataClearBuffer = ByteBuffer.allocate(7)
    private val dataWriteBuffer = ByteBuffer.allocate(125)
    private var dataWriteTotalSize = 0
    private var dataWriteLength = 116
    private var isDataClear = false
    private var dataClearPosition = 0
    private var dataClearLastPosition = 0
    private var firstPosition = 0
    private var dataClearTotalSize = 0L
    private var dataWritePosition = 0
    private var fileName = ""
    private var fileTotalSize = 0

    override fun initFilePath(filePath: String) {
        write(stringToBytes("010000"))
        val fileByte = filePath.readFileToByteArray()
        fileTotalSize = fileByte.size
        //擦除长度 4kb每次
        dataClearTotalSize = (fileTotalSize / 1024 / 4).toLong()
        fileName = filePath
        fileByte.dvSplitByteArr(dataWriteLength).run {
            writeByteArrayList = this
            dataWriteTotalSize = this.size
        }
    }

    override fun onBluetoothNotify(service: UUID?, character: UUID?, value: ByteArray) {
        if (!isDataClear) {
            if (dataClearPosition == 0) {
                val beaconParser = DeviceByteParser(value)
                //跳过包头
                beaconParser.setPosition(4)
                //得到当前第一包扇区地址
                firstPosition = beaconParser.readByte32()
                dataClearLastPosition = firstPosition
            }
            if (!isDataClear) {
                dataClear()
            }
        }
    }

    private fun dataClear() {
        dataClearBuffer.clear()
        dataClearBuffer.put(0x03)
        dataClearBuffer.put(0x07)
        dataClearBuffer.put(0x00)
        dataClearLastPosition.dvToByteArray(UnitEnum.U_32).forEach {
            dataClearBuffer.put(it)
        }
        write(dataClearBuffer.array(), onSuccess = {
            if (dataClearPosition.toLong() >= dataClearTotalSize) {
                isDataClear = true
                logD(TAG, ("数据擦除完成,开始写入ota数据!"))
                otaFormat()
            } else {
                dataClearLastPosition += 4 * 1024
                dataClearPosition++
            }
        })
    }

    private fun otaFormat() {
        if (isFinish) {
            return
        }
        writeByteArrayList.let {
            //如果是最后一包则要把长度写进去 最后一把的数据长度写进去
            if (dataWritePosition == dataWriteTotalSize - 1) {
                dataWriteLength = writeByteArrayList[dataWritePosition].size
            }
            if (dataWritePosition < dataWriteTotalSize) {
                dataWriteBuffer.clear()
                dataWriteBuffer.put(0x05)
                dataWriteBuffer.put(0x09)
                dataWriteBuffer.put(0x00)
                firstPosition.dvToByteArray(UnitEnum.U_32).forEach {
                    dataWriteBuffer.put(it)
                }
                (dataWriteLength).dvToByteArray(UnitEnum.U_16).forEach {
                    dataWriteBuffer.put(it)
                }
                dataWriteBuffer.put(writeByteArrayList[dataWritePosition])
                write(dataWriteBuffer.array(),
                    onSuccess = {
                        firstPosition += dataWriteLength
                        dataWritePosition++
                        deviceOtaListener?.invoke(D_OTA_UPDATE,
                            floor(dataWritePosition * 1.0 / dataWriteTotalSize * 100).toInt())
                        logD(TAG, "固件升级 $dataWritePosition $dataWriteTotalSize")
                        otaFormat()
                    })
            } else {
                logD(TAG, "ota升级完成")
                //发送重启命令
                write("090A00${intTo4HexString(fileTotalSize)}${
                    intTo4HexString(otaCrc(fileName))
                }".dvToByteArray(), onSuccess = {
                    deviceOtaListener?.invoke(D_OTA_SUCCESS, 100)
                })
            }
        }

    }

}