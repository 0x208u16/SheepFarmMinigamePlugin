package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import java.math.BigInteger
import java.util.UUID
import java.util.function.Consumer
import java.util.function.ToIntFunction

internal object SheepSacrificeProgression {

    private val unlockCostMultiplier = BigInteger.valueOf(1000L)
    private val sacrificePointsByPlayer: MutableMap<UUID, BigInteger> = HashMap()
    private val sacrificeUnlockState = SacrificeUnlockState()
    private val totalSacrificeUnlocksPurchasedByPlayer: MutableMap<UUID, Int> = HashMap()

    @JvmStatic
    fun getPoints(player: Player?): BigInteger = getPoints(player?.uniqueId)

    @JvmStatic
    fun getPoints(playerId: UUID?): BigInteger {
        return playerId?.let { sacrificePointsByPlayer[it]?.max(BigInteger.ZERO) } ?: BigInteger.ZERO
    }

    @JvmStatic
    fun getUnlocksBought(player: Player?): Int = getUnlocksBought(player?.uniqueId)

    @JvmStatic
    fun getUnlocksBought(playerId: UUID?): Int = sacrificeUnlockState.getUnlocksBought(playerId)

    @JvmStatic
    fun getUnlockMask(playerId: UUID?): Int = sacrificeUnlockState.getUnlockMask(playerId)

    @JvmStatic
    fun isUnlockActive(playerId: UUID?, unlockId: Int): Boolean {
        return sacrificeUnlockState.isActive(playerId, unlockId)
    }

    @JvmStatic
    fun getUnlockStatusLine(player: Player?, unlockId: Int): String {
        return sacrificeUnlockState.statusLine(player, unlockId)
    }

    @JvmStatic
    fun hasUnlock(player: Player?, unlockId: Int): Boolean {
        return player != null && hasUnlock(player.uniqueId, unlockId)
    }

    @JvmStatic
    fun hasUnlock(playerId: UUID?, unlockId: Int): Boolean {
        return sacrificeUnlockState.hasUnlock(playerId, unlockId)
    }

    @JvmStatic
    fun getUnlockCost(player: Player?): BigInteger {
        return player?.let { getUnlockCost(it.uniqueId) } ?: BigInteger.ONE
    }

    @JvmStatic
    fun getUnlockCost(playerId: UUID?): BigInteger {
        return unlockCostMultiplier.pow(getUnlocksBought(playerId).coerceAtLeast(0))
    }

    @JvmStatic
    fun addPoints(
        playerId: UUID?,
        amount: BigInteger?,
        multiplierProvider: ToIntFunction<Player?>
    ) {
        if (playerId == null || amount == null || amount.signum() <= 0) {
            return
        }
        val multiplier = multiplierProvider.applyAsInt(Bukkit.getPlayer(playerId))
        sacrificePointsByPlayer[playerId] = getPoints(playerId)
            .add(amount.multiply(BigInteger.valueOf(multiplier.toLong())))
    }

    @JvmStatic
    fun getSpentPoints(unlocksBought: Int): BigInteger {
        var total = BigInteger.ZERO
        for (unlockIndex in 0 until unlocksBought.coerceIn(0, SheepMergeManager.SACRIFICE_UNLOCK_MAX)) {
            total = total.add(unlockCostMultiplier.pow(unlockIndex))
        }
        return total
    }

    @JvmStatic
    fun getValueForSheep(sheep: Sheep?): BigInteger {
        if (sheep == null || !sheep.isValid || sheep.isDead) {
            return BigInteger.ZERO
        }
        val tier = SheepMergeManager.getSheepTier(sheep) ?: return BigInteger.ZERO
        var effectiveTier = tier.level.coerceAtLeast(0)
        if (tier == SheepTier.RAINBOW) {
            effectiveTier = (SheepTier.RAINBOW.level + SheepMergeManager.getRainbowTier(sheep) - 1)
                .coerceAtLeast(0)
        }
        return BigInteger.TWO.pow(effectiveTier)
    }

    @JvmStatic
    fun sacrificeAllSheepForPlayer(
        player: Player?,
        multiplierProvider: ToIntFunction<Player?>,
        saveData: Runnable
    ): BigInteger {
        if (player == null || !SheepMergeManager.isSheepFarmWorld(player.world)) {
            return BigInteger.ZERO
        }
        if (!SheepMergeManager.isFarmOwner(player, player.world)) {
            return BigInteger.ZERO
        }
        var gained = BigInteger.ZERO
        val world = player.world
        for (sheep in world.getEntitiesByClass(Sheep::class.java)) {
            gained = gained.add(getValueForSheep(sheep))
            if (sheep.isValid) {
                sheep.remove()
            }
        }
        SheepMergeManager.refreshLiveSheepCount(world)
        if (gained.signum() > 0) {
            addPoints(player.uniqueId, gained, multiplierProvider)
            saveData.run()
        }
        return gained
    }

    @JvmStatic
    fun tryBuyUnlock(
        player: Player?,
        unlockId: Int,
        saveData: Runnable,
        evaluateAchievements: Consumer<Player>
    ): Boolean {
        if (player == null || unlockId !in 1..SheepMergeManager.SACRIFICE_UNLOCK_MAX) {
            return false
        }
        val playerId = player.uniqueId
        if (hasUnlock(playerId, unlockId)) {
            return false
        }
        val current = getUnlocksBought(playerId)
        if (current >= SheepMergeManager.SACRIFICE_UNLOCK_MAX || unlockId != current + 1) {
            return false
        }
        val cost = getUnlockCost(playerId)
        val points = getPoints(playerId)
        if (points < cost) {
            return false
        }
        sacrificePointsByPlayer[playerId] = points.subtract(cost)
        sacrificeUnlockState.recordPurchase(playerId, unlockId)
        val lifetimePurchased = totalSacrificeUnlocksPurchasedByPlayer.getOrDefault(playerId, 0)
        totalSacrificeUnlocksPurchasedByPlayer[playerId] = if (lifetimePurchased == Int.MAX_VALUE) {
            Int.MAX_VALUE
        } else {
            lifetimePurchased + 1
        }
        saveData.run()
        evaluateAchievements.accept(player)
        return true
    }

    @JvmStatic
    fun adminGivePoints(player: Player?, amount: BigInteger?, saveData: Runnable) {
        if (player == null || amount == null || amount.signum() == 0) {
            return
        }
        sacrificePointsByPlayer[player.uniqueId] = getPoints(player.uniqueId).add(amount).max(BigInteger.ZERO)
        saveData.run()
    }

    @JvmStatic
    fun getTotalUnlocksPurchased(playerId: UUID?): Int {
        return playerId?.let { totalSacrificeUnlocksPurchasedByPlayer.getOrDefault(it, 0).coerceAtLeast(0) } ?: 0
    }

    @JvmStatic
    fun removeProgress(playerId: UUID?) {
        if (playerId == null) {
            return
        }
        sacrificePointsByPlayer.remove(playerId)
        sacrificeUnlockState.remove(playerId)
    }

    @JvmStatic
    fun resetPlayer(playerId: UUID?) {
        removeProgress(playerId)
        if (playerId != null) {
            totalSacrificeUnlocksPurchasedByPlayer.remove(playerId)
        }
    }

    @JvmStatic
    fun clear() {
        sacrificePointsByPlayer.clear()
        sacrificeUnlockState.clear()
        totalSacrificeUnlocksPurchasedByPlayer.clear()
    }

    @JvmStatic
    fun getUnlockTrackedPlayerIds(): Set<UUID> = sacrificeUnlockState.getTrackedPlayerIds()

    @JvmStatic
    fun clearPersistedKeys(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        dataConfig.set("sacrificePoints", null)
        dataConfig.set("sacrificeUnlocksBought", null)
        dataConfig.set("sacrificeUnlockMask", null)
        dataConfig.set("sacrificeUnlockPendingMask", null)
        dataConfig.set("totalSacrificeUnlocksPurchased", null)
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        for ((playerId, points) in sacrificePointsByPlayer) {
            dataConfig.set("sacrificePoints.$playerId", points.toString())
        }
        sacrificeUnlockState.saveTo(dataConfig)
        for ((playerId, totalPurchased) in totalSacrificeUnlocksPurchasedByPlayer) {
            dataConfig.set("totalSacrificeUnlocksPurchased.$playerId", totalPurchased.coerceAtLeast(0))
        }
    }

    @JvmStatic
    fun loadFrom(dataConfig: FileConfiguration?) {
        clear()
        if (dataConfig == null) {
            return
        }
        dataConfig.getConfigurationSection("sacrificePoints")?.getKeys(false)?.forEach { key ->
            try {
                val playerId = UUID.fromString(key)
                val path = "sacrificePoints.$key"
                val raw = dataConfig.getString(path, null)
                val parsed = if (!raw.isNullOrBlank()) {
                    BigInteger(raw.trim())
                } else {
                    BigInteger.valueOf(dataConfig.getLong(path, 0L).coerceAtLeast(0L))
                }
                sacrificePointsByPlayer[playerId] = parsed.max(BigInteger.ZERO)
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs and decimal values.
            }
        }
        sacrificeUnlockState.loadFrom(dataConfig)
        dataConfig.getConfigurationSection("totalSacrificeUnlocksPurchased")?.getKeys(false)?.forEach { key ->
            try {
                val playerId = UUID.fromString(key)
                totalSacrificeUnlocksPurchasedByPlayer[playerId] =
                    dataConfig.getInt("totalSacrificeUnlocksPurchased.$key", 0).coerceAtLeast(0)
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
    }
}