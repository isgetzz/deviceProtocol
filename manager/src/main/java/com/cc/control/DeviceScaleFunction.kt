package com.cc.control

import cn.icomon.icdevicemanager.ICDeviceManager
import cn.icomon.icdevicemanager.ICDeviceManagerDelegate
import cn.icomon.icdevicemanager.callback.ICScanDeviceDelegate
import cn.icomon.icdevicemanager.model.data.*
import cn.icomon.icdevicemanager.model.device.ICDevice
import cn.icomon.icdevicemanager.model.device.ICDeviceInfo
import cn.icomon.icdevicemanager.model.device.ICScanDeviceInfo
import cn.icomon.icdevicemanager.model.device.ICUserInfo
import cn.icomon.icdevicemanager.model.other.ICConstant
import cn.icomon.icdevicemanager.model.other.ICDeviceManagerConfig
import com.cc.control.LiveDataBus.CONNECT_BEAN_KEY
import com.cc.control.bean.DeviceConnectBean
import com.cc.control.bean.DevicePropertyBean
import com.cc.control.bean.ScaleUserBean
import com.cc.control.protocol.DeviceConstants

/**
 * @Author      : cc
 * @Date        : on 2022-06-28 10:48.
 * @Description :体脂秤数据
 */
class DeviceScaleFunction : ICDeviceManagerDelegate, ICScanDeviceDelegate {
    companion object {
        const val TAG = "DeviceScaleFunction"

    }

    private var initScaleSDK = false    // 初始化SDK 体脂秤
    private var onMeasureResult: ((ICWeightData) -> Unit)? = null//测量结果回调
    private var onScanResult: ((ICScanDeviceInfo) -> Unit)? = null//搜索结果回调
    private var onInitSdkResult: ((Boolean) -> Unit)? = null//初始化sdk 成功一次就可以

    /**
     * 初始化SDK
     * 但是智慧初始化一次
     * 每次使用前会判断是否有权限
     */
    private fun onCreateScale() {
        if (initScaleSDK)
            return
        val config = ICDeviceManagerConfig()
        config.context = BluetoothManager.mApplication
        ICDeviceManager.shared().delegate = this
        ICDeviceManager.shared().initMgrWithConfig(config)
    }

    /**
     * 开始扫描设备
     * ICDeviceSubTypeDefault	四电极
     * ICDeviceSubTypeEightElectrode	单频8电极
     * ICDeviceSubTypeHeight	身高尺
     * ICDeviceSubTypeEightElectrode2 双频8电极
     * ICDeviceSubTypeScaleDual	BLE+WIFI设备
     * ICDeviceSubTypeLightEffect	带灯效的跳绳
     */
    fun onStartScanDevice(
        age: Int = 0,
        scaleUserBean: ScaleUserBean? = null,
        scanResult: (ICScanDeviceInfo) -> Unit,
    ) {
        onCreateScale()
        updateUserInfo(age, scaleUserBean)
        onScanResult = scanResult
        ICDeviceManager.shared().scanDevice(this)
    }

    fun setOnMeasureResult(measureResult: ((ICWeightData) -> Unit)? = null) {
        onMeasureResult = measureResult
    }

    /**
     * 接口可以时SDK去连接设备并收取数据(请确保设备处于亮屏状态，否则SDK将会不会收到数据,
     * 中途蓝牙关闭或设备息屏，当蓝牙重新开启或设备再次亮屏，SDK会自动去连接，无需再次添加设备或扫描设备).
     */
    fun onConnectDevice(
        mac: String,
        deviceName: String,
        measureResult: ((ICWeightData) -> Unit)? = null,
        age: Int = 0,
        scaleUserBean: ScaleUserBean? = null,
    ) {
        onMeasureResult = measureResult
        onCreateScale()
        val device = ICDevice()
        device.macAddr = mac
        updateUserInfo(age, scaleUserBean)
        ICDeviceManager.shared().addDevice(device) { icDevice, status ->
            writeToFile(TAG, "onConnectDevice:$deviceName $icDevice 状态: $status")
        }
    }

    /**
     *如果不移除会一直自动连接跟扫描
     */
    fun removeICDevice(mac: String) {
        val device = ICDevice()
        device.macAddr = mac
        LiveDataBus.postValue(CONNECT_BEAN_KEY, DeviceConnectBean(mac, isConnect = false))
        BluetoothManager.getConnectBean(mac, false).run { isConnect = false }
        ICDeviceManager.shared().removeDevice(device) { icDevice, status ->
            writeToFile(TAG, "removeICDevice: $icDevice 状态: $status")
        }
    }

    /**
     * 停止搜索
     *onScanDevice
     */
    fun onStopScanDevice() {
        onScanResult = null
        ICDeviceManager.shared().stopScan()
    }

    /**
     * 更新用户数据
     */
    private fun updateUserInfo(
        age: Int,
        scaleUserBean: ScaleUserBean? = null,
    ) {
        scaleUserBean?.run {
            val userInfo = ICUserInfo()
            if (age > 0 && height.isNotEmpty() && id.isNotEmpty()) {
                userInfo.age = age
                userInfo.height = height.toFloat().toInt()
                userInfo.sex =
                    if (sex == 1) ICConstant.ICSexType.ICSexTypeMale else ICConstant.ICSexType.ICSexTypeFemal
                userInfo.kitchenUnit = ICConstant.ICKitchenScaleUnit.ICKitchenScaleUnitG
                userInfo.rulerUnit = ICConstant.ICRulerUnit.ICRulerUnitInch
                userInfo.peopleType = ICConstant.ICPeopleType.ICPeopleTypeNormal//用户类型
                // userInfo.userIndex = userIndex
                ICDeviceManager.shared().updateUserInfo(userInfo)
            }
        }
        writeToFile(TAG, "updateUserInfo 有用户数据 $age $scaleUserBean")
    }

    /**
     * 根据用户数据重新计算
     */
    fun resetScaleData(
        icWeightData: ICWeightData,
        age: Int,
        scaleUserBean: ScaleUserBean,
    ): ICWeightData {
        val userInfo = ICUserInfo()
        userInfo.kitchenUnit = ICConstant.ICKitchenScaleUnit.ICKitchenScaleUnitG
        userInfo.rulerUnit = ICConstant.ICRulerUnit.ICRulerUnitInch
        userInfo.peopleType = ICConstant.ICPeopleType.ICPeopleTypeNormal//用户类型
        userInfo.age = age
        userInfo.height = scaleUserBean.height.toFloat().toInt()
        userInfo.sex =
            if (scaleUserBean.sex == 1) ICConstant.ICSexType.ICSexTypeMale else ICConstant.ICSexType.ICSexTypeFemal
        val reCalcWeightData =
            ICDeviceManager.shared().bodyFatAlgorithmsManager.reCalcBodyFatWithWeightData(
                icWeightData,
                userInfo)
        writeToFile(TAG, "resetScaleData  $userInfo ${scaleUserBean.vbToJson()}")
        return reCalcWeightData
    }

    /**
     * 数据搜索回调
     */
    override fun onScanResult(scanDeviceInfo: ICScanDeviceInfo) {
        onScanResult?.invoke(scanDeviceInfo)
        writeToFile(TAG, "onScanResult  ${scanDeviceInfo.vbToJson()}}")
    }

    //体重秤/体脂秤回调
    override fun onReceiveWeightData(device: ICDevice, data: ICWeightData) {
        writeToFile(TAG, "测量数据：onReceiveWeightData  ${device.vbToJson()} ${data.vbToJson()}")
        onMeasureResult?.invoke(data)
    }

    override fun onInitFinish(bSuccess: Boolean) {
        initScaleSDK = bSuccess
        onInitSdkResult?.invoke(bSuccess)
        writeToFile(TAG, "onInitFinish: $bSuccess")
    }

    /**
     * 蓝牙状态 state
     */
    override fun onBleState(state: ICConstant.ICBleState) {
        writeToFile(TAG, "onBleState: $state")
    }

    /**
     * 设备连接状态 state
     */
    override fun onDeviceConnectionChanged(
        device: ICDevice,
        state: ICConstant.ICDeviceConnectState,
    ) {
        val address = device.getMacAddr()
        val status = state == ICConstant.ICDeviceConnectState.ICDeviceConnectStateConnected
        val bean = DevicePropertyBean(address, DeviceConstants.D_FAT_SCALE, isConnect = status)
        BluetoothManager.saveConnectMap(bean)
        LiveDataBus.postValue(CONNECT_BEAN_KEY,
            DeviceConnectBean(address, DeviceConstants.D_FAT_SCALE, isConnect = status))
        writeToFile(TAG, "onDeviceConnectionChanged: ${device.getMacAddr()} $state")
    }

    /**
     * 体重秤数据回调
     */
    override fun onReceiveKitchenScaleData(device: ICDevice, data: ICKitchenScaleData) {
        writeToFile(TAG, "测量数据:onReceiveKitchenScaleData: ${data.value_fl_oz_milk}")
    }

    override fun onReceiveKitchenScaleUnitChanged(
        device: ICDevice,
        unit: ICConstant.ICKitchenScaleUnit,
    ) {
    }

    override fun onReceiveCoordData(device: ICDevice, data: ICCoordData) {
    }

    override fun onReceiveRulerData(device: ICDevice, data: ICRulerData) {
        if (data.isStabilized()) {
            // demo, auto change device show body parts type
            if (data.getPartsType() == ICConstant.ICRulerBodyPartsType.ICRulerPartsTypeCalf) {
                return
            }
            ICDeviceManager.shared().settingManager.setRulerBodyPartsType(device,
                ICConstant.ICRulerBodyPartsType.valueOf(data.getPartsType().value + 1)
            ) { }
        }
    }

    override fun onReceiveRulerHistoryData(icDevice: ICDevice, icRulerData: ICRulerData) {}

    override fun onReceiveWeightCenterData(icDevice: ICDevice, data: ICWeightCenterData) {
    }

    override fun onReceiveWeightUnitChanged(icDevice: ICDevice, unit: ICConstant.ICWeightUnit) {
    }

    override fun onReceiveRulerUnitChanged(icDevice: ICDevice, unit: ICConstant.ICRulerUnit) {
    }

    override fun onReceiveRulerMeasureModeChanged(
        icDevice: ICDevice,
        mode: ICConstant.ICRulerMeasureMode,
    ) {
    }

    //ICMeasureStepMeasureWeightData	测量体重 (ICWeightData)
    //ICMeasureStepMeasureCenterData	测量平衡 (ICWeightCenterData)
    //ICMeasureStepAdcStart	开始测量阻抗
    //ICMeasureStepAdcResult	测量阻抗结束 (ICWeightData)
    //ICMeasureStepHrStart	开始测量心率
    //ICMeasureStepHrResult	测量心率结束 (ICWeightData)
    //ICMeasureStepMeasureOver	测量结束
    // eight eletrode scale callback
    override fun onReceiveMeasureStepData(
        icDevice: ICDevice,
        step: ICConstant.ICMeasureStep,
        data2: Any,
    ) {
        when (step) {
            ICConstant.ICMeasureStep.ICMeasureStepMeasureWeightData -> {
                val data = data2 as ICWeightData
                onReceiveWeightData(icDevice, data)
            }
            ICConstant.ICMeasureStep.ICMeasureStepMeasureCenterData -> {
                val data = data2 as ICWeightCenterData
                onReceiveWeightCenterData(icDevice, data)
            }
            ICConstant.ICMeasureStep.ICMeasureStepAdcStart -> {
            }
            ICConstant.ICMeasureStep.ICMeasureStepAdcResult -> {
            }
            ICConstant.ICMeasureStep.ICMeasureStepHrStart -> {
            }
            ICConstant.ICMeasureStep.ICMeasureStepHrResult -> {
                //   val hrData = data2 as ICWeightData
            }
            ICConstant.ICMeasureStep.ICMeasureStepMeasureOver -> {
                val data = data2 as ICWeightData
                onReceiveWeightData(icDevice, data)
            }
            else -> {

            }
        }
        writeToFile(TAG, "onReceiveMeasureStepData: $step")
    }

    /**
     * 历史数据仅部分设备支持
     */
    override fun onReceiveWeightHistoryData(
        icDevice: ICDevice,
        icWeightHistoryData: ICWeightHistoryData,
    ) {
        writeToFile(TAG, "onReceiveWeightHistoryData: ${icWeightHistoryData.precision_kg}")
    }

    override fun onReceiveSkipData(icDevice: ICDevice, data: ICSkipData) {
    }

    override fun onReceiveHistorySkipData(icDevice: ICDevice, icSkipData: ICSkipData) {}
    override fun onReceiveSkipBattery(icDevice: ICDevice, i: Int) {}
    override fun onReceiveBattery(icDevice: ICDevice, i: Int) {}
    override fun onReceiveUpgradePercent(
        icDevice: ICDevice,
        icUpgradeStatus: ICConstant.ICUpgradeStatus,
        i: Int,
    ) {
    }

    override fun onReceiveDeviceInfo(icDevice: ICDevice, icDeviceInfo: ICDeviceInfo) {}
    override fun onReceiveDebugData(icDevice: ICDevice, i: Int, o: Any) {}
    override fun onReceiveConfigWifiResult(
        icDevice: ICDevice,
        icConfigWifiState: ICConstant.ICConfigWifiState,
    ) {
    }

}