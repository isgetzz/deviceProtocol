package com.cc.device

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.cc.device.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btBicycle.setOnClickListener(this)
        binding.btTreadmill.setOnClickListener(this)
        binding.btTechnogym.setOnClickListener(this)
        binding.btRow.setOnClickListener(this)
        binding.btSkipping.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.btBicycle.id -> {
            }
            binding.btTreadmill.id -> {

            }
            binding.btTechnogym.id -> {

            }
            binding.btRow.id -> {

            }
            binding.btSkipping.id -> {

            }
        }
        if (v.tag != null)
            goActivity(DeviceSearchActivity::class.java, bundleOf("deviceType" to v.tag.toString()))
    }
}