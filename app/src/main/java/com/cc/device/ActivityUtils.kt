package com.cc.device

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 *  : cc
 *  : on 2022-10-17 15:17.
 */

/**
 *@param cls 类
 * @param bundle 数据
 * @param requestCode code
 */
fun Activity.goActivity(cls: Class<*>, bundle: Bundle? = null, requestCode: Int = -1) {
    val intent = Intent(this, cls)
    if (bundle != null) {
        intent.putExtras(bundle)
    }
    if (requestCode == -1) {
        startActivity(intent)
    } else {
        startActivityForResult(intent, requestCode)
    }
}

/**
 *请求权限需要的权限
 *@return true已有权限
 */
fun Activity.requestPermission(
    array: Array<String>,
    requestCode: Int = 1,
): Boolean {
    if (Build.VERSION.SDK_INT >= 23) {
        if (checkPermission(array)) {
            ActivityCompat.requestPermissions(
                this, array,
                requestCode
            )
            return false
        }
    }
    return true
}

/**
 *判断是否缺少权限，true=缺少权限
 */
fun Context.checkPermission(array: Array<String>): Boolean {
    if (Build.VERSION.SDK_INT >= 23) {
        for (permission in array) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return true
            }
        }
    }
    return false
}

/**
 * 手机是否开启位置服务，如果没有开启那么所有app将不能使用定位功能
 */
fun Activity.isLocServiceEnable(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    return gps || network
}

/**
 *跳转体统设置
 */
fun Activity.goToAppSetting(action: String = Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
    val intent = Intent()
    intent.action = action
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    this.startActivity(intent)
}
