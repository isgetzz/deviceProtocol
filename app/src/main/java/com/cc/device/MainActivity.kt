package com.cc.device

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cc.control.BluetoothManager
import com.peng.ppscale.business.ble.PPScale
import com.peng.ppscale.business.ble.listener.PPBleStateInterface
import com.peng.ppscale.business.ble.listener.PPSearchDeviceInfoInterface
import com.peng.ppscale.business.ble.listener.ProtocalFilterImpl
import com.peng.ppscale.business.state.PPBleSwitchState
import com.peng.ppscale.business.state.PPBleWorkState
import com.peng.ppscale.vo.PPDeviceModel
import com.peng.ppscale.vo.PPUserGender
import com.peng.ppscale.vo.PPUserModel


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = "MainActivity1"
    private var adapter: DeviceListAdapter? = null
    private var deviceModels = ArrayList<DeviceModel>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView: TextView = findViewById(R.id.tv)
        textView.setOnClickListener(this)
//        BluetoothManager.initDeviceManager(this.application, true)
//        BluetoothManager.startSearch({ name, mac ->
////            Log.d(TAG, "getProtocolFilter:$name $mac")
//        })
        adapter = DeviceListAdapter()
        val listView = findViewById<View>(R.id.list_View) as RecyclerView
        listView.layoutManager=LinearLayoutManager(this)
        listView.adapter = adapter
        adapter?.setOnItemClickListener { adapter, view, position ->
            val deviceModel = adapter.getItem(position) as DeviceModel?
            deviceModel?.let { startScanData(it) }
        }
        adapter?.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.tvSetting) {
                val deviceModel = adapter.getItem(position) as DeviceModel
                startScanData(deviceModel)
            }
        }

    }

    private fun startScanData(deviceModel: DeviceModel) {
        val intent = Intent(this, MainActivity2::class.java)
        intent.putExtra("SEARCH_TYPE", 2)
//        intent.putExtra("CONNECT_ADDRESS", deviceModel.deviceMac)
        Log.d(TAG, "startScanData: ${deviceModel.deviceMac} ${deviceModel.deviceName}")
        startActivity(intent)
    }

    private var bleStateInterface = object : PPBleStateInterface {
        override fun monitorBluetoothWorkState(
            ppBleWorkState: PPBleWorkState?,
            deviceModel: PPDeviceModel?,
        ) {
            when (ppBleWorkState) {
                PPBleWorkState.PPBleWorkStateConnected -> {
                    Log.d(TAG, "getProtocolFilter:已连接")
                }
                PPBleWorkState.PPBleWorkStateConnecting -> {
                    Log.d(TAG, "getProtocolFilter:连接中")
                }
                PPBleWorkState.PPBleWorkStateDisconnected -> {
                    Log.d(TAG, "getProtocolFilter:断开连接")
                }
                PPBleWorkState.PPBleStateSearchCanceled -> {
                    Log.d(TAG, "getProtocolFilter:取消搜索")
                }
                PPBleWorkState.PPBleWorkSearchTimeOut -> {
                    Log.d(TAG, "getProtocolFilter:搜索超时")
                }
                PPBleWorkState.PPBleWorkStateSearching -> {
                    Log.d(TAG, "getProtocolFilter:搜索中")
                }
                PPBleWorkState.PPBleWorkStateWritable -> {
                    Log.d(TAG, "getProtocolFilter:可写入")
                }
                else -> {
                    Log.d(TAG, "getProtocolFilter:其他 $ppBleWorkState")
                }
            }
        }

        override fun monitorBluetoothSwitchState(ppBleSwitchState: PPBleSwitchState?) {
            when (ppBleSwitchState) {
                PPBleSwitchState.PPBleSwitchStateOff -> {
                    Log.d(TAG, "getProtocolFilter:蓝牙关闭")
                }
                PPBleSwitchState.PPBleSwitchStateOn -> {
                    Log.d(TAG, "getProtocolFilter:蓝牙打开")
                }
                else -> {
                    Log.d(TAG, "getProtocolFilter:蓝牙其他 $ppBleSwitchState")
                }
            }
        }
    }

    private val ppScale by lazy {
        PPScale.Builder(this)
            .setProtocalFilterImpl(getProtocolFilter()) //                    .setDeviceList(null)
            .setUserModel(PPUserModel.Builder()
                .setAge(18)
                .setHeight(180)
                .setSex(PPUserGender.PPUserGenderMale)
                .setGroupNum(0)
                .build())
            .setBleStateInterface(bleStateInterface)
            .build()
    }

    /**
     * Get around bluetooth scale devices
     */
    private fun startScanDeviceList() {
        ppScale.monitorSurroundDevice(10000) //You can dynamically set the scan time in ms
    }

    private fun getProtocolFilter(): ProtocalFilterImpl {
        deviceModels.clear()
        adapter?.data?.clear()
        return ProtocalFilterImpl().apply {
            searchDeviceInfoInterface =
                PPSearchDeviceInfoInterface { ppDeviceModel ->
                    if (ppDeviceModel != null) {
                        var deviceModel: DeviceModel? = null
                        for (i in deviceModels.indices) {
                            val model = deviceModels[i]
                            if (model.deviceMac.equals(ppDeviceModel.deviceMac)) {
                                model.rssi = ppDeviceModel.rssi
                                deviceModel = model
                                deviceModels[i] = deviceModel
                            }
                        }
                        if (deviceModel == null) {
                            deviceModel = DeviceModel(ppDeviceModel.deviceMac,
                                ppDeviceModel.deviceName,
                                ppDeviceModel.deviceType.getType())
                            deviceModel.rssi = deviceModel.rssi
                            deviceModels.add(deviceModel)
                            adapter?.addData(deviceModel)
                        }
                    }
                    Log.d(TAG, "getProtocolFilter: $ppDeviceModel $adapter")
                }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (ppScale != null) {
            ppScale.stopSearch()
            ppScale.disConnect()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv -> {
                startScanDeviceList()
            }
        }
    }

}