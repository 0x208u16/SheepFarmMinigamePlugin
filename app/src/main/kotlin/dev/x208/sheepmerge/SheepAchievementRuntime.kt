package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import java.util.UUID

internal object SheepAchievementRuntime {
    enum class MilestoneRewardType {
        POINTS,
        WOOL_REGEN,
    }

    data class Definition(
        val id: String,
        val material: Material,
        val name: String,
        val objective: String,
        val reward: String,
        val achievementPoints: Int,
    )

    data class MilestoneDefinition(
        val id: String,
        val requiredPoints: Int,
        val material: Material,
        val name: String,
        val rewardType: MilestoneRewardType,
        val rewardMultiplier: Int,
    ) {
        val reward: String = when (rewardType) {
            MilestoneRewardType.POINTS -> "Bonus: x$rewardMultiplier Coins"
            MilestoneRewardType.WOOL_REGEN -> "Bonus: x$rewardMultiplier wool regen speed"
        }
    }

    private const val MILESTONE_COUNT = 26

    private val definitions = listOf(
        Definition("tutorial_mastery", Material.TARGET, "Tutorial Mastery", "Complete the tutorial", "Reward: +4 achievement points", 4),
        Definition("first_hatch", Material.SHEEP_SPAWN_EGG, "First Hatch", "Spawn at least 10 sheep", "Reward: +2 achievement points", 2),
        Definition("first_shear", Material.SHEARS, "First Cut", "Shear at least 10 sheep", "Reward: +2 achievement points", 2),
        Definition("pair_maker", Material.ANVIL, "Pair Maker", "Merge at least 250 sheep pairs", "Reward: +3 achievement points", 3),
        Definition("socials_explorer", Material.PLAYER_HEAD, "Socials Explorer", "Visit another player's farm at least once", "Reward: +5 achievement points", 5),
        Definition("breeder", Material.CHICKEN_SPAWN_EGG, "Breeder", "Spawn at least 2,000 sheep", "Reward: +6 achievement points", 6),
        Definition("wool_tycoon", Material.WHITE_WOOL, "Wool Tycoon", "Shear at least 1,000 sheep", "Reward: +4 achievement points", 4),
        Definition("fusion_engine", Material.BLAST_FURNACE, "Fusion Engine", "Merge at least 2,500 sheep pairs", "Reward: +7 achievement points", 7),
        Definition("quest_cadet", Material.BOOK, "Quest Cadet", "Complete at least 3 full quest cycles", "Reward: +3 achievement points", 3),
        Definition("quest_veteran", Material.WRITABLE_BOOK, "Quest Veteran", "Complete at least 15 full quest cycles", "Reward: +6 achievement points", 6),
        Definition("upgrade_mechanic", Material.CRAFTING_TABLE, "Upgrade Mechanic", "Reach 20 total regular levels (Limit, Egg Speed, Wool Regen, Tier Chance)", "Reward: +3 achievement points", 3),
        Definition("shear_specialist", Material.SHEARS, "Shear Specialist", "Reach Shear Shop value level 15", "Reward: +4 achievement points", 4),
        Definition("combo_champion", Material.BLAZE_POWDER, "Combo Champion", "Reach Combo Max upgrade level 15", "Reward: +5 achievement points", 5),
        Definition("quest_engineer", Material.CLOCK, "Quest Engineer", "Reach level 10 in both quest upgrades (Duration and Power)", "Reward: +5 achievement points", 5),
        Definition("prestige_initiate", Material.NETHER_STAR, "Prestige Initiate", "Earn at least 3 total prestige levels", "Reward: +4 achievement points", 4),
        Definition("egg_cap_collector", Material.EGG, "Egg Cap Collector", "Reach prestige Egg Cap level 10", "Reward: +6 achievement points", 6),
        Definition("spawn_architect", Material.SPAWNER, "Spawn Architect", "Reach Base Spawn Tier level 8", "Reward: +6 achievement points", 6),
        Definition("prestige_planner", Material.NETHER_STAR, "Prestige Planner", "Reach prestige Quest Reward level 18", "Reward: +7 achievement points", 7),
        Definition("prestige_veteran", Material.BEACON, "Prestige Veteran", "Earn at least 100 total prestige levels", "Reward: +7 achievement points", 7),
        Definition("automation_online", Material.REDSTONE, "Automation Online", "Unlock at least 2 automation tracks", "Reward: +4 achievement points", 4),
        Definition("automation_specialist", Material.REPEATER, "Automation Specialist", "Unlock at least 5 automation tracks", "Reward: +7 achievement points", 7),
        Definition("automation_matrix", Material.COMPARATOR, "Automation Matrix", "Unlock all 6 automation tracks", "Reward: +8 achievement points", 8),
        Definition("sacrifice_initiate", Material.TOTEM_OF_UNDYING, "Sacrifice Initiate", "Buy at least 2 sacrifice unlocks", "Reward: +4 achievement points", 4),
        Definition("sacrifice_mastery", Material.NETHERITE_INGOT, "Sacrifice Mastery", "Buy at least 5 sacrifice unlocks", "Reward: +6 achievement points", 6),
        Definition("reborn", Material.DRAGON_EGG, "Reborn", "Reach rebirth level 3", "Reward: +6 achievement points", 6),
        Definition("rebirth_architect", Material.DRAGON_HEAD, "Rebirth Architect", "Reach rebirth level 10", "Reward: +10 achievement points", 10),
        Definition("rainbow_ascension", Material.PRISMARINE_CRYSTALS, "Rainbow Ascension", "Reach Rainbow tier T4 or higher", "Reward: +6 achievement points", 6),
        Definition("layout_designer", Material.ENDER_CHEST, "Layout Designer", "Set scoreboard to Compact, fill all quick access slots, and open Socials", "Reward: +5 achievement points", 5),
        Definition("quick_access_curator", Material.COMPASS, "Quick Access Curator", "Fill all quick access slots and enable quick-access casting", "Reward: +6 achievement points", 6),
        Definition("sheep_limit_master", Material.OAK_FENCE, "Sheep Limit Master", "Reach your current maximum sheep limit", "Reward: +8 achievement points", 8),
        Definition("wool_guardian", Material.SHIELD, "Wool Guardian", "Reach your current Wool Regen max level", "Reward: +11 achievement points", 11),
        Definition("secret_owner_farm", Material.COMMAND_BLOCK, "Secret: Owner Visitor", "Secret objective", "Reward: +12 achievement points", 12),
        Definition("secret_author_online", Material.DRAGON_BREATH, "Secret: Shared Session", "Secret objective", "Reward: +12 achievement points", 12),
    )

    private val milestones = createMilestones(pointPool())

    @JvmStatic
    fun definitions(): List<Definition> = definitions

    @JvmStatic
    fun milestones(): List<MilestoneDefinition> = milestones

    @JvmStatic
    fun definition(achievementId: String?): Definition? {
        val normalized = normalizeId(achievementId)
        return definitions.firstOrNull { it.id == normalized }
    }

    @JvmStatic
    fun milestone(milestoneId: String?): MilestoneDefinition? =
        milestones.firstOrNull { it.id == milestoneId }

    @JvmStatic
    fun ids(): List<String> = definitions.map { it.id }

    @JvmStatic
    fun displayName(achievementId: String?): String? = definition(achievementId)?.name

    @JvmStatic
    fun isValidId(achievementId: String?): Boolean = definition(achievementId) != null

    @JvmStatic
    fun isValidMilestoneId(milestoneId: String?): Boolean = milestone(milestoneId) != null

    @JvmStatic
    fun points(playerId: java.util.UUID?): Int {
        val unlocked = SheepAchievementState.getUnlockedAchievementIds(playerId)
        var total = 0L
        definitions.forEach { definition ->
            if (definition.id in unlocked) {
                total = (total + definition.achievementPoints).coerceAtMost(Int.MAX_VALUE.toLong())
            }
        }
        return total.toInt()
    }

    @JvmStatic
    fun nextMilestoneTarget(achievementPoints: Int): Int =
        milestones.firstOrNull { achievementPoints.coerceAtLeast(0) < it.requiredPoints }?.requiredPoints ?: 0

    @JvmStatic
    fun pointMultiplier(playerId: java.util.UUID?): Int =
        milestoneMultiplier(playerId, MilestoneRewardType.POINTS)

    @JvmStatic
    fun woolRegenMultiplier(playerId: java.util.UUID?): Double =
        milestoneMultiplier(playerId, MilestoneRewardType.WOOL_REGEN).toDouble()

    @JvmStatic
    fun isUnlocked(player: Player?, achievementId: String?): Boolean {
        if (player == null) return false
        val normalized = normalizeId(achievementId)
        return normalized.isNotBlank() && normalized in SheepAchievementState.getUnlockedAchievementIds(player.uniqueId)
    }

    @JvmStatic
    fun adminComplete(player: Player?, achievementId: String?, notify: Boolean): Boolean {
        if (player == null) return false
        val definition = definition(achievementId) ?: return false
        val unlocked = SheepAchievementState.getOrCreateUnlockedAchievementIds(player.uniqueId)
        if (!unlocked.add(definition.id)) return true
        grantAutomationPoints(player.uniqueId, definition.achievementPoints)
        if (notify) notifyUnlocked(player, definition, points(player.uniqueId))
        SheepMergeManager.achievementSaveData()
        evaluate(player, notify)
        return true
    }

    @JvmStatic
    fun adminCompleteAll(player: Player?, notify: Boolean): Int {
        if (player == null) return 0
        val unlocked = SheepAchievementState.getOrCreateUnlockedAchievementIds(player.uniqueId)
        var unlockedCount = 0
        definitions.forEach { definition ->
            if (unlocked.add(definition.id)) {
                unlockedCount++
                grantAutomationPoints(player.uniqueId, definition.achievementPoints)
                if (notify) notifyUnlocked(player, definition, points(player.uniqueId))
            }
        }
        if (unlockedCount > 0) {
            SheepMergeManager.achievementSaveData()
            evaluate(player, notify)
        }
        return unlockedCount
    }

    @JvmStatic
    fun reconcileAutomationPointGrants() {
        SheepAchievementState.getTrackedPlayerIds().forEach { playerId ->
            val unlockedPoints = points(playerId)
            val grantedPoints = SheepAchievementState.getAutomationPointsGranted(playerId)
            if (unlockedPoints <= 0) {
                SheepAchievementState.removeAutomationPointsGranted(playerId)
                return@forEach
            }
            if (grantedPoints < unlockedPoints) {
                SheepAutomationState.setPoints(
                    playerId,
                    saturatedAdd(SheepAutomationState.getPoints(playerId), unlockedPoints - grantedPoints),
                )
            }
            SheepAchievementState.setAutomationPointsGranted(playerId, unlockedPoints)
        }
    }

    @JvmStatic
    fun evaluate(player: Player?, notify: Boolean) {
        if (player == null) return
        val playerId = player.uniqueId
        val unlocked = SheepAchievementState.getOrCreateUnlockedAchievementIds(playerId)
        val unlockedMilestones = SheepAchievementState.getOrCreateUnlockedAchievementMilestoneIds(playerId)
        val previousWoolMultiplier = woolRegenMultiplier(playerId)
        var changed = false

        definitions.forEach { definition ->
            if (definition.id !in unlocked && hasMetCondition(player, definition.id)) {
                unlocked.add(definition.id)
                grantAutomationPoints(playerId, definition.achievementPoints)
                changed = true
                if (notify) notifyUnlocked(player, definition, points(playerId))
            }
        }

        val achievementPoints = points(playerId)
        milestones.forEach { milestone ->
            if (achievementPoints >= milestone.requiredPoints && unlockedMilestones.add(milestone.id)) {
                changed = true
                if (notify) notifyMilestoneUnlocked(player, milestone, achievementPoints)
            }
        }

        val newWoolMultiplier = woolRegenMultiplier(playerId)
        if (newWoolMultiplier > previousWoolMultiplier) {
            SheepMergeManager.achievementApplyWoolRegenBonus(player, previousWoolMultiplier, newWoolMultiplier)
        }
        if (changed) SheepMergeManager.achievementSaveData()
    }

    @JvmStatic
    fun evaluateAuthorOnlineSecretForOnlinePlayers() {
        val author = Bukkit.getPlayer(AUTHOR_ID) ?: return
        if (!author.isOnline) return
        Bukkit.getOnlinePlayers().forEach { online ->
            if (online.isOnline) evaluate(online, true)
        }
    }

    @JvmStatic
    fun recordShear(player: Player?) {
        recordLifetime(player) { SheepLifetimeProgressState.incrementLifetimeShears(it) }
    }

    @JvmStatic
    fun recordSpawn(player: Player?) {
        recordLifetime(player) { SheepLifetimeProgressState.incrementLifetimeSpawns(it) }
    }

    @JvmStatic
    fun recordMerge(player: Player?) {
        recordLifetime(player) { SheepLifetimeProgressState.incrementLifetimeMerges(it) }
    }

    @JvmStatic
    fun recordVisitedOtherFarm(visitor: Player?, ownerId: UUID?) {
        if (visitor == null || ownerId == null || ownerId == visitor.uniqueId) return
        SheepLifetimeProgressState.incrementLifetimeOtherFarmVisits(visitor.uniqueId)
        if (ownerId == AUTHOR_ID) SheepLifetimeProgressState.setVisitedOwnerFarm(visitor.uniqueId, true)
        evaluate(visitor, true)
        SheepMergeManager.achievementSaveData()
    }

    private inline fun recordLifetime(player: Player?, increment: (UUID) -> Unit) {
        if (player == null) return
        increment(player.uniqueId)
        evaluate(player, true)
    }

    private fun hasMetCondition(player: Player, achievementId: String): Boolean {
        val playerId = player.uniqueId
        return when (achievementId) {
            "first_shear" -> SheepLifetimeProgressState.getLifetimeShears(playerId) >= 10
            "wool_tycoon" -> SheepLifetimeProgressState.getLifetimeShears(playerId) >= 1000
            "first_hatch" -> SheepLifetimeProgressState.getLifetimeSpawns(playerId) >= 10
            "breeder" -> SheepLifetimeProgressState.getLifetimeSpawns(playerId) >= 2000
            "pair_maker" -> SheepLifetimeProgressState.getLifetimeMerges(playerId) >= 250
            "fusion_engine" -> SheepLifetimeProgressState.getLifetimeMerges(playerId) >= 2500
            "quest_cadet" -> SheepLifetimeProgressState.getCompletedQuestCycles(playerId) >= 3
            "quest_veteran" -> SheepLifetimeProgressState.getCompletedQuestCycles(playerId) >= 15
            "upgrade_mechanic" -> SheepEconomyState.getExtraLimit(playerId) +
                SheepEconomyState.getEggSpeedLevel(playerId) + SheepEconomyState.getWoolRegenLevel(playerId) +
                SheepEconomyState.getHigherTierChanceLevel(playerId) >= 20
            "shear_specialist" -> SheepMergeManager.getShearShopLevel(player) >= 15
            "prestige_initiate" -> SheepPrestigeState.getTotalLevelsEarned(playerId) >= 3
            "prestige_veteran" -> SheepPrestigeState.getTotalLevelsEarned(playerId) >= 100
            "automation_online" -> SheepMergeManager.achievementUnlockedAutomationCount(player) >= 2
            "automation_specialist" -> SheepMergeManager.achievementUnlockedAutomationCount(player) >= 5
            "automation_matrix" -> SheepMergeManager.achievementUnlockedAutomationCount(player) >= 6
            "sacrifice_initiate" -> SheepSacrificeProgression.getTotalUnlocksPurchased(playerId) >= 2
            "sacrifice_mastery" -> SheepSacrificeProgression.getUnlocksBought(playerId) >= SheepMergeManager.SACRIFICE_UNLOCK_MAX
            "reborn" -> SheepMergeManager.getRebirthLevel(player) >= 3
            "rebirth_architect" -> SheepMergeManager.getRebirthLevel(player) >= 10
            "rainbow_ascension" -> SheepUpgradeState.getHighestAnnouncedRainbowTier(playerId) >= 4
            "tutorial_mastery" -> SheepTutorialState.isCompleted(playerId)
            "layout_designer" -> SheepMergeManager.achievementScoreboardLayoutMode(player) == 1 &&
                SheepMergeManager.achievementQuickAccessCount(playerId) >= SheepMergeManager.achievementQuickAccessMaxItems() &&
                SheepMergeManager.achievementHasOpenedSocials(playerId)
            "socials_explorer" -> SheepLifetimeProgressState.getLifetimeOtherFarmVisits(playerId) >= 1
            "quick_access_curator" ->
                SheepMergeManager.achievementQuickAccessCount(playerId) >= SheepMergeManager.achievementQuickAccessMaxItems() &&
                    SheepMergeManager.achievementQuickAccessCastingEnabled(playerId)
            "quest_engineer" -> SheepMergeManager.getQuestUpgradeDurationLevel(player) >= 10 &&
                SheepMergeManager.getQuestUpgradePowerLevel(player) >= 10
            "combo_champion" -> SheepMergeManager.getComboMaxUpgradeLevel(player) >= 15
            "egg_cap_collector" -> SheepMergeManager.getPrestigeEggCapLevel(player) >= 10
            "spawn_architect" -> SheepMergeManager.getBaseSpawnTierLevel(player) >= 8
            "prestige_planner" -> SheepMergeManager.getPrestigeQuestRewardLevel(player) >= 18
            "sheep_limit_master" -> SheepMergeManager.getPlayerLimit(player) >= SheepMergeManager.achievementMaxSheepLimit(playerId)
            "wool_guardian" -> SheepMergeManager.getWoolRegenLevel(player) >= SheepMergeManager.getWoolRegenMaxLevel(player)
            "secret_author_online" -> playerId == AUTHOR_ID || run {
                val author = Bukkit.getPlayer(AUTHOR_ID)
                author != null && author.isOnline && playerId != AUTHOR_ID &&
                    SheepMergeManager.isSheepFarmWorld(player.world) && SheepMergeManager.isSheepFarmWorld(author.world)
            }
            "secret_owner_farm" -> playerId == AUTHOR_ID || SheepLifetimeProgressState.hasVisitedOwnerFarm(playerId)
            else -> false
        }
    }

    private fun grantAutomationPoints(playerId: UUID, amount: Int) {
        if (amount <= 0) return
        SheepAutomationState.setPoints(playerId, saturatedAdd(SheepAutomationState.getPoints(playerId), amount))
        SheepAchievementState.setAutomationPointsGranted(
            playerId,
            saturatedAdd(SheepAchievementState.getAutomationPointsGranted(playerId), amount),
        )
    }

    private fun notifyUnlocked(player: Player, definition: Definition, totalPoints: Int) {
        player.sendTitle(color("&6Achievement Unlocked"), color("&f${definition.name}"), 5, 50, 10)
        player.sendMessage(SheepMergeManager.action("Achievement unlocked: ${definition.name} &7(${definition.reward}, total $totalPoints AP)"))
        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f)
    }

    private fun notifyMilestoneUnlocked(player: Player, milestone: MilestoneDefinition, totalPoints: Int) {
        player.sendTitle(color("&bAchievement Milestone"), color("&f${milestone.name}"), 5, 55, 10)
        player.sendMessage(SheepMergeManager.action("Milestone unlocked: ${milestone.name} &7(${milestone.reward}, ${milestone.requiredPoints} AP reached, total $totalPoints AP)"))
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.25f)
        if (milestone.id == "points_26") notifyCommandBlockMilestoneServerwide(player)
    }

    private fun notifyCommandBlockMilestoneServerwide(unlockedBy: Player) {
        val message = color("&d${unlockedBy.name ?: "Unknown"} &7unlocked the &5Command Block &7achievement milestone!")
        Bukkit.getOnlinePlayers().forEach { online ->
            online.sendMessage(message)
            if (SheepMergeManager.isSheepFarmWorld(online.world)) {
                online.playSound(online.location, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 1.0f)
            }
        }
    }

    private fun playSound(player: Player, sound: Sound, volume: Float, pitch: Float) {
        if (SheepMergeManager.areSoundEffectsEnabled(player)) player.playSound(player.location, sound, volume, pitch)
    }

    private fun color(message: String): String = ChatColor.translateAlternateColorCodes('&', message)

    private fun saturatedAdd(current: Int, amount: Int): Int =
        (current.toLong() + amount.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private fun milestoneMultiplier(playerId: java.util.UUID?, rewardType: MilestoneRewardType): Int {
        val unlocked = SheepAchievementState.getUnlockedAchievementMilestoneIds(playerId)
        var multiplier = 1L
        milestones.forEach { milestone ->
            if (milestone.id in unlocked && milestone.rewardType == rewardType) {
                multiplier *= milestone.rewardMultiplier.coerceAtLeast(1)
                if (multiplier >= Int.MAX_VALUE) {
                    return Int.MAX_VALUE
                }
            }
        }
        return multiplier.toInt()
    }

    private fun normalizeId(achievementId: String?): String = achievementId?.trim()?.lowercase().orEmpty()

    private fun pointPool(): Int = definitions
        .filterNot { it.id == "secret_author_online" || it.id == "secret_owner_farm" }
        .sumOf { it.achievementPoints }

    private fun createMilestones(totalAchievementPoints: Int): List<MilestoneDefinition> {
        val materials = listOf(
            Material.COAL, Material.COAL_BLOCK, resolveMaterial("COPPER_NUGGET", Material.GOLD_NUGGET),
            Material.COPPER_INGOT, Material.COPPER_BLOCK, Material.IRON_NUGGET, Material.IRON_INGOT,
            Material.IRON_BLOCK, Material.LAPIS_LAZULI, Material.LAPIS_BLOCK, Material.REDSTONE,
            Material.REDSTONE_BLOCK, Material.GOLD_NUGGET, Material.GOLD_INGOT, Material.GOLD_BLOCK,
            Material.EMERALD, Material.EMERALD_BLOCK, Material.DIAMOND, Material.DIAMOND_BLOCK,
            Material.NETHERITE_SCRAP, Material.ANCIENT_DEBRIS, Material.NETHERITE_INGOT,
            Material.NETHERITE_BLOCK, Material.NETHER_STAR, Material.BEACON, Material.COMMAND_BLOCK,
        )
        val names = listOf(
            "Coal", "Coal Block", "Copper Nugget", "Copper Ingot", "Copper Block", "Iron Nugget",
            "Iron Ingot", "Iron Block", "Lapis Lazuli", "Lapis Block", "Redstone", "Redstone Block",
            "Gold Nugget", "Gold Ingot", "Gold Block", "Emerald", "Emerald Block", "Diamond",
            "Diamond Block", "Netherite Scrap", "Ancient Debris", "Netherite Ingot", "Netherite Block",
            "Nether Star", "Beacon", "Command Block",
        )
        return materials.indices.map { index ->
            val ordinal = index + 1
            MilestoneDefinition(
                "points_$ordinal",
                milestoneTarget(totalAchievementPoints, ordinal),
                materials[index],
                names[index],
                if (ordinal % 2 == 1) MilestoneRewardType.POINTS else MilestoneRewardType.WOOL_REGEN,
                if (ordinal >= 25) 10 else 2,
            )
        }
    }

    private fun milestoneTarget(totalAchievementPoints: Int, milestoneIndex: Int): Int {
        val total = totalAchievementPoints.coerceAtLeast(0)
        val index = milestoneIndex.coerceIn(1, MILESTONE_COUNT)
        if (total <= 0) {
            return index
        }
        val target = kotlin.math.ceil(total * (index / MILESTONE_COUNT.toDouble())).toInt()
        return target.coerceIn(index, total)
    }

    private fun resolveMaterial(materialName: String, fallback: Material): Material =
        Material.matchMaterial(materialName) ?: fallback

    private val AUTHOR_ID: UUID = UUID.fromString("27268675-a9b7-4abd-9628-e6c4515a5cf6")
}