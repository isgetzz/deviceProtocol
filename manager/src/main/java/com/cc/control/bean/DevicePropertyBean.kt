package com.cc.control.bean

import android.os.Parcelable
import com.cc.control.protocol.DeviceConstants
import com.inuker.bluetooth.library.model.BleGattProfile
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-02-15 14:05.
 * @Description :设备配置Bean
 * 正常ota writeNoRes部分特征值需要用write不然无法写入成功。
 * 连接的时候可根据蓝牙返回的状态判断 如:getCharacters().get(0).getProperty()
 */
@Parcelize
data class DevicePropertyBean(
    var address: String = "",
    var type: String = "",//设备类型
    var name: String = "", //蓝牙名称k60
    var bleProfile: BleGattProfile? = null,//设备特征值
    var hasHeartRate: Boolean = false,//开启心跳
    var isConnect: Boolean = false,//绑定状态 true 连接
    var serviceUUID: UUID? = null,//设备服务Id
    var notifyUUID: UUID? = null,//设备接收通道
    var writeUUID: UUID? = null,//写入
    var otaService: UUID? = null,//ota
    var otaWrite: UUID? = null,//ota 写入
    var otaNotify: UUID? = null,//ota 刷新
    var otaControl: UUID? = null,//ota 控制
    var mtu: Int = DeviceConstants.D_MTU_LENGTH,//优化固件升级根据通道特征
    var protocol: Int = 0,// 1麦瑞克   2FTMS  3智健  4柏群   6 筋膜枪
    var otaType: Int = 0, //1:泰凌威 智建设备；2:博通; 3:DFU 4:新向远 5富芮坤 6 凌思威
    var modelNumber: String = "",//ota modelId
    var modelRevision: String = "",//ota version
    var autoConnect: Boolean = true,
    var characteristicList: List<CharacteristicBean> = listOf(),
) : Parcelable {
    constructor(
        deviceType: String = "",
        address: String = "",
        serviceUUID: UUID,
        characterNotify: UUID,
    ) : this(address,
        type = deviceType,
        serviceUUID = serviceUUID,
        notifyUUID = characterNotify)

}
