package dev.x208.sheepmerge

import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard
import java.math.BigInteger

object SheepScoreboardRuntime {
    private const val OBJECTIVE_ID = "sheepmerge_points"
    private const val SCOREBOARD_UPDATE_INTERVAL_MS = 1_000L

    @JvmStatic
    fun maybeRestorePlayerScoreboardOutsideFarm(player: Player?) {
        if (player == null) return
        val playerId = player.uniqueId
        val savedScoreboards = SheepRuntimeUiState.savedScoreboards()
        if (!savedScoreboards.containsKey(playerId)) return

        val current = player.scoreboard
        if (current.getObjective(OBJECTIVE_ID) != null || current.objectives.isEmpty()) {
            restorePlayerScoreboard(player)
            return
        }

        savedScoreboards.remove(playerId)
    }

    @JvmStatic
    fun showPointsScoreboard(player: Player?) {
        if (player == null) return
        val playerId = player.uniqueId
        SheepRuntimeUiState.savedScoreboards().putIfAbsent(playerId, player.scoreboard)

        val scoreboard = player.server.scoreboardManager.newScoreboard
        val objective = scoreboard.registerNewObjective(OBJECTIVE_ID, "dummy", "Sheep Merge Coins")
        objective.displaySlot = DisplaySlot.SIDEBAR
        renderPointsScoreboard(player, scoreboard, objective)
        SheepRuntimeUiState.lastPointsScoreboardUpdates()[playerId] = System.currentTimeMillis()
        player.scoreboard = scoreboard
    }

    @JvmStatic
    fun updatePointsScoreboard(player: Player?) {
        if (player == null) return
        val now = System.currentTimeMillis()
        val playerId = player.uniqueId
        val lastUpdatedAt = SheepRuntimeUiState.lastPointsScoreboardUpdates().getOrDefault(playerId, 0L)
        if (now - lastUpdatedAt < SCOREBOARD_UPDATE_INTERVAL_MS) return

        val scoreboard = player.scoreboard
        val objective = scoreboard?.getObjective(OBJECTIVE_ID)
        if (objective == null) {
            showPointsScoreboard(player)
            return
        }
        renderPointsScoreboard(player, scoreboard, objective)
        SheepRuntimeUiState.lastPointsScoreboardUpdates()[playerId] = now
    }

    private fun getQuestScoreLine(label: String, progress: Int, target: Int, complete: Boolean): String =
        SheepFormatting.color(
            (if (complete) "&a" else "&b") + label + "&8: &f" +
                if (complete) "done" else "$progress/$target",
        )

    private fun makeScoreboardSpacer(index: Int): String = " ".repeat(index.coerceAtLeast(1))

    private fun renderPointsScoreboard(player: Player, scoreboard: Scoreboard, objective: Objective) {
        objective.displayName = SheepFormatting.color("&6&lSheepMerge &f&lStats")

        for (entry in HashSet(scoreboard.entries)) {
            scoreboard.resetScores(entry)
        }

        val playerId = player.uniqueId
        val lines = ArrayList<String>()
        lines.add(
            SheepFormatting.color(
                "&6Coins&8: &f${SheepFormatting.formatPoints(SheepMergeManager.getPlayerPointsBig(player))}",
            ),
        )
        if (SheepUiPreferences.shouldShowScoreboardAchievementPoints(playerId)) {
            lines.add(SheepFormatting.color("&dAchv&8: &f${SheepAchievementRuntime.points(playerId)}"))
        }
        if (SheepUiPreferences.shouldShowScoreboardPrestigeStats(playerId)) {
            lines.add(SheepFormatting.color("&5Prestige&8: &fLv ${SheepPrestigeRuntime.getLevel(player)}"))
            lines.add(
                SheepFormatting.color(
                    "&5P.Pts&8: &f${SheepFormatting.formatPoints(SheepPrestigeRuntime.getPoints(player).toLong())}",
                ),
            )
        }

        if (SheepUiPreferences.shouldShowScoreboardQuestPoints(playerId)) {
            lines.add(
                SheepFormatting.color(
                    "&aQuest&8: &f${SheepFormatting.formatPoints(SheepQuestRuntime.getPoints(player).toLong())}",
                ),
            )
        }
        if (SheepUiPreferences.shouldShowScoreboardAutomationPoints(playerId)) {
            lines.add(
                SheepFormatting.color(
                    "&cAuto&8: &f${SheepFormatting.formatPoints(SheepAutomationRuntime.getPoints(player).toLong())}",
                ),
            )
        }
        if (SheepUiPreferences.shouldShowScoreboardSacrificePoints(playerId)) {
            lines.add(
                SheepFormatting.color(
                    "&4Sac&8: &f${SheepFormatting.formatPoints(SheepSacrificeProgression.getPoints(player))}",
                ),
            )
        }

        val compact = SheepUiPreferences.getScoreboardLayoutMode(playerId) == 1
        if (!compact && SheepUiPreferences.shouldShowScoreboardQuestProgress(playerId)) {
            val questResetSeconds = ((SheepQuestRuntime.getResetRemainingMs(player) + 999L) / 1_000L).coerceAtLeast(0L)
            lines.add(makeScoreboardSpacer(lines.size + 1))
            lines.add(SheepFormatting.color("&a&lQuests &8(&f${questResetSeconds}s&8)"))
            lines.add(
                getQuestScoreLine(
                    "Shear",
                    SheepQuestState.questShears().getOrDefault(playerId, 0),
                    SheepQuestRuntime.getShearsTarget(player),
                    SheepQuestState.questShearsComplete().getOrDefault(playerId, false),
                ),
            )
            lines.add(
                getQuestScoreLine(
                    "Spawn",
                    SheepQuestState.questSpawns().getOrDefault(playerId, 0),
                    SheepQuestRuntime.getSpawnsTarget(player),
                    SheepQuestState.questSpawnsComplete().getOrDefault(playerId, false),
                ),
            )
            lines.add(
                getQuestScoreLine(
                    "Merge",
                    SheepQuestState.questMerges().getOrDefault(playerId, 0),
                    SheepQuestRuntime.getMergesTarget(player),
                    SheepQuestState.questMergesComplete().getOrDefault(playerId, false),
                ),
            )
        }

        if (!compact && SheepUiPreferences.shouldShowScoreboardAbilityStatus(playerId)) {
            lines.add(makeScoreboardSpacer(lines.size + 1))
            lines.add(SheepFormatting.color("&d&lAbilities"))
            lines.add(
                SheepQuestRuntime.getCountAbilityScoreLine(
                    "Lucky",
                    SheepQuestState.activeLuckyBurstUses(),
                    SheepQuestState.luckyBurstEnabled(),
                    playerId,
                ),
            )
            lines.add(
                SheepQuestRuntime.getAbilityScoreLine("Wool", SheepQuestState.activeWoolRushUntil(), playerId),
            )
            lines.add(
                SheepQuestRuntime.getAbilityScoreLine(
                    "Jackpot",
                    SheepQuestState.activeJackpotShearsUntil(),
                    playerId,
                ),
            )
            lines.add(
                SheepQuestRuntime.getCountAbilityScoreLine(
                    "Merge",
                    SheepQuestState.activeAutoMergeUses(),
                    SheepQuestState.autoMergeEnabled(),
                    playerId,
                ),
            )
            lines.add(
                SheepQuestRuntime.getCountAbilityScoreLine(
                    "Shear",
                    SheepQuestState.activeAutoShearUses(),
                    SheepQuestState.autoShearEnabled(),
                    playerId,
                ),
            )
        }

        var score = minOf(15, lines.size)
        for (line in lines) {
            if (score <= 0) break
            objective.getScore(line).score = score
            score--
        }

        updateTabListPointsVisibility(player)
    }

    @JvmStatic
    fun updateTabListPointsVisibility(player: Player?) {
        if (player == null) return
        if (!SheepMergeManager.isSheepFarmWorld(player.world)) {
            player.setPlayerListName(null)
            return
        }
        val points = SheepMergeManager.getPlayerPointsBig(player).max(BigInteger.ZERO)
        player.setPlayerListName(
            SheepFormatting.color("&e${SheepFormatting.formatPoints(points)} &7| &f${player.name}"),
        )
    }

    @JvmStatic
    fun restorePlayerScoreboard(player: Player?) {
        if (player == null) return
        SheepRuntimeUiState.savedScoreboards().remove(player.uniqueId)?.let { player.scoreboard = it }
    }

    @JvmStatic
    fun clearSheepMergeSidebarWithoutSnapshot(player: Player?) {
        if (player?.scoreboard?.getObjective(OBJECTIVE_ID) == null) return
        player.scoreboard = player.server.scoreboardManager.mainScoreboard
    }
}