package com.cc.control.protocol

import com.cc.control.bean.DeviceTrainBO
import com.cc.control.protocol.DeviceConvert.intArrToHexString
import com.inuker.bluetooth.library.beacon.BeaconParser
import com.inuker.bluetooth.library.utils.ByteUtils
import kotlin.math.min

/**
 * @Author      : cc
 * @Date        : on 2022-02-22 14:20.
 * @Description :fff0 数据协议 智健、柏群设备都包含
 */
fun onFFF0Protocol(
    deviceNotifyBean: DeviceTrainBO,
    deviceType: String,
    beaconParser: BeaconParser,
    length: Int,
    writeCallBack: (startNotify: Boolean, byte: ByteArray?) -> Unit,
) {
    beaconParser.readByte()
    val cmd = beaconParser.readByte()
    deviceNotifyBean.run {
        when (cmd) {
            0x41 -> {//设备基础配置信息
                beaconParser.readByte()//0x02
                beaconParser.readByte()//阻力
                beaconParser.readByte()//坡度
                deviceNotifyBean.unitDistance = DeviceConvert.getBit(beaconParser.readByte(), 0)
            }
            0x42 ->//状态
                if (length == 15) {
                    beaconParser.readByte()
                    speed = (beaconParser.readShort() / 100.0).toFloat() //当前速度
                    drag = beaconParser.readByte()//阻力
                    val frequency = beaconParser.readShort().toFloat()
                    spm = when (deviceType) {
                        DeviceConstants.D_ROW -> {
                            min(frequency, 60f).toInt()
                        }
                        DeviceConstants.D_TECHNOGYM -> {
                            min(frequency, 150f).toInt()
                        }
                        else -> {
                            min(frequency, 200f).toInt()
                        }
                    }
                    deviceRate = (min(beaconParser.readByte(), 200)) //设备心率上限200
                    power = ((beaconParser.readShort() / 10).toFloat())//当前功率
                    beaconParser.readByte() //当前坡度
                    writeCallBack.invoke(true, null)
                }
            0x43 ->//运动
                if (length == 13) {
                    beaconParser.readByte() //状态
                    deviceTime = (beaconParser.readShort()).toLong() //时间
                    var realDistance = beaconParser.readShort() //距离
                    energy = beaconParser.readShort() / 10.0f// 10.0//热量
                    count = (beaconParser.readShort()) //数量
                    if (realDistance and 0x8000 == 0x8000) {
                        realDistance = (realDistance and 0x7FFF) * 10 //5.30 绍兴同步协议修改
                    }
                    distance = realDistance
                    writeCallBack.invoke(true, null)
                }
            0xB7 -> {//4401错误才处理,柏群累加和，智健异或
                val status = beaconParser.readByte() //标识位1
                val status2 = beaconParser.readByte() //标识位
                writeCallBack.invoke(false, writeBQBicycleConnect(status, status2))
            }
            0xB0 -> {
                val status = beaconParser.readByte() //标识位1
                val status2 = beaconParser.readByte() //标识位1
                writeCallBack.invoke(false, writeBQBicycleStart(status, status2))
            }
            0xB5 -> {
                val status = beaconParser.readByte() //标识位1
                val status2 = beaconParser.readByte() //标识位1
                val status3 = beaconParser.readByte() //标识位1
                if (status3 == 2) { //开始
                    writeCallBack.invoke(false, writeBQBicycleData(status, status2))
                } else if (status3 == 4) { //重置完发连接A0
                    writeCallBack.invoke(false, writeBQBicycleConnect())
                }
            }
            0xB2 -> { //柏群椭圆机、划船机
                val status1 = beaconParser.readByte() //标识位1
                val status2 = beaconParser.readByte() //标识位2
                //兼容了划船机因为根据status 判断了
                if (status2 == 0xE7) { //划船机 数据位统一减一
                    val min = (beaconParser.readByte() - 1) * 60 //分钟
                    val second = beaconParser.readByte() - 1.toLong() //秒数
                    count = (beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1 //蹋数
                    deviceTime = (min + second)
                    spm =
                        min((beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1.toFloat(),
                            60f).toInt()//桨频
                    distance =
                        ((beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1) //距离
                    energy =
                        ((beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1).toFloat() //卡路里
                    (beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1 //心率
                    power =
                        ((((beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1) / 10.0).toFloat()) //功率
                    beaconParser.readByte()
                    beaconParser.readByte()
                    drag = beaconParser.readByte() - 1 //阻力
                } else if (status2 == 0x01) { //椭圆机
                    val min = (beaconParser.readByte() - 1) * 60 //分钟
                    val second = beaconParser.readByte() - 1 //秒数
                    speed =
                        (((beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1) / 10.0).toFloat() //速度
                    deviceTime = (min + second).toLong()
                    val byte1 = beaconParser.readByte()
                    val byte2 = beaconParser.readByte()
                    val frequency = (byte1 - 1) * 100 + byte2 - 1 //踏频
                    spm = min(frequency, 150)
                    val distanceM =
                        (((beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1) / 10.0).toFloat() //距离
                    distance = (distanceM * 1000).toInt()
                    //卡路里
                    energy =
                        ((beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1).toFloat()
                    //设备心率
                    deviceRate =
                        (min((beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1,
                            200))
                    power =
                        ((((beaconParser.readByte() - 1) * 100 + beaconParser.readByte() - 1) / 10.0).toFloat()) //功率
                    drag = beaconParser.readByte() - 1 //阻力
                }
                writeCallBack.invoke(true, writeBQBicycleData(status1, status2))
            }
        }
    }
}

fun writeBQBicycleData(flag1: Int, flag2: Int): ByteArray { //柏群单车获取数据指令
    val cmd: String = intArrToHexString(0xF0, 0xA2, flag1, flag2)
    return ByteUtils.stringToBytes(cmd + DeviceConvert.byteSum(cmd))
}

fun writeBQBicycleConnect(): ByteArray { //柏群单车连接指令
    return ByteUtils.stringToBytes(intArrToHexString(0xF0, 0xA0, 0x44, 0x01, 0xD5))
}

fun writeBQBicycleConnect(flag: Int, flag1: Int): ByteArray { //柏群单车连接指令, //E7划船机//C8椭圆机//01单车椭圆机
    val cmd: String = intArrToHexString(0xF0, 0xA0, flag, flag1)
    return ByteUtils.stringToBytes(cmd + DeviceConvert.byteSum(cmd))
}

fun writeBQBicycleStart(flag: Int, flag1: Int): ByteArray { //柏群单车开始指令
    val cmd: String = intArrToHexString(0xF0, 0xA5, flag, flag1, 0x02)
    return ByteUtils.stringToBytes(cmd + DeviceConvert.byteSum(cmd))
}

fun writeBQBicycleClear(): ByteArray { //柏群单车清除数据
    val stopCmd: String = intArrToHexString(0xF0, 0xA5, 0x44, 0x01, 0x04)
    return ByteUtils.stringToBytes(stopCmd + DeviceConvert.byteSum(stopCmd))
}

fun writeBQBicycle5Resistance(resistance: Int): ByteArray { //柏群设置阻力K50
    val dragCmd = intArrToHexString(0xF0, 0xA6, 0x44, 0x01, resistance + 1)
    return ByteUtils.stringToBytes(dragCmd + DeviceConvert.byteSum(dragCmd))
}

fun writeBQBicycle6Resistance(resistance: Int): ByteArray {//柏群椭圆机
    val cmd = intArrToHexString(0xF0, 0xA6, 0x44, 0xE7, resistance + 1)
    return ByteUtils.stringToBytes(cmd + DeviceConvert.byteSum(cmd))
}

/**
 * 筋膜枪开始指令
 */
fun writeFasciaGunStart(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xFD, 0x10, 0x01,
        0xFD xor 0x10 xor 0x01, 0xFE))
}

/**
 * 筋膜枪连接指令
 */
fun writeFasciaGunConnect(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xFD, 0x40, 0x01,
        0xFD xor 0x40 xor 1, 0xFE))
}

/**
 * 筋膜枪控制档位
 */
fun writeFasciaGunControl(drag: Int = 1): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xFD, 0x20, drag,
        0xFD xor 0x20 xor drag, 0xFE))
}

/**
 * 清除数据
 */
fun writeFasciaGunClear(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xFD, 0x10, 0x0,
        0xFD xor 0x10, 0xFE))
}
