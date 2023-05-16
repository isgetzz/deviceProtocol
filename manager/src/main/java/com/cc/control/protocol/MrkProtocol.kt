package com.cc.control.protocol

import com.cc.control.bean.DeviceTrainBO
import com.cc.control.protocol.DeviceConvert.*
import com.inuker.bluetooth.library.beacon.BeaconParser
import com.inuker.bluetooth.library.utils.ByteUtils

/**
 * @Author      : cc
 * @Date        : on 2022-02-22 13:59.
 * @Description :MRK 通用协议
 * 跳绳 复位-设置模式-开始
 */
fun onMrkProtocol(
    deviceNotifyBean: DeviceTrainBO,
    parser: BeaconParser,
    endPosition: Int,
) {
    var position = 0
    parser.setPosition(3)
    deviceNotifyBean.run {
        //因为立聪旋钮改为触发才广播数据，防止点击之后状态还是保留上一次状态
        direction = -1
        press = -1
        while (position < endPosition) {
            when (parser.readByte()) {
                0 -> {//电量
                    electric = parser.readByte()
                    position += 2
                }
                1 -> {//备状态 00H待机/休眠 01H空闲 02H预启动 03H运动中 04H暂停 05H预结束 80H故障安全锁脱离
                    parser.readByte()
                    position += 2
                }
                2 -> {//设备模式
                    model = parser.readByte()
                    position += 2
                }
                3 -> {//瞬时速度
                    speed = (parser.readShort() / 100.0).toFloat()
                    position += 3
                }
                4 -> {//瞬时配速
                    parser.readByte() / 10
                    position += 2
                }
                5 -> {//瞬时配速(划船机)
                    parser.readShort()
                    position += 3
                }
                6 -> {//瞬时踏频
                    spm = parser.readShort() / 2
                    position += 3
                }
                7 -> {//瞬时桨频 划船机
                    spm = parser.readByte() / 2
                    position += 2
                }
                8 -> {//瞬时功率
                    power = parser.readShort().toFloat()
                    position += 3
                }
                9 -> {//平均速度
                    avgSpeed = (parser.readShort() / 100.0).toFloat()
                    position += 3
                }
                0x0A -> {//平均配速
                    parser.readByte() / 10
                    position += 2
                }
                0x0B -> {//平均配速(划船机)
                    parser.readShort()
                    position += 3
                }
                0x0C -> {//平均踏频
                    avgSpm = (parser.readShort() / 2.0).toFloat()
                    position += 3
                }
                0x0D -> {//平均桨频
                    spm = (parser.readShort() / 2.0).toInt()
                    parser.readByte()
                    position += 2
                }
                0x0E -> {//平均功率
                    avgPower = parser.readShort().toFloat()
                    position += 3
                }
                0x0F -> {//总距离
                    val distance1 = parser.readShort()
                    val distance2 = parser.readShort()
                    distance = (distance1 or (distance2 shl 16))
                    position += 5
                }
                0x10 -> {//总步数/踏数
                    count = parser.readShort()
                    parser.readShort()
                    position += 5
                }
                0x11 -> {//总桨数
                    count = parser.readShort()
                    position += 3
                }
                0x12 -> {//总消耗卡路里
                    energy = parser.readShort().toFloat()
                    position += 3
                }
                0x13 -> {//每小时消耗卡路里
                    parser.readShort()
                    position += 3
                }
                0x14 -> {//每分钟消耗卡路里
                    parser.readByte()
                    position += 2
                }
                0x15 -> {//代谢当量
                    parser.readByte()
                    position += 2
                }
                0x16 -> {//心率
                    deviceRate = parser.readByte()
                    position += 2
                }
                0x17 -> {//阻力等级
                    drag = parser.readShort() / 10
                    position += 3
                }
                0x18 -> {//坡度
                    gradient = parser.readShort() / 10
                    position += 3
                }
                0x19 -> {//坡度角设置
                    parser.readShort()
                    position += 3
                }
                0x1A -> {//正海拔增益
                    parser.readShort()
                    position += 3
                }
                0x1B -> {//负海拔增益
                    parser.readShort()
                    position += 3
                }
                0x1C -> {//皮带受力
                    parser.readShort()
                    position += 3
                }
                0x1D -> {//功率输出
                    parser.readShort()
                    position += 3
                }
                0x1E -> {//运动时间
                    deviceTime = parser.readShort().toLong()
                    position += 3
                }
                0x1F -> {//剩余时间
                    parser.readShort()
                    position += 3
                }
                0x20 -> {//瞬时运动圈数/次数
                    parser.readShort()
                    position += 3
                }
                0x21 -> {//平均运动圈数/次数
                    parser.readShort()
                    position += 3
                }
                0x22 -> {//总运动圈数/次数
                    count = parser.readShort()
                    position += 3
                }
                0x23 -> {//剩余运动圈数/次数
                    parser.readShort()
                    position += 3
                }
                0x24 -> {//档位等级
                    grade = parser.readByte()
                    position += 2
                }
                0x25 -> {//体重
                    parser.readShort()
                    position += 3
                }
                0x26 -> {//体脂
                    parser.readByte()
                    position += 2
                }
                0x27 -> {//血氧饱和度
                    parser.readByte()
                    position += 2
                }
                0x28 -> {//血压
                    parser.readShort()
                    position += 3
                }
                0x29 -> {//任务完成标记 00H：未完成 01H：完成
                    parser.readByte()
                    position += 2
                }
                0x2A -> {//蓝牙连接状态
                    parser.readByte()
                    position += 2
                }
                0x2B -> {//摇摆方向 00H：直线 01H：左 02H：右
                    direction = parser.readByte()
                    position += 2
                }
                0x2C -> {//摇摆角度
                    parser.readShort()
                    position += 3
                }
                0x2D -> {//平均速度（跳绳）
                    speed = parser.readShort().toFloat()
                    position += 3
                }
                0x2E -> {//最高速度（跳绳）
                    parser.readShort()
                    position += 3
                }
                0x2F -> {//绊绳次数
                    parser.readByte()
                    position += 2
                }
                0x33 -> {//按压信号 00H：存在按压信号
                    press = parser.readByte()
                    position += 2
                }
            }
        }
    }
}

/**
 * 心跳
 */
fun writeHeartRate(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xaa, 0x01, 0, 0x01, 0x55))
}

/**
 * mrk复位设置为正常模式并且清零数据
 * 页面退出调用
 */
fun writeMrkStop(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x04, 0x02, 0x05,
        0x04 xor 0x02 xor 0x05,
        0x55))
}

/**
 * MRK开始
 */
fun writeMrkStart(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x04, 0x02, 0x03,
        0x04 xor 0x02 xor 0x03,
        0x55)
    )
}

/**
 * 复位设置为正常模式并且清零数据 目前用于跳绳切换模式
 *
 */
fun writeMrkReset(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x04, 0x02, 0x06,
        0x04 xor 0x02 xor 0x06,
        0x55))
}

/**
 *00H 正常模式（自由训练,数据不会清除）01H 倒计数 02H 倒计时 03H 超燃脂 (用于J001跳绳) 04H 游戏模式 游戏模式下屏蔽飞梭阻力调节功能
 *数据格式:AA标识位  05长度位 (包长 = 包长 + 数据类型 + 数据内容 + 校验码)  0x02(指令类型 OO设备信息 01 运动信息 02控制 03 测试)
 * 0x17设置设备模式(参考mrk协议Tab2-10)  异或校验码 固定包尾 0x55
 */
fun writeMrkModel(status: Int = 0): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x05, 0x02, 0x17, status,
        0x05 xor 0x02 xor 0x17 xor status,
        0x55))
}

/**
 * 控制0.1单位所以x10
 */
fun writeMrkControl(resistance: Int = 0): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x07, 0x02, 0x09) +
            intTo2HexString(resistance * 10) +
            intTo2HexString(0x07 xor 0x02 xor 0x09 xor (resistance * 10)) + intToHexString(0x55))
}

/**
 * mrk  设置目标
 * 0x10 总协议设置次数
 * 0x11 总协议设置时间
 */
fun writeTargetNum(model: Int, targetNum: Int): ByteArray {
    val cmd = if (model == DeviceConstants.D_TRAIN_NUM) 0x10 else 0x11
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x08, 0x02, cmd) +
            intTo4HexString(targetNum) + intArrToHexString(0x08 xor 0x02 xor cmd xor byteXor(
        targetNum), 0x55))
}