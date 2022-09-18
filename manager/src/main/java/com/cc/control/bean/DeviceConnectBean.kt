package com.cc.control.bean

import com.inuker.bluetooth.library.model.BleGattProfile
import com.cc.control.protocol.DeviceConstants
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-02-15 14:05.
 * @Description :设备配置Bean
 * 正常ota writeNoRes部分特征值需要用write不然无法写入成功。
 * 连接的时候可根据蓝牙返回的状态判断 如:getCharacters().get(0).getProperty()
 */
class DeviceConnectBean(
    var address: String = "",
    var isDeviceConnect: Boolean = false,//绑定状态 true 连接
    var deviceType: String = "",//设备类型
    var deviceName: String = "", //蓝牙名称k60
    var deviceId: String = "",//设备唯一Id
    var deviceTwoTypeId: String = "",//二级id
    var heartEquipmentId: String = "",//心率后台标识一Id
    var serviceUUId: UUID? = null,//设备服务Id
    var characterNotify: UUID? = null,
    var characterWrite: UUID? = null,
    var otaService: UUID? = null,
    var otaWriteCharacter: UUID? = null,
    var otaNotifyCharacter: UUID? = null,
    var otaControlCharacter: UUID? = null,
    var mtu: Int = DeviceConstants.D_MTU_LENGTH,//优化固件升级根据通道特征
    var deviceProtocol: Int = 0,// 1麦瑞克   2FTMS  3智健  4柏群   6 筋膜枪
    var bleProfile: BleGattProfile? = null,//设备特征值
    var hasHeartRate: Boolean = false,//需要心跳
    var deviceOtaType: Int = 0, //1:泰凌威 智建设备；2:博通; 3:DFU 4:新向远 5富芮坤 6 凌思威
    var modelNumber: String = "",//ota modelId
    var modelRevision: String = "",//ota version
) {
    constructor(
        deviceType: String = "",
        address: String = "",
        serviceUUID: UUID,
        characterNotify: UUID,
    ) : this(address,
        deviceType = deviceType,
        serviceUUId = serviceUUID,
        characterNotify = characterNotify)
}
