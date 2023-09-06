package com.cc.control

import com.cc.control.protocol.*
import com.inuker.bluetooth.library.beacon.BeaconParser
import java.util.*

/**
 * Author      : cc
 * Date        : on 2022-02-18 16:53.
 * Description :跳绳获取数据
 * 智健、mrk 协议
 */
class DeviceSkippingFunction(device: String) : BaseDeviceFunction(device) {
    override fun startWrite(isCreate: Boolean) {
        if (dateArray.isEmpty()) {
            dateArray.add(writeZJSkippingData())
            dateArray.add(writeZJSkippingElectric())
        }
        if (propertyBean.protocol != DeviceConstants.D_SERVICE_TYPE_MRK) {
            writeData()
        }
        notifyBean.status = DEVICE_TREADMILL_RUNNING
    }

    /**
     * 跳绳如果是自由训练需要设置，其他模式设置完目标的时候就下发模式
     */
    override fun setDeviceModel(model: Int, targetNum: Int, onSuccess: (() -> Unit)) {
        if (propertyBean.protocol == DeviceConstants.D_SERVICE_TYPE_MRK) {
            write(writeMrkReset()) {
                if (model == DeviceConstants.D_TRAIN_FREE) {
                    write(writeMrkStart())
                } else {
                    write(writeTargetNum(model, targetNum), onSuccess)
                }
            }
        } else write(writeZJSkippingModel(model, targetNum), onSuccess)
    }

    override fun onControl(
        speed: Int,
        resistance: Int,
        slope: Int,
        isDelayed: Boolean,
        isSlope: Boolean,
    ) {
    }

    override fun onBluetoothNotify(service: UUID, character: UUID, parser: BeaconParser) {
        val adr = parser.readByte()
        val length = parser.readByte()
        if (propertyBean.protocol == 1) {
            if (dataLength - 2 == length) {
                onMrkProtocol(notifyBean, parser, dataLength - 5)
                mDataListener?.invoke(notifyBean)
            }
        } else {
            when (parser.readByte()) {
                DEVICE_ZJ_ELECTRIC -> {
                    notifyBean.electric = parser.readByte()
                    mDataListener?.invoke(notifyBean)
                }
                DEVICE_ZJ_DATA -> {
                    if (adr == dataLength) {
                        //速率 个数/时间//0.05 0.06 0.07
                        val num = parser.readShort()
                        val baseKarl = (if (Math.random() > 0.5) 0.05 else 0.07).toFloat()
                        val second = parser.readShort()
                        notifyBean.run {
                            val karl = (if (num < count) num else num - count) * baseKarl
                            if (second > 0 && num > 0) {
                                energy += karl
                                speed = num.toFloat() / second * 60.0f
                                deviceTime = second.toLong()
                                count = num
                            }
                            model = parser.readByte()
                        }
                        mDataListener?.invoke(notifyBean)
                    }
                }
                DEVICE_ZJ_MODEL_ADR -> {
                    write(writeSkippingStart(), ::writeData)
                }
            }
        }
    }

    override fun onDestroyWrite(): ByteArray {
        return if (propertyBean.protocol == 1) writeMrkStop() else writeZJSkippingClear()
    }
}