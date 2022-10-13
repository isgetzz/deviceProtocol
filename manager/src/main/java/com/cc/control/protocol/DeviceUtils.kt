package com.cc.control.protocol

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.cc.control.*
import com.cc.control.ota.*
import com.inuker.bluetooth.library.model.BleGattProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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
        DeviceConstants.D_OTA_DFU -> {
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

/**
 * 协程 flow 倒计时
 *@param scope 协程
 *@param totalTime 总时间
 *@param countDownTime 执行间隔时间
 *@param onTick 每次执行回调
 *@param onFinish 倒计时结束
 */
fun countDownCoroutines(
    scope: CoroutineScope,
    totalTime: Long = Long.MAX_VALUE,
    countDownTime: Long = 1000,
    onTick: (Long) -> Unit,
    onFinish: (() -> Unit)? = null,
): Job {
    var timer = 0L
    return flow {
        for (num in totalTime downTo 0) {
            emit(num)
            delay(countDownTime)
        }
    }
        .flowOn(Dispatchers.Default)
        .onEach {
            timer++
            onTick.invoke(it)
        }
        .onCompletion {
            if (timer == totalTime) {
                onFinish?.invoke()
            }
        }
        .flowOn(Dispatchers.Main)
        .launchIn(scope) //保证在一个协程中执行
}

fun testFlow(deviceName: String, lifecycleScope: CoroutineScope) {
    //    runCatching { }.onSuccess { }.onFailure { }
    flow {
        //Network.instance.get("/equip/equipment/"
        emit("数据请求")
    }
        .flowOn(Dispatchers.Default)
//        .zip(flowOf("1111")) {a,b->
//           // 多个请求执行完成在调用 collect
//        }
        .map {
            //用于顺序完成多个请求
            //   "$it\n============\n" + Network.instance.get("/equip/equipment/)
            "第一个请求成功，继续请求下一个"
        }
        .onEach {
            //onCompletion 执行之后调用
            Log.d("testFlow1", "onEach: $it")
        }
        .onCompletion {
            //请求完成
            Log.d("testFlow1", "onCompletion: $this")
        }
//        .retryWhen { cause, attempt ->
//           //请求失败重试
//            cause !is NullPointerException && attempt <= 2
//        }
        .catch { e ->
            //异常处理，放在后面捕获上游所有异常
            Log.d("testFlow1", "catch: $this ${e.message}")
        }
        .launchIn(lifecycleScope)//指定协程，类似suspend 挂起调用 collect
}


