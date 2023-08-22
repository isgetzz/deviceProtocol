package com.cc.control

import android.util.Log
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
import com.cc.control.BluetoothManager.getBleOptions
import com.cc.control.LiveDataBus.CONNECT_BEAN_KEY
import com.cc.control.bean.DeviceConnectBean
import com.cc.control.bean.DevicePropertyBean
import com.cc.control.bean.ScaleUserBean
import com.cc.control.protocol.DeviceConstants
import com.peng.ppscale.business.ble.PPScale
import com.peng.ppscale.business.ble.listener.*
import com.peng.ppscale.business.device.PPUnitType
import com.peng.ppscale.business.state.PPBleSwitchState
import com.peng.ppscale.business.state.PPBleWorkState
import com.peng.ppscale.data.PPBodyDetailModel
import com.peng.ppscale.util.Logger
import com.peng.ppscale.util.PPUtil
import com.peng.ppscale.vo.*

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
    private val unitType: PPUnitType = PPUnitType.Unit_KG
    private var searchType = 2//0.绑定 2.连接


    //乐福体脂秤
    private val ppScale by lazy {
        PPScale.Builder(BluetoothManager.mApplication)
            .setProtocalFilterImpl(getProtocolFilter())
            .setBleOptions(getBleOptions())
            .setDeviceList(listOf(""))
            .setBleStateInterface(bleStateInterface)
            .build()
    }

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
        sdkType: String,
        age: Int = 0,
        scaleUserBean: ScaleUserBean? = null,
        scanResult: (ICScanDeviceInfo) -> Unit,
    ) {
        when (sdkType) {
            DeviceConstants.D_SCALE_WL -> {
                onCreateScale()
                updateUserInfo(age, scaleUserBean)
                onScanResult = scanResult
                ICDeviceManager.shared().scanDevice(this)
            }
            DeviceConstants.D_SCALE_LF -> {

            }
        }

    }

    fun setOnMeasureResult(measureResult: ((ICWeightData) -> Unit)? = null) {
        onMeasureResult = measureResult
    }

    /**
     * 接口可以时SDK去连接设备并收取数据(请确保设备处于亮屏状态，否则SDK将会不会收到数据,
     * 中途蓝牙关闭或设备息屏，当蓝牙重新开启或设备再次亮屏，SDK会自动去连接，无需再次添加设备或扫描设备).
     */
    fun onConnectDevice(
        sdkType: String,
        mac: String,
        deviceName: String,
        measureResult: ((ICWeightData) -> Unit)? = null,
        age: Int = 0,
        scaleUserBean: ScaleUserBean? = null,
    ) {
        onMeasureResult = measureResult
        when (sdkType) {
            DeviceConstants.D_SCALE_WL -> {
                onCreateScale()
                val device = ICDevice()
                device.macAddr = mac
                updateUserInfo(age, scaleUserBean)
                ICDeviceManager.shared().addDevice(device) { icDevice, status ->
                    writeToFile(TAG, "onConnectDevice:$deviceName $icDevice 状态: $status")
                }
            }
            DeviceConstants.D_SCALE_LF -> {
                val userModel = PPUserModel.Builder().setAge(age).build()
                scaleUserBean?.run {
                    userModel.userHeight = height.toDouble().toInt()
                    userModel.sex =
                        if (scaleUserBean.sex == 1) PPUserGender.PPUserGenderMale else PPUserGender.PPUserGenderFemale
                }
                ppScale.builder.setDeviceList(listOf(mac))
                ppScale.builder.setUserModel(userModel)
            }
        }
        //启动扫描
        ppScale?.startSearchBluetoothScaleWithMacAddressList()
    }

    private fun getProtocolFilter(): ProtocalFilterImpl {
        val protocolFilter = ProtocalFilterImpl()
        if (searchType == 0) {
            protocolFilter.bindDeviceInterface = PPBindDeviceInterface { deviceModel ->
                Log.d(TAG, "PPBindDeviceInterface: $deviceModel")
            }
        } else {
            protocolFilter.setPPProcessDateInterface(object : PPProcessDateInterface() {
                // 过程数据
                override fun monitorProcessData(
                    bodyBaseModel: PPBodyBaseModel,
                    deviceModel: PPDeviceModel,
                ) {
                    val weightStr = PPUtil.getWeight(bodyBaseModel.unit,
                        bodyBaseModel.getPpWeightKg().toDouble(),
                        deviceModel.deviceAccuracyType.getType())
                    Log.d("MainActivity2", "monitorLockData2 $bodyBaseModel $weightStr")
                }
            })
            protocolFilter.setPPLockDataInterface(object : PPLockDataInterface() {
                //锁定数据
                override fun monitorLockData(
                    bodyFatModel: PPBodyBaseModel,
                    deviceModel: PPDeviceModel,
                ) {
                    onDataLock(bodyFatModel, deviceModel)
                }

                override fun monitorOverWeight() {
                    Log.d(TAG, "monitorOverWeight: over weight ")
                }
            })
        }
        protocolFilter.deviceInfoInterface = object : PPDeviceInfoInterface() {
            override fun serialNumber(deviceModel: PPDeviceModel) {
                if (deviceModel.devicePowerType == PPScaleDefine.PPDevicePowerType.PPDevicePowerTypeSolar && deviceModel.deviceFuncType and PPScaleDefine.PPDeviceFuncType.PPDeviceFuncTypeHeartRate.getType() == PPScaleDefine.PPDeviceFuncType.PPDeviceFuncTypeHeartRate.getType() && deviceModel.serialNumber == "20220212") {
                    disConnect()
                } else {
                    Log.d(TAG, "PPDeviceInfoInterface :  $deviceModel")
                }
            }

            override fun onIlluminationChange(illumination: Int) {}
            override fun readDeviceInfoComplete(deviceModel: PPDeviceModel) {
                //如果需要modelNumber 设备型号信息，则要主动发起连接并且见监听readDeviceInfoComplete时回调
                Log.d(TAG, "DeviceInfo :  $deviceModel")
            }
        }
        return protocolFilter
    }

    private fun onDataLock(bodyBaseModel: PPBodyBaseModel?, deviceModel: PPDeviceModel) {
        if (bodyBaseModel != null) {
            if (!bodyBaseModel.isHeartRating) {
                Log.d(TAG, "onDataLock  bodyFatModel weightKg = $bodyBaseModel")
                ppScale?.stopSearch()
                val weightStr = PPUtil.getWeight(bodyBaseModel.unit,
                    bodyBaseModel.getPpWeightKg().toDouble(),
                    deviceModel.deviceAccuracyType.getType())
                Log.d(TAG, "onDataLock1$bodyBaseModel")
                Log.d(TAG, "onDataLock2$weightStr")
            } else {
                Logger.d("正在测量心率")
            }
        }
    }

    /**
     * 重新测量
     */
    private fun recalculateData(
        bodyModel: PPBodyBaseModel,
        ppDeviceModel: PPDeviceModel,
        ppUserModel: PPUserModel,
    ) {
        PPBodyDetailModel.context = BluetoothManager.mApplication
        val deviceModel = PPDeviceModel(ppDeviceModel.deviceMac, ppDeviceModel.deviceName)
        deviceModel.deviceCalcuteType =
            PPScaleDefine.PPDeviceCalcuteType.PPDeviceCalcuteTypeAlternate
        val bodyBaseModel = PPBodyBaseModel()
        bodyBaseModel.impedance = bodyModel.impedance
        bodyBaseModel.weight = getWeight(bodyModel.getPpWeightKg().toDouble())
        bodyBaseModel.deviceModel = deviceModel
        bodyBaseModel.userModel = ppUserModel

        val fatModel = PPBodyFatModel(bodyBaseModel)
        Log.d(TAG, "recalculateData $fatModel")
    }

    /**
     * 处理精度问题
     */
    private fun getWeight(double: Double): Int {
        return ((double + 0.005) * 100).toInt()
    }

    private fun disConnect() {
        ppScale?.stopSearch()
        ppScale?.disConnect()
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
        ppScale?.stopSearch()
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
        BluetoothManager.savaConnectMap(bean)
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
     * 状态回调接口
     */
    private var bleStateInterface: PPBleStateInterface = object : PPBleStateInterface {
        override fun monitorBluetoothWorkState(
            ppBleWorkState: PPBleWorkState?,
            deviceModel: PPDeviceModel?,
        ) {
            if (ppBleWorkState == PPBleWorkState.PPBleWorkStateConnected) {
                Log.d(TAG, "monitorBluetoothWorkState: PPBleWorkStateConnected")
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateConnecting) {
                Log.d(TAG, "monitorBluetoothWorkState: PPBleWorkStateConnecting")
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateDisconnected) {
                Log.d(TAG, "monitorBluetoothWorkState: PPBleWorkStateDisconnected")
            } else if (ppBleWorkState == PPBleWorkState.PPBleStateSearchCanceled) {
                Log.d(TAG, "monitorBluetoothWorkState: PPBleStateSearchCanceled")
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkSearchTimeOut) {
                Log.d(TAG, "monitorBluetoothWorkState: PPBleWorkSearchTimeOut")
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateSearching) {
                Log.d(TAG, "monitorBluetoothWorkState: PPBleWorkStateSearching")
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateWritable) {
                Log.d(TAG, "monitorBluetoothWorkState: PPBleWorkStateWritable")
                //可写状态，可以发送指令，例如切换单位，获取历史数据等
                sendUnitDataScale(deviceModel) { sendState ->
                    when (sendState) {
                        PPScaleSendState.PP_SEND_FAIL -> {
                            //Failed to send
                        }
                        PPScaleSendState.PP_SEND_SUCCESS -> {
                            //sentSuccessfully
                        }
                        PPScaleSendState.PP_DEVICE_ERROR -> {
                            //Device error, indicating that the command is not supported
                        }
                        PPScaleSendState.PP_DEVICE_NO_CONNECT -> {
                            //deviceNotConnected
                        }
                    }
                }
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateConnectable) {
                Log.d(TAG, "monitorBluetoothWorkState: PPBleWorkStateConnectable")
                //连接，在ppBleWorkState == PPBleWorkState.PPBleWorkStateWritable时开始发送数据
                if (searchType != 0 && deviceModel?.isDeviceConnectAbled == true) {
                    ppScale?.stopSearch()
                    ppScale?.connectDevice(deviceModel)
                } else {
                    //绑定设备时不发起连接，非可连接设备，不发起连接
                }
            } else {
                Log.d(TAG, "monitorBluetoothWorkState: ppBleWorkState")
            }
        }

        override fun monitorBluetoothSwitchState(ppBleSwitchState: PPBleSwitchState) {
            when (ppBleSwitchState) {
                PPBleSwitchState.PPBleSwitchStateOff -> {
                    Log.d(TAG, "monitorBluetoothSwitchState: 关闭")
                }
                PPBleSwitchState.PPBleSwitchStateOn -> {
//                    delayScan()
                    Log.d(TAG, "monitorBluetoothSwitchState: 开启")
                }
                else -> {
                    Log.d(TAG, "monitorBluetoothSwitchState: $ppBleSwitchState")
                }
            }
        }
    }

    /**
     * 切换单位指令
     */
    private fun sendUnitDataScale(
        deviceModel: PPDeviceModel?,
        sendResultCallBack: PPBleSendResultCallBack,
    ) {
        if (deviceModel?.getDeviceCalcuteType() == PPScaleDefine.PPDeviceCalcuteType.PPDeviceCalcuteTypeInScale) {
            //秤端计算，需要发送身体详情数据给秤，发送完成后不断开（切换单位）
            ppScale?.sendData2ElectronicScale(unitType, sendResultCallBack)
        } else {
            //切换单位
            ppScale?.sendUnitDataScale(unitType, sendResultCallBack)
        }
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