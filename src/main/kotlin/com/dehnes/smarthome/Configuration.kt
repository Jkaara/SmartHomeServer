package com.dehnes.smarthome

import com.dehnes.smarthome.api.dtos.EvChargingStationClient
import com.dehnes.smarthome.api.dtos.ProximityPilotAmps
import com.dehnes.smarthome.datalogging.InfluxDBClient
import com.dehnes.smarthome.energy_pricing.tibber.TibberService
import com.dehnes.smarthome.ev_charging.*
import com.dehnes.smarthome.garage_door.GarageDoorService
import com.dehnes.smarthome.heating.UnderFloorHeaterService
import com.dehnes.smarthome.rf433.Rf433Client
import com.dehnes.smarthome.room_sensors.ChipCap2SensorService
import com.dehnes.smarthome.utils.PersistenceService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.Clock
import java.util.concurrent.Executors
import kotlin.reflect.KClass

class Configuration {
    private var beans = mutableMapOf<KClass<*>, Any>()

    fun init() {

        val executorService = Executors.newCachedThreadPool()
        val objectMapper = jacksonObjectMapper()

        val influxDBClient = InfluxDBClient(objectMapper, System.getProperty("DST_HOST"))
        val persistenceService = PersistenceService()

        val serialConnection = Rf433Client(executorService, System.getProperty("DST_HOST"))
        serialConnection.start()

        val garageDoorService = GarageDoorService(serialConnection, influxDBClient)
        val chipCap2SensorService = ChipCap2SensorService(influxDBClient)

        val tibberService = TibberService(
            Clock.systemDefaultZone(),
            objectMapper,
            persistenceService,
            influxDBClient,
            executorService
        )
        tibberService.start()

        val heaterService = UnderFloorHeaterService(
            serialConnection,
            executorService,
            persistenceService,
            influxDBClient,
            tibberService
        )
        heaterService.start()

        val evChargingStationConnection = EVChargingStationConnection(9091, executorService, persistenceService)
        evChargingStationConnection.start()

        val evChargingService = EvChargingService(
            evChargingStationConnection,
            executorService,
            tibberService,
            persistenceService,
            Clock.systemDefaultZone(),
            mapOf(
                PriorityLoadSharing::class.java.simpleName to PriorityLoadSharing(persistenceService)
            )
        )
        evChargingService.start()

        val firmwareUploadService = FirmwareUploadService(evChargingStationConnection)

        serialConnection.listeners.add(heaterService::onRfMessage)
        serialConnection.listeners.add(garageDoorService::handleIncoming)
        serialConnection.listeners.add(chipCap2SensorService::handleIncoming)

        /*
         *  a fake charging station for (webapp) development
         * TODO clean up
        evChargingService.onIncomingDataUpdate(
            EvChargingStationClient(
                "TeslaLader",
                "Tesla Lader",
                "127.0.0.1",
                9091,
                1,
                "unknown"
            ),
            DataResponse(
                false,
                100,
                PilotVoltage.Volt_12,
                ProximityPilotAmps.Amp32,
                231123,
                232456,
                233789,
                30256,
                29541,
                31154,
                -53,
                5000,
                emptyList()
            )
        )
         */

        beans[Rf433Client::class] = serialConnection
        beans[UnderFloorHeaterService::class] = heaterService
        beans[GarageDoorService::class] = garageDoorService
        beans[ObjectMapper::class] = objectMapper
        beans[EVChargingStationConnection::class] = evChargingStationConnection
        beans[FirmwareUploadService::class] = firmwareUploadService
        beans[EvChargingService::class] = evChargingService
    }

    fun <T> getBean(klass: KClass<*>): T {
        return (beans[klass] ?: error("No such bean for $klass")) as T
    }

}