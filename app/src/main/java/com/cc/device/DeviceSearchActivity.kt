package com.cc.device

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cc.control.BluetoothClientManager
import com.cc.device.adapter.DeviceSearchAdapter
import com.cc.device.databinding.ActivityDeviceSearchBinding
import com.cc.device.dialog.SelectDialog
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener

/**
 * 搜索页面
 */
class DeviceSearchActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val REQUEST_ACCESS_COARSE_LOCATION = 1//蓝牙权限请求

        //权限
        private val android12 = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN")
        private val android11 = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION)

        val PERMISSION_SEARCH_DEVICE = if (Build.VERSION.SDK_INT >= 30) android12 else android11

    }

    private val binding by lazy {
        ActivityDeviceSearchBinding.inflate(layoutInflater)
    }
    private val selectDialog by lazy {
        SelectDialog(this) { protocol, ota ->

        }
    }
    private val adapter by lazy {
        DeviceSearchAdapter().apply {
            binding.recyclerView.layoutManager = LinearLayoutManager(this@DeviceSearchActivity)
            binding.recyclerView.adapter = this
            setOnItemChildClickListener { _, _, _ ->
                selectDialog.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        BluetoothClientManager.client.registerBluetoothStateListener(mBluetoothStateListener)
        binding.btFresh.setOnClickListener(this)
        startScan()

    }

    /**
     * 手机蓝牙开关状态监听
     */
    private val mBluetoothStateListener: BluetoothStateListener =
        object : BluetoothStateListener() {
            override fun onBluetoothStateChanged(openOrClosed: Boolean) {
                if (openOrClosed) startScan()
            }
        }

    private fun startScan() {
        if (requestPermission(PERMISSION_SEARCH_DEVICE, REQUEST_ACCESS_COARSE_LOCATION)) {
            if (isLocServiceEnable()) {
                if (BluetoothClientManager.isBluetoothOpened()) {
                    BluetoothClientManager.openBluetooth()
                }
                BluetoothClientManager.startSearch(::scanResult, listOf())
            } else {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, 0)
            }
        }
    }

    private fun scanResult(device: BluetoothDevice?, isSearch: Boolean) {
        if (isSearch) {
            device?.run {
                if (!adapter.data.contains(this)) {
                    adapter.addData(device)
                }
            }
        }
    }

    private val goSettingDialog by lazy {
        val normalDialog = AlertDialog.Builder(this)
        normalDialog.setIcon(R.mipmap.ic_launcher);
        normalDialog.setTitle("请在系统设置中打开定位服务")
        normalDialog.setPositiveButton("确定") { _, _ ->
            goToAppSetting()
        }
        normalDialog.setNegativeButton("暂不") { _, _ ->

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var isAllGranted = true
        for (grantResult in grantResults) {
            isAllGranted = isAllGranted and (grantResult == PackageManager.PERMISSION_GRANTED)
        }
        if (isAllGranted) {
            startScan()
        } else {
            goSettingDialog.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothClientManager.client.unregisterBluetoothStateListener(mBluetoothStateListener)
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    override fun onClick(v: View) {
        if (v.id == binding.btFresh.id) {
            adapter.data.clear()
            startScan()
        }
    }
}