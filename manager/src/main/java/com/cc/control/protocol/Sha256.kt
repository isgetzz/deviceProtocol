package com.cc.control.protocol

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * sha256算法
 */
fun ByteArray.getSHA256(): String {
    val messageDigest: MessageDigest
    var str = ""
    try {
        messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(this)
        str = byte2Hex(messageDigest.digest())
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    }
    return str
}

private fun byte2Hex(bytes: ByteArray): String {
    val stringBuffer = StringBuilder()
    var temp: String
    for (aByte in bytes) {
        temp = Integer.toHexString((aByte.toInt() and 0xff))
        if (temp.length == 1) {
            stringBuffer.append("0")
        }
        stringBuffer.append(temp)
    }
    return stringBuffer.toString()
}