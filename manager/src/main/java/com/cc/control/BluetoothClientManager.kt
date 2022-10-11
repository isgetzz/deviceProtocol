package com.cc.control

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.cc.control.bean.*
import com.cc.control.ota.MtuGattCallback
import com.cc.control.protocol.DeviceConstants
import com.cc.control.protocol.DeviceConstants.D_CHARACTER_OTA_HEART
import com.cc.control.protocol.DeviceConstants.D_SERVICE_OTA_HEART
import com.cc.control.protocol.DeviceConvert
import com.cc.control.protocol.getUUIdFromString
import com.cc.control.protocol.string2UUID
import com.inuker.bluetooth.library.BluetoothClient
import com.inuker.bluetooth.library.Constants
import com.inuker.bluetooth.library.connect.options.BleConnectOptions
import com.inuker.bluetooth.library.model.BleGattProfile
import com.inuker.bluetooth.library.search.SearchRequest
import com.inuker.bluetooth.library.search.SearchResult
import com.inuker.bluetooth.library.search.response.SearchResponse
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * cc
 * 蓝牙控制类
 */
object BluetoothClientManager {
    private var mClient: BluetoothClient? = null
    const val TAG = "BluetoothClientManager"

    /**
     * 连接回调
     */
    private var onConnectListener: ((Boolean, String, String) -> Unit)? = null

    /**
     * ota 信息回调
     */
    private var onOtaUpdateListener: ((DeviceConnectBean) -> Unit)? = null

    /**
     * 设备数据订阅监听
     */
    var deviceNotify = MutableLiveData<DeviceNotifyBean>()

    // 蓝牙状态监听
    private var mBleConnectStatusListener: DeviceConnectStatusListener =
        DeviceConnectStatusListener()

    //连接监听
    var deviceConnectObserverBean = MutableLiveData<DeviceConnectObserverBean>()

    //修改别名通知
    var deviceAliasBean = MutableLiveData<DeviceAliasBean>()

    //保存最新连接设备详情
    val deviceManagerMap = MutableLiveData<HashMap<String, DeviceConnectBean>?>()

    //心率带
    var heartDeviceFunction: DeviceHeartFunction? = null

    //app 实例化
    internal lateinit var app: Application
    var isShowLog = false//是否打印日志

    /**
     * 必须先初始化不然无法引用context
     */
    fun initDeviceManager(app: Application, showLog: Boolean = false) {
        this.app = app
        isShowLog = showLog
    }

    /**
     * 蓝牙唯一操作对象
     */
    val client: BluetoothClient
        get() {
            if (mClient == null) {
                synchronized(BluetoothClientManager::class.java) {
                    if (mClient == null) {
                        mClient = BluetoothClient(app)
                    }
                }
            }
            return mClient!!
        }

    /**
     * 连接设备
     * 连接成功才注册回调防止测试设备太多导致注册太多回调
     * 首台连接设备需要手动发送设备状态刷新,注册完之后自动监听状态
     *@param  deviceConnectBean 连接bean
     *
     */
    fun onDeviceConnect(
        deviceConnectBean: DeviceConnectBean,
        connectListener: (isConnect: Boolean, f8c4: String, modelId: String) -> Unit,
    ) {
        onConnectListener = connectListener
        deviceConnectBean.run {
            client.connect(address, connectOptions) { code, data ->
                if (code == Constants.REQUEST_SUCCESS) {
                    //服务跟特征值
                    bleProfile = data
                    val array = data.getUUIdFromString("f8c0", "f8c4")
                    if (array.isNotEmpty()) {
                        getZJDeviceInfo(address, array)
                    } else {
                        onConnectListener?.invoke(true, "", "")
                    }
                    client.registerConnectStatusListener(address, mBleConnectStatusListener)
                } else {
                    writeToFile("onDeviceConnect ", "$deviceType address: $address $code")
                    logD(TAG, "${deviceConnectBean.deviceType} ${deviceConnectBean.address} $code")
                    onConnectListener?.invoke(false, "", "")
                }
            }
        }
    }

    /**
     * 连接心率带设备
     *
     */
    fun onDeviceHeartConnect(
        heartMac: String,
        onConnectListener: ((isConnect: Boolean) -> Unit)? = null,
    ) {
        if (heartMac.isEmpty()) {
            onConnectListener?.invoke(false)
        } else {
            if (isBluetoothOpened()) {
                openBluetooth()
                onConnectListener?.invoke(false)
                return
            }
            client.connect(heartMac, connectOptions) { code, _ ->
                val isConnect = code == Constants.REQUEST_SUCCESS
                if (isConnect) {
                    client.registerConnectStatusListener(heartMac, mBleConnectStatusListener)
                    saveDeviceManageBean(DeviceConnectBean().apply {
                        deviceType = DeviceConstants.D_HEART
                        address = heartMac
                        serviceUUId = string2UUID(DeviceConstants.D_SERVICE_DATA_HEART)
                        characterNotify = string2UUID(DeviceConstants.D_CHARACTER_DATA_HEART)
                    })
                }
                onConnectListener?.invoke(isConnect)
                deviceConnectObserverBean.postValue(DeviceConnectObserverBean(heartMac,
                    isConnect,
                    DeviceConstants.D_HEART))
            }
        }
    }

    /**
     * 连接完获取最终设备配置
     */
    fun onDeviceConfig(
        deviceAddBean: DeviceAddBean?,
        deviceConnectBean: DeviceConnectBean,
        otaUpdateListener: ((DeviceConnectBean) -> Unit)? = null,
    ) {
        deviceAddBean?.run {
            onOtaUpdateListener = otaUpdateListener
            readOtaVersion(deviceConnectBean, eigenValue)
            saveDeviceManageBean(deviceConnectBean.apply {
                //目前J003 LSW 用到了mtu交换
                if (deviceName.contains("J003", true)) {
                    MtuGattCallback(address) { num -> mtu = num }
                }
                if (bleProfile == null)
                    return
                serviceUUId = onServiceUUID(deviceConnectBean, communicationProtocol, bleProfile!!)
                //服务里面包含心跳就发送
                hasHeartRate =
                    bleProfile!!.containsCharacter(string2UUID(DeviceConstants.D_SERVICE_MRK),
                        string2UUID(DeviceConstants.D_CHARACTER_HEART_MRK))
                serviceUUId.toString().run {
                    val contains: Boolean = equals(DeviceConstants.D_SERVICE_BQ, ignoreCase = true)
                    if (contains) {
                        characterWrite = string2UUID(DeviceConstants.D_CHARACTER_BQ)
                        characterNotify = characterWrite
                    } else if (contains("1826")) {
                        when (deviceConnectBean.deviceType) {
                            DeviceConstants.D_ROW -> {
                                characterNotify = string2UUID(DeviceConstants.D_SERVICE1826_2AD1)
                            }
                            DeviceConstants.D_BICYCLE -> {
                                characterNotify =
                                    string2UUID(DeviceConstants.D_SERVICE1826_2AD2)
                            }
                            DeviceConstants.D_TECHNOGYM -> {
                                characterNotify =
                                    string2UUID(DeviceConstants.D_SERVICE1826_2ACE)
                            }
                        }
                        characterWrite = string2UUID(DeviceConstants.D_SERVICE1826_2AD9)
                    } else if (lowercase(Locale.getDefault()).contains(DeviceConstants.D_SERVICE_MRK)) {
                        characterNotify = string2UUID(DeviceConstants.D_CHARACTER_DATA_MRK)
                        characterWrite = characterNotify
                    } else {
                        //其他类型直接根据特征值获取
                        for (character in bleProfile!!.getService(serviceUUId).characters) {
                            if (character.property and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0
                                || character.property and BluetoothGattCharacteristic.PROPERTY_WRITE > 0
                            ) {
                                characterWrite = character.uuid
                            } else if (character.property and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0 ||
                                character.property and BluetoothGattCharacteristic.PROPERTY_INDICATE > 0
                            ) {
                                characterNotify = character.uuid
                            }
                        }
                    }
                }
                //获取到设备信息发送回调
                isDeviceConnect = true
                deviceConnectObserverBean.postValue(DeviceConnectObserverBean(address, true,
                    deviceType, deviceName))
            })
        }
    }

    /**
     * 读取心率设备ota信息
     */
    fun getHeartOtaInfo(
        eigenValue: Int,
        deviceConnectBean: DeviceConnectBean,
        otaUpdateListener: ((DeviceConnectBean) -> Unit)? = null,
    ) {
        onOtaUpdateListener = otaUpdateListener
        readOtaVersion(deviceConnectBean, eigenValue)
    }

    /**
     * 心率臂带进入ota指令（第四位为校验位，前三个数之和）
     * @return
     */
    private fun otaModeByte(): ByteArray {
        return byteArrayOf(-94, 4, 1, -89)
    }

    /**
     *进入心率臂带ota模式
     */
    fun enterHeartOta(heartMac: String, enterOta: ((Boolean) -> Unit)) {
        mClient!!.write(
            heartMac,
            UUID.fromString(D_SERVICE_OTA_HEART),
            UUID.fromString(D_CHARACTER_OTA_HEART),
            otaModeByte()
        ) { otaCode ->
            mClient!!.disconnect(heartMac)
            enterOta.invoke(otaCode == 0)
        }
    }

    /**
     *
     * 获取设备硬件跟软件版本
     */
    private fun readOtaVersion(
        deviceConnectBean: DeviceConnectBean,
        eigenValue: Int = 0,
    ) {
        deviceConnectBean.run {
            client.read(address,
                string2UUID(DeviceConstants.D_EQUIPMENT_INFORMATION),
                string2UUID(DeviceConstants.D_CHARACTER_2A24)
            ) { _, data1 ->
                modelNumber = DeviceConvert.bytesToAsciiString(data1)
                if (modelNumber.isNotEmpty() && deviceConnectBean.modelRevision.isNotEmpty()) {
                    saveDeviceManageBean(this)
                    onOtaUpdateListener?.invoke(this)
                }
            }
            client.read(address,
                string2UUID(DeviceConstants.D_EQUIPMENT_INFORMATION),
                string2UUID(if (eigenValue == 1) DeviceConstants.D_CHARACTER_2A26 else DeviceConstants.D_CHARACTER_2A28)
            ) { _, data1 ->
                modelRevision = DeviceConvert.bytesToAsciiString(data1)
                if (modelNumber.isNotEmpty() && deviceConnectBean.modelRevision.isNotEmpty()) {
                    saveDeviceManageBean(this)
                    onOtaUpdateListener?.invoke(this)
                }
            }
        }
    }

    /**
     * 根据协议类型获取服务
     */
    fun getOtaType(deviceConnectBean: DeviceConnectBean, otaType: Int) {
        deviceConnectBean.run {
            deviceOtaType = otaType
            when (otaType) {
                DeviceConstants.D_OTA_BT -> {
                    otaService = string2UUID(DeviceConstants.D_SERVICE_OTA_BT)
                    otaControlCharacter = string2UUID(DeviceConstants.D_CHARACTER_OTA_BT1)
                    otaWriteCharacter = string2UUID(DeviceConstants.D_CHARACTER_OTA_BT2)
                }
                DeviceConstants.D_OTA_DFU -> {

                }
                DeviceConstants.D_OTA_TLW -> {
                    otaService =
                        string2UUID(DeviceConstants.D_SERVICE_OTA_TLW)
                    otaWriteCharacter =
                        string2UUID(DeviceConstants.D_CHARACTER_OTA_TLW)
                }
                DeviceConstants.D_OTA_XXY -> {
                    otaService = string2UUID(DeviceConstants.D_SERVICE_OTA_XXY)
                    otaWriteCharacter = string2UUID(DeviceConstants.D_CHARACTER_OTA_XXY)
                }
                DeviceConstants.D_OTA_FRK -> {
                    otaService =
                        string2UUID(DeviceConstants.D_SERVICE_OTA_FRK)
                    otaWriteCharacter =
                        string2UUID(DeviceConstants.D_CHARACTER_OTA_FRK)
                    otaNotifyCharacter =
                        string2UUID(DeviceConstants.D_NOTIFY_OTA_FRK)
                }
                DeviceConstants.D_OTA_LSW -> {
                    otaService =
                        string2UUID(DeviceConstants.D_SERVICE_OTA_LSW)
                    otaWriteCharacter =
                        string2UUID(DeviceConstants.D_CHARACTER_OTA_LSW)
                    otaControlCharacter =
                        string2UUID(DeviceConstants.D_CONTROL_OTA_LSW)
                    otaNotifyCharacter =
                        string2UUID(DeviceConstants.D_CONTROL_OTA_LSW)
                }
            }
        }
    }

    /**
     * 获取智健的设备modelId 跟
     * @param mac       mac地址
     * @explain 延时10s没有读取返回false防止一直等待
     */
    private fun getZJDeviceInfo(mac: String, arrays: Array<UUID>) {
        var model = ""//modelId
        var f8c4 = "" //智健唯一标识符
        val job = GlobalScope.launch {
            delay(10000)
            onConnectListener?.invoke(false, "", "")
            cancel()
        }
        client.read(mac,
            string2UUID(DeviceConstants.D_EQUIPMENT_INFORMATION),
            string2UUID(DeviceConstants.D_CHARACTER_2A24)) { _: Int, data: ByteArray ->
            model = DeviceConvert.bytesToAsciiString(data)
            if (f8c4.isNotEmpty() && model.isNotEmpty()) {
                job.cancel()
                onConnectListener?.invoke(true, f8c4, model)
            }
        }
        client.read(mac,
            arrays[0],
            arrays[1]) { _: Int, data: ByteArray ->
            f8c4 = DeviceConvert.bytesToAsciiString(data)
            if (f8c4.isNotEmpty() && model.isNotEmpty()) {
                job.cancel()
                onConnectListener?.invoke(true, f8c4, model)
            }
        }
    }

    /**
     * 根据蓝牙名匹配
     * @param deviceName
     */
    inline fun startSearch(
        crossinline searchBackResult: (BluetoothDevice?, Boolean) -> Unit,
        equipNames: List<String>,
        deviceName: String? = null,
    ) {
        var toast = true
        client.search(searchRequest,
            object : SearchResponse {
                override fun onSearchStarted() {}

                @SuppressLint("MissingPermission")
                override fun onDeviceFounded(device: SearchResult) {
                    val name = device.device.name
                    if (name.isNullOrEmpty()) {
                        return
                    }
                    if (equipNames.isEmpty()) {
                        if (name.startsWith("HW401", true) || name.contains("HEART-B2",
                                true) || name.contains("MERACH", true) ||
                            name.startsWith("FS", true) || name.startsWith("TF", true) ||
                            name.contains("CONSOLE", true) || name.contains("MRK", true) ||
                            name.contains("HI-", true)
                        ) {
                            if (deviceName == null) {
                                searchBackResult.invoke(device.device, true)
                            } else if (device.name == deviceName) {
                                toast = false
                                client.stopSearch()
                                searchBackResult.invoke(device.device, true)
                            }
                        }
                    } else {
                        if (equipNames.find { name.startsWith(it) } != null) {
                            if (deviceName == null) {
                                searchBackResult.invoke(device.device, true)
                            } else if (device.name == deviceName) {
                                toast = false
                                client.stopSearch()
                                searchBackResult.invoke(device.device, true)
                            }
                        }
                    }
                    Log.d(TAG, "onDeviceFounded: ${device.rssi} ${device.device.name}")
                }

                override fun onSearchStopped() {
                    if (toast) searchBackResult.invoke(null, false)
                }

                override fun onSearchCanceled() {
                    if (toast) searchBackResult.invoke(null, false)
                }
            })
    }

    /**
     *蓝牙管理
     */
    fun saveDeviceManageBean(
        deviceConnectBean: DeviceConnectBean,
        connectStatus: Boolean = true,
    ) {
        var hashMap = deviceManagerMap.value
        if (hashMap == null) {
            hashMap = HashMap<String, DeviceConnectBean>()
        }
        deviceConnectBean.isDeviceConnect = connectStatus
        hashMap[deviceConnectBean.deviceType] = deviceConnectBean
        deviceManagerMap.postValue(hashMap)
    }

    /**
     *连接状态
     *@param deviceAddress mac地址
     * @return true 连接
     */
    fun deviceConnectBean(deviceAddress: String): DeviceConnectBean {
        deviceManagerMap.value?.forEach {
            if (it.value.address == deviceAddress) {
                return it.value
            }
        }
        return DeviceConnectBean()
    }

    /**
     *deviceType 设备类型
     */
    fun deviceConnectStatus(deviceType: String): DeviceConnectBean {
        deviceManagerMap.value?.forEach {
            if (it.value.deviceType == deviceType) {
                return it.value
            }
        }
        return DeviceConnectBean()
    }

    fun hasBindRateDevice(): Boolean {
        deviceManagerMap.value?.forEach {
            if (it.value.deviceType == DeviceConstants.D_HEART) {
                return true
            }
        }
        return false
    }

    /**
     * 根据后台配置获取对应的服务跟特征值
     */
    private fun onServiceUUID(
        deviceConnectBean: DeviceConnectBean,
        communicationProtocol: Int,
        profile: BleGattProfile,
    ): UUID {
        return when (communicationProtocol) {
            DeviceConstants.D_SERVICE_TYPE_MRK -> {
                deviceConnectBean.deviceProtocol = 1
                string2UUID(DeviceConstants.D_SERVICE_MRK)
            }
            DeviceConstants.D_SERVICE_TYPE_FTMS -> {
                deviceConnectBean.deviceProtocol = 2
                string2UUID(DeviceConstants.D_SERVICE1826)
            }
            DeviceConstants.D_SERVICE_TYPE_ZJ -> {
                deviceConnectBean.deviceProtocol = 3
                string2UUID(DeviceConstants.D_SERVICE_FFFO)
            }
            DeviceConstants.D_SERVICE_TYPE_FASCIA -> {
                deviceConnectBean.deviceProtocol = 6
                string2UUID(DeviceConstants.D_SERVICE_FFFO)
            }
            DeviceConstants.D_SERVICE_TYPE_BQ -> {
                deviceConnectBean.deviceProtocol = 4
                val service = profile.getService(string2UUID(DeviceConstants.D_SERVICE_BQ))
                if (service == null) {
                    string2UUID(DeviceConstants.D_SERVICE_FFFO)
                } else {
                    service.uuid
                }
            }
            DeviceConstants.D_SERVICE_TYPE_OTHER -> {
                val service =
                    profile.getService(string2UUID(DeviceConstants.D_SERVICE_FFFO))
                if (service == null) {
                    deviceConnectBean.deviceProtocol = 2
                    string2UUID(DeviceConstants.D_SERVICE1826)
                } else {
                    deviceConnectBean.deviceProtocol = 3
                    service.uuid
                }
            }
            else -> string2UUID(DeviceConstants.D_SERVICE_MRK)
        }
    }

    /**
     * 蓝牙连接配置
     */
    private val connectOptions: BleConnectOptions by lazy {
        BleConnectOptions.Builder().apply {
            setConnectRetry(1)   // 连接如果失败重试3次
            setConnectTimeout(5000)   // 连接超时5s
            setServiceDiscoverRetry(1)  // 发现服务如果失败重试3次
            setServiceDiscoverTimeout(5000)  // 发现服务超时5s
        }.build()
    }

    /**
     * 蓝牙搜索配置
     */
    val searchRequest: SearchRequest by lazy {
        SearchRequest.Builder()
            .searchBluetoothLeDevice(2000, 1) // 先扫BLE设备3次，每次3s
            .searchBluetoothClassicDevice(1000) // 再扫经典蓝牙5s
            .searchBluetoothLeDevice(2000) // 再扫BLE设备2s
            .build()
    }

    /**
     *true开启蓝牙
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
     * 断连
     */
    fun disConnect(mac: String?) {
        client.disconnect(mac)
    }

    /**
     * 解绑连接监听
     */
    fun unRegisterConnectListener() {
        onConnectListener = null
    }

    /**
     * 解绑ota监听
     */
    fun unRegisterOtaListener() {
        onOtaUpdateListener = null
    }
}
