package com.cc.control.protocol

import com.inuker.bluetooth.library.beacon.BeaconParser
import com.inuker.bluetooth.library.utils.ByteUtils
import com.cc.control.bean.DeviceTrainBean
import com.cc.control.protocol.DeviceConvert.intArrToHexString

/**
 * @Author      : cc
 * @Date        : on 2022-02-22 13:59.
 * @Description :MRK 通用协议
 * 跳绳 复位-设置模式-开始
 */
fun onMrkProtocol(
    deviceNotifyBean: DeviceTrainBean.DeviceTrainBO,
    beaconParser: BeaconParser,
    endPosition: Int,
) {
    var position = 0
    beaconParser.setPosition(3)
    val stringBuffer = StringBuffer()
    deviceNotifyBean.run {
        while (position < endPosition) {
            val cmd = beaconParser.readByte()
            stringBuffer.append("$cmd,")
            when (cmd) {
                0 -> {//电量
                    electric = beaconParser.readByte()
                    position += 2
                }
                1 -> {//设备状态  00H 待机/休眠 01H 空闲 02H 预启动 03H 运动中 04H 暂停 05H 预结束 80H 故障 安全锁脱离
                    beaconParser.readByte()
                    position += 2
                }
                2 -> {//设备模式 00H 正常模式（自由训练）01H 倒计数模式 02H 倒计时模式
                    skippingModel = beaconParser.readByte()
                    position += 2
                }
                3 -> {//瞬时速度
                    beaconParser.readShort()
                    position += 3
                }
                4 -> {//瞬时配速
                    beaconParser.readByte()
                    position += 2
                }
                5 -> {//瞬时配速(划船机)
                    beaconParser.readShort()
                    position += 3
                }
                6 -> {//瞬时踏频
                    beaconParser.readShort()
                    position += 3
                }
                7 -> {//瞬时桨频
                    beaconParser.readByte()
                    position += 2
                }
                8 -> {//瞬时功率
                    beaconParser.readShort()
                    position += 3
                }
                9 -> {//平均速度
                    beaconParser.readShort()
                    position += 3
                }
                0x0A -> {//平均配速
                    beaconParser.readByte()
                    position += 2
                }
                0x0B -> {//平均配速(划船机)
                    beaconParser.readShort()
                    position += 3
                }
                0x0C -> {//平均踏频
                    spm = beaconParser.readShort()
                    position += 3
                }
                0x0D -> {//平均桨频
                    spm = beaconParser.readShort()
                    beaconParser.readByte()
                    position += 2
                }
                0x0E -> {//平均功率
                    beaconParser.readShort()
                    position += 3
                }
                0x0F -> {//总距离
                    beaconParser.readShort()
                    beaconParser.readShort()
                    position += 5
                }
                0x10 -> {//总步数/踏数
                    beaconParser.readShort()
                    beaconParser.readShort()
                    position += 5
                }
                0x11 -> {//总桨数
                    beaconParser.readShort()
                    position += 3
                }
                0x12 -> {//总消耗卡路里
                    energy = beaconParser.readShort().toFloat()
                    position += 3
                }
                0x13 -> {//每小时消耗卡路里
                    beaconParser.readShort()
                    position += 3
                }
                0x14 -> {//每分钟消耗卡路里
                    beaconParser.readByte()
                    position += 2
                }
                0x15 -> {//代谢当量
                    beaconParser.readByte()
                    position += 2
                }
                0x16 -> {//心率
                    beaconParser.readByte()
                    position += 2
                }
                0x17 -> {//阻力等级
                    beaconParser.readShort()
                    position += 3
                }
                0x18 -> {//坡度
                    beaconParser.readShort()
                    position += 3
                }
                0x19 -> {//坡度角设置
                    beaconParser.readShort()
                    position += 3
                }
                0x1A -> {//正海拔增益
                    beaconParser.readShort()
                    position += 3
                }
                0x1B -> {//负海拔增益
                    beaconParser.readShort()
                    position += 3
                }
                0x1C -> {//皮带受力
                    beaconParser.readShort()
                    position += 3
                }
                0x1D -> {//功率输出
                    beaconParser.readShort()
                    position += 3
                }
                0x1E -> {//运动时间
                    deviceTime = beaconParser.readShort().toLong()
                    position += 3
                }
                0x1F -> {//剩余时间
                    beaconParser.readShort()
                    position += 3
                }
                0x20 -> {//瞬时运动圈数/次数
                    beaconParser.readShort()
                    position += 3
                }
                0x21 -> {//平均运动圈数/次数
                    beaconParser.readShort()
                    position += 3
                }
                0x22 -> {//总运动圈数/次数
                    count = beaconParser.readShort()
                    position += 3
                }
                0x23 -> {//剩余运动圈数/次数
                    beaconParser.readShort()
                    position += 3
                }
                0x24 -> {//档位等级
                    beaconParser.readByte()
                    position += 2
                }
                0x25 -> {//体重
                    beaconParser.readShort()
                    position += 3
                }
                0x26 -> {//体脂
                    beaconParser.readByte()
                    position += 2
                }
                0x27 -> {//血氧饱和度
                    beaconParser.readByte()
                    position += 2
                }
                0x28 -> {//血压
                    beaconParser.readShort()
                    position += 3
                }
                0x29 -> {//任务完成标记 00H：未完成 01H：完成
                    beaconParser.readByte()
                    position += 2
                }
                0x2A -> {//蓝牙连接状态
                    beaconParser.readByte()
                    position += 2
                }
                0x2B -> {//摇摆方向 00H：直线 01H：左 02H：右
                    beaconParser.readByte()
                    position += 2
                }
                0x2C -> {//摇摆角度
                    beaconParser.readShort()
                    position += 3
                }
                0x2D -> {//平均速度（跳绳）
                    speed = beaconParser.readShort().toFloat()
                    position += 3
                }
                0x2E -> {//最高速度（跳绳）
                    beaconParser.readShort()
                    position += 3
                }
                0x2F -> {//绊绳次数
                    beaconParser.readByte()
                    position += 2
                }
            }
        }
    }
}

/**
 * 心跳
 */
fun onWriteHeartRate(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xaa, 0x01, 0, 0x01, 0x55))
}

/**
 * mrk复位设置为正常模式并且清零数据
 * 页面退出调用
 */
fun onWriteMrkStop(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x04, 0x02, 0x05,
        0x04 xor 0x02 xor 0x05,
        0x55))
}

/**
 * MRK开始
 */
fun onWriteMrkStart(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x04, 0x02, 0x03,
        0x04 xor 0x02 xor 0x03,
        0x55)
    )
}

/**
 * 复位设置为正常模式并且清零数据
 * 目前用于跳绳切换模式
 */
fun onWriteMrkReset(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x04, 0x02, 0x06,
        0x04 xor 0x02 xor 0x06,
        0x55))
}

/**
 * 设置模式
 * 总协议 00H 正常模式（自由训练,数据不会清除）01H 倒计数模式 02H 倒计时模式
 *数据格式AA标识位  0x05 长度位 包长 = 包长 + 数据类型 + 数据内容 + 校验码  0x02 指令类型 + 内容长度可变，
 * 参考 + 异或校验码 固定包尾 0x55
 * 目前用于跳绳
 */
fun onWriteMrkModel(status: Int = 0): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x05, 0x02, 0x17, status,
        0x05 xor 0x02 xor 0x17 xor status,
        0x55))
}

/**
 * mrk  设置目标
 * 0x10 总协议设置次数
 * 0x11 总协议设置时间
 */
fun onWriteTargetNum(model: Int, targetNum: Int): ByteArray {
    val cmd = if (model == DeviceConstants.D_TRAIN_NUM) 0x10 else 0x11
    return ByteUtils.stringToBytes(intArrToHexString(0xAA, 0x08, 0x02, cmd) +
            DeviceConvert.intTo4HexString(targetNum) +
            intArrToHexString(0x08 xor 0x02 xor cmd xor DeviceConvert.byteXor(targetNum), 0x55))
}