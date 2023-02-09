package com.cc.control.bean

/**
 * @Author : cc
 * @Date : on 2022-04-13 16:14.
 * @Description : 训练数据
 */
class DeviceTrainBean(
    val trainingType: Int = 0, //	训练模式：0-无任何模式，1-定数练，2-定时练
    var type: Int = 0, //	训练类型：1-课程训练，2-自由训练
    var equipmentId: String = "", //	设备ID
    var modelId: String = "", //	型号ID
    var courseId: String = "", //
    var playTime: Int = 0, //
    var deviceTrainBO: DeviceTrainBO? = null, //	型课程ID	号ID
) : Cloneable {
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        val deviceTrainBean = super.clone() as DeviceTrainBean
        deviceTrainBean.deviceTrainBO = deviceTrainBean.deviceTrainBO?.clone() as DeviceTrainBO
        return deviceTrainBean
    }

    //设备训练相关数据
    class DeviceTrainBO(
        var linkId: Long = 0, //小节ID
        var speed: Float = 0f, //速度
        var avgSpeed: Float = 0f, //平均速度
        var distance: Int = 0, //距离：米
        var spm: Int = 0, //踏频/桨频
        var count: Int = 0, //总踏数/总桨数/总个数/总圈数
        var avgSpm: Float = 0f, //平均踏频/平均桨频
        var drag: Int = 0, //阻力
        var power: Float = 0f, //功率
        var avgPower: Float = 0f, //平均功率
        var energy: Float = 0f, //消耗：kcal
        var deviceTime: Long = 0,//设备时长：秒
        var deviceRate: Int = 0, //设备心率
        var rate: Int = 0,//心率设备心率
        var gradient: Int = 0, //坡度
        var grade: Int = 0, //挡位
        var electric: Int = 0, //电量
        var timestamp: Long = 0,
        var status: Int = -1, //设备状态用来规避x1彩屏跑步机暂停也会给数据;
        var skippingModel: Int = -1,
        var deviceTimestamp: Long = 0,//设备收到数据的时间戳
    ) : Cloneable {
        constructor(deviceTime: Long, timestamp: Long) : this(deviceTime = deviceTime,
            timestamp = timestamp,
            skippingModel = -1)

        constructor(energy: Float, deviceTime: Long, timestamp: Long) : this(energy = energy,
            deviceTime = deviceTime,
            timestamp = timestamp,
            skippingModel = -1)

        constructor(
            energy: Float,
            deviceTime: Long,
            timestamp: Long,
            distance: Int,
            count: Int,
        ) : this(energy = energy,
            deviceTime = deviceTime,
            timestamp = timestamp,
            distance = distance,
            count = count,
            skippingModel = -1)

        public override fun clone(): Any {
            return try {
                super.clone()
            } catch (e: CloneNotSupportedException) {
                DeviceTrainBO(energy = energy,
                    deviceTime = deviceTime,
                    timestamp = timestamp,
                    distance = distance,
                    count = count)
            }
        }

        override fun toString(): String {
            return super.toString()
        }
    }
}