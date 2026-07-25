package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import java.util.UUID

internal class SacrificeUnlockState {

    private val unlocksBoughtByPlayer: MutableMap<UUID, Int> = HashMap()
    private val unlockMaskByPlayer: MutableMap<UUID, Int> = HashMap()

    fun getUnlocksBought(playerId: UUID?): Int {
        if (playerId == null) {
            return 0
        }
        if (unlockMaskByPlayer.containsKey(playerId)) {
            return Integer.bitCount(getUnlockMask(playerId))
        }
        return unlocksBoughtByPlayer[playerId]
            ?.coerceIn(0, SheepMergeManager.SACRIFICE_UNLOCK_MAX)
            ?: 0
    }

    fun getUnlockMask(playerId: UUID?): Int {
        if (playerId == null) {
            return 0
        }
        return normalizeMask(unlockMaskByPlayer[playerId] ?: 0)
    }

    fun hasUnlock(playerId: UUID?, unlockId: Int): Boolean {
        if (playerId == null || unlockId <= 0) {
            return false
        }
        val unlockBit = getUnlockBit(unlockId)
        if (unlockBit == 0) {
            return false
        }
        if (unlockMaskByPlayer.containsKey(playerId)) {
            return (getUnlockMask(playerId) and unlockBit) != 0
        }
        return getUnlocksBought(playerId) >= unlockId
    }

    fun isActive(playerId: UUID?, unlockId: Int): Boolean {
        return hasUnlock(playerId, unlockId)
    }

    fun statusLine(player: Player?, unlockId: Int): String {
        if (player == null) {
            return "LOCKED"
        }
        val playerId = player.uniqueId
        if (!hasUnlock(playerId, unlockId)) {
            return "LOCKED"
        }
        return "ACTIVE"
    }

    fun recordPurchase(playerId: UUID?, unlockId: Int) {
        val unlockBit = getUnlockBit(unlockId)
        if (playerId == null || unlockBit == 0) {
            return
        }
        unlocksBoughtByPlayer[playerId] = getUnlocksBought(playerId) + 1
        unlockMaskByPlayer[playerId] = getUnlockMask(playerId) or unlockBit
    }

    fun remove(playerId: UUID?) {
        if (playerId == null) {
            return
        }
        unlocksBoughtByPlayer.remove(playerId)
        unlockMaskByPlayer.remove(playerId)
    }

    fun clear() {
        unlocksBoughtByPlayer.clear()
        unlockMaskByPlayer.clear()
    }

    fun getTrackedPlayerIds(): Set<UUID> {
        val playerIds = HashSet<UUID>()
        playerIds.addAll(unlocksBoughtByPlayer.keys)
        playerIds.addAll(unlockMaskByPlayer.keys)
        return playerIds
    }

    fun saveTo(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        dataConfig.set("sacrificeUnlocksBought", null)
        dataConfig.set("sacrificeUnlockMask", null)

        for ((key, value) in unlocksBoughtByPlayer) {
            dataConfig.set(
                "sacrificeUnlocksBought.$key",
                value.coerceIn(0, SheepMergeManager.SACRIFICE_UNLOCK_MAX)
            )
        }
        for ((key, value) in unlockMaskByPlayer) {
            dataConfig.set("sacrificeUnlockMask.$key", normalizeMask(value))
        }
    }

    fun loadFrom(dataConfig: FileConfiguration?) {
        clear()
        if (dataConfig == null) {
            return
        }

        val boughtSection = dataConfig.getConfigurationSection("sacrificeUnlocksBought")
        if (boughtSection != null) {
            for (key in boughtSection.getKeys(false)) {
                try {
                    val uuid = UUID.fromString(key)
                    unlocksBoughtByPlayer[uuid] = dataConfig.getInt("sacrificeUnlocksBought.$key", 0)
                        .coerceIn(0, SheepMergeManager.SACRIFICE_UNLOCK_MAX)
                } catch (_: IllegalArgumentException) {
                    // Ignore invalid UUIDs.
                }
            }
        }

        val maskSection = dataConfig.getConfigurationSection("sacrificeUnlockMask")
        if (maskSection != null) {
            for (key in maskSection.getKeys(false)) {
                try {
                    val uuid = UUID.fromString(key)
                    val mask = normalizeMask(dataConfig.getInt("sacrificeUnlockMask.$key", 0))
                    unlockMaskByPlayer[uuid] = mask
                    unlocksBoughtByPlayer[uuid] = Integer.bitCount(mask)
                } catch (_: IllegalArgumentException) {
                    // Ignore invalid UUIDs.
                }
            }
        }

        for ((key, value) in unlocksBoughtByPlayer) {
            if (!unlockMaskByPlayer.containsKey(key)) {
                unlockMaskByPlayer[key] = firstUnlockBits(value)
            }
        }
    }

    private fun getUnlockBit(unlockId: Int): Int {
        if (unlockId <= 0 || unlockId > SheepMergeManager.SACRIFICE_UNLOCK_MAX) {
            return 0
        }
        return 1 shl (unlockId - 1)
    }

    private fun normalizeMask(mask: Int): Int {
        val allUnlocksMask = (1 shl SheepMergeManager.SACRIFICE_UNLOCK_MAX) - 1
        return mask and allUnlocksMask
    }

    private fun firstUnlockBits(unlocksBought: Int): Int {
        val clamped = unlocksBought.coerceIn(0, SheepMergeManager.SACRIFICE_UNLOCK_MAX)
        if (clamped <= 0) {
            return 0
        }
        return (1 shl clamped) - 1
    }
}
