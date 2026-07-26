package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import java.util.UUID

object SheepComboRuntime {
    private const val BASE_COMBO_DECAY_PER_SECOND = 1.3
    private const val COMBO_DECAY_HIGH_LEVEL_SCALING = 2.2
    private const val COMBO_BASE_MAX_SCORE = 100.0
    private const val COMBO_MAX_SCORE_PER_LEVEL = 50.0
    private const val COMBO_GAIN_PERCENT_PER_LEVEL = 10.0
    private const val COMBO_POINT_MULTIPLIER_PER_SCORE = 0.015

    @JvmStatic
    fun clearRuntime(player: Player?) {
        if (player == null) return
        val playerId = player.uniqueId
        SheepMergeManager.comboStopQueuedShearAllTask(playerId)
        SheepRuntimeUiState.lastPointsScoreboardUpdates().remove(playerId)
        SheepComboState.resetRuntime(playerId)
        SheepRuntimeUiState.lastPointsOverlays().remove(playerId)
        SheepRuntimeUiState.pointsOverlayExpirations().remove(playerId)
        SheepQuestState.lastAbilityAuraSoundTimestamps().remove(playerId)
        removeBossBar(playerId)
        SheepMergeManager.clearVisitFarmBossBar(player)
    }

    @JvmStatic
    fun recordSheepMerge(player: Player?, mergedFromTier: SheepTier?, woolReadySourceSheep: Int) {
        if (player == null || mergedFromTier == null) return
        val playerId = player.uniqueId
        SheepAchievementRuntime.recordMerge(player)
        val now = System.currentTimeMillis()
        SheepRuntimeUiState.lastMergeTimestamps()[playerId] = now
        SheepRuntimeUiState.lastMergeReminderTimestamps().remove(playerId)
        SheepRuntimeUiState.mergeTitleReminderShown().remove(playerId)

        tickDecay(player, now)
        var comboGain = (mergedFromTier.level + 1) *
            (1.0 + getGainUpgradeLevel(player) * (COMBO_GAIN_PERCENT_PER_LEVEL / 100.0))
        comboGain *= SheepRandomEventRuntime.getComboGainMultiplier(now)
        val updatedScore = minOf(getMaxScore(player), getScore(player) + comboGain)
        SheepComboState.setScore(playerId, updatedScore)
        SheepComboState.setLastUpdateTimestamp(playerId, now)

        SheepMergeManager.showOverlay(
            player,
            SheepMergeManager.accent("Merge combo x${SheepFormatting.formatComboMultiplier(getMultiplier(player, updatedScore))}"),
        )
        updateBossBar(player, updatedScore)
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun getMultiplier(player: Player?, comboScore: Double): Double =
        maxOf(1.0, 1.0 + comboScore * COMBO_POINT_MULTIPLIER_PER_SCORE)

    @JvmStatic
    fun getScore(player: Player?): Double =
        player?.let { maxOf(0.0, SheepComboState.getScore(it.uniqueId)) } ?: 0.0

    @JvmStatic
    fun getMaxScore(player: Player?): Double =
        COMBO_BASE_MAX_SCORE + getMaxUpgradeLevel(player) * COMBO_MAX_SCORE_PER_LEVEL

    @JvmStatic
    fun getDecayMultiplier(player: Player?): Double =
        maxOf(0.15, 1.0 - getDecayUpgradeLevel(player) * 0.08)

    @JvmStatic
    fun getGainPercentPerLevel(): Double = COMBO_GAIN_PERCENT_PER_LEVEL

    @JvmStatic
    fun resetMaxUpgrade(playerId: UUID?) {
        if (playerId == null) return
        SheepComboState.resetMaxUpgrade(playerId, COMBO_BASE_MAX_SCORE)
    }

    @JvmStatic
    fun clampScoreToMax(player: Player?) {
        if (player == null) return
        SheepComboState.setScore(player.uniqueId, minOf(getMaxScore(player), getScore(player)))
    }

    @JvmStatic
    fun tick(player: Player?) {
        if (player == null) return
        if (!SheepMergeManager.isSheepFarmWorld(player.world)) {
            removeBossBar(player.uniqueId)
            return
        }

        val now = System.currentTimeMillis()
        tickDecay(player, now)
        updateBossBar(player, getScore(player))
    }

    private fun tickDecay(player: Player?, now: Long) {
        if (player == null) return

        val playerId = player.uniqueId
        val currentScore = getScore(player)
        val lastTick = SheepComboState.getLastUpdateTimestamp(playerId, now)
        SheepComboState.setLastUpdateTimestamp(playerId, now)

        if (currentScore <= 0.0 || now <= lastTick) {
            if (currentScore <= 0.0) SheepComboState.removeScore(playerId)
            return
        }

        val elapsedSeconds = (now - lastTick) / 1000.0
        val maxScore = getMaxScore(player)
        val levelScaling = 1.0 + (currentScore / maxOf(1.0, maxScore)) * COMBO_DECAY_HIGH_LEVEL_SCALING
        val decayPerSecond = BASE_COMBO_DECAY_PER_SECOND * levelScaling * getDecayMultiplier(player)
        val updatedScore = maxOf(0.0, currentScore - decayPerSecond * elapsedSeconds)

        if (updatedScore <= 0.01) {
            SheepComboState.removeScore(playerId)
            return
        }
        SheepComboState.setScore(playerId, minOf(maxScore, updatedScore))
    }

    @JvmStatic
    fun updateBossBar(player: Player?, comboScore: Double) {
        if (player == null || !SheepMergeManager.isSheepFarmWorld(player.world)) return

        val playerId = player.uniqueId
        val now = System.currentTimeMillis()
        val frenzyActive = SheepRandomEventRuntime.isComboFrenzyActive(now)
        if (comboScore <= 0.0 && !frenzyActive) {
            removeBossBar(playerId)
            return
        }

        val bar = SheepRuntimeUiState.comboBossBars()[playerId]
            ?: Bukkit.createBossBar("Combo", BarColor.YELLOW, BarStyle.SEGMENTED_10).also {
                SheepRuntimeUiState.comboBossBars()[playerId] = it
            }

        val maxScore = getMaxScore(player)
        bar.progress = if (frenzyActive) {
            SheepRandomEventRuntime.getComboFrenzyProgress(now)
        } else {
            (comboScore / maxOf(1.0, maxScore)).coerceIn(0.0, 1.0)
        }
        var title = SheepFormatting.color(
            "&6Combo &f${kotlin.math.floor(comboScore).toInt()}" +
                "&7/&f${kotlin.math.floor(maxScore).toInt()}" +
                " &7| &eCoins x${SheepFormatting.formatComboMultiplier(getMultiplier(player, comboScore))}",
        )
        if (frenzyActive) {
            val remaining = SheepRandomEventRuntime.getComboFrenzyRemainingMs(now)
            title += SheepFormatting.color(" &7| &cFrenzy ${SheepFormatting.formatDuration(remaining)}")
        }
        bar.setTitle(title)
        bar.isVisible = true
        if (!bar.players.contains(player)) bar.addPlayer(player)
    }

    @JvmStatic
    fun removeBossBar(playerId: UUID?) {
        if (playerId == null) return
        val bar = SheepRuntimeUiState.comboBossBars().remove(playerId) ?: return
        bar.removeAll()
        bar.isVisible = false
    }

    private fun getDecayUpgradeLevel(player: Player?): Int =
        player?.let { SheepComboState.getDecayUpgrade(it.uniqueId) } ?: 0

    private fun getMaxUpgradeLevel(player: Player?): Int =
        player?.let { SheepComboState.getMaxUpgrade(it.uniqueId) } ?: 0

    private fun getGainUpgradeLevel(player: Player?): Int =
        player?.let { SheepComboState.getGainUpgrade(it.uniqueId) } ?: 0
}