package com.cc.control.protocol

/**
 *协议相关
 */
object DeviceConstants {
    //MTU
    const val D_MTU_LENGTH = 512

    //设备类型
    const val D_BICYCLE = "1" //动感单车
    const val D_TREADMILL = "2" //跑步机
    const val D_OTHER = "3" //其他
    const val D_ROW = "5" //划船机
    const val D_TECHNOGYM = "6" //椭圆机
    const val D_FASCIA_GUN = "9" //筋膜枪
    const val D_SKIPPING = "10" //跳绳
    const val D_HULA_HOOP = "21" //呼啦圈
    const val D_HEART = "100" //心率带
    const val D_FAT_SCALE = "41"
    const val D_PHYLLISRODS = "27" //菲利斯棒

    //后台定义OTA类型
    const val D_OTA_TLW = 1
    const val D_OTA_BT = 2
    const val D_OTA_DFU = 3
    const val D_OTA_XXY = 4
    const val D_OTA_FRK = 5
    const val D_OTA_LSW = 6

    //训练类型
    const val D_TRAIN_FREE = 0 //自由训练
    const val D_TRAIN_NUM = 1 //倒计数
    const val D_TRAIN_TIME = 2 //倒计时

    const val D_TRAIN_SLEEP = 78 //休眠数据上报
    const val D_TRAIN_FINISH = 77 //训练完成
    const val D_TRAIN_ADD = 79 //上报数据不退出页面
    const val D_TRAIN_RECONNECT = 80 //重连上报数据
    const val D_TARGET_FINISH = 76 //目标完成

    //公司协议
    const val D_SERVICE_MRK = "59554c55-8000-6666-8888-4d4552414348"
    const val D_CHARACTER_HEART_MRK = "59554c55-0000-6666-8888-4d4552414348" //心跳包特征自己
    const val D_CHARACTER_DATA_MRK = "59554c55-0001-6666-8888-4d4552414348" //数据
    const val D_CHARACTER_HISTORY_MRK = "59554c55-0000-6666-8888-4d4552414348" //历史报文数据

    //柏群
    const val D_SERVICE_BQ = "49535343-fe7d-4ae5-8fa9-9fafd205e455"
    const val D_CHARACTER_BQ = "49535343-1e4d-4bd9-ba61-23c647249616"

    //FTMS
    const val D_SERVICE1826 = "00001826-0000-1000-8000-00805f9b34fb"
    const val D_SERVICE1826_2ADA = "00002ada-0000-1000-8000-00805f9b34fb" //读取控制状态
    const val D_SERVICE1826_2AD9 = "00002ad9-0000-1000-8000-00805f9b34fb" //控制指令
    const val D_SERVICE1826_2AD1 = "00002ad1-0000-1000-8000-00805f9b34fb" //划船机读取数据
    const val D_SERVICE1826_2AD2 = "00002ad2-0000-1000-8000-00805f9b34fb" //单车读取数据
    const val D_SERVICE1826_2ACE = "00002ace-0000-1000-8000-00805f9b34fb" //椭圆机读取数据

    //FFFO
    const val D_SERVICE_FFFO = "0000fff0-0000-1000-8000-00805f9b34fb"

    //固件升级服务 DFU 目前用的第三方库
    const val D_EQUIPMENT_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb" //获取设备信息服务码
    const val D_CHARACTER_2A24 = "00002a24-0000-1000-8000-00805f9b34fb" //DFU获取版本信息特征值 model
    const val D_CHARACTER_2A26 = "00002a26-0000-1000-8000-00805f9b34fb" //DFU获取版本信息特征值 Software
    const val D_CHARACTER_2A28 = "00002a28-0000-1000-8000-00805f9b34fb" //其他获取版本信息特征值 Software
    //XXY
    const val D_SERVICE_OTA_XXY = "0000ff00-0000-1000-8000-00805f9b34fb" //新向远
    const val D_CHARACTER_OTA_XXY = "0000ff01-0000-1000-8000-00805f9b34fb" //新向远

    //TLW
    const val D_SERVICE_OTA_TLW = "00010203-0405-0607-0809-0a0b0c0d1912"
    const val D_CHARACTER_OTA_TLW = "00010203-0405-0607-0809-0a0b0c0d2b12"

    //BT
    const val D_SERVICE_OTA_BT = "f000ffc0-0451-4000-b000-000000000000"
    const val D_CHARACTER_OTA_BT1 = "f000ffc1-0451-4000-b000-000000000000" //博通OTA特征首包
    const val D_CHARACTER_OTA_BT2 = "f000ffc2-0451-4000-b000-000000000000" //博通OTA特征后续包

    //FRK
    const val D_SERVICE_OTA_FRK = "02f00000-0000-0000-0000-00000000fe00"
    const val D_CHARACTER_OTA_FRK = "02f00000-0000-0000-0000-00000000ff01"
    const val D_NOTIFY_OTA_FRK = "02f00000-0000-0000-0000-00000000ff02"

    //LSW
    const val D_SERVICE_OTA_LSW = "00002600-0000-1000-8000-00805f9b34fb"

    // 数据特征值
    const val D_CHARACTER_OTA_LSW = "00007001-0000-1000-8000-00805f9b34fb"

    //控制特征值的、接收数据
    const val D_CONTROL_OTA_LSW = "00007000-0000-1000-8000-00805f9b34fb"

    //心率带其他数据
    const val D_SERVICE_DATA_HEART = "0000180d-0000-1000-8000-00805f9b34fb"
    const val D_CHARACTER_DATA_HEART = "00002a37-0000-1000-8000-00805f9b34fb"

    //心率带电量
    const val D_SERVICE_ELECTRIC_HEART = "0000180f-0000-1000-8000-00805f9b34fb"
    const val D_CHARACTER_ELECTRIC_HEART = "00002a19-0000-1000-8000-00805f9b34fb"

    const val D_SERVICE_TYPE_MRK = 1
    const val D_SERVICE_TYPE_FTMS = 2
    const val D_SERVICE_TYPE_ZJ = 3
    const val D_SERVICE_TYPE_BQ = 4
    const val D_SERVICE_TYPE_OTHER = 5
    const val D_SERVICE_TYPE_FASCIA = 6

}