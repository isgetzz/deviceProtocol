package com.cc.device

import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.peng.ppscale.vo.PPScaleDefine
import com.peng.ppscale.vo.PPScaleDefine.PPDeviceProtocolType
import java.lang.String
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2023-05-05 10:42.
 * @Description :描述
 */
class DeviceListAdapter :
    BaseQuickAdapter<DeviceModel, BaseViewHolder>(R.layout.activity_scan_list_item) {
    init {
        addChildClickViewIds(R.id.tvSetting)
    }

    override fun convert(holder: BaseViewHolder, item: DeviceModel) {

        val tv_ssid = holder.getView<TextView>(R.id.device_ssid)
        holder.setText(R.id.device_name, item.deviceName)
        holder.setText(R.id.device_mac, item.deviceMac)
        val tvSetting: TextView = holder.getView(R.id.tvSetting)

        holder.setText(R.id.device_rssi,
            String.format(Locale.getDefault(), "RSSI: %d dBm", item.rssi))

        if (item.deviceType === PPScaleDefine.PPDeviceType.PPDeviceTypeCC.getType()) {
            if (!TextUtils.isEmpty(item.ssid)) {
                tv_ssid.text = item.ssid
            } else {
                tv_ssid.setText(R.string.to_config_the_network)
            }
        } else if (item.deviceProtocolType === PPDeviceProtocolType.PPDeviceProtocolTypeTorre.getType()) {
            if (!TextUtils.isEmpty(item.getSsid())) {
                tv_ssid.setText(item.getSsid())
            } else {
                tv_ssid.setText(R.string.to_config_the_network)
            }
        } else {
            tv_ssid.visibility = View.GONE
//            tvSetting.setVisibility(View.GONE);
        }
    }
}