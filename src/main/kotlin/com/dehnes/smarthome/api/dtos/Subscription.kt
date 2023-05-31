package com.dehnes.smarthome.api.dtos

enum class SubscriptionType {
    evChargingStationEvents,
    environmentSensorEvents,

}

data class Subscribe(
    val subscriptionId: String,
    val type: SubscriptionType,
)

data class Unsubscribe(
    val subscriptionId: String
)

data class Notify(
    val subscriptionId: String,
    val underFloorHeaterStatus: UnderFloorHeaterResponse?,
    val evChargingStationEvent: EvChargingEvent?,
    val environmentSensorEvent: EnvironmentSensorEvent?,
    val quickStatsResponse: QuickStatsResponse?,
)
