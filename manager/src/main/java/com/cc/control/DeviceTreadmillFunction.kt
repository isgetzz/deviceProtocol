package com.cc.control

import com.cc.control.protocol.*
import com.inuker.bluetooth.library.beacon.BeaconParser
import java.util.*

/**
 * Author      : cc
 * Date        : on 2022-02-18 18:38.
 * Description : 智健跑步机 x5、x3、x1、马克龙
 */
open class DeviceTreadmillFunction(device: String) : BaseDeviceFunction(device) {
    /**
     * 先获取跑步机的状态，自由训练、直播录播、视频根据当前状态处理，避免x5、T05部分设备不主动获取而无法开始
     */
    override fun startWrite(isCreate: Boolean) {
        if (isCreate) {
            write(readZJModelId())
            write(writeTreadmillData())
        } else {
            onWriteStart()
        }
    }

    /**
     * 兼容跑步机未连接状态教案开始需要先发送连接再发送速度
     */
    private fun onWriteStart(onSuccessCallback: (() -> Unit)? = null) {
        if (dateArray.isEmpty()) {
            dateArray.add(writeTreadmillData())
        }
        if (readyConnect) {
            writeData()
            onSuccessCallback?.invoke()
        } else {
            write(readZJModelId())
            write(writeTreadmillStart()) {
                write(writeTreadmillReady()) {
                    readyConnect = true
                    writeData()
                    onSuccessCallback?.invoke()
                }
            }
        }
    }


    /**
     * 智健协议单位0.1所以跑步机速度需要*10
     */
    override fun onControl(speed: Int, resistance: Int, slope: Int, isDelayed: Boolean,isSlope: Boolean) {
        onWriteStart {
            deviceControl(writeTreadmillControl(speed, slope), isDelayed)
        }
        writeToFile(TAG, "onControl ${propertyBean.name} $speed $slope $readyConnect")
    }

    override fun onTreadmillControl(isPause: Boolean) {
        if (!isPause) {
            onWriteStart {
                write(writeTreadmillStart())
            }
        } else if (notifyBean.status == DEVICE_TREADMILL_RUNNING) {
            write(writeTreadmillStop())
        }
        writeToFile("onTreadmillControl 跑步机", "isPause: $isPause")
    }

    override fun onBluetoothNotify(service: UUID, character: UUID, parser: BeaconParser) {
        parser.readByte()//标识位
        if (parser.readByte() == 0x51) {
            notifyBean.run {
                val deviceStatus = parser.readByte()
                when (deviceStatus) {
                    DEVICE_TREADMILL_PAUSE -> { //跑步机暂停并处于减速完成
                        if (status != deviceStatus) {
                            mStatusListener?.onPause()
                        }
                    }
                    DEVICE_TREADMILL_AWAIT, DEVICE_TREADMILL_STOP, DEVICE_TREADMILL_DISABLE, DEVICE_TREADMILL_MALFUNCTION ->
                        //因为刚连跑步机状态可能是待机、停机 获取过数据并且，不是重连
                        if (deviceStatus != status) {
                            mStatusListener?.onFinish()
                        }
                    DEVICE_TREADMILL_LAUNCHING -> {//倒计时
                        if (deviceStatus != status)
                            mStatusListener?.onCountTime()
                    }
                    DEVICE_TREADMILL_COUNTDOWN -> { //减速中
                        if (deviceStatus != status) mStatusListener?.onSlowDown()
                    }
                    DEVICE_TREADMILL_RUNNING -> {//启动把状态改为运行中
                        if (deviceStatus != status) mStatusListener?.onRunning()
                    }
                }
                status = deviceStatus
                //运行中并且长度符合防止脏数据
                if (deviceStatus != DEVICE_TREADMILL_RUNNING || dataLength != DEVICE_TREADMILL_LENGTH)
                    return
                speed = (parser.readByte() / 10.0).toFloat() //当前速度
                gradient = parser.readByte()//当前坡度
                deviceTime = parser.readShort().toLong() //时间
                var realDistance = parser.readShort() //距离
                if (realDistance and 0x8000 == 0x8000) {
                    realDistance = (realDistance and 0x7FFF) * 10
                }
                distance = realDistance
                energy = (parser.readShort() / 10.0f) // 10.0//热量
                count = parser.readShort() //步数
                val rat = parser.readByte()
                this.deviceRate = rat.coerceAtMost(200) //当前心率
                mDataListener?.invoke(this)
            }
        }
    }

    override fun onDestroyWrite(): ByteArray {
        return writeTreadmillClear()
    }
}