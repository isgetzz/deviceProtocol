package com.cc.control.protocol

import com.inuker.bluetooth.library.connect.options.BleConnectOptions
import com.inuker.bluetooth.library.search.SearchRequest
import com.peng.ppscale.business.ble.BleOptions

/**
 * Author      : cc
 * Date        : on 2023-01-13 10:26.
 * Description : 蓝牙连、接搜素配置
 */
object BleConfigOptions {
    /**
     * mrk支持设备
     */
    val nameList =
        listOf("HW401", "HEART-B2", "FS", "TF", "ICONSOLE", "I-CONSOLE", "HI-", "MERACH", "MRK")

    /**
     * 蓝牙连接配置
     */
    val connectOptions: BleConnectOptions by lazy {
        BleConnectOptions.Builder()
            .setConnectRetry(1)// 重试次数
            .setConnectTimeout(5000) // 连接超时5s
            .setServiceDiscoverRetry(1)// 发现服务重试次数
            .setServiceDiscoverTimeout(5000)// 发现服务超时5s
            .build()

    }

    /**
     * 蓝牙搜索配置
     */
    val bleSearchRequest: SearchRequest by lazy {
        SearchRequest.Builder()
            .searchBluetoothLeDevice(2000, 3) // 先扫BLE设备3次，每次3s
            .searchBluetoothClassicDevice(1000) // 再扫经典蓝牙5s
            .searchBluetoothLeDevice(3000) // 再扫BLE设备2s
            .build()
    }
    /**
     * lf 体脂秤配置
     */
    val lfOptions: BleOptions by lazy  {
        BleOptions.Builder()
            .setSearchTag(BleOptions.SEARCH_TAG_NORMAL) //broadcast
            // .setSearchTag(BleOptions.SEARCH_TAG_DIRECT_CONNECT)//direct connection
            .build()
    }
}

