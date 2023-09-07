package com.cc.device

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cc.control.BluetoothManager
import com.cc.control.DeviceScaleFunction
import com.cc.control.protocol.DeviceConstants


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = "MainActivity1"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView: TextView = findViewById(R.id.tv)
        val tvDisconnect: TextView = findViewById(R.id.tvDisconnect)
        textView.setOnClickListener(this)
        tvDisconnect.setOnClickListener(this)
        BluetoothManager.initDeviceManager(this.application, true)
    }
    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv -> {
            }
            R.id.tvDisconnect -> {
            }
        }
    }
}