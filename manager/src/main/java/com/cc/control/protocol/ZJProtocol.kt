package com.cc.control.protocol

import com.cc.control.protocol.DeviceConvert.intArrToHexString
import com.inuker.bluetooth.library.utils.ByteUtils

/**
 * @Author      : cc
 * @Date        : on 2022-08-03 15:08.
 * @Description :智健协议相关控制
 * 10.跳绳 设置模式-开始指令(会清除数据)-读取数据(其他数据/电量 300ms 一循环)
 */
const val DEVICE_ZJ_MODEL_ADR = 0x83//智健设置模式成功响应
const val DEVICE_ZJ_ELECTRIC = 0x02//智健电量
const val DEVICE_ZJ_DATA = 0x03//智健数据

/**
 * 跑步机
 */
const val DEVICE_TREADMILL_PAUSE = 0x0A//暂停
const val DEVICE_TREADMILL_RUNNING = 0x03//运行中
const val DEVICE_TREADMILL_AWAIT = 0x00 //待机
const val DEVICE_TREADMILL_LAUNCHING = 0x02 //启动中
const val DEVICE_TREADMILL_COUNTDOWN = 0x04 //减速中
const val DEVICE_TREADMILL_STOP = 0x01//停机
const val DEVICE_TREADMILL_MALFUNCTION = 0x05//故障，部分跑步机安全锁脱落返回
const val DEVICE_TREADMILL_DISABLE = 0x06//禁用 1，安全锁触发； 2，设备睡眠
const val DEVICE_TREADMILL_LENGTH = 17  //数据长度

/**
 * 智健获取modelId 除了跳绳其他设备都需要发送一个指令在获取数据,防止指令发送失效
 */
fun readZJModelId(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x50, 0x00, 0x50, 0x03))
}

/**
 * 智健跳绳
 ***********************************************
 *设置模式 0-自由训练  1-倒计数  2-倒计时
 */
fun writeZJSkippingModel(
    model: Int = 0,
    targetNum: Int = 0,
): ByteArray {
    var num = 0
    var time = 0
    when (model) {
        DeviceConstants.D_TRAIN_NUM -> {
            num = targetNum
        }
        DeviceConstants.D_TRAIN_TIME -> {
            time = targetNum
        }
    }
    return ByteUtils.stringToBytes(intArrToHexString(14, 0x02, 0x83, 0, 0, 0, 0, model) +
            DeviceConvert.intTo2HexString(targetNum) + DeviceConvert.intTo2HexString(time) +
            intArrToHexString(0,
                14 xor 0x02 xor 0x83 xor model xor DeviceConvert.byteXor(targetNum)
                        xor DeviceConvert.byteXor(num)))
}

/**
 * 开始指令,模式切换，之后调用会清除数据
 */
fun writeSkippingStart(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(4, 0x02, 0x81, 0x04 xor 0x02 xor 0x81))
}

/**
 * 300ms读取一次数据
 */
fun writeZJSkippingData(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(4, 0x02, 0x03, 0x04 xor 0x02 xor 0x03))
}

/**
 * 300ms电量
 */
fun writeZJSkippingElectric(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(4, 0x02, 0x02, 0x04 xor 0x02 xor 0x02))
}

/**
 * 清除数据
 */
fun writeZJSkippingClear(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x04, 0x02, 0x84, 0x04 xor 0x02 xor 0x84))
}

/**
 * 智健单车
 * ***********************************************
 * 设置智健阻力Q1划船机、单车
 */
fun writeZJBicycleControl(resistance: Int, slope: Int): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x44, 0x05,
        resistance, slope,
        0x44 xor 0x05 xor resistance xor slope,
        0x03))
}

/**
 * 智建设备参数指令
 */

fun writeZJInfo(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x41, 0x02, 0x43, 0x03))
}

/**
 * 获取智健运动数据
 */
fun writeZJBicycleData(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x43, 0x01, 0x42, 0x03))
}

fun writeZJBicycleClear(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x44, 0x04, 0x40, 0x03))
}

/**
 * 获取智健运动状态
 */
fun writeZJBicycleStatus(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x42, 0x42, 0x03))
}

/**
 * 智健跑步机************************************
 */
fun writeTreadmillClear(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x53, 0x03, 0x53 xor 0x03, 0x03))
}

/**
 *获取数据
 */
fun writeTreadmillData(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x51, 0x51, 0x03))
}

fun writeTreadmillStop(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x53, 0x0A, 0x53 xor 0x0A, 0x03))
}

/**
 * 跑步机控制速度坡度
 */
fun writeTreadmillControl(speed: Int, slope: Int): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02,
        0x53, 0x02, speed, slope,
        0x53 xor 0x02 xor speed xor slope, 0x03))
}

fun writeTreadmillStart(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x53, 0x09, 0x53 xor 9, 0x03))
}

fun writeTreadmillReady(): ByteArray {
    return ByteUtils.stringToBytes(intArrToHexString(0x02, 0x53, 0x01,
        0, 0, 0, 0, 0, 0, 0, 0,
        0x02 xor 0x53 xor 0x01, 0x3))
}
