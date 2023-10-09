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
import com.cc.control.LiveDataBus.CONNECT_BEAN_KEY
import com.cc.control.TimeUtil.BirthdayToAge
import com.cc.control.bean.DeviceConnectBean
import com.cc.control.bean.DevicePropertyBean
import com.cc.control.bean.ScaleUserBean
import com.cc.control.protocol.BleConfigOptions
import com.cc.control.protocol.DeviceConstants
import com.peng.ppscale.business.ble.PPScale
import com.peng.ppscale.business.ble.listener.*
import com.peng.ppscale.business.device.PPUnitType
import com.peng.ppscale.business.state.PPBleSwitchState
import com.peng.ppscale.business.state.PPBleWorkState
import com.peng.ppscale.data.PPBodyDetailModel
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
    private var onMeasureResultWL: ((ICWeightData) -> Unit)? = null//测量结果回调
    private var onMeasureResultLF: ((PPBodyBaseModel, Boolean) -> Unit)? = null//测量结果回调
    private var onScanResultLF: ((PPDeviceModel) -> Unit)? = null//搜索结果回调 乐福
    private var onScanResultWL: ((ICScanDeviceInfo) -> Unit)? = null//搜索结果回调 沃莱
    private val unitType: PPUnitType = PPUnitType.Unit_KG
    private var searchType = 2//0.绑定 2.连接

    //乐福体脂秤
    private val ppScale by lazy {
        PPScale.Builder(BluetoothManager.mApplication).setProtocalFilterImpl(getProtocolFilter())
            .setBleOptions(BleConfigOptions.lfOptions).setBleStateInterface(bleStateInterface)
            .setUserModel(PPUserModel.Builder().setAge(18).setHeight(180)
                .setSex(PPUserGender.PPUserGenderMale).setGroupNum(0).build()).build()
    }

    /**
     * 只会初始化一次SDK 每次使用前会判断是否有权限
     */
    private fun createScale() {
        if (initScaleSDK) return
        val config = ICDeviceManagerConfig()
        config.context = BluetoothManager.mApplication
        ICDeviceManager.shared().delegate = this
        ICDeviceManager.shared().initMgrWithConfig(config)
    }

    /**
     * 开始扫描设备
     * ICDeviceSubTypeDefault 四电极 ICDeviceSubTypeEightElectrode	单频8电极 ICDeviceSubTypeHeight	身高尺
     * ICDeviceSubTypeEightElectrode2 双频8电极 ICDeviceSubTypeScaleDual	BLE+WIFI设备 ICDeviceSubTypeLightEffect	带灯效的跳绳
     */
    fun startScanDevice(
        sdkType: Int,
        scaleUserBean: ScaleUserBean? = null,
        scanResultLF: ((PPDeviceModel) -> Unit)? = null,
        scanResultWL: ((ICScanDeviceInfo) -> Unit)? = null,
    ) {
        when (sdkType) {
            0 -> {
                createScale()
                onScanResultWL = scanResultWL
                onScanResultLF = scanResultLF
                updateUserInfo(scaleUserBean)
                ppScale.builder.setDeviceList(null)
                ppScale.monitorSurroundDevice(30000)
                ICDeviceManager.shared().scanDevice(this)
            }
            DeviceConstants.D_SERVICE_SCALE_WL -> {
                createScale()
                updateUserInfo(scaleUserBean)
                onScanResultWL = scanResultWL
                ICDeviceManager.shared().scanDevice(this)
            }
            DeviceConstants.D_SERVICE_SCALE_LF -> {
                onScanResultLF = scanResultLF
                updateUserInfo(scaleUserBean)
                ppScale.builder.setDeviceList(null)
                ppScale.monitorSurroundDevice(30000)
            }

        }

    }


    /**
     * 接口可以时SDK去连接设备并收取数据(请确保设备处于亮屏状态，否则SDK将会不会收到数据,
     * 中途蓝牙关闭或设备息屏，当蓝牙重新开启或设备再次亮屏，SDK会自动去连接，无需再次添加设备或扫描设备).
     */
    fun connectDevice(
        mac: String,
        deviceName: String,
        protocol: Int,
        scaleUserBean: ScaleUserBean? = null,
    ) {
        updateUserInfo(scaleUserBean)
        if (protocol == 0 || protocol == DeviceConstants.D_SERVICE_SCALE_WL) {
            createScale()
            val device = ICDevice()
            device.macAddr = mac
            ICDeviceManager.shared().addDevice(device) { icDevice, status ->
                writeToFile(TAG, "onConnectDevice:$deviceName $icDevice 状态: $status")
            }
        }
        if (protocol == 0 || protocol == DeviceConstants.D_SERVICE_SCALE_LF) {
            ppScale.builder.setDeviceList(listOf(mac))
            //启动扫描
            ppScale.startSearchBluetoothScaleWithMacAddressList()
        }
    }

    /**
     *测量回调
     */
    fun setMeasureResult(
        measureResult: ((ICWeightData) -> Unit)? = null,
        measureResultLF: ((PPBodyBaseModel, Boolean) -> Unit)? = null,
    ) {
        onMeasureResultWL = measureResult
        onMeasureResultLF = measureResultLF
    }

    /**
     * 设备回调 LF
     */
    private fun getProtocolFilter(): ProtocalFilterImpl {
        return ProtocalFilterImpl().apply {
            searchDeviceInfoInterface = PPSearchDeviceInfoInterface { ppDeviceModel ->
                onScanResultLF?.invoke(ppDeviceModel)
            }
            deviceInfoInterface = object : PPDeviceInfoInterface() {
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
            setPPProcessDateInterface(object : PPProcessDateInterface() {
                override fun monitorProcessData(
                    bodyBaseModel: PPBodyBaseModel,
                    deviceModel: PPDeviceModel,
                ) {
                    // 过程数据
                    val weightStr = PPUtil.getWeight(bodyBaseModel.unit,
                        bodyBaseModel.getPpWeightKg().toDouble(),
                        deviceModel.deviceAccuracyType.getType())
                    onMeasureResultLF?.invoke(bodyBaseModel, false)
                    Log.d(TAG, "monitorLockData2 $onMeasureResultLF $bodyBaseModel $weightStr")
                }
            })
            setPPLockDataInterface(object : PPLockDataInterface() {
                //锁定数据
                override fun monitorLockData(
                    bodyFatModel: PPBodyBaseModel,
                    deviceModel: PPDeviceModel,
                ) {
                    onDataLock(bodyFatModel, deviceModel)
                }

                override fun monitorLockDataByCalculateInScale(fatModel: PPBodyFatModel) {
                    super.monitorLockDataByCalculateInScale(fatModel)
                    Log.d(TAG, "monitorLockDataByCalculateInScale: $fatModel")
                }

                override fun monitorOverWeight() {
                    Log.d(TAG, "monitorOverWeight: over weight ")
                }
            })
        }
    }

    /**
     * 乐福数据稳定 LF
     */
    private fun onDataLock(bodyBaseModel: PPBodyBaseModel?, deviceModel: PPDeviceModel) {
        if (bodyBaseModel != null && !bodyBaseModel.isHeartRating) {
            ppScale?.stopSearch()
            val weightStr = PPUtil.getWeight(bodyBaseModel.unit,
                bodyBaseModel.getPpWeightKg().toDouble(),
                deviceModel.deviceAccuracyType.getType())
            Log.d(TAG, "onDataLock1$bodyBaseModel")
            Log.d(TAG, "onDataLock2 $onMeasureResultLF $weightStr")
            onMeasureResultLF?.invoke(bodyBaseModel, true)
        }
    }

    /**
     * 处理精度问题 LF
     */
    private fun getWeight(double: Double): Int {
        return ((double + 0.005) * 100).toInt()
    }

    /**
     * LF
     */
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
        disConnect()
    }

    /**
     * 停止搜索f
     */
    fun stopScanDevice() {
        onScanResultLF = null
        onScanResultWL = null
        ppScale?.stopSearch()
        ICDeviceManager.shared().stopScan()
    }

    /**
     * 更新用户数据
     */
    private fun updateUserInfo(scaleUserBean: ScaleUserBean? = null) {
        scaleUserBean?.run {
            val age = BirthdayToAge(scaleUserBean.birthday)
            val userInfo = ICUserInfo()
            if (age > 0 && height.isNotEmpty() && id.isNotEmpty()) {
                userInfo.age = age
                userInfo.height = height.toFloat().toInt()
                userInfo.sex =
                    if (sex == 1) ICConstant.ICSexType.ICSexTypeMale else ICConstant.ICSexType.ICSexTypeFemal
                userInfo.kitchenUnit = ICConstant.ICKitchenScaleUnit.ICKitchenScaleUnitG
                userInfo.rulerUnit = ICConstant.ICRulerUnit.ICRulerUnitInch
                userInfo.peopleType = ICConstant.ICPeopleType.ICPeopleTypeNormal//用户类型
                ICDeviceManager.shared().updateUserInfo(userInfo)
            }
            val userModel = PPUserModel.Builder().setAge(age).build()
            userModel.userHeight = height.toDouble().toInt()
            userModel.sex =
                if (scaleUserBean.sex == 1) PPUserGender.PPUserGenderMale else PPUserGender.PPUserGenderFemale
            ppScale.builder.setUserModel(userModel)

        }
        writeToFile(TAG, "updateUserInfo 有用户数据 $scaleUserBean")
    }

    /**
     * 根据用户数据重新计算 WL
     */
    fun resetScaleData(icWeightData: ICWeightData, scaleUserBean: ScaleUserBean): ICWeightData {
        val userInfo = ICUserInfo()
        userInfo.kitchenUnit = ICConstant.ICKitchenScaleUnit.ICKitchenScaleUnitG
        userInfo.rulerUnit = ICConstant.ICRulerUnit.ICRulerUnitInch
        userInfo.peopleType = ICConstant.ICPeopleType.ICPeopleTypeNormal//用户类型
        userInfo.age = BirthdayToAge(scaleUserBean.birthday)
        userInfo.height = scaleUserBean.height.toFloat().toInt()
        userInfo.sex =
            if (scaleUserBean.sex == 1) ICConstant.ICSexType.ICSexTypeMale else ICConstant.ICSexType.ICSexTypeFemal
        val reCalcWeightData =
            ICDeviceManager.shared().bodyFatAlgorithmsManager.reCalcBodyFatWithWeightData(
                icWeightData,
                userInfo)
        writeToFile(TAG, "resetScaleData  $userInfo $scaleUserBean")
        return reCalcWeightData
    }

    /**
     * 重新计算 LF
     */
    fun resetScaleData(bodyModel: PPBodyBaseModel, scaleUserBean: ScaleUserBean): PPBodyFatModel {
        PPBodyDetailModel.context = BluetoothManager.mApplication
        val deviceModel =
            PPDeviceModel(bodyModel.deviceModel?.deviceMac, bodyModel.deviceModel?.deviceName)
        deviceModel.deviceCalcuteType =
            PPScaleDefine.PPDeviceCalcuteType.PPDeviceCalcuteTypeAlternate
        val bodyBaseModel = PPBodyBaseModel()
        bodyBaseModel.impedance = bodyModel.impedance
        bodyBaseModel.weight = getWeight(bodyModel.getPpWeightKg().toDouble())
        bodyBaseModel.deviceModel = deviceModel
        val userModel = PPUserModel.Builder().setAge(BirthdayToAge(scaleUserBean.birthday)).build()
        userModel.userHeight = scaleUserBean.height.toDouble().toInt()
        userModel.sex =
            if (scaleUserBean.sex == 1) PPUserGender.PPUserGenderMale else PPUserGender.PPUserGenderFemale
        bodyBaseModel.userModel = userModel
        val bodyFatModel = PPBodyFatModel(bodyBaseModel)
        writeToFile(TAG,
            "resetScaleData 完整：$bodyFatModel \n身体数据：$bodyModel \nlf用户信息：$userModel \n体脂秤用户信息：$scaleUserBean")
        return bodyFatModel
    }

    /**
     * 数据搜索回调 WL
     */
    override fun onScanResult(scanDeviceInfo: ICScanDeviceInfo) {
        onScanResultWL?.invoke(scanDeviceInfo)
        writeToFile(TAG, "onScanResult  ${scanDeviceInfo.vbToJson()}}")
    }

    //体重秤/体脂秤回调 WL
    override fun onReceiveWeightData(device: ICDevice, data: ICWeightData) {
        writeToFile(TAG, "测量数据：onReceiveWeightData  ${device.vbToJson()} ${data.vbToJson()}")
        onMeasureResultWL?.invoke(data)
    }

    //LW
    override fun onInitFinish(bSuccess: Boolean) {
        initScaleSDK = bSuccess
        writeToFile(TAG, "onInitFinish: $bSuccess")
    }

    /**
     * 蓝牙状态 state WL
     */
    override fun onBleState(state: ICConstant.ICBleState) {
        writeToFile(TAG, "onBleState: $state")
    }

    /**
     * WL 设备连接状态 state
     */
    override fun onDeviceConnectionChanged(
        device: ICDevice,
        state: ICConstant.ICDeviceConnectState,
    ) {
        val address = device.getMacAddr()
        val status = state == ICConstant.ICDeviceConnectState.ICDeviceConnectStateConnected
        val bean = DevicePropertyBean(address,
            DeviceConstants.D_FAT_SCALE,
            isConnect = status,
            protocol = DeviceConstants.D_SERVICE_SCALE_WL)
        BluetoothManager.saveConnectMap(bean)
        LiveDataBus.postValue(CONNECT_BEAN_KEY,
            DeviceConnectBean(address, DeviceConstants.D_FAT_SCALE, isConnect = status))
        writeToFile(TAG, "onDeviceConnectionChanged: ${device.getMacAddr()} $state")
    }

    /**
     * 体重秤数据回调 WL
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
                ICConstant.ICRulerBodyPartsType.valueOf(data.getPartsType().value + 1)) { }
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

    //WL
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
     * LF 状态回调接口
     */
    private var bleStateInterface: PPBleStateInterface = object : PPBleStateInterface {
        override fun monitorBluetoothWorkState(
            ppBleWorkState: PPBleWorkState?,
            deviceModel: PPDeviceModel?,
        ) {
            deviceModel?.run {
                val address = deviceModel.deviceMac
                val status = ppBleWorkState == PPBleWorkState.PPBleWorkStateConnected
                val bean = DevicePropertyBean(address,
                    DeviceConstants.D_FAT_SCALE,
                    deviceModel.deviceName,
                    isConnect = status,
                    protocol = DeviceConstants.D_SERVICE_SCALE_LF)
                if (ppBleWorkState == PPBleWorkState.PPBleWorkStateConnected) {
                    BluetoothManager.saveConnectMap(bean)
                    LiveDataBus.postValue(CONNECT_BEAN_KEY,
                        DeviceConnectBean(address, DeviceConstants.D_FAT_SCALE, isConnect = true))
                } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateConnecting) {
                    Log.d(TAG, "monitorBluetoothWorkState: PPBleWorkStateConnecting")
                } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateDisconnected) {
                    BluetoothManager.saveConnectMap(bean)
                    LiveDataBus.postValue(CONNECT_BEAN_KEY,
                        DeviceConnectBean(address, DeviceConstants.D_FAT_SCALE, isConnect = false))
                    ppScale.connectDevice(deviceModel)
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
                    if (searchType != 0 && deviceModel.isDeviceConnectAbled) {
                        ppScale?.stopSearch()
                        ppScale?.connectDevice(deviceModel)
                    } else {
                        //绑定设备时不发起连接，非可连接设备，不发起连接
                    }
                } else {
                    Log.d(TAG, "monitorBluetoothWorkState: ppBleWorkState")
                }
                writeToFile(TAG,
                    "onDeviceConnectionChanged: ${deviceModel.deviceMac} ${deviceModel.deviceName} $ppBleWorkState")
            }
        }

        override fun monitorBluetoothSwitchState(ppBleSwitchState: PPBleSwitchState) {
            when (ppBleSwitchState) {
                PPBleSwitchState.PPBleSwitchStateOff -> {
                    Log.d(TAG, "monitorBluetoothSwitchState: 关闭")
                }
                PPBleSwitchState.PPBleSwitchStateOn -> {
                    Log.d(TAG, "monitorBluetoothSwitchState: 开启")
                }
                else -> {
                    Log.d(TAG, "monitorBluetoothSwitchState: $ppBleSwitchState")
                }
            }
        }
    }

    /**
     * 切换单位指令 LF
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
     * 历史数据仅部分设备支持 WL
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