package com.cc.control

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cc.control.bean.DeviceConnectBean
import com.cc.control.bean.DeviceNotifyBean
import com.cc.control.bean.DeviceTrainBean
import com.cc.control.protocol.*
import com.cc.control.protocol.DeviceConstants.D_SERVICE1826_2ADA
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.beacon.BeaconParser
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.inuker.bluetooth.library.utils.ByteUtils
import kotlinx.coroutines.*
import java.util.*

/**
 * author : cc
 * desc    : 设备数据类
 * time    : 2022/2/15
 */
abstract class BaseDeviceFunction : DefaultLifecycleObserver {
    companion object {
        const val TAG = "BaseDeviceFunction"
    }

    private var job: Job? = null//数据相关
    private var deviceHeartJob: Job? = null//心率相关
    protected var dateArray: ArrayList<ByteArray> = ArrayList()//获取数据、状态array

    /**
     * 柏群单车、跑步机需要先发连接指令,用于直播开启设备判断
     */
    protected var readyConnect = false

    /**
     * 通知数据回调，防止页面结束还收到数据并上报接口导致数据异常跟内存泄漏
     */
    open var refreshData = true

    /**
     * 数据获取，防止控制指令跟数据指令间隔太短导致无法控制
     */
    open var writeData = true

    /**
     * 记录设备解析完的数据
     */
    open var deviceNotifyBean = DeviceTrainBean.DeviceTrainBO()

    /**
     *根据设备类型获取当前设备信息`
     */
    open var deviceConnectInfoBean: DeviceConnectBean = DeviceConnectBean()

    /**
     *设备状态回调
     */
    protected var deviceStatusListener: DeviceStatusListener? = null

    /**
     * 数据回调
     */
    protected var deviceDataListener: ((DeviceTrainBean.DeviceTrainBO) -> Unit)? = null

    /**
     *退出通道关闭
     */
    abstract fun onDestroyWrite(): ByteArray?

    /**
     * 开始下发指令
     * isCreate 目前用于跑步机
     */
    abstract fun onDeviceWrite(isCreate: Boolean)

    /**
     * 如果是跑步机 传速度跟坡度 阻力0 如果是阻力设备 传阻力坡度数据0
     * speed 速度
     * resistance 阻力
     * slope 坡度
     */
    abstract fun onDeviceControl(speed: Int = 0, resistance: Int = 0, slope: Int = 0)

    /**
     * 跑步机 暂停、继续、开始
     * 会先判断当前状态再去发送对应指令
     * 例如：当前暂停那么就会发送继续指令，当前运行就会发送暂停
     */
    open fun onDeviceTreadmillControl(isPause: Boolean = false) {
    }

    /**
     *设置模式
     */
    open fun onDeviceSetModel(model: Int = 0, targetNum: Int = 0, onSuccess: (() -> Unit)?) {

    }

    /**
     * 设备类型
     */
    private var deviceType = ""

    /**
     * 开始连接并获取数据
     */
    open fun create(
        deviceType: String = "",
        dataListener: ((DeviceTrainBean.DeviceTrainBO) -> Unit),
        statusListener: DeviceStatusListener? = null,
    ) {
        this.deviceType = deviceType
        deviceDataListener = dataListener
        deviceStatusListener = statusListener
        initDevice()
    }

    /**
     * 初始化读写跟数据监听
     * 第一次初始化设备连接才赋值，避免后续回调异常
     * isFirst true 初始状态 false 蓝牙状态改变回调
     */
    private fun initDevice(isFirst: Boolean = true) {
        val connectBean = BluetoothClientManager.getDeviceConnectBean(deviceType)
        if (connectBean.isDeviceConnect) {
            deviceConnectInfoBean = connectBean
            notifyRegister()
            if (!deviceConnectInfoBean.deviceName.contains("Merach-MR636D")) {
                onDeviceWrite(true)
            }
            writeDeviceHeart()
        } else {
            deviceConnectInfoBean.isDeviceConnect = false
        }
        deviceStatusListener?.onDeviceConnectStatus(deviceConnectInfoBean.isDeviceConnect, isFirst)
    }

    override fun onCreate(owner: LifecycleOwner) {
        BluetoothClientManager.deviceLastConnectBean.observe(owner) {
            //已连接进入用mac地址判断  未连接进入连接完成再初始化。
            if (it.deviceAddress == deviceConnectInfoBean.address || (deviceConnectInfoBean.address.isNotEmpty() && deviceConnectInfoBean.isDeviceConnect)) {
                initDevice(false)
            }
        }
        super.onCreate(owner)
    }

    /**
     * 下发指令
     */
    protected fun write(
        byteArray: ByteArray?,
        onSuccess: (() -> Unit)? = null,
    ) {
        if (byteArray == null) {
            return
        }
        deviceConnectInfoBean.run {
            BluetoothClientManager.client.write(
                address,
                serviceUUId,
                characterWrite,
                byteArray
            ) {
                if (it == Constants.REQUEST_SUCCESS) {
                    onSuccess?.invoke()
                }
                logI(TAG,
                    "write: $serviceUUId $characterWrite ${ByteUtils.byteToString(byteArray)} $it")
            }
        }
    }

    /**
     *读取
     */
    protected fun read(
        readServiceUUId: UUID,
        readCharacterUUId: UUID,
        onSuccess: ((ByteArray) -> Unit)?,
    ) {
        deviceConnectInfoBean.run {
            BluetoothClientManager.client.read(
                address,
                readServiceUUId,
                readCharacterUUId) { code, data ->
                if (code == Constants.REQUEST_SUCCESS) {
                    onSuccess?.invoke(data)
                }
            }
            logI(TAG, "read $readServiceUUId $readCharacterUUId $address")
        }
    }

    /**
     *  设备发送获取数据指令
     *  statusCmd 部分设备需要先获取状态
     */
    open fun writeDeviceCmd() {
        job?.cancel()
        job = null
        job = GlobalScope.launch {
            dateArray.forEach {
                if (writeData) {
                    write(it)
                }
                delay(600)
                writeDeviceCmd()
            }
        }
    }

    /**
     * 蓝牙的回调
     */
    protected abstract fun onBluetoothNotify(
        service: UUID,
        character: UUID,
        value: ByteArray,
        beaconParser: BeaconParser,
    )

    /**
     * 开始获取数据
     * 华为服务通道订阅数据通知
     */
    open fun notifyRegister() {
        deviceConnectInfoBean.run {
            BluetoothClientManager.client.notify(address,
                serviceUUId,
                characterNotify,
                mNotifyRsp)
            //华为通道订阅数据通知
            if (serviceUUId.toString().contains("1826")) {
                BluetoothClientManager.client.notify(address,
                    serviceUUId,
                    string2UUID(D_SERVICE1826_2ADA), mNotifyRsp)
            }
            BluetoothClientManager.deviceNotify.postValue(DeviceNotifyBean(true,
                deviceType,
                address))
            logI(TAG, "notifyRegister $serviceUUId $characterNotify $address")
        }
    }


    protected var adr = 0 //当前设备唯一标识
    protected var len = 0
    protected var deviceStatus = -1//状态
    protected var length = 0//数据长度判断是否需要解析当前数据
    private val mNotifyRsp: BleNotifyResponse = object : BleNotifyResponse {
        override fun onResponse(code: Int) {
            logD(TAG, "mNotifyRsp：onResponse：$code")
        }

        override fun onNotify(service: UUID, character: UUID, value: ByteArray) {
            val data = DeviceConvert.bytesToHexString(value)
            if (service.toString() == DeviceConstants.D_SERVICE_DATA_HEART && refreshData) {
                onBluetoothNotify(service, character, value, BeaconParser(value))
            } else if (value.size >= 2) {
                adr = (value[0].toInt() and 0xff)
                len = (value[1].toInt() and 0xff)
                length = value.size
                if (value.size >= 4) {
                    deviceStatus = ((value[2].toInt() and 0xff))
                    if (refreshData) {
                        onBluetoothNotify(service, character, value, BeaconParser(value))
                    }
                }
                if (data.startsWith("025302")) {
                    writeToFile("$TAG onWriteStart  跑步机控制回调", data)
                }
            }
            logD(TAG,
                "mNotifyData: $data 服务特征值: $service $character " +
                        "refreshData:$refreshData  adr: ${adr.dvToHex()}  len: ${len.dvToHex()} " +
                        "deviceStatus: ${deviceStatus.dvToHex()} length ${length.dvToHex()}")
        }
    }

    /**
     * 设备下发心跳
     */
    open fun writeDeviceHeart() {
        if (deviceConnectInfoBean.hasHeartRate) {
            deviceHeartJob = GlobalScope.launch(Dispatchers.IO) {
                BluetoothClientManager.client.write(
                    deviceConnectInfoBean.address,
                    string2UUID(DeviceConstants.D_SERVICE_MRK),
                    string2UUID(DeviceConstants.D_CHARACTER_HEART_MRK),
                    writeHeartRate()
                ) {
                    logD(TAG, "deviceHeartRate: $it")
                }
                delay(20000)
                deviceHeartJob?.cancel()
                writeDeviceHeart()
            }
        }
    }

    /**
     * 根据类型下发清除指令
     * canClear 部分设备不能清除
     *  onSuccess 指令发送成功回调
     */
    open fun writeDeviceClear(canClear: Boolean = true, onSuccess: (() -> Unit)? = null) {
        if (canClear) {
            deviceConnectInfoBean.run {
                if (deviceProtocol == 2) {
                    write(writeFTMSControl()) {
                        write(writeFTMSClear()) {
                            //老版本华为单车需要断开连接
                            if (deviceName.contains("MERACH-MR667", true) ||
                                deviceName.contains("MERACH MR-667", true)
                            ) {
                                BluetoothClientManager.disConnect(address)
                            }
                            onSuccess?.invoke()
                        }
                    }
                } else {
                    write(onDestroyWrite(), onSuccess)
                }
                writeToFile("onDeviceClear", "$canClear $deviceType $deviceName")
            }
        }
    }

    /**
     * 控制的时候清除所有指令防止阻塞
     *@param
     * Constants.REQUEST_READ，所有读请求
     * Constants.REQUEST_WRITE，所有写请求
     *Constants.REQUEST_NOTIFY，所有通知相关的请求
     *Constants.REQUEST_RSSI，所有读信号强度的请求
     * 清除所有请求，则传入0
     */
    open fun clearAllRequest(clearType: Int = 0) {
        if (deviceConnectInfoBean.address.isNotEmpty())
            BluetoothClientManager.client.clearRequest(deviceConnectInfoBean.address, clearType)
    }

    /**
     *状态回调
     */
    open fun registerStatusListener(deviceStatusListener: DeviceStatusListener) {
        this.deviceStatusListener = deviceStatusListener
    }

    /**
     *数据回调回调
     */
    open fun registerDataListener(dataListener: ((DeviceTrainBean.DeviceTrainBO) -> Unit)) {
        this.deviceDataListener = dataListener
    }


    /**
     * 训练结束，回调接口置空防止持有activity引用内存泄漏
     */
    override fun onDestroy(owner: LifecycleOwner) {
        job?.cancel()
        job = null
        deviceHeartJob?.cancel()
        deviceHeartJob = null
        writeDeviceClear()
        deviceConnectInfoBean.serviceUUId?.run {
            BluetoothClientManager.client.unnotify(deviceConnectInfoBean.address,
                deviceConnectInfoBean.serviceUUId,
                deviceConnectInfoBean.characterNotify) {}
            BluetoothClientManager.deviceNotify.postValue(DeviceNotifyBean(false,
                deviceConnectInfoBean.deviceType,
                deviceConnectInfoBean.address))
        }
        refreshData = false
        deviceDataListener = null
        deviceStatusListener = null
        super.onDestroy(owner)
    }
}
