package com.cc.device

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.peng.ppscale.business.ble.BleOptions
import com.peng.ppscale.business.ble.PPScale
import com.peng.ppscale.business.ble.listener.*
import com.peng.ppscale.business.device.PPUnitType
import com.peng.ppscale.business.state.PPBleSwitchState
import com.peng.ppscale.business.state.PPBleWorkState
import com.peng.ppscale.data.PPBodyDetailModel
import com.peng.ppscale.util.Logger
import com.peng.ppscale.util.PPUtil
import com.peng.ppscale.vo.*
import com.peng.ppscale.vo.PPScaleDefine.PPDeviceCalcuteType

class MainActivity2 : AppCompatActivity() {
    private var ppScale: PPScale? = null
    private val unitType: PPUnitType = PPUnitType.Unit_KG

    /**
     * searchType
     * 0 is to bind the device
     * 1 is to search for an existing device
     * 2 to connect to the specified device
     */
    private var searchType = 0
    private var connectAddress: String = "" ////Specify the connected device Address
    private val TAG = "MainActivity2"
    private var isOnResume = false //页面可见时再重新发起扫描

    private val userModel by lazy {
        PPUserModel.Builder()
            .setAge(18)
            .setHeight(180)
            .setSex(PPUserGender.PPUserGenderMale)
            .setGroupNum(0)
            .build()
    }
    private val userModel1 by lazy {
        PPUserModel.Builder()
            .setAge(32)
            .setHeight(173)
            .setSex(PPUserGender.PPUserGenderFemale)
            .setGroupNum(0)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        searchType = intent.getIntExtra("SEARCH_TYPE", 0)
        connectAddress = intent.getStringExtra("CONNECT_ADDRESS") ?: "CF:E7:13:10:01:98"
        initPPScale()
    }

    private fun startScanData() {
        ppScale?.startSearchBluetoothScaleWithMacAddressList()
    }

    private fun initPPScale() {
        if (searchType == 0) {
            //绑定新设备 Bind new device
            ppScale = PPScale.Builder(this)
                .setProtocalFilterImpl(getProtocolFilter())
                .setBleOptions(getBleOptions()) //                    .setDeviceList(null)
                .setUserModel(userModel)
                .setBleStateInterface(bleStateInterface)
                .build()
        } else if (searchType == 2) {
            if (ppScale == null) {
                ppScale = PPScale.Builder(this).setProtocalFilterImpl(getProtocolFilter())
                    .setBleOptions(getBleOptions())
                    .setDeviceList(listOf(connectAddress))
                    .setUserModel(userModel)
                    .setBleStateInterface(bleStateInterface)
                    .build()
            }
            if (ppScale?.builder != null) {
                ppScale?.builder!!.setUserModel(userModel)
            }
        }
        //启动扫描
        startScanData()
    }

    /**
     * 解析数据回调
     *
     *
     * bodyFatModel 身体数据
     * deviceModel 设备信息
     */
    private fun getProtocolFilter(): ProtocalFilterImpl {
        val protocolFilter = ProtocalFilterImpl()
        if (searchType == 0) {
            protocolFilter.bindDeviceInterface =
                PPBindDeviceInterface { deviceModel ->
                    if (deviceModel != null) {
                        //如果需要modelNumber 设备型号信息，则要主动发起连接并且见监听readDeviceInfoComplete回调里面包含ModelNumber
//                        val device: DeviceModel =
//                            DBManager.manager().getDevice(deviceModel.deviceMac)
//                        if (device == null) {
//                            saveDevice(deviceModel)
//                            finish()
//                            startActivity(Intent(this@BindingDeviceActivity, DeviceListActivity::class.java))
//                        }
                        Log.d(TAG, "PPBindDeviceInterface: $deviceModel")
                    }
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
//                    val intent = Intent(this@BindingDeviceActivity, OTAActivity::class.java)
//                    intent.putExtra("otaAddress", deviceModel.deviceMac)
//                    startActivityForResult(intent, 0x0003)
                } else {
                    Logger.d("getSerialNumber  " + deviceModel.serialNumber)
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
                if (ppScale != null) {
                    ppScale!!.stopSearch()
                }
                val weightStr = PPUtil.getWeight(bodyBaseModel.unit,
                    bodyBaseModel.getPpWeightKg().toDouble(),
                    deviceModel.deviceAccuracyType.getType())
                Log.d(TAG, "onDataLock1$bodyBaseModel")
                Log.d(TAG, "onDataLock2$weightStr")
                recalculateData(bodyBaseModel, deviceModel)
//                if (weightTextView != null) {
//                    weightTextView.setText(weightStr)
//                    //                    showDialog(deviceModel, bodyFatModel);
//                }
//                if (deviceModel.deviceType == PPScaleDefine.PPDeviceType.PPDeviceTypeCC) {
//                    //Bluetooth WiFi scale
//                    showWiFiConfigDialog(bodyBaseModel, deviceModel)
//                } else {
//                    //Ordinary bluetooth scale
//                    showDialog(deviceModel, bodyBaseModel)
//                }
            } else {
                Logger.d("正在测量心率")
            }
        }
    }

    /**
     * 重新测量
     */
    private fun recalculateData(bodyModel: PPBodyBaseModel, ppDeviceModel: PPDeviceModel) {
        PPBodyDetailModel.context = this
        val deviceModel = PPDeviceModel(ppDeviceModel.deviceMac, ppDeviceModel.deviceName)
        deviceModel.deviceCalcuteType = PPDeviceCalcuteType.PPDeviceCalcuteTypeAlternate
        val bodyBaseModel = PPBodyBaseModel()
        bodyBaseModel.impedance = bodyModel.impedance
        bodyBaseModel.weight = getWeight(bodyModel.getPpWeightKg().toDouble())
        bodyBaseModel.deviceModel = deviceModel
        bodyBaseModel.userModel = userModel1

        val fatModel = PPBodyFatModel(bodyBaseModel)
        Log.d(TAG, "recalculateData $fatModel")
    }

    /**
     * 处理精度问题
     */
    private fun getWeight(double: Double): Int {
        return ((double + 0.005) * 100).toInt()
    }

    private var bleStateInterface: PPBleStateInterface = object : PPBleStateInterface {
        override fun monitorBluetoothWorkState(
            ppBleWorkState: PPBleWorkState?,
            deviceModel: PPDeviceModel?,
        ) {
            if (ppBleWorkState == PPBleWorkState.PPBleWorkStateConnected) {
                Logger.d(getString(R.string.device_connected))
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateConnecting) {
                Logger.d(getString(R.string.device_connecting))
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateDisconnected) {
                Logger.d(getString(R.string.device_disconnected))
            } else if (ppBleWorkState == PPBleWorkState.PPBleStateSearchCanceled) {
                Logger.d(getString(R.string.stop_scanning)) //主动取消扫描
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkSearchTimeOut) {
                Logger.d(getString(R.string.scan_timeout)) //可以在这里重启扫描
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateSearching) {
                Logger.d(getString(R.string.scanning))
            } else if (ppBleWorkState == PPBleWorkState.PPBleWorkStateWritable) {
                Logger.d(getString(R.string.writable))
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
                Logger.d(getString(R.string.Connectable))
                //连接，在ppBleWorkState == PPBleWorkState.PPBleWorkStateWritable时开始发送数据
                if (searchType != 0 && deviceModel?.isDeviceConnectAbled == true) {
                    ppScale!!.stopSearch()
                    ppScale!!.connectDevice(deviceModel)
                } else {
                    //绑定设备时不发起连接，非可连接设备，不发起连接
                }
            } else {
                Logger.e(getString(R.string.bluetooth_status_is_abnormal))
            }
        }

        override fun monitorBluetoothSwitchState(ppBleSwitchState: PPBleSwitchState) {
            when (ppBleSwitchState) {
                PPBleSwitchState.PPBleSwitchStateOff -> {
                    Logger.e(getString(R.string.system_bluetooth_disconnect))
                    Toast.makeText(this@MainActivity2,
                        getString(R.string.system_bluetooth_disconnect),
                        Toast.LENGTH_SHORT).show()
                }
                PPBleSwitchState.PPBleSwitchStateOn -> {
                    delayScan()
                    Logger.d(getString(R.string.system_blutooth_on))
                    Toast.makeText(this@MainActivity2,
                        getString(R.string.system_blutooth_on),
                        Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Logger.e(getString(R.string.system_bluetooth_abnormal))
                }
            }
        }
    }

    fun delayScan() {
        Handler().postDelayed({
            if (isOnResume) {
                startScanData()
            }
        }, 1000)
    }

    /**
     * 切换单位指令
     */
    private fun sendUnitDataScale(
        deviceModel: PPDeviceModel?,
        sendResultCallBack: PPBleSendResultCallBack,
    ) {
        if (ppScale != null) {
            if (deviceModel?.getDeviceCalcuteType() == PPDeviceCalcuteType.PPDeviceCalcuteTypeInScale) {
                //秤端计算，需要发送身体详情数据给秤，发送完成后不断开（切换单位）
                ppScale!!.sendData2ElectronicScale(unitType, sendResultCallBack)
            } else {
                //切换单位
                ppScale!!.sendUnitDataScale(unitType, sendResultCallBack)
            }
        }
    }

    /**
     * Connection configuration
     *
     * @return
     */
    private fun getBleOptions(): BleOptions? {
        return BleOptions.Builder()
            .setSearchTag(BleOptions.SEARCH_TAG_NORMAL) //broadcast
            //                .setSearchTag(BleOptions.SEARCH_TAG_DIRECT_CONNECT)//direct connection
            .build()
    }

    private fun disConnect() {
        if (ppScale != null) {
            ppScale!!.stopSearch()
            ppScale!!.disConnect()
        }
    }

    override fun onResume() {
        super.onResume()
        isOnResume = true
    }

    override fun onPause() {
        super.onPause()
        ppScale!!.stopSearch()
        isOnResume = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ppScale != null) {
            ppScale!!.stopSearch()
            ppScale!!.disConnect()
        }
    }
}