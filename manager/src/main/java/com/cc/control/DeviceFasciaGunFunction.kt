package com.cc.control

import com.inuker.bluetooth.library.beacon.BeaconParser
import com.cc.control.protocol.onWriteFasciaGunClear
import com.cc.control.protocol.onWriteFasciaGunConnect
import com.cc.control.protocol.onWriteFasciaGunControl
import com.cc.control.protocol.onWriteFasciaGunStart
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-02-18 16:53.
 * @Description :筋膜枪 FFFO
 */
class DeviceFasciaGunFunction: BaseDeviceFunction() {
    companion object {
        const val STATUS_RUNNING = 0x01//运行:0x01 待机:0x00 低电量：0x04
        const val STATUS_PAUSE = 0x02// 连接未发启动
        const val STATUS_WAIT = 0x03 //连接发启动
    }

    /**
     * 连接-开始-设置阻力
     */
    override fun onDeviceWrite(isCreate: Boolean) {
        write(onWriteFasciaGunConnect()) {
            write(onWriteFasciaGunStart()) {
                write(onWriteFasciaGunControl())
            }
        }
    }

    override fun onDeviceControl(
        speed: Int,
        resistance: Int,
        slope: Int,
    ) {
        writeToFile("onDeviceControl 筋膜枪", "speed: $speed resistance: $resistance slope $slope")
        write(onWriteFasciaGunControl(resistance))
    }

    override fun onBluetoothNotify(
        service: UUID,
        character: UUID,
        value: ByteArray,
        beaconParser: BeaconParser,
    ) {
        if (adr == 0xfb && len == 0x50) {
            deviceNotifyBean.run {
                when (deviceStatus) {
                    STATUS_PAUSE -> {
                        write(onWriteFasciaGunStart())
                    }
                    STATUS_WAIT -> {
                        deviceNotifyBean.status = deviceStatus
                        deviceDataListener?.invoke(this)
                    }
                    STATUS_RUNNING -> {
                        beaconParser.setPosition(3)
                        electric = beaconParser.readByte() //电量
                        grade = beaconParser.readByte() //档位
                        beaconParser.readByte() //模式
                        beaconParser.readShort().toLong() //累积时间
                        beaconParser.readByte() //蓝牙
                        deviceNotifyBean.status = deviceStatus
                        deviceDataListener?.invoke(this)
                    }
                    else -> {

                    }
                }
            }
        }
    }

    override fun onDestroyWrite(): ByteArray {
        return onWriteFasciaGunClear()
    }
}