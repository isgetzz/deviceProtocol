package com.cc.control.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
    data class CharacteristicBean(
    var service: String = "",
    var characteristicProperties: String = "",
    var characteristicValue: String = "",
    var serviceUUID: String? = "",
    var characteristicUUID: String? = "",
    var isContains: Boolean? = false,
    ) : Parcelable