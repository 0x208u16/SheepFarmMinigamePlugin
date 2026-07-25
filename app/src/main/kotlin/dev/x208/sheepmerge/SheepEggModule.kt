package dev.x208.sheepmerge

import org.bukkit.entity.Player
import java.util.UUID

internal class SheepEggModule {

    private val nextEggTimestampByPlayer: MutableMap<UUID, Long> = HashMap()
    private val eggCountByPlayer: MutableMap<UUID, Int> = HashMap()
    private val savedLevels: MutableMap<UUID, Int> = HashMap()
    private val savedExpProgress: MutableMap<UUID, Float> = HashMap()

    fun tickEggDistribution(player: Player?) {
        if (player == null || !SheepMergeManager.isSheepFarmWorld(player.world)) {
            return
        }

        ensureEggCountInitialized(player)
        val playerId = player.uniqueId
        val now = System.currentTimeMillis()
        val next = nextEggTimestampByPlayer.computeIfAbsent(playerId) {
            now + SheepMergeManager.getEggIntervalSeconds(player) * 1000L
        }

        if (now < next) {
            updateEggHud(player)
            return
        }

        if (getEggCount(player) >= SheepMergeManager.getEggCap(player)) {
            nextEggTimestampByPlayer[playerId] = now + 2000L
            updateEggHud(player)
            return
        }

        addEggsInternal(player, 1, false)
        nextEggTimestampByPlayer[playerId] = now + SheepMergeManager.getEggIntervalSeconds(player) * 1000L
        updateEggHud(player)
    }

    fun addEggs(player: Player?, amount: Int) {
        addEggsInternal(player, amount, true)
    }

    fun resetEggTimer(player: Player?) {
        if (player == null) {
            return
        }
        ensureEggCountInitialized(player)
        nextEggTimestampByPlayer[player.uniqueId] =
            System.currentTimeMillis() + SheepMergeManager.getEggIntervalSeconds(player) * 1000L
        updateEggHud(player)
    }

    fun clearEggTimer(player: Player?) {
        if (player == null) {
            return
        }
        val playerId = player.uniqueId
        nextEggTimestampByPlayer.remove(playerId)
        eggCountByPlayer.remove(playerId)
        restoreSavedExperience(player)
    }

    fun clearRuntimeState(playerId: UUID?) {
        if (playerId == null) {
            return
        }
        nextEggTimestampByPlayer.remove(playerId)
        eggCountByPlayer.remove(playerId)
    }

    fun clearSavedExperienceCache() {
        savedLevels.clear()
        savedExpProgress.clear()
    }

    private fun addEggsInternal(player: Player?, amount: Int, updateHud: Boolean) {
        if (player == null || amount <= 0) {
            return
        }
        ensureEggCountInitialized(player)
        val playerId = player.uniqueId
        val capped = (getEggCount(player) + amount).coerceAtMost(SheepMergeManager.getEggCap(player))
        eggCountByPlayer[playerId] = capped
        if (updateHud) {
            updateEggHud(player)
        }
    }

    fun tryConsumeEgg(player: Player?): Boolean {
        if (player == null) {
            return false
        }
        ensureEggCountInitialized(player)
        val current = getEggCount(player)
        if (current <= 0) {
            return false
        }
        eggCountByPlayer[player.uniqueId] = current - 1
        updateEggHud(player)
        return true
    }

    private fun getEggCount(player: Player?): Int {
        if (player == null) {
            return 0
        }
        return (eggCountByPlayer[player.uniqueId] ?: 0).coerceAtLeast(0)
    }

    private fun ensureEggCountInitialized(player: Player?) {
        if (player == null) {
            return
        }
        val playerId = player.uniqueId
        if (eggCountByPlayer.containsKey(playerId)) {
            return
        }
        val initialEggs = SheepMergeManager.getStartEggsBonus(player)
            .coerceAtLeast(0)
            .coerceAtMost(SheepMergeManager.getEggCap(player))
        eggCountByPlayer[playerId] = initialEggs
    }

    private fun updateEggHud(player: Player?) {
        if (player == null || !SheepMergeManager.isSheepFarmWorld(player.world)) {
            return
        }

        saveExperienceStateIfNeeded(player)
        val eggCount = getEggCount(player)
        val eggCap = SheepMergeManager.getEggCap(player)
        player.level = eggCount

        if (eggCount >= eggCap) {
            player.exp = 1.0f
            return
        }

        val now = System.currentTimeMillis()
        val intervalMs = (SheepMergeManager.getEggIntervalSeconds(player) * 1000L).coerceAtLeast(1000L)
        val next = nextEggTimestampByPlayer[player.uniqueId] ?: (now + intervalMs)
        val remainingMs = (next - now).coerceAtLeast(0L)
        val progress = 1.0f - (remainingMs / intervalMs.toFloat()).coerceAtMost(1.0f)
        player.exp = progress.coerceIn(0.0f, 1.0f)
    }

    private fun saveExperienceStateIfNeeded(player: Player?) {
        if (player == null) {
            return
        }
        val playerId = player.uniqueId
        if (savedLevels.containsKey(playerId)) {
            return
        }
        savedLevels[playerId] = player.level
        savedExpProgress[playerId] = player.exp
    }

    private fun restoreSavedExperience(player: Player?) {
        if (player == null) {
            return
        }
        val playerId = player.uniqueId
        val savedLevel = savedLevels.remove(playerId)
        val savedExp = savedExpProgress.remove(playerId)
        if (savedLevel == null || savedExp == null) {
            return
        }
        player.level = savedLevel
        player.exp = savedExp
    }
}
