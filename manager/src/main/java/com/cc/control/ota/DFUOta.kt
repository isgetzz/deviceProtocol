package com.cc.control.ota

import android.app.LoaderManager
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.os.bundleOf
import com.cc.control.DeviceDfuListener
import com.cc.control.DfuService
import com.cc.control.protocol.isServiceRunning
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-09-18 12:12.
 * @Description :dfu固件升级
 */
class DFUOta : BaseDeviceOta() {
    companion object {
        private const val EXTRA_URI = "uri"
        private const val SELECT_FILE_REQ = 1
    }

    private val dfuProgressListener by lazy {
        DeviceDfuListener { onCompleted, progress ->
            if (progress == -1) {
                deviceOtaListener?.invoke(D_OTA_ERROR, 0)
            } else {
                if (onCompleted)
                    deviceOtaListener?.invoke(D_OTA_SUCCESS, 100)
                else
                    deviceOtaListener?.invoke(D_OTA_UPDATE, progress)
            }
        }
    }

    override fun initFilePath(filePath: String) {
        if (dfuUri == null)
            return
        mActivity?.run {
            DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener)
            loaderManager.restartLoader(SELECT_FILE_REQ, bundleOf(EXTRA_URI to dfuUri),
                object : LoaderManager.LoaderCallbacks<Cursor?> {
                    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor?> {
                        val uri = args.getParcelable<Uri>(EXTRA_URI)
                        return CursorLoader(this@run, uri, null, null, null, null)
                    }

                    override fun onLoadFinished(loader: Loader<Cursor?>, data: Cursor?) {
                        if (data != null && data.moveToNext()) {
                            var path: String? = null
                            val dataIndex = data.getColumnIndex(MediaStore.MediaColumns.DATA)
                            if (dataIndex != -1) path = data.getString(dataIndex /* 2 DATA */)
                            if (isServiceRunning(DfuService::class.java, this@run)) return
                            val starter: DfuServiceInitiator =
                                DfuServiceInitiator(deviceConnectBean.address)
                                    .setDeviceName(deviceConnectBean.deviceName)
                                    .setKeepBond(false)
                                    .setForeground(false)
                                    .setForceDfu(false)
                                    .setPacketsReceiptNotificationsEnabled(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                                    .setPacketsReceiptNotificationsValue(DfuServiceInitiator.DEFAULT_PRN_VALUE)
                                    .setPrepareDataObjectDelay(400)
                                    .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                            starter.setZip(dfuUri, path)
                            starter.start(this@run, DfuService::class.java)
                        }
                    }

                    override fun onLoaderReset(loader: Loader<Cursor?>) {}
                })

        }
    }

    override fun onBluetoothNotify(service: UUID?, character: UUID?, value: ByteArray) {

    }

    override fun unregisterListener() {
        if (mActivity != null) {
            DfuServiceListenerHelper.unregisterProgressListener(mActivity!!, dfuProgressListener)
        }
    }
}