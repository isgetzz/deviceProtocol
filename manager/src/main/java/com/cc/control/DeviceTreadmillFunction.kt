package com.cc.control

import com.inuker.bluetooth.library.beacon.BeaconParser
import com.cc.control.protocol.*
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
            onDeviceCmd()
            onSuccessCallback?.invoke()
        } else {
            write(onWriteZJModelId())
            write(onWriteTreadmillStart()) {
                write(onWriteTreadmillReady()) {
                    readyConnect = true
                    onDeviceCmd()
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
        onWriteStart {
            write(onWriteTreadmillControl(speed, slope))
        }
    }

    override fun onDeviceTreadmillControl(isPause: Boolean) {
        clearAllRequest()
        onWriteStart {
            write(if (isPause) onWriteTreadmillStop() else onWriteTreadmillStart())
        }
    }

    override fun onBluetoothNotify(
        service: UUID,
        character: UUID,
        value: ByteArray,
        beaconParser: BeaconParser,
    ) {
        if (len == 0x51) {
            deviceNotifyBean.run {
                //跑步机暂停并处于减速完成
                if (deviceStatus == DEVICE_TREADMILL_PAUSE && status != deviceStatus) {
                    deviceStatusListener?.onDevicePause()
                }
                //停机
                if ((deviceStatus == DEVICE_TREADMILL_AWAIT || deviceStatus == DEVICE_TREADMILL_STOP || deviceStatus == DEVICE_TREADMILL_DISABLE) && deviceStatus != status) {
                    //因为刚连跑步机状态可能是待机、停机
                    //获取过数据并且，不是重连
                    deviceStatusListener?.onDeviceFinish()
                }
                //倒计时
                if (deviceStatus == DEVICE_TREADMILL_LAUNCHING) {
                    beaconParser.setPosition(3)
                    deviceStatusListener?.onDeviceCountTime(beaconParser.readByte())
                }
                //减速中
                if (deviceStatus == DEVICE_TREADMILL_COUNTDOWN && deviceStatus != status) {
                    deviceStatusListener?.onDeviceSpeedCut()
                }
                //启动把状态改为运行中
                if (deviceStatus == DEVICE_TREADMILL_RUNNING && deviceStatus != status) {
                    deviceStatusListener?.onDeviceRunning()
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