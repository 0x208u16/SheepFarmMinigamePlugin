package dev.x208.sheepmerge

import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import java.util.UUID

object SheepRebirthRuntime {
    private const val PRESTIGE_LEVEL_COST_STEP = 10
    private const val SKILL_ROOT_COST = 1
    private const val RESPEC_COOLDOWN_MS = 30L * 60L * 1000L

    private const val POINTS_X10_ROOT = 1
    private const val POINTS_X10_LEFT = 2
    private const val QUEST_POINTS_X10 = 3
    private const val SACRIFICE_POINTS_X10 = 4
    private const val KEEP_POINTS_AFTER_PRESTIGE = 5
    private const val KEEP_SACRIFICE_AFTER_REBIRTH = 6
    private const val KEEP_SHEEP_AFTER_PRESTIGE = 7
    private const val WOOL_REGEN_X10 = 8
    private const val QUEST_MASTER = 9

    data class SkillNode(
        val id: Int,
        val parentId: Int,
        val layer: Int,
        val slot: Int,
        val material: Material,
        val name: String,
        val effectLine: String,
    )

    private val skillNodes = listOf(
        SkillNode(POINTS_X10_ROOT, 0, 1, 49, Material.NETHER_STAR, "Point Surge", "x10 Coins"),
        SkillNode(POINTS_X10_LEFT, POINTS_X10_ROOT, 2, 38, Material.EMERALD, "Deep Surge", "x10 more Coins"),
        SkillNode(QUEST_POINTS_X10, POINTS_X10_LEFT, 3, 28, Material.BOOK, "Quest Tide", "x10 quest points"),
        SkillNode(
            SACRIFICE_POINTS_X10,
            POINTS_X10_LEFT,
            3,
            30,
            Material.TOTEM_OF_UNDYING,
            "Sacrifice Tide",
            "x10 sacrifice points",
        ),
        SkillNode(WOOL_REGEN_X10, QUEST_POINTS_X10, 4, 18, Material.LIME_WOOL, "Wool Surge", "x10 wool regen speed"),
        SkillNode(
            QUEST_MASTER,
            QUEST_POINTS_X10,
            4,
            19,
            Material.ENCHANTED_BOOK,
            "Quest Master",
            "2x quest size, 2x rewards, 2.5m resets",
        ),
        SkillNode(
            KEEP_SACRIFICE_AFTER_REBIRTH,
            POINTS_X10_ROOT,
            2,
            42,
            Material.MILK_BUCKET,
            "Keep Sacrifice",
            "Keep sacrifice points after rebirth",
        ),
        SkillNode(
            KEEP_POINTS_AFTER_PRESTIGE,
            KEEP_SACRIFICE_AFTER_REBIRTH,
            3,
            32,
            Material.CHEST,
            "Keep Coins",
            "Keep Coins after prestige",
        ),
        SkillNode(
            KEEP_SHEEP_AFTER_PRESTIGE,
            KEEP_POINTS_AFTER_PRESTIGE,
            3,
            34,
            Material.SHEEP_SPAWN_EGG,
            "Keep Sheep",
            "Keep sheep after prestige; no sacrifice gain",
        ),
    )

    @JvmStatic fun getLevel(player: Player?): Int = player?.let { SheepRebirthState.getLevel(it.uniqueId) } ?: 0
    @JvmStatic fun getPoints(player: Player?): Int = player?.let { SheepRebirthState.getPoints(it.uniqueId) } ?: 0
    @JvmStatic fun getSkillNodes(): List<SkillNode> = skillNodes
    @JvmStatic fun getTreeMask(): Int = (1 shl skillNodes.size) - 1
    @JvmStatic fun getSkillCost(node: SkillNode?): Int = if (node == null) Int.MAX_VALUE else skillCost(node)

    @JvmStatic
    fun rebirth(player: Player?, prestigeLevel: Int): Int {
        if (player == null) return 0
        val currentRebirth = getLevel(player)
        val affordable = getAffordableLevels(player, prestigeLevel)
        if (affordable <= 0) return 0

        val playerId = player.uniqueId
        val gainedPoints = getPointsRewardForNextLevels(currentRebirth, affordable)
        SheepRebirthState.setLevel(playerId, currentRebirth + affordable)
        SheepRebirthState.setPoints(playerId, addSaturated(getPoints(player), gainedPoints))

        SheepMergeManager.prestigeResetUpgrades(playerId, true)
        SheepPrestigeState.removeLevel(playerId)
        SheepPrestigeState.removePoints(playerId)
        SheepMergeManager.prestigeClearReminder(player)
        SheepMergeManager.prestigeClearMergeReminder(player)
        SheepRebirthState.clearReminder(playerId)

        SheepMergeManager.prestigeRunResetEffects(player, true)
        if (!keepsSacrificeAfterRebirth(playerId)) SheepSacrificeProgression.removeProgress(playerId)
        SheepMergeManager.prestigeSaveData()
        SheepMergeManager.prestigeEvaluateAchievements(player)
        return affordable
    }

    @JvmStatic
    fun getNextCostInPrestigeLevels(rebirthLevel: Int): Int =
        maxOf(PRESTIGE_LEVEL_COST_STEP, (maxOf(0, rebirthLevel) + 1) * PRESTIGE_LEVEL_COST_STEP)

    @JvmStatic
    fun getAffordableLevels(player: Player?, prestigeLevel: Int): Int {
        if (player == null) return 0
        var remainingPrestigeLevels = maxOf(0, prestigeLevel)
        val rebirthLevel = maxOf(0, getLevel(player))
        var affordable = 0
        while (remainingPrestigeLevels > 0) {
            val cost = getNextCostInPrestigeLevels(rebirthLevel + affordable)
            if (remainingPrestigeLevels < cost) break
            remainingPrestigeLevels -= cost
            affordable++
        }
        return affordable
    }

    @JvmStatic
    fun getPointsRewardForNextLevels(currentRebirth: Int, levels: Int): Int {
        var total = 0
        for (offset in 1..maxOf(0, levels)) total += currentRebirth + offset
        return maxOf(0, total)
    }

    @JvmStatic fun getUnspentPoints(player: Player?): Int =
        if (player == null) 0 else maxOf(0, getPoints(player) - getRefundAmount(player))

    @JvmStatic
    fun getRespecRemainingMs(player: Player?): Long = player?.let {
        maxOf(0L, SheepRebirthState.getNextRespecTimestamp(it.uniqueId) - System.currentTimeMillis())
    } ?: 0L

    @JvmStatic
    fun getRefundAmount(player: Player?): Int {
        if (player == null) return 0
        val mask = SheepRebirthState.getSkillUnlockMask(player.uniqueId)
        return skillNodes.sumOf { node -> if (mask and skillBit(node.id) != 0) skillCost(node) else 0 }
    }

    @JvmStatic
    fun tryRespec(player: Player?): Boolean {
        if (player == null) return false
        val playerId = player.uniqueId
        val now = System.currentTimeMillis()
        if (now < SheepRebirthState.getNextRespecTimestamp(playerId)) return false
        if (SheepRebirthState.getSkillUnlockMask(playerId) == 0) return false
        SheepRebirthState.clearSkillUnlockMask(playerId)
        SheepRebirthState.clearSkillPendingMask(playerId)
        SheepRebirthState.setNextRespecTimestamp(playerId, now + RESPEC_COOLDOWN_MS)
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic
    fun tryUnlock(player: Player?, skillId: Int): Boolean {
        if (player == null) return false
        val node = skillNodes.firstOrNull { it.id == skillId } ?: return false
        if (hasSkill(player, skillId)) return false
        if (node.parentId > 0 && !hasSkill(player, node.parentId)) return false
        if (getUnspentPoints(player) < skillCost(node)) return false
        val playerId = player.uniqueId
        SheepRebirthState.setSkillUnlockMask(
            playerId,
            SheepRebirthState.getSkillUnlockMask(playerId) or skillBit(skillId),
        )
        SheepRebirthState.clearSkillPendingMask(playerId)
        SheepMergeManager.prestigeSaveData()
        return true
    }

    @JvmStatic fun hasSkill(player: Player?, skillId: Int): Boolean =
        player != null && hasSkill(player.uniqueId, skillId)

    @JvmStatic
    fun hasSkill(playerId: UUID?, skillId: Int): Boolean {
        if (playerId == null) return false
        val bit = skillBit(skillId)
        return bit != 0 && SheepRebirthState.getSkillUnlockMask(playerId) and bit != 0
    }

    @JvmStatic
    fun getPointsGainMultiplier(player: Player?): Int {
        var multiplier = 1
        if (hasSkill(player, POINTS_X10_ROOT)) multiplier *= 10
        if (hasSkill(player, POINTS_X10_LEFT)) multiplier *= 10
        val unspent = getUnspentPoints(player)
        if (unspent > 0) multiplier *= 1 + unspent
        return maxOf(1, multiplier)
    }

    @JvmStatic fun getQuestPointsGainMultiplier(player: Player?): Int = if (hasSkill(player, QUEST_POINTS_X10)) 10 else 1
    @JvmStatic fun getSacrificePointsGainMultiplier(player: Player?): Int =
        if (hasSkill(player, SACRIFICE_POINTS_X10)) 10 else 1
    @JvmStatic fun hasQuestMaster(player: Player?): Boolean = hasSkill(player, QUEST_MASTER)
    @JvmStatic fun keepsSacrificeAfterRebirth(playerId: UUID?): Boolean = hasSkill(playerId, KEEP_SACRIFICE_AFTER_REBIRTH)
    @JvmStatic fun keepsPointsAfterPrestige(playerId: UUID?): Boolean = hasSkill(playerId, KEEP_POINTS_AFTER_PRESTIGE)
    @JvmStatic fun keepsSheepAfterPrestige(playerId: UUID?): Boolean = hasSkill(playerId, KEEP_SHEEP_AFTER_PRESTIGE)
    @JvmStatic fun hasWoolRegenBoost(playerId: UUID?): Boolean = hasSkill(playerId, WOOL_REGEN_X10)

    @JvmStatic fun resetPlayer(playerId: UUID?) = SheepRebirthState.resetPlayer(playerId)
    @JvmStatic fun clear() = SheepRebirthState.clear()
    @JvmStatic fun clearPersistedKeys(dataConfig: FileConfiguration) = SheepRebirthState.clearPersistedKeys(dataConfig)
    @JvmStatic fun saveTo(dataConfig: FileConfiguration) = SheepRebirthState.saveTo(dataConfig, getTreeMask())
    @JvmStatic fun loadFrom(dataConfig: FileConfiguration) = SheepRebirthState.loadFrom(dataConfig, getTreeMask())
    @JvmStatic fun clearReminder(player: Player?) = SheepRebirthState.clearReminder(player?.uniqueId)

    @JvmStatic
    fun tickReminder(player: Player?, isSheepFarmWorld: Boolean, prestigeLevel: Int) {
        if (player == null || !isSheepFarmWorld) return
        if (getAffordableLevels(player, prestigeLevel) <= 0) {
            clearReminder(player)
            return
        }

        val playerId = player.uniqueId
        val now = System.currentTimeMillis()
        if (now - SheepRebirthState.getLastReminderTimestamp(playerId) < 20_000L) return

        if (!SheepRebirthState.isTitleReminderShown(playerId)) {
            player.sendTitle(
                SheepMergeManager.rebirthColor("&dRebirth ready"),
                SheepMergeManager.rebirthColor("&7Open the rebirth menu"),
                10,
                60,
                10,
            )
            SheepRebirthState.setTitleReminderShown(playerId, true)
        } else {
            player.sendMessage(SheepMergeManager.rebirthHint("Rebirth ready. Open the rebirth menu from /sheepmerge upgrade."))
        }
        SheepRebirthState.setLastReminderTimestamp(playerId, now)
    }

    private fun skillBit(skillId: Int): Int = if (skillId in 1..skillNodes.size) 1 shl (skillId - 1) else 0
    private fun skillCost(node: SkillNode): Int = maxOf(1, SKILL_ROOT_COST + node.layer - 1)
    private fun addSaturated(current: Int, delta: Int): Int {
        val total = current.toLong() + maxOf(0L, delta.toLong())
        return if (total >= Int.MAX_VALUE) Int.MAX_VALUE else total.toInt()
    }
}