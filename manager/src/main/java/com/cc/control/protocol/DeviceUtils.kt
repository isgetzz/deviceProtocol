package com.cc.control.protocol

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.text.TextUtils
import com.cc.control.*
import com.cc.control.ota.*
import com.inuker.bluetooth.library.model.BleGattProfile
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-06-29 10:07.
 * @Description :描述
 */
fun String.readFileToByteArray(): ByteArray {
    var bytes = ByteArray(0)
    try {
        val fis = FileInputStream(this)
        val max = fis.available()
        bytes = ByteArray(max)
        fis.read(bytes)
        fis.close()
    } catch (e: IOException) {
        e.printStackTrace()
        logD("readFileToByteArray", e.toString())
    }
    return bytes
}

/**
 * Indicates if this file represents colorlist file on the underlying file system.
 *
 *  文件路径
 * @return 是否存在文件
 */
fun String.isFileExist(): Boolean {
    if (TextUtils.isEmpty(this)) {
        return false
    }
    val file = File(this)
    return file.exists() && file.isFile
}

/**
 *根据指定字符串获取设备的服务码跟特征值
 */
fun BleGattProfile.getUUIdFromString(serviceString: String, characterString: String): Array<UUID> {
    for (service in services) {
        if (service.toString().contains(serviceString, true)) {
            for (character in service.characters) {
                if (character.uuid.toString().contains(characterString)) {
                    return arrayOf(service.uuid, character.uuid)
                }
            }
        }
    }
    return arrayOf()
}

/**
 *string转为UUID
 */
fun string2UUID(uuid: String): UUID {
    return UUID.fromString(uuid)
}

/**
 * 直播设备控制
 */
fun getDeviceFunction(deviceType: String): BaseDeviceFunction {
    return when (deviceType) {
        //单车协议
        DeviceConstants.D_BICYCLE, DeviceConstants.D_ROW, DeviceConstants.D_TECHNOGYM -> {
            DeviceBicycleFunction()
        }
        DeviceConstants.D_SKIPPING -> {
            DeviceSkippingFunction()
        }
        DeviceConstants.D_FASCIA_GUN -> {
            DeviceFasciaGunFunction()
        }
        DeviceConstants.D_TREADMILL -> {
            DeviceTreadmillFunction()
        }
        else -> {
            DeviceOtherFunction()
        }
    }
}

/**
 * ota类型
 */
fun getDeviceOtaFunction(otaType: Int): BaseDeviceOta {
    return when (otaType) {
        DeviceConstants.D_OTA_TLW -> {
            TLWOta()
        }
        DeviceConstants.D_OTA_BT -> {
            BTOta()
        }
        DeviceConstants.D_OTA_LSW -> {
            LSWOta()
        }
        DeviceConstants.D_OTA_FRK -> {
            FRKOta()
        }
        DeviceConstants.D_OTA_XXY -> {
            XXYOta()
        }
        DeviceConstants.D_OTA_DFU->{
            DFUOta()
        }
        else -> {
            OtherOta()
        }
    }
}

/**
 * @param cls      服务名称
 * @param activity a
 * @return 服务是否启动
 */
fun isServiceRunning(cls: Class<*>, activity: Activity): Boolean {
    val manager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (cls.name == service.service.className) {
            return true
        }
    }
    return false
}

