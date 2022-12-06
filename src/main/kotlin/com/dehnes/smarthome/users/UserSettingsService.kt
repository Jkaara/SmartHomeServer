package com.dehnes.smarthome.users

import com.dehnes.smarthome.utils.PersistenceService
import mu.KotlinLogging

class UserSettingsService(private val persistenceService: PersistenceService) {

    private val logger = KotlinLogging.logger { }

    fun canUserRead(user: String?, userRole: UserRole): Boolean {
        val result = getUserAccessLevel(user, userRole) in listOf(Level.read, Level.readWrite)
        logger.info { "canUserRead: user=$user userRole=$userRole result=$result" }
        return result
    }

    fun canUserWrite(user: String?, userRole: UserRole): Boolean {
        val result = getUserAccessLevel(user, userRole) == Level.readWrite
        logger.info { "canUserWrite: user=$user userRole=$userRole result=$result" }
        return result
    }

    private fun getUserAccessLevel(user: String?, userRole: UserRole) = if (user == SystemUser) {
        Level.readWrite
    } else {
        Level.valueOf(persistenceService["UserSettingsService.auth.${userKey(user)}.$userRole", userRole.defaultLevel.name]!!)
    }

    fun getUserSettings(user: String?) = UserSettings(
        authorization = UserRole.values().associateWith { getUserAccessLevel(user, it) }
    ).apply {
        logger.info { "getUserSettings user=$user userSettings=$this" }
    }

    private fun userKey(user: String?) = user?.replace(".", "_") ?: "unknown"

}

const val SystemUser = "SystemUser"

data class UserSettings(
    val authorization: Map<UserRole, Level>,
)

enum class UserRole(
    val defaultLevel: Level
) {
    garageDoor(Level.readWrite),
    evCharging(Level.read),
    energyStorageSystem(Level.read),
    energyPricing(Level.read),
    heaterUnderFloor(Level.read),
    environmentSensors(Level.read),
    cameras(Level.read),
    recordings(Level.none),
    firmwareUpgrades(Level.none),
}

enum class Level {
    none,
    read,
    readWrite,
}