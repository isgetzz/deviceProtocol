package com.cc.control

import com.cc.control.protocol.*
import com.inuker.bluetooth.library.beacon.BeaconParser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-02-18 18:38.
 * @Description : 智健跑步机 x5、x3、x1、马克龙
 */
open class DeviceTreadmillFunction : BaseDeviceFunction() {
    /**
     * 智健协议除了跳绳需要发送获取状态前发送一个指令
     * BaseDeviceFunction 不做onWriteStart处理，根据跑步机状态再发送对应指令
     */
    override fun onDeviceWrite(isCreate: Boolean) {
        if (!isCreate) {
            onWriteStart()
        }
    }

    /**
     * 兼容跑步机未连接状态教案开始需要先发送连接再发送速度
     */
    private fun onWriteStart(onSuccessCallback: (() -> Unit)? = null) {
        if (dateArray.isEmpty()) {
            dateArray.add(onWriteTreadmillData())
        }
        if (readyConnect) {
            writeDeviceCmd()
            onSuccessCallback?.invoke()
        } else {
            write(onWriteZJModelId())
            write(onWriteTreadmillStart()) {
                write(onWriteTreadmillReady()) {
                    readyConnect = true
                    writeDeviceCmd()
                    onSuccessCallback?.invoke()
                }
            }
        }
    }


    /**
     * 智健协议单位0.1所以跑步机速度需要*10
     */
    override fun onDeviceControl(
        speed: Int,
        resistance: Int,
        slope: Int,
    ) {
        writeToFile(TAG, "onWriteStart 清除其他指令:跑步机 控制延时 $speed $resistance $slope $readyConnect")
        clearAllRequest()
        GlobalScope.launch {
            writeData = false
            delay(300)
            onWriteStart {
                write(onWriteTreadmillControl(speed, slope))
                writeToFile(TAG, "onWriteStart 成功:跑步机 控制延时 $speed $resistance $slope")
                //  writeData = true
            }
            delay(300)
            writeData = true
        }
    }

    override fun onDeviceTreadmillControl(isPause: Boolean) {
        clearAllRequest()
        onWriteStart {
            write(if (isPause) onWriteTreadmillStop() else onWriteTreadmillStart())
        }
        writeToFile("onDeviceTreadmillControl 跑步机", "isPause: $isPause")
    }

    override fun onBluetoothNotify(
        service: UUID,
        character: UUID,
        value: ByteArray,
        beaconParser: BeaconParser,
    ) {
        if (len == 0x51) {
            deviceNotifyBean.run {
                when (deviceStatus) {
                    DEVICE_TREADMILL_PAUSE -> { //跑步机暂停并处于减速完成
                        if (status != deviceStatus) {
                            deviceStatusListener?.onDevicePause()
                        }
                    }
                    DEVICE_TREADMILL_AWAIT, DEVICE_TREADMILL_STOP, DEVICE_TREADMILL_DISABLE, DEVICE_TREADMILL_MALFUNCTION ->
                        //因为刚连跑步机状态可能是待机、停机
                        //获取过数据并且，不是重连
                        if (deviceStatus != status) {
                            writeToFile("onDeviceFinish 跑步机状态", "$deviceStatus")
                            deviceStatusListener?.onDeviceFinish()
                        }
                    DEVICE_TREADMILL_LAUNCHING -> {//倒计时
                        beaconParser.setPosition(3)
                        deviceStatusListener?.onDeviceCountTime(beaconParser.readByte())
                    }
                    DEVICE_TREADMILL_COUNTDOWN -> { //减速中
                        if (deviceStatus != status)
                            deviceStatusListener?.onDeviceSpeedCut()
                    }
                    DEVICE_TREADMILL_RUNNING -> {//启动把状态改为运行中
                        if (deviceStatus != status)
                            deviceStatusListener?.onDeviceRunning()

                    }
                }
                status = deviceStatus
                //运行中并且长度符合防止脏数据
                if (deviceStatus != DEVICE_TREADMILL_RUNNING || length != DEVICE_TREADMILL_LENGTH)
                    return
                //运行状态,第三位开始数据位
                beaconParser.setPosition(3)
                speed = (beaconParser.readByte() / 10.0).toFloat() //当前速度
                gradient = beaconParser.readByte()//当前坡度
                deviceTime = beaconParser.readShort().toLong() //时间
                var realDistance = beaconParser.readShort() //距离
                if (realDistance and 0x8000 == 0x8000) {
                    realDistance = (realDistance and 0x7FFF) * 10
                }
                distance = realDistance
                energy = (beaconParser.readShort() / 10.0f) // 10.0//热量
                count = beaconParser.readShort() //步数
                val rat = beaconParser.readByte()
                this.deviceRate = rat.coerceAtMost(200) //当前心率
                deviceDataListener?.invoke(this)
            }
        }
    }

    override fun onDestroyWrite(): ByteArray {
        return onWriteTreadmillClear()
    }
}