package com.cc.control

import com.cc.control.protocol.writeFasciaGunClear
import com.cc.control.protocol.writeFasciaGunConnect
import com.cc.control.protocol.writeFasciaGunControl
import com.cc.control.protocol.writeFasciaGunStart
import com.inuker.bluetooth.library.beacon.BeaconParser
import java.util.*

/**
 * Author      : cc
 * Date        : on 2022-02-18 16:53.
 * Description :筋膜枪 FFFO
 */
class DeviceFasciaGunFunction(device: String) : BaseDeviceFunction(device) {
    companion object {
        const val STATUS_RUNNING = 0x01//运行:0x01 待机:0x00 低电量：0x04
        const val STATUS_PAUSE = 0x02// 连接未发启动
        const val STATUS_WAIT = 0x03 //连接发启动
    }

    /**
     * 连接-开始-设置阻力
     */
    override fun startWrite(isCreate: Boolean) {
        write(writeFasciaGunConnect()) {
            write(writeFasciaGunStart()) {
                write(writeFasciaGunControl())
            }
        }
    }

    override fun onControl(
        speed: Int,
        resistance: Int,
        slope: Int,
        isDelayed: Boolean,
        isSlope: Boolean,
    ) {
        deviceControl(writeFasciaGunControl(resistance), isDelayed)
    }

    override fun onBluetoothNotify(service: UUID, character: UUID, parser: BeaconParser) {
        if (parser.readByte() == 0xfb && parser.readByte() == 0x50) {
            notifyBean.run {
                when (val deviceStatus = parser.readByte()) {
                    STATUS_PAUSE -> {
                        write(writeFasciaGunStart())
                    }
                    STATUS_WAIT -> {
                        if (status != deviceStatus) {
                            status = deviceStatus
                            mDataListener?.invoke(this)
                        } else {
                            status = deviceStatus
                        }
                    }
                    STATUS_RUNNING -> {
                        electric = parser.readByte() //电量
                        grade = parser.readByte() //档位
                        parser.readByte() //模式
                        parser.readShort().toLong() //累积时间
                        parser.readByte() //蓝牙
                        notifyBean.status = deviceStatus
                        mDataListener?.invoke(this)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyWrite(): ByteArray {
        return writeFasciaGunClear()
    }
}