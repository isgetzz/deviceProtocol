package com.cc.control.bean

import com.cc.control.protocol.DeviceConstants
import java.util.*

/**
 * @Author      : cc
 * @Date        : on 2022-02-16 10:22.
 * @isWriteNoRsp
 * @Description :固件升级信息
 */
data class DeviceOtaBean(
    //Model Number  型号
    var modelNumber: String = "",
    //Software 版本
    var softwareRevision: String = "",
    var address: String = "",
    //数据解析类型1:麦瑞克,2:FTMS,3:智健,4:柏群,5:FTMS+智健
    var communicationProtocol: Int = 0,
    //ota类型
    var otaType: Int = 0,
    var otaService: UUID? = null,
    var otaWriteCharacter: UUID? = null,
    var otaControlCharacter: UUID? = null, //控制特征值LSW
    var otaNotify: UUID? = null,//注册数据刷新通道
    var deviceType: String = "",
    var isWriteNoRsp: Boolean = false,//优化固件升级根据通道特征
    var mtu: Int = DeviceConstants.D_MTU_LENGTH,//优化固件升级根据通道特征
)