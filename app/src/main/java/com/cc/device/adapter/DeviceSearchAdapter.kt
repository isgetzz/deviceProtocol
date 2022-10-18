package com.cc.device.adapter

import android.bluetooth.BluetoothDevice
import com.cc.device.R
import com.cc.device.databinding.ItemDeviceSearchBinding
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseDataBindingHolder

/**
 * @Author      : cc
 * @Date        : on 2022-10-17 15:54.
 * @Description :设备适配器
 */
class DeviceSearchAdapter :
    BaseQuickAdapter<BluetoothDevice, BaseDataBindingHolder<ItemDeviceSearchBinding>>(
        R.layout.item_device_search) {
    init {
        addChildClickViewIds(R.id.btConnect)
    }

    override fun convert(
        holder: BaseDataBindingHolder<ItemDeviceSearchBinding>,
        item: BluetoothDevice,
    ) {
        holder.dataBinding?.run {
            bean = item
            executePendingBindings()
        }
    }
}