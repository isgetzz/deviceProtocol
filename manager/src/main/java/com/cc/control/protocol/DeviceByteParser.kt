package com.cc.control.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

class DeviceByteParser(private val bytes: ByteArray) {

    private val mByteBuffer: ByteBuffer = ByteBuffer.wrap(bytes).order(
        ByteOrder.LITTLE_ENDIAN
    )

    fun getSize(): Int {
        return bytes.size
    }

    fun getPosition(): Int {
        return mByteBuffer.position()
    }


    fun setPosition(position: Int) {
        mByteBuffer.position(position)
    }


    //读1个字节
    fun readByte8(isSymbol: Boolean = false): Int {
        return mByteBuffer.get().toInt()
    }

    //读2个字节
    fun readByte16(isSymbol: Boolean = false): Int {
        return mByteBuffer.short.toInt()
    }

    //读4个字节
    fun readByte32(isSymbol: Boolean = false): Int {
        return mByteBuffer.int
    }

    //读8个字节
    fun readByte64(isSymbol: Boolean = false): Long {
        return mByteBuffer.long
    }


}