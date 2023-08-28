package com.cc.control

import android.app.Application
import android.bluetooth.BluetoothGattCharacteristic.*
import android.util.Log
import com.cc.control.LiveDataBus.BLUETOOTH_STATUS_KEY
import com.cc.control.LiveDataBus.CONNECT_BEAN_KEY
import com.cc.control.LiveDataBus.CONNECT_LISTENER_KEY
import com.cc.control.LiveDataBus.STOP_AUTO_KEY
import com.cc.control.bean.DeviceConnectBean
import com.cc.control.bean.DevicePropertyBean
import com.cc.control.bean.StopAutoBean
import com.cc.control.ota.MtuGattCallback
import com.cc.control.protocol.*
import com.cc.control.protocol.BleConfigOptions.bleSearchRequest
import com.cc.control.protocol.BleConfigOptions.connectOptions
import com.inuker.bluetooth.library.BluetoothClient
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener
import com.inuker.bluetooth.library.search.SearchResult
import com.inuker.bluetooth.library.search.response.SearchResponse
import com.peng.ppscale.business.ble.BleOptions
import java.util.*

/**
 * cc
 * 蓝牙控制类
 */
object BluetoothManager {
    private var mClient: BluetoothClient? = null
    const val TAG = "BluetoothManager"

    /**
     * 蓝牙状态监听
     */
    val mBleStatusListener by lazy {
        BleStatusListener()
    }

    /**
     * 设备连接详情集合,连接中、历史连接记录
     */
    private val deviceConnectMap = HashMap<String, DevicePropertyBean>()

    lateinit var mApplication: Application

    /**
     * 是否打印日志
     */
    var isShowLog = false

    /**
     * 蓝牙唯一操作对象
     */
    val client: BluetoothClient
        get() {
            if (mClient == null) {
                synchronized(BluetoothManager::class.java) {
                    if (mClient == null) {
                        mClient = BluetoothClient(mApplication)
                    }
                }
            }
            return mClient!!
        }

    /**
     * 必须先初始化不然无法引用context
     */
    fun initDeviceManager(app: Application, showLog: Boolean = false, logPath: String = "log") {
        mApplication = app
        isShowLog = showLog
        logPathName = logPath
        client.registerBluetoothStateListener(object : BluetoothStateListener() {
            override fun onBluetoothStateChanged(openOrClosed: Boolean) {
                LiveDataBus.postValue(BLUETOOTH_STATUS_KEY, openOrClosed)
            }
        })
    }

    /**
     * 创建设备连接成功后根据设备模块编号再去后台请求设备具体的设备属性
     * 用户主动连接isUser,先断开自动重连，等连接成功或者失败再继续重连
     */
    fun connect(mac: String, type: String, name: String, isUser: Boolean = true) {
        if (mac.isEmpty()) {
            startSearch(listener = { deviceName, address ->
                if (address.isNotEmpty()) {
                    bluetoothConnect(address, deviceName, type, isUser)
                } else {
                    val bean = DeviceConnectBean(mac, type, name, isAuto = !isUser)
                    LiveDataBus.postValue(CONNECT_LISTENER_KEY, bean)
                }
            }, deviceName = name)
        } else {
            bluetoothConnect(mac, name, type, isUser)
        }
    }

    /**
     * 连接健康设备不关闭重连
     */
    private fun bluetoothConnect(mac: String, name: String, type: String, isUser: Boolean) {
        if (isUser && type != DeviceConstants.D_HEART && type != DeviceConstants.D_FAT_SCALE) {
            LiveDataBus.postValue(STOP_AUTO_KEY, StopAutoBean(true, mac))
        }
        val bean = DeviceConnectBean(mac, type, name, connecting = true, isAuto = !isUser)
        LiveDataBus.postValue(CONNECT_LISTENER_KEY, bean)
        client.connect(mac, connectOptions) { code, data ->
            //心率带可以直接获取设备信息
            bean.connecting = false
            if (code == Constants.REQUEST_SUCCESS) {
                val heartService = string2UUID(DeviceConstants.D_SERVICE_MRK)
                val heartCharacter = string2UUID(DeviceConstants.D_CHARACTER_HEART_MRK)
                val heart = data.containsCharacter(heartService, heartCharacter)
                deviceConnectMap[type] = DevicePropertyBean(mac, type, name, data, heart)
                if (name.vbContains("J003")) {
                    MtuGattCallback(mac)
                }
                //部分设备获取设备模块信息才能区分
                val characterBeanList = data.getUUIdFromList()
                bean.requestDevice = true
                if (characterBeanList.isEmpty()) {
                    LiveDataBus.postValue(CONNECT_LISTENER_KEY, bean)
                } else {
                    var countValue = 0 //read完所有设备信息后进行通知
                    getConnectBean(mac, false).run {
                        characterBeanList.forEach {
                            if (it.isContains == true) {
                                if (it.serviceUUID!!.isNotEmpty() && it.characteristicUUID!!.isNotEmpty()) {
                                    client.read(mac,
                                        string2UUID(it.serviceUUID!!),
                                        string2UUID(it.characteristicUUID!!)) { _: Int, data: ByteArray ->
                                        val value = DeviceConvert.bytesToAsciiString(data)
                                        it.characteristicValue = value
                                        countValue++
                                        if (countValue == characterBeanList.size) {
                                            bean.characteristic = characterBeanList
                                            LiveDataBus.postValue(CONNECT_LISTENER_KEY, bean)
                                        }
                                    }
                                }
                            } else {
                                countValue++
                                if (countValue == characterBeanList.size) {
                                    LiveDataBus.postValue(CONNECT_LISTENER_KEY, bean)
                                }
                            }
                        }
                    }
                }
                if (type == DeviceConstants.D_HEART) {
                    initProperty(DeviceConstants.D_HEART, DeviceConstants.D_SERVICE_TYPE_HEART)
                }
                client.registerConnectStatusListener(mac, mBleStatusListener)
            } else {
                LiveDataBus.postValue(CONNECT_LISTENER_KEY, bean)
            }
            writeToFile(TAG, "bluetoothConnect:$code")
        }
    }

    /**
     * 初始化ota跟协议类型
     */
    fun initProperty(
        type: String,
        mProtocol: Int,
        mOtaType: Int = 0,
        deviceUserRelId: String = "",
    ) {
        getConnectBean(type).run {
            //协议
            protocol = mProtocol
            serviceUUID = when (protocol) {
                DeviceConstants.D_SERVICE_TYPE_MRK -> {
                    notifyUUID = string2UUID(DeviceConstants.D_CHARACTER_DATA_MRK)
                    writeUUID = notifyUUID
                    string2UUID(DeviceConstants.D_SERVICE_MRK)
                }
                DeviceConstants.D_SERVICE_TYPE_FTMS -> {
                    notifyUUID = getHWNotify(type)
                    writeUUID = string2UUID(DeviceConstants.D_SERVICE1826_2AD9)
                    string2UUID(DeviceConstants.D_SERVICE1826)
                }
                DeviceConstants.D_SERVICE_TYPE_ZJ -> {
                    string2UUID(DeviceConstants.D_SERVICE_FFFO)
                }
                DeviceConstants.D_SERVICE_TYPE_FASCIA -> {
                    string2UUID(DeviceConstants.D_SERVICE_FFFO)
                }
                DeviceConstants.D_SERVICE_TYPE_BQ -> {
                    val service = bleProfile?.getService(string2UUID(DeviceConstants.D_SERVICE_BQ))
                    if (service == null) {
                        string2UUID(DeviceConstants.D_SERVICE_FFFO)
                    } else {
                        writeUUID = string2UUID(DeviceConstants.D_CHARACTER_BQ)
                        notifyUUID = writeUUID
                        service.uuid
                    }
                }
                DeviceConstants.D_SERVICE_TYPE_OTHER -> {
                    val service =
                        bleProfile?.getService(string2UUID(DeviceConstants.D_SERVICE_FFFO))
                    if (service == null) {
                        protocol = 2
                        notifyUUID = getHWNotify(type)
                        writeUUID = string2UUID(DeviceConstants.D_SERVICE1826_2AD9)
                        string2UUID(DeviceConstants.D_SERVICE1826)
                    } else {
                        protocol = 3
                        service.uuid
                    }
                }
                DeviceConstants.D_SERVICE_TYPE_HEART -> {
                    notifyUUID = string2UUID(DeviceConstants.D_CHARACTER_DATA_HEART)
                    string2UUID(DeviceConstants.D_SERVICE_DATA_HEART)
                }
                else -> string2UUID(DeviceConstants.D_SERVICE_MRK)
            }
            //主要FFF0
            if (writeUUID == null && DeviceConstants.D_SERVICE_TYPE_HEART != protocol) bleProfile?.getService(
                serviceUUID
            )?.characters?.forEach {
                if (it.property and PROPERTY_WRITE_NO_RESPONSE > 0 || it.property and PROPERTY_WRITE > 0) {
                    writeUUID = it.uuid
                } else if (it.property and PROPERTY_NOTIFY > 0 || it.property and PROPERTY_INDICATE > 0) {
                    notifyUUID = it.uuid
                }
            }
            //ota
            when (mOtaType) {
                DeviceConstants.D_OTA_BT -> {
                    otaService = string2UUID(DeviceConstants.D_SERVICE_OTA_BT)
                    otaControl = string2UUID(DeviceConstants.D_CHARACTER_OTA_BT1)
                    otaWrite = string2UUID(DeviceConstants.D_CHARACTER_OTA_BT2)
                }
                DeviceConstants.D_OTA_TLW -> {
                    otaService = string2UUID(DeviceConstants.D_SERVICE_OTA_TLW)
                    otaWrite = string2UUID(DeviceConstants.D_CHARACTER_OTA_TLW)
                }
                DeviceConstants.D_OTA_XXY -> {
                    otaService = string2UUID(DeviceConstants.D_SERVICE_OTA_XXY)
                    otaWrite = string2UUID(DeviceConstants.D_CHARACTER_OTA_XXY)
                }
                DeviceConstants.D_OTA_FRK -> {
                    otaService = string2UUID(DeviceConstants.D_SERVICE_OTA_FRK)
                    otaWrite = string2UUID(DeviceConstants.D_CHARACTER_OTA_FRK)
                    otaNotify = string2UUID(DeviceConstants.D_NOTIFY_OTA_FRK)
                }
                DeviceConstants.D_OTA_LSW -> {
                    otaService = string2UUID(DeviceConstants.D_SERVICE_OTA_LSW)
                    otaWrite = string2UUID(DeviceConstants.D_CHARACTER_OTA_LSW)
                    otaControl = string2UUID(DeviceConstants.D_CONTROL_OTA_LSW)
                    otaNotify = string2UUID(DeviceConstants.D_CONTROL_OTA_LSW)
                }
            }
            isConnect = true
            otaType = mOtaType
            deviceConnectMap[type] = this
            val bean = DeviceConnectBean(address, type, name, true, deviceRelId = deviceUserRelId)
            LiveDataBus.postValue(CONNECT_BEAN_KEY, bean)
            writeToFile(TAG, "$type $mProtocol $mOtaType")
        }
    }

    /**
     * 获取设备硬件跟软件版本
     */
    fun readOtaVersion(
        type: String,
        eigenValue: Int = 0,
        otaListener: ((String, String) -> Unit)? = null,
    ) {
        getConnectBean(type).run {
            client.read(
                address,
                string2UUID(DeviceConstants.D_EQUIPMENT_INFORMATION),
                string2UUID(DeviceConstants.D_CHARACTER_2A24)
            ) { _, data1 ->
                modelNumber = DeviceConvert.bytesToAsciiString(data1)
                if (modelNumber.isNotEmpty() && modelRevision.isNotEmpty()) {
                    deviceConnectMap[type] = this
                    otaListener?.invoke(modelNumber, modelRevision)
                }
            }
            client.read(
                address,
                string2UUID(DeviceConstants.D_EQUIPMENT_INFORMATION),
                string2UUID(if (eigenValue == 1) DeviceConstants.D_CHARACTER_2A26 else DeviceConstants.D_CHARACTER_2A28)
            ) { _, data1 ->
                modelRevision = DeviceConvert.bytesToAsciiString(data1)
                if (modelNumber.isNotEmpty() && modelRevision.isNotEmpty()) {
                    deviceConnectMap[type] = this
                    otaListener?.invoke(modelNumber, modelRevision)
                }
            }
        }
    }

    /**
     * 根据蓝牙名匹配
     * deviceName 指定单个设备名称
     */
    fun startSearch(listener: (name: String, mac: String) -> Unit, deviceName: String = "") {
        var needSearch = true//根据名称搜索到了就立刻关闭
        client.search(bleSearchRequest, object : SearchResponse {
            override fun onSearchStarted() {}
            override fun onDeviceFounded(device: SearchResult) {
                device.run {
                    if (name.isNullOrEmpty() || name.equals("NULL")) {
                        return
                    }
                    if (deviceName.isEmpty()) {
                        listener.invoke(name, address)
                    } else if (name == deviceName && needSearch) {
                        needSearch = false
                        stopSearch()
                        listener.invoke(name, address)
                    }
                    Log.d(TAG, "onDeviceFounded: $rssi $name $deviceName")
                }
            }

            override fun onSearchStopped() {
                if (needSearch) listener.invoke("", "")
            }

            override fun onSearchCanceled() {
                if (needSearch) listener.invoke("", "")
            }

            override fun onSearchFail(p0: Int) {
            }
        })
    }

    fun stopSearch() {
        client.stopSearch()
    }

    /**
     *根据设备类型、mac 获取设备bean
     */
    fun getConnectBean(deviceParams: String, isType: Boolean = true): DevicePropertyBean {
        if (deviceParams.isEmpty())
            return DevicePropertyBean()
        deviceConnectMap.forEach {
            val param = if (isType) it.value.type else it.value.address
            if (param == deviceParams) {
                return it.value
            }
        }
        return DevicePropertyBean()
    }

    /**
     *根据设备类型、mac 获取连接状态
     */
    fun isConnect(deviceParams: String, isType: Boolean = true): Boolean {
        return if (deviceParams.isEmpty())
            false
        else getConnectBean(deviceParams, isType).isConnect
    }

    /**
     * 断连
     * isUser true 用户自动断开
     * unBind 解绑会更新重连设备不需要处理
     */
    fun disConnect(canAutoConnect: Boolean = false, mac: String, unBind: Boolean = false) {
        if (!unBind) {
            getConnectBean(mac, false).run {
                autoConnect = canAutoConnect
            }
        }
        client.disconnect(mac)
    }

    /**
     *获取华为特征值
     */
    private fun getHWNotify(type: String): UUID? {
        return when (type) {
            DeviceConstants.D_ROW -> {
                string2UUID(DeviceConstants.D_SERVICE1826_2AD1)
            }
            DeviceConstants.D_BICYCLE -> {
                string2UUID(DeviceConstants.D_SERVICE1826_2AD2)
            }
            DeviceConstants.D_TECHNOGYM -> {
                string2UUID(DeviceConstants.D_SERVICE1826_2ACE)
            }
            else -> null
        }
    }

    /**
     *true需要开启蓝牙
     */
    fun isBluetoothOpened(): Boolean {
        return !client.isBluetoothOpened
    }

    /**
     *请求开启蓝牙
     */
    fun openBluetooth() {
        client.openBluetooth()
    }

    /**
     *清除所有连接缓存
     */
    fun disConnectMap() {
        deviceConnectMap.forEach { (_, value) ->
            if (value.isConnect) {
                disConnect(mac = value.address)
            }
        }
        deviceConnectMap.clear()
    }

    /**
     *存储bean
     */
    fun saveConnectMap(bean: DevicePropertyBean) {
        deviceConnectMap[bean.type] = bean
    }

    /**
     * 乐福体脂秤配置
     */
    fun getBleOptions(): BleOptions {
        return BleOptions.Builder()
            .setSearchTag(BleOptions.SEARCH_TAG_NORMAL)
            //.setSearchTag(BleOptions.SEARCH_TAG_DIRECT_CONNECT)//direct connection
            .build()
    }

}
