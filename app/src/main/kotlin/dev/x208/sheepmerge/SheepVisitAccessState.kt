package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import java.util.UUID

object SheepVisitAccessState {

    private val farmVisitEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val farmVisitBlockedUsersByPlayer: MutableMap<UUID, MutableSet<UUID>> = HashMap()
    private val visitAccessPageByPlayer: MutableMap<UUID, Int> = HashMap()

    @JvmStatic
    fun isFarmVisitable(ownerId: UUID?): Boolean {
        return ownerId != null && farmVisitEnabledByPlayer.getOrDefault(ownerId, true)
    }

    @JvmStatic
    fun toggleFarmVisitable(owner: Player?): Boolean {
        if (owner == null) {
            return false
        }
        val ownerId = owner.uniqueId
        val enabled = !farmVisitEnabledByPlayer.getOrDefault(ownerId, true)
        farmVisitEnabledByPlayer[ownerId] = enabled
        return enabled
    }

    @JvmStatic
    fun getBlockedFarmVisitors(ownerId: UUID?): Set<UUID> {
        if (ownerId == null) {
            return emptySet()
        }
        return farmVisitBlockedUsersByPlayer.getOrPut(ownerId) { LinkedHashSet() }
    }

    @JvmStatic
    fun isFarmVisitorBlocked(ownerId: UUID?, visitorId: UUID?): Boolean {
        return ownerId != null && visitorId != null && getBlockedFarmVisitors(ownerId).contains(visitorId)
    }

    @JvmStatic
    fun isFarmVisitorBlocked(owner: Player?, visitor: Player?): Boolean {
        return owner != null && visitor != null && isFarmVisitorBlocked(owner.uniqueId, visitor.uniqueId)
    }

    @JvmStatic
    fun toggleFarmVisitorBlocked(owner: Player?, visitorId: UUID?): Boolean {
        if (owner == null || visitorId == null || visitorId == owner.uniqueId) {
            return false
        }
        val blockedVisitors = farmVisitBlockedUsersByPlayer.getOrPut(owner.uniqueId) { LinkedHashSet() }
        if (blockedVisitors.remove(visitorId)) {
            return false
        }
        blockedVisitors.add(visitorId)
        return true
    }

    @JvmStatic
    fun getBlockedFarmVisitorCount(ownerId: UUID?): Int {
        return if (ownerId == null) 0 else getBlockedFarmVisitors(ownerId).size
    }

    @JvmStatic
    fun setVisitAccessPage(ownerId: UUID?, page: Int) {
        if (ownerId != null) {
            visitAccessPageByPlayer[ownerId] = page
        }
    }

    @JvmStatic
    fun getVisitAccessPage(ownerId: UUID?): Int {
        return if (ownerId == null) 0 else visitAccessPageByPlayer.getOrDefault(ownerId, 0).coerceAtLeast(0)
    }

    @JvmStatic
    fun resetPlayer(playerId: UUID?) {
        if (playerId == null) {
            return
        }
        farmVisitEnabledByPlayer.remove(playerId)
        farmVisitBlockedUsersByPlayer.remove(playerId)
        visitAccessPageByPlayer.remove(playerId)
    }

    @JvmStatic
    fun clear() {
        farmVisitEnabledByPlayer.clear()
        farmVisitBlockedUsersByPlayer.clear()
        visitAccessPageByPlayer.clear()
    }

    @JvmStatic
    fun clearPersistedKeys(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        dataConfig.set("farmVisitEnabled", null)
        dataConfig.set("farmVisitBlockedUsers", null)
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        for ((ownerId, enabled) in farmVisitEnabledByPlayer) {
            dataConfig.set("farmVisitEnabled.$ownerId", enabled)
        }
        for ((ownerId, blockedUsers) in farmVisitBlockedUsersByPlayer) {
            if (blockedUsers.isEmpty()) {
                continue
            }
            val blockedIds = blockedUsers.mapNotNull { blockedId -> blockedId?.toString() }
            if (blockedIds.isNotEmpty()) {
                dataConfig.set("farmVisitBlockedUsers.$ownerId", blockedIds)
            }
        }
    }

    @JvmStatic
    fun loadFrom(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        dataConfig.getConfigurationSection("farmVisitEnabled")?.getKeys(false)?.forEach { key ->
            try {
                val ownerId = UUID.fromString(key)
                farmVisitEnabledByPlayer[ownerId] = dataConfig.getBoolean("farmVisitEnabled.$key", true)
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
        dataConfig.getConfigurationSection("farmVisitBlockedUsers")?.getKeys(false)?.forEach { key ->
            try {
                val ownerId = UUID.fromString(key)
                val loaded = dataConfig.getStringList("farmVisitBlockedUsers.$key")
                if (loaded.isEmpty()) {
                    return@forEach
                }
                val blocked = LinkedHashSet<UUID>()
                for (raw in loaded) {
                    if (raw == null || raw.isBlank()) {
                        continue
                    }
                    try {
                        blocked.add(UUID.fromString(raw))
                    } catch (_: IllegalArgumentException) {
                        // Ignore invalid UUIDs.
                    }
                }
                if (blocked.isNotEmpty()) {
                    farmVisitBlockedUsersByPlayer[ownerId] = blocked
                }
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
    }
}