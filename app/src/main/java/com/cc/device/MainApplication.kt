package com.cc.device

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import com.cc.control.BluetoothClientManager
import me.jessyan.autosize.AutoSize
import me.jessyan.autosize.AutoSizeConfig
import me.jessyan.autosize.onAdaptListener
import me.jessyan.autosize.utils.ScreenUtils

/**
 * @Author      : cc
 * @Date        : on 2022-10-17 15:58.
 * @Description :app
 */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAutoSize()
        BluetoothClientManager.initDeviceManager(this, true)
    }

    private fun initAutoSize() {
        AutoSize.initCompatMultiProcess(this)
        //屏幕适配监听器
        AutoSizeConfig.getInstance().onAdaptListener = object : onAdaptListener {
            override fun onAdaptBefore(target: Any, activity: Activity) {
                //使用以下代码, 可以解决横竖屏切换时的屏幕适配问题
                //首先设置最新的屏幕尺寸，ScreenUtils.getScreenSize(activity) 的参数一定要不要传 Application !!!
                AutoSizeConfig.getInstance().screenWidth = ScreenUtils.getScreenSize(activity)[0]
                AutoSizeConfig.getInstance().screenHeight = ScreenUtils.getScreenSize(activity)[1]
                //根据屏幕方向，设置设计尺寸
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    //设置横屏设计尺寸
                    AutoSizeConfig.getInstance()
                        .setDesignWidthInDp(812).designHeightInDp = 375
                } else {
                    //设置竖屏设计尺寸
                    AutoSizeConfig.getInstance()
                        .setDesignWidthInDp(375).designHeightInDp = 812
                }
            }

            override fun onAdaptAfter(target: Any, activity: Activity) {}
        }
    }

}