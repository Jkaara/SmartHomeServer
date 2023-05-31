package com.dehnes.smarthome.api.dtos

import com.dehnes.smarthome.config.UserSettings
import com.dehnes.smarthome.energy_consumption.EnergyConsumptionData
import com.dehnes.smarthome.energy_consumption.EnergyConsumptionQuery

enum class RequestType {
    subscribe,
    unsubscribe,

    userSettings,

    evChargingStationRequest,
    environmentSensorRequest,
    videoBrowser,
    quickStats,
    energyConsumptionQuery,

    readAllUserSettings,
    writeUserSettings,
}

data class RpcRequest(
    val type: RequestType,
    val subscribe: Subscribe?,
    val unsubscribe: Unsubscribe?,

    val underFloorHeaterRequest: UnderFloorHeaterRequest?,
    val evChargingStationRequest: EvChargingStationRequest?,
    val environmentSensorRequest: EnvironmentSensorRequest?,
    val videoBrowserRequest: VideoBrowserRequest?,
    val energyConsumptionQuery: EnergyConsumptionQuery?,
    val writeUserSettings: WriteUserSettings?,
)

data class RpcResponse(
    val subscriptionCreated: Boolean? = null,
    val subscriptionRemoved: Boolean? = null,

    val underFloorHeaterResponse: UnderFloorHeaterResponse? = null,
    val evChargingStationResponse: EvChargingStationResponse? = null,
    val environmentSensorResponse: EnvironmentSensorResponse? = null,
    val videoBrowserResponse: VideoBrowserResponse? = null,
    val quickStatsResponse: QuickStatsResponse? = null,
    val userSettings: UserSettings? = null,
    val energyConsumptionData: EnergyConsumptionData? = null,
    val allUserSettings: Map<String, UserSettings>? = null,
)
