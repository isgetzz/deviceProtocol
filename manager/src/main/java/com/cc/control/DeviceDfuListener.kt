package com.cc.control

import no.nordicsemi.android.dfu.DfuProgressListenerAdapter

/**
 * Author      : cc
 * Date        : on 2022-08-25 15:01.
 * Description :dfu 固件更新监听
 */
class DeviceDfuListener(private val onProgressListener: (onCompleted: Boolean, Int) -> Unit) :
    DfuProgressListenerAdapter() {
    override fun onDfuCompleted(deviceAddress: String) {
        onProgressListener.invoke(true, 360)
    }

    override fun onProgressChanged(
        deviceAddress: String, percent: Int,
        speed: Float, avgSpeed: Float,
        currentPart: Int, partsTotal: Int,
    ) {
        onProgressListener.invoke(false, percent)
    }

    override fun onError(
        deviceAddress: String,
        error: Int,
        errorType: Int,
        message: String,
    ) {
        logD("BaseDeviceOta onError", "mac: $deviceAddress  error: $error  type: $errorType  message: $message")
        onProgressListener.invoke(false, -1)
    }
}