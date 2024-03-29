package com.cc.control.ota

import android.net.Uri
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cc.control.BluetoothManager
import com.cc.control.bean.DevicePropertyBean
import com.cc.control.logD
import com.cc.control.protocol.CRC16
import com.cc.control.protocol.DeviceConvert.bytesToHexString
import com.inuker.bluetooth.library.Code.REQUEST_SUCCESS
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.inuker.bluetooth.library.utils.ByteUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
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

    protected lateinit var devicePropertyBean: DevicePropertyBean

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
        connectBean: DevicePropertyBean,
        activity: AppCompatActivity? = null,
        uri: Uri? = null,
        otaListener: ((Int, Int) -> Unit)? = null,
    ) {
        mActivity = activity
        dfuUri = uri
        deviceOtaListener = otaListener
        devicePropertyBean = connectBean
        devicePropertyBean.run {
            if (otaNotify != null) {
                BluetoothManager.client.notify(address, otaService, otaNotify,
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
        devicePropertyBean.run {
            val uuid = if (control) otaControl else otaWrite // true 7000
            BluetoothManager.client.write(address, otaService, uuid, byteArray) {
                val sb = StringBuffer()
                sb.append("mtu:$mtu")
                sb.append("\t")
                sb.append(bytesToHexString(byteArray))
                sb.append("\t")
                sb.append("地址:$address")
                sb.append("\t")
                sb.append("服务:" + otaService.toString())
                sb.append("\t")
                sb.append("特征:$uuid")
                val writeStatus = if (it == Constants.REQUEST_SUCCESS) {
                    onSuccess?.invoke()
                    "OTA写入成功:"
                } else {
                    "OTA写入失败:"
                }
                sb.append(writeStatus)
                logD(TAG, "write: $sb")
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
        onSuccess: (() -> Unit)? = null,
    ) {
        devicePropertyBean.run {
            BluetoothManager.client.write(address, otaService, otaWrite, value) { code ->
                if (code == REQUEST_SUCCESS) {
                    val progress = position + 1
                    logD(TAG, "writeNoRsp:总:$totalSize pro:$progress ${bytesToHexString(value)}")
                    deviceOtaListener?.invoke(D_OTA_UPDATE,
                        floor(progress * 1.0 / totalSize * 100).toInt())
                    onSuccess?.invoke()
                } else {
                    deviceOtaListener?.invoke(D_OTA_ERROR, 0)
                    logD(TAG, "writeNoRsp 失败:${bytesToHexString(value)} ")
                }
            }
        }
    }

    /**
     *读取
     */
    protected fun read(onSuccess: ((ByteArray) -> Unit)? = null) {
        devicePropertyBean.run {
            BluetoothManager.client.read(address, otaService, otaWrite) { code, data ->
                if (code == Constants.REQUEST_SUCCESS) {
                    onSuccess?.invoke(data)
                    logD(TAG, "read: ${bytesToHexString(data)}$otaService $otaWrite")
                }
            }
        }

    }

    /**
     *  关闭刷新通知
     */
    fun resetUpdate() {
        devicePropertyBean.run {
            if (otaNotify != null)
                BluetoothManager.client.unnotify(address, otaService, otaNotify) {}
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
  protected  fun String.readFileToByteArray(): ByteArray {
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
    protected  fun String.isFileExist(): Boolean {
        if (TextUtils.isEmpty(this)) {
            return false
        }
        val file = File(this)
        return file.exists() && file.isFile
    }

}