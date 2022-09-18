package com.cc.control

import com.inuker.bluetooth.library.beacon.BeaconParser
import com.cc.control.protocol.*
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-02-18 16:53.
 * @Description :跳绳获取数据
 * 智健、mrk 协议
 */
class DeviceSkippingFunction : BaseDeviceFunction() {
    override fun onDeviceWrite(isCreate: Boolean) {
        if (dateArray.isEmpty()) {
            dateArray.add(onWriteZJSkippingData())
            dateArray.add(onWriteZJSkippingElectric())
        }
        if (deviceDateBean.deviceProtocol != DeviceConstants.D_SERVICE_TYPE_MRK) {
            onDeviceCmd()
        }
    }

    /**
     * 跳绳如果是自由训练需要设置，其他模式设置完目标的时候就下发模式
     */
    override fun onDeviceSetModel(model: Int, targetNum: Int, onSuccess: (() -> Unit)?) {
        if (deviceDateBean.deviceProtocol == DeviceConstants.D_SERVICE_TYPE_MRK) {
            write(onWriteMrkReset()) {
                if (model == DeviceConstants.D_TRAIN_FREE) {
                    write(onWriteMrkStart())
                } else {
                    write(onWriteTargetNum(model, targetNum), onSuccess)
                }
            }
        } else write(onWriteZJSkippingModel(model, targetNum), onSuccess)
    }

    override fun onDeviceControl(
        speed: Int,
        resistance: Int,
        slope: Int,
    ) {
    }

    override fun onBluetoothNotify(
        service: UUID,
        character: UUID,
        value: ByteArray,
        beaconParser: BeaconParser,
    ) {
        if (deviceDateBean.deviceProtocol == 1) {
            if (value.size - 2 == len) {
                onMrkProtocol(deviceNotifyBean, beaconParser, value.size - 5)
                deviceDataListener?.invoke(deviceNotifyBean)
            }
        } else {
            beaconParser.setPosition(3)
            when (deviceStatus) {
                DEVICE_ZJ_ELECTRIC -> {
                    deviceNotifyBean.electric = beaconParser.readByte()
                    deviceDataListener?.invoke(deviceNotifyBean)
                }
                DEVICE_ZJ_DATA -> {
                    if (adr == value.size) {
                        //速率 个数/时间//0.05 0.06 0.07
                        val num = beaconParser.readShort()
                        val baseKarl = (if (Math.random() > 0.5) 0.05 else 0.07).toFloat()
                        val second = beaconParser.readShort()
                        deviceNotifyBean.run {
                            val karl = (if (num < count) num else num - count) * baseKarl
                            if (second > 0 && num > 0) {
                                energy += karl
                                speed = num.toFloat() / second * 60.0f
                                deviceTime = second.toLong()
                                count = num
                            }
                            skippingModel = beaconParser.readByte()
                        }
                        deviceDataListener?.invoke(deviceNotifyBean)
                    }
                }
                DEVICE_ZJ_MODEL_ADR -> {
                    write(onWriteSkippingStart(), ::onDeviceCmd)
                }
            }
        }
    }

    override fun onDestroyWrite(): ByteArray {
        return if (deviceDateBean.deviceProtocol == 1) onWriteMrkStop() else onWriteZJSkippingClear()
    }
}