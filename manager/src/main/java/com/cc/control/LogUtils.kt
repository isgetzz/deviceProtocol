package com.cc.control

import android.content.Context
import android.os.Environment
import android.util.Log
import com.cc.control.BluetoothClientManager.isShowLog
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 测试或者手动开启
 */
fun logD(tag: String, content: String, showLog: Boolean = false) {
    if (showLog || isShowLog) {
        Log.d(tag, content)
    }
}

fun logE(tag: String, content: String, showLog: Boolean = false) {
    if (showLog || isShowLog) {
        Log.e(tag, content)
    }
}

fun logV(tag: String, content: String, showLog: Boolean = false) {
    if (showLog || isShowLog) {
        Log.v(tag, content)
    }
}

fun logI(tag: String, content: String, showLog: Boolean = false) {
    if (showLog || isShowLog) {
        Log.i(tag, content)
    }
}

private val dateFormatFile: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US) //日期格式;
private val dateFormatHour: SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) //日期格式;
private val logFilePath = getFilePath(BluetoothClientManager.app) + "/Logs"
private val date = Date()

/**
 * 写入log日志
 */
fun writeToFile(tag: String, msg: String) {
    val data = dateFormatFile.format(date)
    val fileName = "$logFilePath/log_$data.log" //log日志名，使用时间命名，保证不重复
    val file = File(logFilePath)
    if (!file.exists()) {
        file.mkdirs()
    }
    val fos: FileOutputStream?
    var bw: BufferedWriter? = null
    try {
        fos = FileOutputStream(fileName, true) //这里的第二个参数代表追加还是覆盖，true为追加，false 为覆盖
        bw = BufferedWriter(OutputStreamWriter(fos))
        bw.write("${dateFormatHour.format(Date())} $tag\t $msg\n")
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            bw?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    logD("writeToFile: $tag", msg)
}

/**
 * 获得文件存储路径
 *
 * @return
 */
fun getFilePath(context: Context): String? {
    return if (Environment.MEDIA_MOUNTED == Environment.MEDIA_MOUNTED || !Environment.isExternalStorageRemovable()) {
        //外部储存可用
        context.getExternalFilesDir(null)!!.path //获得外部存储路径
    } else {
        context.filesDir.path //直接存在/data/data里，非root手机是看不到的
    }
}