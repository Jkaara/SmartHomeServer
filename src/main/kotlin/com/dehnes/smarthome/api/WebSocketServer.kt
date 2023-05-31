package com.dehnes.smarthome.api

import com.dehnes.smarthome.VideoBrowser
import com.dehnes.smarthome.api.dtos.*
import com.dehnes.smarthome.api.dtos.RequestType.*
import com.dehnes.smarthome.configuration
import com.dehnes.smarthome.datalogging.QuickStatsService
import com.dehnes.smarthome.energy_consumption.EnergyConsumptionService
import com.dehnes.smarthome.environment_sensors.EnvironmentSensorService
import com.dehnes.smarthome.ev_charging.EvChargingService
import com.dehnes.smarthome.ev_charging.FirmwareUploadService
import com.dehnes.smarthome.users.UserSettingsService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.websocket.CloseReason
import jakarta.websocket.Endpoint
import jakarta.websocket.EndpointConfig
import jakarta.websocket.Session
import mu.KotlinLogging
import java.io.Closeable
import java.util.*

// one instance per sessions
class WebSocketServer : Endpoint() {

    private val instanceId = UUID.randomUUID().toString()

    private val objectMapper = configuration.getBean<ObjectMapper>(ObjectMapper::class)
    private val logger = KotlinLogging.logger { }
    private val subscriptions = mutableMapOf<String, Subscription<*>>()
    private val evChargingService =
        configuration.getBean<EvChargingService>(EvChargingService::class)
    private val firmwareUploadService =
        configuration.getBean<FirmwareUploadService>(FirmwareUploadService::class)
    private val loRaSensorBoardService =
        configuration.getBean<EnvironmentSensorService>(EnvironmentSensorService::class)
    private val videoBrowser =
        configuration.getBean<VideoBrowser>(VideoBrowser::class)
    private val quickStatsService =
        configuration.getBean<QuickStatsService>(QuickStatsService::class)
    private val userSettingsService =
        configuration.getBean<UserSettingsService>(UserSettingsService::class)
    private val energyConsumptionService =
        configuration.getBean<EnergyConsumptionService>(EnergyConsumptionService::class)

    override fun onOpen(sess: Session, p1: EndpointConfig?) {
        logger.info("$instanceId Socket connected: $sess")
        sess.addMessageHandler(String::class.java) { msg -> onWebSocketText(sess, msg) }
    }

    override fun onClose(session: Session, closeReason: CloseReason) {
        subscriptions.values.toList().forEach { it.close() }
        logger.info("$instanceId Socket Closed: $closeReason")
    }

    override fun onError(session: Session?, cause: Throwable?) {
        logger.warn("$instanceId ", cause)
    }

    fun onWebSocketText(argSession: Session, argMessage: String) {
        val userEmail = argSession.userProperties["userEmail"] as String?
        val websocketMessage: WebsocketMessage = objectMapper.readValue(argMessage)

        if (websocketMessage.type != WebsocketMessageType.rpcRequest) {
            return
        }

        val rpcRequest = websocketMessage.rpcRequest!!
        val response: RpcResponse = when (rpcRequest.type) {
            userSettings -> RpcResponse(userSettings = userSettingsService.getUserSettings(userEmail))
            readAllUserSettings -> RpcResponse(allUserSettings = userSettingsService.getAllUserSettings(userEmail))
            writeUserSettings -> {
                userSettingsService.handleWrite(userEmail, rpcRequest.writeUserSettings!!)
                RpcResponse(allUserSettings = userSettingsService.getAllUserSettings(userEmail))
            }

            quickStats -> RpcResponse(quickStatsResponse = quickStatsService.getStats())
            energyConsumptionQuery -> RpcResponse(energyConsumptionData = energyConsumptionService.report(rpcRequest.energyConsumptionQuery!!))
            subscribe -> {
                val subscribe = rpcRequest.subscribe!!
                val subscriptionId = subscribe.subscriptionId

                val existing = subscriptions[subscriptionId]
                if (existing == null) {
                    val sub = when (subscribe.type) {

                        SubscriptionType.evChargingStationEvents -> EvChargingStationSubscription(
                            subscriptionId,
                            argSession,
                        ).apply {
                            evChargingService.addListener(userEmail, subscriptionId, this::onEvent)
                        }

                        SubscriptionType.environmentSensorEvents -> EnvironmentSensorSubscription(
                            subscriptionId,
                            argSession,
                        ).apply {
                            loRaSensorBoardService.addListener(userEmail, subscriptionId, this::onEvent)
                        }
                    }

                    subscriptions.put(subscriptionId, sub)?.close()
                    logger.info { "$instanceId New subscription id=$subscriptionId type=${subscribe.type}" }
                } else {
                    logger.info { "$instanceId re-subscription id=$subscriptionId type=${subscribe.type}" }
                }

                RpcResponse(subscriptionCreated = true)
            }

            unsubscribe -> {
                val subscriptionId = rpcRequest.unsubscribe!!.subscriptionId
                subscriptions.remove(subscriptionId)?.close()
                logger.info { "$instanceId Removed subscription id=$subscriptionId" }
                RpcResponse(subscriptionRemoved = true)
            }

            evChargingStationRequest -> RpcResponse(
                evChargingStationResponse = evChargingStationRequest(
                    userEmail,
                    rpcRequest.evChargingStationRequest!!
                )
            )

            environmentSensorRequest -> RpcResponse(environmentSensorResponse = environmentSensorRequest(userEmail, rpcRequest.environmentSensorRequest!!))
            RequestType.videoBrowser -> RpcResponse(videoBrowserResponse = videoBrowser.rpc(userEmail, rpcRequest.videoBrowserRequest!!))
        }

        argSession.basicRemote.sendText(
            objectMapper.writeValueAsString(
                WebsocketMessage(
                    websocketMessage.id,
                    WebsocketMessageType.rpcResponse,
                    null,
                    response,
                    null
                )
            )
        )
    }

    private fun environmentSensorRequest(userEmail: String?, request: EnvironmentSensorRequest) = when (request.type) {
        EnvironmentSensorRequestType.getAllEnvironmentSensorData -> loRaSensorBoardService.getEnvironmentSensorResponse(userEmail)
        EnvironmentSensorRequestType.scheduleFirmwareUpgrade -> {
            loRaSensorBoardService.firmwareUpgrade(userEmail, request.sensorId!!, true)
            loRaSensorBoardService.getEnvironmentSensorResponse(userEmail)
        }

        EnvironmentSensorRequestType.cancelFirmwareUpgrade -> {
            loRaSensorBoardService.firmwareUpgrade(userEmail, request.sensorId!!, false)
            loRaSensorBoardService.getEnvironmentSensorResponse(userEmail)
        }

        EnvironmentSensorRequestType.scheduleTimeAdjustment -> {
            loRaSensorBoardService.timeAdjustment(userEmail, request.sensorId, true)
            loRaSensorBoardService.getEnvironmentSensorResponse(userEmail)
        }

        EnvironmentSensorRequestType.cancelTimeAdjustment -> {
            loRaSensorBoardService.timeAdjustment(userEmail, request.sensorId, false)
            loRaSensorBoardService.getEnvironmentSensorResponse(userEmail)
        }

        EnvironmentSensorRequestType.scheduleReset -> {
            loRaSensorBoardService.configureReset(userEmail, request.sensorId, true)
            loRaSensorBoardService.getEnvironmentSensorResponse(userEmail)
        }

        EnvironmentSensorRequestType.cancelReset -> {
            loRaSensorBoardService.configureReset(userEmail, request.sensorId, false)
            loRaSensorBoardService.getEnvironmentSensorResponse(userEmail)
        }

        EnvironmentSensorRequestType.adjustSleepTimeInSeconds -> {
            loRaSensorBoardService.adjustSleepTimeInSeconds(userEmail, request.sensorId!!, request.sleepTimeInSecondsDelta!!)
            loRaSensorBoardService.getEnvironmentSensorResponse(userEmail)
        }

        EnvironmentSensorRequestType.uploadFirmware -> {
            loRaSensorBoardService.setFirmware(userEmail, request.firmwareFilename!!, request.firmwareBased64Encoded!!)
            loRaSensorBoardService.getEnvironmentSensorResponse(userEmail)
        }
    }

    private fun evChargingStationRequest(user: String?, request: EvChargingStationRequest) = when (request.type) {
        EvChargingStationRequestType.uploadFirmwareToClient -> EvChargingStationResponse(
            uploadFirmwareToClientResult = firmwareUploadService.uploadVersion(
                user = user,
                clientId = request.clientId!!,
                firmwareBased64Encoded = request.firmwareBased64Encoded!!
            ),
            chargingStationsDataAndConfig = evChargingService.getChargingStationsDataAndConfig(user)
        )

        EvChargingStationRequestType.getChargingStationsDataAndConfig -> EvChargingStationResponse(
            chargingStationsDataAndConfig = evChargingService.getChargingStationsDataAndConfig(user)
        )

        EvChargingStationRequestType.setLoadSharingPriority -> EvChargingStationResponse(
            configUpdated = evChargingService.setPriorityFor(
                user,
                request.clientId!!,
                request.newLoadSharingPriority!!
            ),
            chargingStationsDataAndConfig = evChargingService.getChargingStationsDataAndConfig(user)
        )

        EvChargingStationRequestType.setMode -> EvChargingStationResponse(
            configUpdated = evChargingService.updateMode(user, request.clientId!!, request.newMode!!),
            chargingStationsDataAndConfig = evChargingService.getChargingStationsDataAndConfig(user)
        )

        EvChargingStationRequestType.setChargeRateLimit -> EvChargingStationResponse(
            configUpdated = evChargingService.setChargeRateLimitFor(
                user,
                request.clientId!!,
                request.chargeRateLimit!!
            ),
            chargingStationsDataAndConfig = evChargingService.getChargingStationsDataAndConfig(user)
        )
    }


    inner class QuickStatsSubscription(
        subscriptionId: String,
        sess: Session,
    ) : Subscription<QuickStatsResponse>(subscriptionId, sess) {
        override fun onEvent(e: QuickStatsResponse) {
            logger.info("$instanceId onEvent GarageStatusSubscription $subscriptionId ")
            sess.basicRemote.sendText(
                objectMapper.writeValueAsString(
                    WebsocketMessage(
                        UUID.randomUUID().toString(),
                        WebsocketMessageType.notify,
                        notify = Notify(
                            subscriptionId,
                            null,
                            null,
                            null,
                            e,
                        )
                    )
                )
            )
        }

        override fun close() {
            quickStatsService.listeners.remove(subscriptionId)
            subscriptions.remove(subscriptionId)
        }
    }

    inner class EnvironmentSensorSubscription(
        subscriptionId: String,
        sess: Session,
    ) : Subscription<EnvironmentSensorEvent>(subscriptionId, sess) {
        override fun onEvent(e: EnvironmentSensorEvent) {
            logger.info("$instanceId onEvent EnvironmentSensorSubscription $subscriptionId ")
            sess.basicRemote.sendText(
                objectMapper.writeValueAsString(
                    WebsocketMessage(
                        UUID.randomUUID().toString(),
                        WebsocketMessageType.notify,
                        notify = Notify(
                            subscriptionId,
                            null,
                            null,
                            e,
                            null
                        )
                    )
                )
            )
        }

        override fun close() {
            loRaSensorBoardService.removeListener(subscriptionId)
            subscriptions.remove(subscriptionId)
        }
    }

    inner class EvChargingStationSubscription(
        subscriptionId: String,
        sess: Session,
    ) : Subscription<EvChargingEvent>(subscriptionId, sess) {
        override fun onEvent(e: EvChargingEvent) {
            logger.info("$instanceId onEvent EvChargingStationSubscription $subscriptionId ")
            sess.basicRemote.sendText(
                objectMapper.writeValueAsString(
                    WebsocketMessage(
                        UUID.randomUUID().toString(),
                        WebsocketMessageType.notify,
                        notify = Notify(
                            subscriptionId,
                            null,
                            e,

                            null,
                            null,
                        )
                    )
                )
            )
        }

        override fun close() {
            evChargingService.removeListener(subscriptionId)
            subscriptions.remove(subscriptionId)
        }
    }
}

abstract class Subscription<E>(
    val subscriptionId: String,
    val sess: Session,
) : Closeable {
    abstract fun onEvent(e: E)
}

