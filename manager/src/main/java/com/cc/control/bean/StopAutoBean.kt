package com.cc.control.bean

/**
 * @Author      : cc
 * @Date        : on 2023-03-24 17:30.
 * @Description :手动断开通知
 */
data class StopAutoBean(
    var stopAuto: Boolean = false,
    var mac: String = "",
    val unBind: Boolean = false,
)