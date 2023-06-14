package com.cc.control

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.cc.control.DeviceFasciaGunFunction.Companion.STATUS_RUNNING
import com.cc.control.LiveDataBus.CONNECT_BEAN_KEY
import com.cc.control.LiveDataBus.STOP_AUTO_KEY
import com.cc.control.bean.*
import com.cc.control.ota.MtuGattCallback
import com.cc.control.protocol.*
import com.cc.control.protocol.DeviceConstants.D_SERVICE1826_2ADA
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.beacon.BeaconParser
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse
import com.inuker.bluetooth.library.utils.ByteUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * author  : cc
 * desc    : 蓝牙设备处理
 * time    : 2022/8/15
 */
abstract class BaseDeviceFunction(private var mDeviceType: String = "") : DefaultLifecycleObserver {
    companion object {
        const val TAG = "BaseDeviceFunction"
    }

    /**
     * 获取数据、状态array
     */
    protected var dateArray: ArrayList<ByteArray> = ArrayList()
    private var writeIndex = 0  //循环发送index
    protected var mLifecycleScope: LifecycleCoroutineScope? = null//主协程
    private var mHeartScope: Job? = null//心跳
    private var mDataScope: Job? = null//数据指令

    /**
     * 柏群单车、跑步机需要先发连接指令,用于直播开启设备判断
     */
    protected var readyConnect = false

    /**
     * 刷新阻力速度坡度，避免自由训练控制过程中更新UI显示
     */
    open var isRefreshResistance = true

    /**
     * 通知数据回调，防止页面结束还收到数据并上报接口导致数据异常跟内存泄漏
     */
    open var isNotifyData = true

    /**
     * 记录设备解析完的数据
     */
    protected var notifyBean = DeviceTrainBO()

    /**
     *根据设备类型获取当前设备信息`
     */
    protected var propertyBean: DevicePropertyBean = DevicePropertyBean()

    /**
     *设备运行状态回调
     */
    protected var mStatusListener: DeviceStatusListener? = null

    /**
     * 连接状态回调
     */
    private var mConnectListener: ((deviceName: String, isConnect: Boolean) -> Unit)? = null

    /**
     * 数据回调
     */
    protected var mDataListener: ((DeviceTrainBO) -> Unit)? = null

    /**
     * 蓝牙的回调
     */
    protected abstract fun onBluetoothNotify(service: UUID, character: UUID, parser: BeaconParser)

    /**
     * 开始下发指令 isCreate 目前用于跑步机
     */
    abstract fun startWrite(isCreate: Boolean)

    /**
     * 目前智健跑步机速度跟坡度一条指令
     * speed 速度 resistance 阻力 slope 坡度
     */
    abstract fun onControl(
        speed: Int = 0, resistance: Int = 0, slope: Int = 0, isDelayed: Boolean = false,
    )

    /**
     * 跑步机 暂停、继续、开始
     */
    open fun onTreadmillControl(isPause: Boolean = false) {}

    /**
     *00H 正常模式（自由训练,数据不会清除）01H 倒计数 02H 倒计时 03H 超燃脂 (用于J001跳绳) 04H 游戏模式 游戏模式下屏蔽飞梭阻力调节功能
     */
    open fun setDeviceModel(model: Int = 0, targetNum: Int = 0, onSuccess: (() -> Unit) = {}) {}

    /**
     *页面销毁数据清除等
     */
    abstract fun onDestroyWrite(): ByteArray?

    override fun onCreate(owner: LifecycleOwner) {
        initConfig(owner)
        initDevice()
        super.onCreate(owner)
    }

    open fun initConfig(owner: LifecycleOwner) {
        mLifecycleScope = owner.lifecycleScope
        LiveDataBus.with<DeviceConnectBean>(CONNECT_BEAN_KEY).observe(owner) {
            if ((propertyBean.address.isEmpty() && it.isConnect && mDeviceType == it.type) || it.address == propertyBean.address) {
                initDevice()
            }
        }
    }

    private var needCallBack = false//连接过该设备才需要回调，避免未连接设备进入也收到断开监听

    /**
     * 根据设备状态开启或者断开设备相关交互
     */
    open fun initDevice() {
        val bean = BluetoothManager.getConnectBean(mDeviceType)
        if (bean.isConnect) {
            propertyBean = bean
            notifyRegister()
            writeHeart()
            startWrite(true)
            needCallBack = true
        } else {
            mDataScope?.cancel()
            mHeartScope?.cancel()
            readyConnect = false
            notifyBean.status = -1
            propertyBean.isConnect = false
        }
        if (needCallBack) mConnectListener?.invoke(propertyBean.name, bean.isConnect)
    }

    /**
     * 写入指令
     */
    protected fun write(byteArray: ByteArray?, onSuccess: (() -> Unit)? = null) {
        if (byteArray == null) {
            return
        }
        propertyBean.run {
            BluetoothManager.client.write(address, serviceUUID, writeUUID, byteArray) {
                if (it == Constants.REQUEST_SUCCESS) {
                    onSuccess?.invoke()
                }
                logI(TAG, "write: $serviceUUID $writeUUID ${ByteUtils.byteToString(byteArray)} $it")
            }
        }
    }

    /**
     *读取指令
     */
    protected fun read(serviceUUID: UUID, characterUUId: UUID, onSuccess: ((ByteArray) -> Unit)?) {
        propertyBean.run {
            BluetoothManager.client.read(address, serviceUUID, characterUUId) { code, data ->
                if (code == Constants.REQUEST_SUCCESS) {
                    onSuccess?.invoke(data)
                }
            }
            logI(TAG, "read $serviceUUID $characterUUId $address")
        }
    }

    /**
     *  设备请求数据指令
     */
    protected fun writeData() {
        if ((mDataScope == null || !mDataScope!!.isActive) && dateArray.size > 0) mDataScope =
            countDownCoroutines(mLifecycleScope!!, countDownTime = 500, onTick = {
                write(dateArray[writeIndex % dateArray.size])
                writeIndex++
            })
    }

    /**
     * 设备心跳间隔20s一次
     */
    private fun writeHeart() {
        if (propertyBean.hasHeartRate && (mHeartScope == null || !mHeartScope!!.isActive)) mHeartScope =
            countDownCoroutines(mLifecycleScope!!, countDownTime = 20000, onTick = {
                BluetoothManager.client.write(propertyBean.address,
                    string2UUID(DeviceConstants.D_SERVICE_MRK),
                    string2UUID(DeviceConstants.D_CHARACTER_HEART_MRK),
                    writeHeartRate()) {
                    logD(TAG, "deviceHeartRate: $it")
                }
            })
    }

    /**
     * 订阅数据通道，华为服务订阅控制通知
     */
    open fun notifyRegister() {
        propertyBean.run {
            BluetoothManager.client.notify(address, serviceUUID, notifyUUID, mNotifyRsp)
            //华为通道订阅数据通知
            if (serviceUUID.toString().contains("1826")) {
                BluetoothManager.client.notify(address,
                    serviceUUID,
                    string2UUID(D_SERVICE1826_2ADA),
                    mNotifyRsp)
            }
            logI(TAG, "notifyRegister $serviceUUID $notifyUUID $address")
        }
    }

    protected var dataLength = 0//数据长度判断是否需要解析当前数据

    /**
     * 数据接收处理
     */
    private val mNotifyRsp: BleNotifyResponse = object : BleNotifyResponse {
        override fun onResponse(code: Int) {
            logD(TAG, "mNotifyRsp：onResponse：$code")
        }

        override fun onNotify(service: UUID, character: UUID, value: ByteArray) {
            dataLength = value.size
            if (isNotifyData) {
                if (service.toString() == DeviceConstants.D_SERVICE_DATA_HEART) {
                    onBluetoothNotify(service, character, BeaconParser(value))
                } else if (dataLength >= 4) {
                    onBluetoothNotify(service, character, BeaconParser(value))
                }
            }
            notifyBean.originalData =
                "接收数据=$isNotifyData 时间戳${System.currentTimeMillis()} 服务值=$service 特征值=$character " +
                        "数据=$${DeviceConvert.bytesToHexString(value)}"
        }
    }

    /**
     * 发清除指令 canClear 部分设备不能清除
     */
    open fun writeClear(canClear: Boolean = true, onSuccess: (() -> Unit)? = null) {
        if (canClear) {
            propertyBean.run {
                if (protocol == DeviceConstants.D_SERVICE_TYPE_FTMS) {
                    write(writeFTMSControl()) {
                        write(writeFTMSClear()) {
                            //老版本华为单车需要断开连接
                            if (name.vbContains("MERACH-MR667") || name.vbContains("MERACH MR-667")) {
                                BluetoothManager.disConnect(false, address, true)
                            }
                            onSuccess?.invoke()
                        }
                    }
                } else {
                    write(onDestroyWrite(), onSuccess)
                }
            }
        }
    }

    /**
     * 清除指令 0所有  读取Constants.REQUEST_READ  写请求Constants.REQUEST_WRITE，
     * 通知Constants.REQUEST_NOTIFY  读信号Constants.REQUEST_RSSI
     */
    private fun clearAllRequest(clearType: Int = Constants.REQUEST_WRITE) {
        BluetoothManager.client.clearRequest(propertyBean.address, clearType)
    }

    /**
     *设置控制并延迟，旋钮的指令保护间隔>200ms isDelayed 是否延迟 自由训练实时数据采集，避免阻力冲突
     */
    private var deviceControlScope: Job? = null

    protected fun deviceControl(writeData: ByteArray, isDelayed: Boolean = true) {
        if (!isDelayed) {
            write(writeData)
        } else if (mLifecycleScope != null && propertyBean.isConnect) {
            deviceControlScope?.cancel()
            deviceControlScope = mLifecycleScope!!.launch {
                isRefreshResistance = false
                delay(300)
                clearAllRequest()
                write(writeData)
                delay(600)
                isRefreshResistance = true
                cancel()
            }
        }
    }

    /**
     * 设备重连 回调状态 1连接中 2连接成功 3连接失败
     * @param isReconnect 区分视频重连跟主动连接，重连需要后续操作，主动重连直接提示设备断开
     */
    fun connect(
        isReconnect: Boolean = true,
        connectListener: ((Int, Boolean) -> Unit)? = null,
        records: DeviceListBean.Records? = null,
    ) {
        val mac = records?.mac ?: propertyBean.address
        LiveDataBus.postValue(STOP_AUTO_KEY, StopAutoBean(true, mac))
        connectListener?.invoke(if (mac.isEmpty()) 3 else 1, isReconnect)
        BluetoothManager.client.connect(mac, BleConfigOptions.connectOptions) { code, data ->
            if (code == Constants.REQUEST_SUCCESS) {
                BluetoothManager.client.registerConnectStatusListener(mac,
                    BluetoothManager.mBleStatusListener)
                //没有连接过，初始化设备信息
                val name = if (records != null) {
                    propertyBean.address = records.mac
                    val heartService = string2UUID(DeviceConstants.D_SERVICE_MRK)
                    val heartCharacter = string2UUID(DeviceConstants.D_CHARACTER_HEART_MRK)
                    val heart = data.containsCharacter(heartService, heartCharacter)
                    BluetoothManager.savaConnectMap(DevicePropertyBean(records.mac,
                        records.productId,
                        records.bluetoothName,
                        data,
                        heart))
                    BluetoothManager.initProperty(records.productId,
                        records.communicationProtocol,
                        records.otaProtocol,
                        records.deviceUserRelId)
                    BluetoothManager.readOtaVersion(records.productId, records.versionEigenValue)
                    records.bluetoothName
                } else {
                    val bean = BluetoothManager.getConnectBean(propertyBean.address, false).apply {
                        isConnect = true
                    }
                    initDevice()
                    bean.name
                }
                if (name.vbContains("J003")) {
                    MtuGattCallback(mac)
                }
                connectListener?.invoke(2, isReconnect)
            } else {
                if (isReconnect) initDevice()
                connectListener?.invoke(3, isReconnect)
            }
            writeToFile(TAG, "bluetoothConnect:$code")
        }
    }

    /**
     * 获取当前设备是否运行
     */
    open fun isDeviceRunning(): Boolean {
        return if (mDeviceType == DeviceConstants.D_FASCIA_GUN) notifyBean.status == STATUS_RUNNING else notifyBean.status == DEVICE_TREADMILL_RUNNING
    }

    /**
     * 手动设置数据，当前用于跳绳设置模式
     */
    open fun setConnectData(deviceBean: DevicePropertyBean, scope: LifecycleCoroutineScope) {
        propertyBean = deviceBean
        mLifecycleScope = scope
    }

    /**
     *  注册连接状态
     */
    open fun registerConnectListener(listener: (String, Boolean) -> Unit) {
        mConnectListener = listener
    }

    /**
     *状态回调
     */
    open fun registerStatusListener(deviceStatusListener: DeviceStatusListener) {
        this.mStatusListener = deviceStatusListener
    }

    /**
     *数据回调回调
     */
    open fun registerDataListener(dataListener: ((DeviceTrainBO) -> Unit)) {
        this.mDataListener = dataListener
    }

    /**
     * 清除回调避免持有activity引用内存泄漏
     */
    override fun onDestroy(owner: LifecycleOwner) {
        propertyBean.run {
            if (serviceUUID != null) BluetoothManager.client.unnotify(address,
                serviceUUID,
                notifyUUID) {}
            writeToFile(TAG,
                "onDestroy: $$serviceUUID $writeUUID $notifyUUID $name $type $protocol")
        }
        writeClear()
        isNotifyData = false
        mLifecycleScope?.cancel()
        mLifecycleScope = null
        mDataListener = null
        mStatusListener = null
        mConnectListener = null
        super.onDestroy(owner)
    }
}
