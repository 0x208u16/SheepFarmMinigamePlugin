package dev.x208.sheepmerge

import org.bukkit.boss.BossBar
import org.bukkit.scoreboard.Scoreboard
import java.math.BigInteger
import java.util.UUID

object SheepRuntimeUiState {
    private val lastMergeTimestampByPlayer = HashMap<UUID, Long>()
    private val lastMergeReminderTimestampByPlayer = HashMap<UUID, Long>()
    private val mergeTitleReminderShownByPlayer = HashMap<UUID, Boolean>()
    private val pointsOverlayExpiresAtByPlayer = HashMap<UUID, Long>()
    private val lastPointsOverlayByPlayer = HashMap<UUID, BigInteger>()
    private val comboBossBarByPlayer = HashMap<UUID, BossBar>()
    private val visitFarmBossBarByPlayer = HashMap<UUID, BossBar>()
    private val socialsPageByPlayer = HashMap<UUID, Int>()
    private val savedInventories = HashMap<UUID, InventoryDataUtils.Snapshot>()
    private val savedScoreboards = HashMap<UUID, Scoreboard>()
    private val lastPointsScoreboardUpdateAtByPlayer = HashMap<UUID, Long>()
    private val lastSpawnLimitWarningTimestampByPlayer = HashMap<UUID, Long>()
    private val lastOutOfEggWarningTimestampByPlayer = HashMap<UUID, Long>()

    @JvmStatic fun lastMergeTimestamps(): MutableMap<UUID, Long> = lastMergeTimestampByPlayer
    @JvmStatic fun lastMergeReminderTimestamps(): MutableMap<UUID, Long> = lastMergeReminderTimestampByPlayer
    @JvmStatic fun mergeTitleReminderShown(): MutableMap<UUID, Boolean> = mergeTitleReminderShownByPlayer
    @JvmStatic fun pointsOverlayExpirations(): MutableMap<UUID, Long> = pointsOverlayExpiresAtByPlayer
    @JvmStatic fun lastPointsOverlays(): MutableMap<UUID, BigInteger> = lastPointsOverlayByPlayer
    @JvmStatic fun comboBossBars(): MutableMap<UUID, BossBar> = comboBossBarByPlayer
    @JvmStatic fun visitFarmBossBars(): MutableMap<UUID, BossBar> = visitFarmBossBarByPlayer
    @JvmStatic fun socialsPages(): MutableMap<UUID, Int> = socialsPageByPlayer
    @JvmStatic
    @JvmName("savedInventories")
    internal fun savedInventoriesInternal(): MutableMap<UUID, InventoryDataUtils.Snapshot> = savedInventories
    @JvmStatic fun savedScoreboards(): MutableMap<UUID, Scoreboard> = savedScoreboards
    @JvmStatic fun lastPointsScoreboardUpdates(): MutableMap<UUID, Long> = lastPointsScoreboardUpdateAtByPlayer
    @JvmStatic fun lastSpawnLimitWarnings(): MutableMap<UUID, Long> = lastSpawnLimitWarningTimestampByPlayer
    @JvmStatic fun lastOutOfEggWarnings(): MutableMap<UUID, Long> = lastOutOfEggWarningTimestampByPlayer
}