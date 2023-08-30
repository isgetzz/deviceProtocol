package com.cc.control.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @Author      : cc
 * @Date        : on 2022-06-27 15:38.
 * @Description :体脂秤用户
 */
@Parcelize
data class ScaleUserBean(
    val avatar: String = "", // https
    val birthday: String = "", // 2000-10-14
    val height: String = "0", // 170
    val nickName: String = "", // CC
    val id: String = "", // 1541319128676057090
    val createId: Long = 0, // 1377431331972788225
    val isDelete: Int = 0, // 0
    var age: Int = 0,//年龄
    val isMain: Int = 0, // 1是否是主账号，1是0否
    val lastTime: String = "",
    val remark: String = "",
    val sex: Int = 0, // 1
    val status: Int = 0, // 0
    val updateId: String = "", // 0
    val updateTime: String = "",
    val userId: String = "", // 1377431331972788225
) : Parcelable {
    @Parcelize
    data class BodyFatScaleFrom(val id: String) : Parcelable
}