package com.cc.control.protocol

import com.inuker.bluetooth.library.beacon.BeaconParser
import com.inuker.bluetooth.library.utils.ByteUtils
import com.cc.control.bean.DeviceTrainBean
import kotlin.math.max
import kotlin.math.min

/**
 * @Author      : cc
 * @Date        : on 2022-02-22 11:15.
 * @Description :FTMS 协议
 */
fun onFTMSProtocol(
    deviceNotifyBean: DeviceTrainBean.DeviceTrainBO,
    deviceName: String,
    deviceType: String,
    beaconParser: BeaconParser,
) {
    val adr = beaconParser.readShort()
    deviceNotifyBean.run {
        when (deviceType) {
            DeviceConstants.D_BICYCLE -> {
                if (adr and 1 == 0) {  //瞬时速度
                    speed = (beaconParser.readShort() / 100.0).toFloat()
                }
                if (adr shr 1 and 1 == 1) {//平均速度
                    beaconParser.readShort()
                }
                if (adr shr 2 and 1 == 1) {
                    //636需要0.5单位值基础再除以2
                    val divisor = if (adr == 0x0bfe && deviceName.contains("Merach-MR636")) 4 else 2
                    //当前SPM、踏频 单位0.5
                    spm = (beaconParser.readShort() / divisor).coerceAtMost(200)
                }
                if (adr shr 3 and 1 == 1) {
                    beaconParser.readShort() //平均节奏 如果用到也需要/2
                }
                if (adr shr 4 and 1 == 1) {
                    val distance1 = beaconParser.readShort() ////总距离三位
                    val distance2 = beaconParser.readByte()
                    //老的0934距离x2、新版距离正确0x0974;
                    val oldSpinning = adr == 0x0934
                    //2.7.0.1 产品说暂时保留
                    distance = (distance1 and 0xffffff or distance2) * if (oldSpinning) 2 else 1
                    count = (distance / 4.6).toInt()
                }
                if (adr shr 5 and 1 == 1) {//阻力等级
                    drag = beaconParser.readShort()
                }
                if (adr shr 6 and 1 == 1) { //瞬时功率
                    power = beaconParser.readShort().toFloat()
                }
                if (adr shr 7 and 1 == 1) { //平均功率
                    beaconParser.readShort()
                }
                if (adr shr 8 and 1 == 1) {
                    energy = beaconParser.readShort().toFloat()
                    beaconParser.readShort() //每小时消耗能量(卡路里)0
                    beaconParser.readByte() //每分钟消耗能量（卡路里）
                }
                //当前心率
                if (adr shr 9 and 1 == 1) {
                    deviceRate = (beaconParser.readByte().coerceAtMost(200))
                }
                if (adr shr 10 and 1 == 1) {
                    beaconParser.readByte() //代谢当量
                }
                if (adr shr 11 and 1 == 1) {
                    deviceTime = (beaconParser.readShort().toLong()) //运动时间
                }
                if (adr shr 12 and 1 == 1) {
                    beaconParser.readShort() //剩余时间
                }
            }
            DeviceConstants.D_TECHNOGYM -> {
                // 判断有无数据 adr  0 运行方向 00H：向前	01H：向后 1 more data
                val index = 0
                //运行方向 必有 adr  数据对应  00H：向前	01H：向后
                beaconParser.readByte()
                if (adr and 1 == 0) { //more data 0 有
                    speed = max(beaconParser.readShort() / 100.0f, speed) //瞬时速度
                }
                if (adr shr 1 + index and 1 == 1) {
                    beaconParser.readShort() //平均速度
                }
                if (adr shr 2 + index and 1 == 1) {
                    val distance1 = beaconParser.readShort() //总距离三位
                    val distance2 = beaconParser.readByte()
                    distance = (distance1 and 0xffffff or distance2)
                }
                if (adr shr 3 + index and 1 == 1) { //1
                    spm = min(beaconParser.readShort() / 2, 150)//踏频
                    beaconParser.readShort() / 2 //平均踏频
                }
                if (adr shr 4 + index and 1 == 1) { //踏数//1
                    count = (beaconParser.readShort() / 10) //总步幅 /10
                }
                if (adr shr 5 + index and 1 == 1) { //坡度//0
                    gradient = beaconParser.readShort()
                }
                if (adr shr 6 + index and 1 == 1) { //坡度设置 0
                    beaconParser.readShort()
                    beaconParser.readShort()
                    beaconParser.readShort()
                }
                if (adr shr 7 + index and 1 == 1) { //阻力
                    drag = beaconParser.readShort() / 10
                }
                if (adr shr 8 + index and 1 == 1) {
                    power = beaconParser.readShort().toFloat() //瞬时功率
                }
                if (adr shr 9 + index and 1 == 1) { //平均功率 1
                    beaconParser.readShort()
                }
                if (adr shr 10 + index and 1 == 1) { //总消耗 1
                    energy = beaconParser.readShort().toFloat()
                    beaconParser.readShort()
                    beaconParser.readByte()
                }
                if (adr shr 11 + index and 1 == 1) { //当前心率 1
                    deviceRate = (min(beaconParser.readByte(), 200))
                }
                if (adr shr 12 + index and 1 == 1) { //代谢当量 1
                    beaconParser.readByte()
                }
                if (adr shr 13 + index and 1 == 1) { //运动时间
                    deviceTime = (beaconParser.readShort().toLong())
                }
                if (adr shr 14 + index and 1 == 1) { //剩余时间
                    beaconParser.readShort()
                }
            }
            DeviceConstants.D_ROW -> {
                // TODO: 2021/8/16  低位往高位取  bit 0 有数据  其他bit 1有数据
                //0 其他数据 - 桨频  1 平均桨频  2 总距离 3 瞬时速度 4 平均速度 5 瞬时功率 6 平均功率 7阻力等级 8消耗能量 9心率 10代谢当量 11运动时间 12剩余时间
                if (adr and 1 == 0) { //0  //桨频
                    spm = min(beaconParser.readByte() / 2, 60)
                }
                if (adr shr 1 and 1 == 1) { //1  //总桨数
                    count = (beaconParser.readShort())
                    beaconParser.readByte() //平均桨数
                }
                if (adr shr 2 and 1 == 1) { //1
                    val distance1 = beaconParser.readShort() //总距离三位
                    val distance2 = beaconParser.readByte()
                    distance = (distance1 and 0xffffff or distance2)
                }
                if (adr shr 3 and 1 == 1) { //1
                    beaconParser.readShort() //瞬间速度
                }
                if (adr shr 4 and 1 == 1) { //1
                    beaconParser.readShort() //平均速度
                }
                if (adr shr 5 and 1 == 1) { //1
                    power = beaconParser.readShort().toFloat()//瞬时功率
                }
                if (adr shr 6 and 1 == 1) { //1
                    beaconParser.readShort() //平均功率
                }
                if (adr shr 7 and 1 == 1) { //0
                    beaconParser.readShort()
                }
                if (adr shr 8 and 1 == 1) { //1
                    energy = beaconParser.readShort().toFloat()
                    beaconParser.readShort() //每小时
                    beaconParser.readByte() //每分钟
                }
                if (adr shr 9 and 1 == 1) { //0
                    beaconParser.readByte() //心率
                }
                if (adr shr 10 and 1 == 1) { //0
                    beaconParser.readByte()
                }
                if (adr shr 11 and 1 == 1) { //1
                    deviceTime = (beaconParser.readShort()).toLong() //时间
                }
                if (adr shr 12 and 1 == 1) { //1
                    beaconParser.readShort() //剩余时间
                }
            }
        }
    }
}

/**
 * 华为控制前置指令
 */
fun onFTMSControl(): ByteArray {
    return ByteUtils.stringToBytes("00")
}

/**
 * 华为清除数据
 */
fun onFTMSClear(): ByteArray {
    return ByteUtils.stringToBytes("0801")
}

/**
 * 单车
 * 华为控制
 */
fun onBicycleControl(resistance: Int): ByteArray {
    return ByteUtils.stringToBytes(DeviceConvert.intArrToHexString(0x04, resistance))
}
