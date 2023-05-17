package com.cc.control.protocol

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.cc.control.*
import com.cc.control.bean.CharacteristicBean
import com.cc.control.ota.*
import com.inuker.bluetooth.library.model.BleGattProfile
import com.cc.control.DeviceOtherFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-06-29 10:07.
 * @Description :描述
 */

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
 *根据多个字符串获取设备的服务码跟特征值
 */
fun BleGattProfile.getUUIdFromList(): List<CharacteristicBean> {
    val beanList = mutableListOf<CharacteristicBean>()
    beanList.add(CharacteristicBean(service = "F8C0", characteristicProperties = "F8C4"))
    beanList.add(CharacteristicBean(service = DeviceConstants.D_EQUIPMENT_INFORMATION,
        characteristicProperties = DeviceConstants.D_CHARACTER_2A24))
    beanList.add(CharacteristicBean(service = DeviceConstants.D_EQUIPMENT_INFORMATION,
        characteristicProperties = DeviceConstants.D_CHARACTER_2A26))
    beanList.add(CharacteristicBean(service = DeviceConstants.D_EQUIPMENT_INFORMATION,
        characteristicProperties = DeviceConstants.D_CHARACTER_2A28))
    for (service in services) {
        beanList.forEach {
            if (service.uuid.toString().contains(it.service, true)) {
                it.serviceUUID = service.uuid.toString()
                for (character in service.characters) {
                    if (character.uuid.toString().contains(it.characteristicProperties, true)) {
                        it.characteristicUUID = character.uuid.toString()
                        it.isContains = true
                    }
                }
            }
        }
    }
    return beanList
}

/**
 *string转为UUID
 */
fun string2UUID(uuid: String): UUID {
    return UUID.fromString(uuid)
}

/**
 * 设备控制类
 */
fun getDeviceFunction(deviceType: String): BaseDeviceFunction {
    return when (deviceType) {
        DeviceConstants.D_BICYCLE, DeviceConstants.D_ROW, DeviceConstants.D_TECHNOGYM -> {
            DeviceBicycleFunction(deviceType)
        }
        DeviceConstants.D_SKIPPING -> {
            DeviceSkippingFunction(deviceType)
        }
        DeviceConstants.D_HEART -> {
            DeviceHeartFunction(deviceType)
        }
        DeviceConstants.D_FASCIA_GUN -> {
            DeviceFasciaGunFunction(deviceType)
        }
        DeviceConstants.D_TREADMILL -> {
            DeviceTreadmillFunction(deviceType)
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
 * sha256算法
 */
fun ByteArray.getSHA256(): String {
    val messageDigest: MessageDigest
    var str = ""
    try {
        messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(this)
        str = byte2Hex(messageDigest.digest())
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }
    return str
}

private fun byte2Hex(bytes: ByteArray): String {
    val stringBuffer = StringBuilder()
    var temp: String
    for (aByte in bytes) {
        temp = Integer.toHexString((aByte.toInt() and 0xff))
        if (temp.length == 1) {
            stringBuffer.append("0")
        }
        stringBuffer.append(temp)
    }
    return stringBuffer.toString()
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
 * 字符串包含默认忽略大小写
 *
 */
fun String.vbContains(param: String, ignoreCase: Boolean = true): Boolean {
    return contains(param, ignoreCase)
}

fun String.vbStartWith(param: String, ignoreCase: Boolean = true): Boolean {
    return startsWith(param, ignoreCase)
}

/**
 * 协程 flow 倒计时
 *@param scope 协程
 *@param totalTime 总时间
 *@param countDownTime 执行间隔时间
 *@param onTick 每次执行回调
 *@param onFinish 倒计时结束
 *@param isDelay 第一次延迟再倒计时
 */
fun countDownCoroutines(
    scope: CoroutineScope,
    totalTime: Long = Long.MAX_VALUE,
    countDownTime: Long = 1000,
    onTick: (Long) -> Unit,
    onFinish: (() -> Unit)? = null,
    isDelay: Boolean = false,
    delayTime: Long = countDownTime,
): Job {
    var timer = 0L
    var isFirst = isDelay//第一次延迟时间后续用countDownTime
    return flow {
        for (num in totalTime downTo 0) {
            if (isDelay) {
                delay(if (isFirst) delayTime else countDownTime)
                emit(num)
                isFirst = false
            } else {
                emit(num)
                delay(countDownTime)
            }
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

//网络请求
fun httpsTest() {
    flow {
        emit("")//block()// crossinline block: suspend CoroutineScope.() -> String,
    }
        .catch {
            Log.d("BaseViewModel", "catch:${it.message} ")
        }
    //  .collect {
    //try{}catch(){e:Exception}
//            val type: Type = object : TypeReference<ApiResponse<T>>() {}.type
//            val bean = JSON.parseObject(it, type) as ApiResponse<T>
//            if (bean.status == 200) {
//                testSuccess.invoke(bean.data)
//                //     resultData?.postValue(tClass)
//                Log.d("BaseViewModel",
//                    "request: ${bean.message} == ${bean.status}== ${bean.data}")
    //   }

}


