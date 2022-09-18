package com.cc.control.protocol

import android.util.Log
import com.cc.control.bean.UnitEnum
import com.cc.control.logD
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.math.ceil
import kotlin.math.floor


/**
 * 字节转换成字节流
 * @param byte 可变形参
 * @param isXor 全部数据是否需要异或运算
 */
fun dvByteArrayFormat(vararg byte: Int, isXor: Boolean = false): ByteArray {

    val sb = StringBuffer()
    // 1.intToHexString
    byte.forEach {
        sb.append(it.dvToHex())
    }

    //2.判断是否需要异或运算
    if (isXor) {
        var aim = 0
        for (i in byte.indices) {
            aim = aim xor byte[i]
        }
        sb.append(aim.dvToHex())
    }

    return sb.toString().dvToByteArray()
}


/**
 * 字节转换成字节流 除了byteStar byteEnd 所有输入的值会异或运算
 * @param byteStar 开始码
 * @param byte 可变形参
 * @param byteEnd 结束吗
 */
fun dvByteArrayFormat(byteStar: Int, vararg byte: Int, byteEnd: Int): ByteArray {

    val sb = StringBuffer()

    // 1.添加开始码
    sb.append(byteStar.dvToHex())

    // 2.添加形参
    byte.forEach {
        sb.append(it.dvToHex())
    }

    //3.异或运算
    var aim = 0
    for (i in byte.indices) {
        aim = aim xor byte[i]
    }
    sb.append(aim.dvToHex())

    // 4.添加结束码
    sb.append(byteEnd.dvToHex())

    return sb.toString().dvToByteArray()
}


/**
 * 异或运算
 */
fun ByteArray.dvXor(): String {
    val sb = StringBuffer()
    var aim = 0
    for (i in this.indices) {
        aim = aim xor this[i].toInt()
    }
    sb.append(aim)
    return sb.toString()
}

/**
 * 字节数组转换为Ascii码
 */
fun ByteArray.dvToAsciiString(): String {
    val strBuilder = StringBuilder()
    for (b in this) {
        if (b.toInt() != 0) {
            strBuilder.append(b.toChar())
        }
    }
    return strBuilder.toString()
}

/**
 * 字符串转换成字节数组
 */
fun String.dvToByteArray(): ByteArray {
    val len = this.length
    val bytes = ByteArray((len + 1) / 2)
    var i = 0
    while (i < len) {
        val size = Math.min(2, len - i)
        val sub = this.substring(i, i + size)
        bytes[i / 2] = sub.toInt(16).toByte()
        i += 2
    }
    return bytes
}


/**
 * int转换为 16 进制字符串
 *
 * @return Hex 字符串
 */
//fun Any.dvToHex(vararg byte: Int): String {
//
//    if (byte.isNotEmpty()) {
//        val sb = StringBuilder()
//        byte.forEach {
//            sb.append(it.dvToByteArray(UnitEnum.U_8).dvToString())
//        }
//        return sb.toString()
//    } else {
//
//        return when (this) {
//            is Int -> {
//                this.dvToByteArray(UnitEnum.U_8).dvToString()
//            }
//            is String -> {
//                this.toInt().dvToByteArray(UnitEnum.U_8).dvToString()
//            }
//            is Byte -> {
//                this.dvToString().toInt().dvToByteArray(UnitEnum.U_8).dvToString()
//            }
//            is ByteArray -> {
//                val sb = StringBuilder()
//                this.forEach {
//                    sb.append(it.dvToString().toInt().dvToByteArray(UnitEnum.U_8).dvToString())
//                }
//                sb.toString()
//            }
//
//            else -> {
//                ""
//            }
//        }
//    }
//}

fun Int.dvToHex(): String {
    var hex = Integer.toHexString(this and 0xFF)
    if (hex.length == 1) hex = "0$hex"
    return hex
}


/**
 * 字节数组转换为 16 进制字符串
 * @return Hex 字符串
 */
fun ByteArray.dvToString(): String {
    val sb = StringBuilder()
    this.forEach {
        sb.append(it.dvToString())
    }
    return sb.toString()
}

/**
 * 字节数组转换为 16 进制字符串
 * @return Hex 字符串
 */
fun Byte.dvToString(): String {
    return String.format("%02X", this)
}

/**
 * 字节数组转换为 16 进制字符串 打印的时候使用
 * @return Hex 字符串
 */
fun ByteArray.dvToStringLog(isFormat: Boolean = false): String {
    val sb = StringBuilder()
    val sb1 = StringBuilder()

    this.forEach {
        sb.append(String.format("%02X", it) + " ")
        sb1.append("$it ")
    }
    return if (isFormat) "$sb== $sb1" else "$sb"
}

/**
 * 2位字节转换成1位
 */
fun dvReadShort(byte0: Byte, byte1: Byte): Int {
    val bytes = ByteArray(2)
    bytes[0] = byte0
    bytes[1] = byte1
    val mByteBuffer = ByteBuffer.wrap(bytes).order(
        ByteOrder.LITTLE_ENDIAN
    )
    val result = mByteBuffer.short and 0xffff.toShort()
    return result.toInt()
}

/**
 * @param bytes 当前扇区数据
 * @param length 每个扇区的长度
 * @param mtu mtu-4
 * 校验不通过 false 重发
 */
fun ackCheck(bytes: ByteArray, length: Int, mtu: Int): Boolean {
    for (i in bytes.indices) {
        if (i.toDouble() == ceil(length * 1.0 / mtu % 8 - 1)) {
            if (ceil(length / 1.0 / mtu) % 8 > 0) {
                val bit = (ceil(length * 1.0 / mtu % 8) - 1).toInt()
                return if (bit shl 1 == bytes[i].toInt()) continue else false
            }
        }
        if (i < ceil(length * 1.0 / mtu % 8 - 1)) {
            return false
        }
    }
    return true
}


/**
 * @param bytes 扇区数据
 * @param nowPosition 哪包开始出错
 *校验下包的下标
 */
fun checkPosition(bytes: ByteArray, nowPosition: Int): Int {
    var index = nowPosition
    while ((bytes[floor(nowPosition * 1.0 / 8).toInt()] and 1).toInt() shl nowPosition % 8 > 0) {
        index += 1
    }
    return index
}


/**
 * 将byte数组按照指定大小分割成多个数组
 * @param size 分割的块大小  单位：字节
 */
fun ByteArray.dvSplitByteArr(size: Int): ArrayList<ByteArray> {
    var fromIndex: Int
    var toIndex: Int
    val arrayList = ArrayList<ByteArray>()

    if (size > this.size) {
        arrayList.add(this)
        return arrayList
    }
    for (i in 0..this.size step size) {
        fromIndex = i
        toIndex = fromIndex + size
        //字节截取 新建一个size个字节的数组
        //从目标value第3个字节开始往后截取5个字节 截取出来以后从versionsBytes的0位开始替换
        if (toIndex >= this.size) {
            val versionsBytes = ByteArray(this.size - fromIndex)
            System.arraycopy(this, fromIndex, versionsBytes, 0, versionsBytes.size)
            arrayList.add(versionsBytes)
        } else {
            val versionsBytes = ByteArray(size)
            System.arraycopy(this, fromIndex, versionsBytes, 0, versionsBytes.size)
            arrayList.add(versionsBytes)
        }
    }
    return arrayList
}

/**
 * 将byte数组按照指定大小分割成多个数组
 * @param size 分割的块大小  组后包补齐
 * 最后一包不够补位ff
 */
fun ByteArray.dvSplitByteArrEndSeamProtection(size: Int): ArrayList<ByteArray> {
    var fromIndex: Int
    var toIndex: Int
    val arrayList = ArrayList<ByteArray>()

    if (size > this.size) {
        arrayList.add(this)
        return arrayList
    }
    for (i in 0 until this.size step size) {
        fromIndex = i
        toIndex = fromIndex + size
        if (toIndex >= this.size) {
            val versionsBytes = ByteArray(size)
            System.arraycopy(this, fromIndex, versionsBytes, 0, this.size - fromIndex)
            for (j in (this.size - fromIndex) until versionsBytes.size) {
                versionsBytes[j] = 0xff.toByte()
            }
            arrayList.add(versionsBytes)
           logD("dvSplitByteArrEndSeamProtection1",DeviceConvert.bytesToHexString(versionsBytes) )
        } else {
            val versionsBytes = ByteArray(size)
            System.arraycopy(this, fromIndex, versionsBytes, 0, versionsBytes.size)
            arrayList.add(versionsBytes)
            logD("dvSplitByteArrEndSeamProtection2",DeviceConvert.bytesToHexString(versionsBytes) )
        }
    }
    return arrayList
}

/**
 * 将byte数组按照指定大小分割成多个数组
 * LSW 数据位需要把position 也加进去
 * @param size 分割的块大小  单位：字节
 */
fun ByteArray.dvPositionSplitByte(size: Int): ArrayList<ByteArray> {
    var fromIndex: Int
    var toIndex: Int
    val arrayList = ArrayList<ByteArray>()

    if (size > this.size) {
        arrayList.add(this)
        return arrayList
    }
    for ((index, i) in (0..this.size step size).withIndex()) {
        fromIndex = i
        toIndex = fromIndex + size
        val length: Int = if (toIndex >= this.size)//剩余长度
            this.size - fromIndex + 1
        else
            size + 1
        val finalByte = ByteArray(length)//最终数组
        finalByte[0] = index.toByte()
        //分割的数据源
        System.arraycopy(this,
            fromIndex,
            finalByte,
            1,
            length - 1)
        arrayList.add(finalByte)
    }
    return arrayList
}

fun Int.intToByteShort(): ByteArray { //两个字节16进制  00 01
    val bytes = ByteArray(2)
    bytes[0] = (this and 0xff).toByte()
    bytes[1] = (this ushr 8 and 0xff).toByte()
    return bytes
}
fun otaCrc(path: String?): Int {
    var crcInit = 0
    try {
        val fis = FileInputStream(path)
        var readCount: Int
        val input: InputStream = BufferedInputStream(fis)
        val inputBuffer = ByteArray(256)
        var couts = 0
        while (input.read(inputBuffer, 0, 256).also { readCount = it } != -1) {
            if (couts != 0) {
                crcInit = crc32CalByByte(
                    crcInit,
                    inputBuffer,
                    0,
                    readCount)
            }
            couts++
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return crcInit
}

fun crc32CalByByte(oldCrc: Int, ptr: ByteArray, offset: Int, len: Int): Int {
    var len = len
    var crc = oldCrc
    var i = offset
    while (len-- != 0) {
        val high = crc / 256 //取CRC高8位
        crc = crc shl 8
        crc = crc xor crc_ta_8[high xor ptr[i].toInt() and 0xff]
        crc = crc and -0x1
        i++
    }
    return crc and -0x1
}

private val crc_ta_8 = intArrayOf(
    0x00000000, 0x77073096, -0x11f19ed4, -0x66f6ae46,
    0x076dc419, 0x706af48f, -0x169c5acb, -0x619b6a5d, 0x0edb8832,
    0x79dcb8a4, -0x1f2a16e2, -0x682d2678, 0x09b64c2b, 0x7eb17cbd,
    -0x1847d2f9, -0x6f40e26f, 0x1db71064, 0x6ab020f2, -0xc468eb8,
    -0x7b41be22, 0x1adad47d, 0x6ddde4eb, -0xb2b4aaf, -0x7c2c7a39,
    0x136c9856, 0x646ba8c0, -0x29d0686, -0x759a3614, 0x14015c4f,
    0x63066cd9, -0x5f0c29d, -0x72f7f20b, 0x3b6e20c8, 0x4c69105e,
    -0x2a9fbe1c, -0x5d988e8e, 0x3c03e4d1, 0x4b04d447, -0x2df27a03,
    -0x5af54a95, 0x35b5a8fa, 0x42b2986c, -0x2444362a, -0x534306c0,
    0x32d86ce3, 0x45df5c75, -0x2329f231, -0x542ec2a7, 0x26d930ac,
    0x51de003a, -0x3728ae80, -0x402f9eea, 0x21b4f4b5, 0x56b3c423,
    -0x30456a67, -0x47425af1, 0x2802b89e, 0x5f058808, -0x39f3264e,
    -0x4ef416dc, 0x2f6f7c87, 0x58684c11, -0x3e9ee255, -0x4999d2c3,
    0x76dc4190, 0x01db7106, -0x672ddf44, -0x102aefd6, 0x71b18589,
    0x06b6b51f, -0x60401b5b, -0x17472bcd, 0x7807c9a2, 0x0f00f934,
    -0x69f65772, -0x1ef167e8, 0x7f6a0dbb, 0x086d3d2d, -0x6e9b9369,
    -0x199ca3ff, 0x6b6b51f4, 0x1c6c6162, -0x7a9acf28, -0xd9dffb2,
    0x6c0695ed, 0x1b01a57b, -0x7df70b3f, -0xaf03ba9, 0x65b0d9c6,
    0x12b7e950, -0x74414716, -0x3467784, 0x62dd1ddf, 0x15da2d49,
    -0x732c830d, -0x42bb39b, 0x4db26158, 0x3ab551ce, -0x5c43ff8c,
    -0x2b44cf1e, 0x4adfa541, 0x3dd895d7, -0x5b2e3b93, -0x2c290b05,
    0x4369e96a, 0x346ed9fc, -0x529877ba, -0x259f4730, 0x44042d73,
    0x33031de5, -0x55f5b3a1, -0x22f28337, 0x5005713c, 0x270241aa,
    -0x41f4eff0, -0x36f3df7a, 0x5768b525, 0x206f85b3, -0x46992bf7,
    -0x319e1b61, 0x5edef90e, 0x29d9c998, -0x4f2f67de, -0x3828574c,
    0x59b33d17, 0x2eb40d81, -0x4842a3c5, -0x3f459353, -0x12477ce0,
    -0x65404c4a, 0x03b6e20c, 0x74b1d29a, -0x152ab8c7, -0x622d8851,
    0x04db2615, 0x73dc1683, -0x1c9cf4ee, -0x6b9bc47c, 0x0d6d6a3e,
    0x7a6a5aa8, -0x1bf130f5, -0x6cf60063, 0x0a00ae27, 0x7d079eb1,
    -0xff06cbc, -0x78f75c2e, 0x1e01f268, 0x6906c2fe, -0x89da8a3,
    -0x7f9a9835, 0x196c3671, 0x6e6b06e7, -0x12be48a, -0x762cd420,
    0x10da7a5a, 0x67dd4acc, -0x6462091, -0x71411007, 0x17b7be43,
    0x60b08ed5, -0x29295c18, -0x5e2e6c82, 0x38d8c2c4, 0x4fdff252,
    -0x2e44980f, -0x5943a899, 0x3fb506dd, 0x48b2364b, -0x27f2d426,
    -0x50f5e4b4, 0x36034af6, 0x41047a60, -0x209f103d, -0x579820ab,
    0x316e8eef, 0x4669be79, -0x349e4c74, -0x43997ce6, 0x256fd2a0,
    0x5268e236, -0x33f3886b, -0x44f4b8fd, 0x220216b9, 0x5505262f,
    -0x3a45c442, -0x4d42f4d8, 0x2bb45a92, 0x5cb36a04, -0x3d280059,
    -0x4a2f30cf, 0x2cd99e8b, 0x5bdeae1d, -0x649b3d50, -0x139c0dda,
    0x756aa39c, 0x026d930a, -0x63f6f957, -0x14f1c9c1, 0x72076785,
    0x05005713, -0x6a40b57e, -0x1d4785ec, 0x7bb12bae, 0x0cb61b38,
    -0x6d2d7165, -0x1a2a41f3, 0x7cdcefb7, 0x0bdbdf21, -0x792c2d2c,
    -0xe2b1dbe, 0x68ddb3f8, 0x1fda836e, -0x7e41e933, -0x946d9a5,
    0x6fb077e1, 0x18b74777, -0x77f7a51a, -0xf09590, 0x66063bca,
    0x11010b5c, -0x709a6101, -0x79d5197, 0x616bffd3, 0x166ccf45,
    -0x5ff51d88, -0x28f22d12, 0x4e048354, 0x3903b3c2, -0x5898d99f,
    -0x2f9fe909, 0x4969474d, 0x3e6e77db, -0x512e95b6, -0x2629a524,
    0x40df0b66, 0x37d83bf0, -0x564351ad, -0x2144613b, 0x47b2cf7f,
    0x30b5ffe9, -0x42420de4, -0x35453d76, 0x53b39330, 0x24b4a3a6,
    -0x452fc9fb, -0x3228f96d, 0x54de5729, 0x23d967bf, -0x4c9985d2,
    -0x3b9eb548, 0x5d681b02, 0x2a6f2b94, -0x4bf441c9, -0x3cf3715f,
    0x5a05df1b, 0x2d02ef8d)

/**
 * 将int转为低字节在前，高字节在后的byte数组
 */
fun Int.dvToByteArray(enum: UnitEnum): ByteArray {

    return when (enum) {
        UnitEnum.U_8 -> {
            val bytes = ByteArray(1)
            bytes[0] = (this and 0xff).toByte()
            return bytes
        }
        UnitEnum.U_16 -> {
            val bytes = ByteArray(2)
            bytes[0] = (this and 0xff).toByte()
            bytes[1] = (this ushr 8 and 0xff).toByte()
            return bytes
        }
        UnitEnum.U_32 -> {
            val bytes = ByteArray(4)
            bytes[0] = (this and 0xff).toByte()
            bytes[1] = (this ushr 8 and 0xff).toByte()
            bytes[2] = (this ushr 16 and 0xff).toByte()
            bytes[3] = (this ushr 24 and 0xff).toByte()
            return bytes
        }
        UnitEnum.U_64 -> {
            val bytes = ByteArray(8)
            bytes[0] = (this and 0xff).toByte()
            bytes[1] = (this ushr 8 and 0xff).toByte()
            bytes[2] = (this ushr 16 and 0xff).toByte()
            bytes[3] = (this ushr 24 and 0xff).toByte()
            bytes[4] = (this ushr 32 and 0xff).toByte()
            bytes[5] = (this ushr 40 and 0xff).toByte()
            bytes[6] = (this ushr 48 and 0xff).toByte()
            bytes[7] = (this ushr 56 and 0xff).toByte()
            return bytes
        }

        UnitEnum.S_8 -> {
            val bytes = ByteArray(1)
            bytes[0] = (this and 0xff).toByte()
            return bytes
        }
        UnitEnum.S_16 -> {
            val bytes = ByteArray(2)
            bytes[0] = (this and 0xff).toByte()
            bytes[1] = (this ushr 8 and 0xff).toByte()
            return bytes
        }
        UnitEnum.S_32 -> {
            val bytes = ByteArray(4)
            bytes[0] = (this and 0xff).toByte()
            bytes[1] = (this ushr 8 and 0xff).toByte()
            bytes[2] = (this ushr 16 and 0xff).toByte()
            bytes[3] = (this ushr 24 and 0xff).toByte()
            return bytes
        }
        UnitEnum.S_64 -> {
            val bytes = ByteArray(8)
            bytes[0] = (this and 0xff).toByte()
            bytes[1] = (this ushr 8 and 0xff).toByte()
            bytes[2] = (this ushr 16 and 0xff).toByte()
            bytes[3] = (this ushr 24 and 0xff).toByte()
            bytes[4] = (this ushr 32 and 0xff).toByte()
            bytes[5] = (this ushr 40 and 0xff).toByte()
            bytes[6] = (this ushr 48 and 0xff).toByte()
            bytes[7] = (this ushr 56 and 0xff).toByte()
            return bytes
        }
    }

}
