package com.cc.control


/**
 * onRunning 运行中 onPause 暂停  onSlowDown 减速中 onFinish 待机、停机、结束 onCountTime 倒计时
 */
interface DeviceStatusListener {

    fun onRunning() {}

    fun onPause() {}

    fun onSlowDown() {}

    fun onFinish() {}

    fun onCountTime() {}

}
