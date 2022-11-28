package com.cc.control.ota

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cc.control.BluetoothClientManager
import com.cc.control.bean.DeviceConnectBean
import com.cc.control.logD
import com.cc.control.protocol.CRC16
import com.cc.control.protocol.DeviceConvert.bytesToHexString
import com.inuker.bluetooth.library.Code.REQUEST_SUCCESS
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.inuker.bluetooth.library.utils.ByteUtils
import java.util.*
import kotlin.math.floor

/**
 * @Author      : cc
 * @Date        : on 2022-02-15 13:41.
 * @Description :ota 基类
 */
abstract class BaseDeviceOta : DefaultLifecycleObserver {
    protected val TAG = "BaseDeviceOta"

    companion object {
        const val D_OTA_UPDATE = 0//更新中
        const val D_OTA_SUCCESS = 1//成功
        const val D_OTA_ERROR = 2//失败
    }

    protected lateinit var deviceConnectBean: DeviceConnectBean

    /**
     *  ota回调
     */
    protected var deviceOtaListener: ((Int, Int) -> Unit)? = null

    /**
     * fileName 文件路径
     */
    abstract fun initFilePath(filePath: String)

    /**
     * 结束标识
     */
    protected var isFinish = false

    /**
     * 数据回调
     */
    protected abstract fun onBluetoothNotify(service: UUID?, character: UUID?, value: ByteArray)

    /**
     * DFU
     */
    protected var mActivity: AppCompatActivity? = null
    protected var dfuUri: Uri? = null

    /**
     * 创建对象
     */
    open fun create(
        connectBean: DeviceConnectBean,
        activity: AppCompatActivity? = null,
        uri: Uri? = null,
        otaListener: ((Int, Int) -> Unit)? = null,
    ) {
        mActivity = activity
        dfuUri = uri
        deviceOtaListener = otaListener
        deviceConnectBean = connectBean
        deviceConnectBean.run {
            if (otaNotifyCharacter != null) {
                BluetoothClientManager.client.notify(address, otaService, otaNotifyCharacter,
                    object : BleNotifyResponse {
                        override fun onResponse(code: Int) {}
                        override fun onNotify(service: UUID?, character: UUID?, value: ByteArray) {
                            if (value.isNotEmpty()) {
                                onBluetoothNotify(service, character, value)
                            }
                        }
                    })
            }
        }
    }

    /**
     * 写入数据 LSW 需要用来两个特征值操作
     */
    protected fun write(
        byteArray: ByteArray,
        control: Boolean = false,
        onSuccess: (() -> Unit)? = null,
    ) {
        deviceConnectBean.run {
            BluetoothClientManager.client.write(address, otaService,
                if (control) otaControlCharacter else otaWriteCharacter,// true 7000
                byteArray
            )
            {
                val sb = StringBuffer()
                sb.append(if (it == Constants.REQUEST_SUCCESS) {
                    "OTA写入成功:"
                } else {
                    "OTA写入失败:"
                })
                sb.append("mtu:$mtu")
                sb.append("\t")
                sb.append(bytesToHexString(byteArray))
                sb.append("\t")
                sb.append("地址:$address")
                sb.append("\t")
                sb.append("服务:" + otaService.toString())
                sb.append("\t")
                sb.append("特征:" + if (control) otaControlCharacter else otaWriteCharacter)
                logD(TAG, "write: $sb")
                if (it == Constants.REQUEST_SUCCESS) {
                    onSuccess?.invoke()
                }
            }
        }
    }

    /**
     * 因为部分设备不支持 writeNoRsp 所以目前暂时统一write后续优化
     */
    protected fun writeNoRsp(
        value: ByteArray,
        totalSize: Int,
        position: Int,
        littleSize: Int = 0,
        littlePosition: Int = 0,
        onSuccess: (() -> Unit)? = null,
    ) {
        deviceConnectBean.run {
            BluetoothClientManager.client.write(
                address,
                otaService,
                otaWriteCharacter,
                value
            )
            { code ->
                if (code == REQUEST_SUCCESS) {
                    val progress = position + 1
                    logD(TAG,
                        "writeNoRsp: \"OTA总长度:$totalSize 当前长度:$progress ===>当前扇区总长度:$littleSize " + "当前长度:${littlePosition + 1} ===>\"\n ${
                            bytesToHexString(value)
                        }")
                    deviceOtaListener?.invoke(D_OTA_UPDATE,
                        floor(progress * 1.0 / totalSize * 100).toInt())
                    onSuccess?.invoke()
                } else {
                    deviceOtaListener?.invoke(D_OTA_ERROR, 0)
                    logD(TAG, "writeNoRsp 失败u:${bytesToHexString(value)} ")
                }
            }
        }
    }

    /**
     *读取
     */
    protected fun read(
        onSuccess: ((ByteArray) -> Unit)? = null,
    ) {
        deviceConnectBean.run {
            BluetoothClientManager.client.read(
                address,
                otaService,
                otaWriteCharacter
            ) { code, data ->
                if (code == Constants.REQUEST_SUCCESS) {
                    onSuccess?.invoke(data)
                    logD(TAG, "read: ${bytesToHexString(data)}$otaService $otaWriteCharacter")
                }
            }
        }

    }

    /**
     *  关闭刷新通知
     */
    fun resetUpdate() {
        deviceConnectBean.run {
            if (otaNotifyCharacter != null)
                BluetoothClientManager.client.unnotify(address, otaService, otaNotifyCharacter) {}
        }
        deviceOtaListener = null
        isFinish = true
    }

    /**
     * 释放回调监听
     */
    protected open fun unregisterListener() {

    }

    /**
     * 结束指令
     */
    protected fun writeOtaFinish(index: Int): ByteArray {
        return ByteUtils.stringToBytes("02ff" + CRC16.stringTransposition(index) + CRC16.stringTransposition(
            index.inv() and 0xffff))
    }

    override fun onDestroy(owner: LifecycleOwner) {
        resetUpdate()
        unregisterListener()
        super.onDestroy(owner)
    }
}