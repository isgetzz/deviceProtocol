package com.cc.device

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val TAG = "MainActivity1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView: TextView = findViewById(R.id.tv)
        val tvDisconnect: TextView = findViewById(R.id.tvDisconnect)
        textView.setOnClickListener(this)
        tvDisconnect.setOnClickListener(this)
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