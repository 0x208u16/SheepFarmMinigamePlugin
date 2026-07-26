package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

internal object SheepProgressionMenus {

    @JvmStatic
    fun isUpgradeMenuTitle(title: String?): Boolean = SheepMergeManager.UPGRADE_MENU_TITLE == title

    @JvmStatic
    fun isPrestigeMenuTitle(title: String?): Boolean = SheepMergeManager.PRESTIGE_MENU_TITLE == title

    @JvmStatic
    fun isQuestMenuTitle(title: String?): Boolean = SheepMergeManager.QUEST_MENU_TITLE == title

    @JvmStatic
    fun isQuestUpgradesMenuTitle(title: String?): Boolean = SheepMergeManager.QUEST_UPGRADES_MENU_TITLE == title

    @JvmStatic
    fun isShopMenuTitle(title: String?): Boolean = SheepMergeManager.SHOP_MENU_TITLE == title

    @JvmStatic
    fun isComboShopMenuTitle(title: String?): Boolean = SheepMergeManager.COMBO_SHOP_MENU_TITLE == title

    @JvmStatic
    fun isAutomationMenuTitle(title: String?): Boolean = SheepMergeManager.AUTOMATION_MENU_TITLE == title

    @JvmStatic
    fun openUpgradeMenu(player: Player?) {
        if (player == null) return
        SheepMergeManager.progressionMarkTutorialUpgradeOpened(player)
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.UPGRADE_MENU_TITLE)
        renderUpgradeMenu(player, inventory::setItem)
        player.openInventory(inventory)
    }

    private fun refreshOpenUpgradeMenuItems(player: Player, inventory: Inventory) {
        renderUpgradeMenu(player) { slot, item -> setMenuItemIfChanged(inventory, slot, item) }
    }

    private fun renderUpgradeMenu(player: Player, setItem: (Int, ItemStack) -> Unit) {
        setItem(SheepMergeManager.LAYOUTS_MENU_OPEN_SLOT, MenuItemFactory.create(
            Material.ENDER_CHEST, "Settings", listOf(
                "Scoreboard, inventory, sounds, particles, and visits",
                "Configure settings for your farm and UI",
                "Click to open"
            )
        ))

        val limitLevel = SheepMergeManager.getLimitUpgradeLevel(player)
        val currentLimit = SheepMergeManager.getPlayerLimit(player)
        val maxLimit = SheepMergeManager.progressionMaxSheepLimit(player)
        val limitMaxed = currentLimit >= maxLimit
        setItem(SheepMergeManager.LIMIT_UPGRADE_SLOT, MenuItemFactory.create(
            Material.OAK_FENCE, "Sheep Limit", listOf(
                "Level: $limitLevel / ${(maxLimit - SheepMergeManager.progressionBaseSheepLimit()) / SheepMergeManager.progressionLimitUpgradeStep()}",
                "Current limit: $currentLimit / $maxLimit",
                "Next: $currentLimit -> ${minOf(maxLimit, currentLimit + SheepMergeManager.progressionLimitUpgradeStep())}",
                if (limitMaxed) "MAXED" else "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.getUpgradeCost(player))} Coins",
                if (limitMaxed) "Limit cap reached" else "Click to purchase"
            )
        ))

        val eggLevel = SheepMergeManager.getEggSpeedLevel(player)
        val eggMaxLevel = SheepMergeManager.getEggSpeedMaxLevel(player)
        val eggCurrentSeconds = SheepMergeManager.getEggIntervalSeconds(player)
        val eggNextSeconds = SheepMergeManager.progressionEggIntervalSecondsAtLevel(minOf(eggMaxLevel, eggLevel + 1))
        setItem(SheepMergeManager.EGG_SPEED_UPGRADE_SLOT, MenuItemFactory.create(
            Material.CLOCK, "Faster Egg Spawn", listOf(
                "Level: $eggLevel / $eggMaxLevel",
                "Current: ${eggCurrentSeconds}s per egg",
                if (eggLevel >= eggMaxLevel) "Next: MAXED" else "Next: ${eggCurrentSeconds}s -> ${eggNextSeconds}s",
                if (eggLevel >= eggMaxLevel) "MAXED" else "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionEggSpeedUpgradeCost(player))} Coins",
                "Click to purchase"
            )
        ))

        val woolLevel = SheepMergeManager.getWoolRegenLevel(player)
        val woolMaxLevel = SheepMergeManager.getWoolRegenMaxLevel(player)
        val woolNextLevel = minOf(woolMaxLevel, woolLevel + 1)
        val woolCurrentCooldown = SheepMergeManager.progressionWoolCooldownPercentDisplayAtLevel(player, woolLevel)
        val woolCurrentReduction = SheepMergeManager.progressionWoolCooldownReductionPercentDisplayAtLevel(player, woolLevel)
        val woolCurrentFactor = SheepMergeManager.progressionWoolCooldownFactorDisplayAtLevel(player, woolLevel)
        val woolNextCooldown = SheepMergeManager.progressionWoolCooldownPercentDisplayAtLevel(player, woolNextLevel)
        val woolNextReduction = SheepMergeManager.progressionWoolCooldownReductionPercentDisplayAtLevel(player, woolNextLevel)
        val woolNextFactor = SheepMergeManager.progressionWoolCooldownFactorDisplayAtLevel(player, woolNextLevel)
        setItem(SheepMergeManager.WOOL_REGEN_UPGRADE_SLOT, MenuItemFactory.create(
            Material.WHITE_WOOL, "Faster Wool Regen", listOf(
                "Level: $woolLevel / $woolMaxLevel",
                "Current: $woolCurrentReduction% reduced (duration * $woolCurrentFactor)",
                if (woolLevel >= woolMaxLevel) "Next: MAXED" else "Next: $woolCurrentCooldown% -> $woolNextCooldown% cooldown ($woolNextReduction% faster, x$woolNextFactor)",
                if (woolLevel >= woolMaxLevel) "MAXED" else "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionWoolRegenUpgradeCost(player))} Coins",
                "Click to purchase"
            )
        ))

        val chanceLevel = SheepMergeManager.getHigherTierChanceLevel(player)
        val chanceMaxLevel = SheepMergeManager.getHigherTierChanceMaxLevel(player)
        val chanceCurrentPercent = SheepMergeManager.getHigherTierChancePercent(player)
        val chanceNextPercent = SheepMergeManager.progressionHigherTierChancePercentAtLevel(minOf(chanceMaxLevel, chanceLevel + 1))
        setItem(SheepMergeManager.HIGHER_TIER_CHANCE_UPGRADE_SLOT, MenuItemFactory.create(
            Material.GOLDEN_APPLE, "Higher Tier Spawn Chance", listOf(
                "Level: $chanceLevel / $chanceMaxLevel",
                "Current: $chanceCurrentPercent% bonus chance",
                if (chanceLevel >= chanceMaxLevel) "Next: MAXED" else "Next: $chanceCurrentPercent% -> $chanceNextPercent%",
                "Hard cap: ${SheepMergeManager.progressionHigherTierChanceBaseCapPercent()}%",
                if (chanceLevel >= chanceMaxLevel) "MAXED" else "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionHigherTierChanceUpgradeCost(player))} Coins",
                "Click to purchase"
            )
        ))

        setItem(SheepMergeManager.PRESTIGE_MENU_OPEN_SLOT, MenuItemFactory.create(
            Material.NETHER_STAR, "Prestige Upgrades", listOf(
                "Prestige level: ${SheepMergeManager.getPrestigeLevel(player)}",
                "Prestige points: ${SheepMergeManager.formatPoints(SheepMergeManager.getPrestigePoints(player).toLong())}",
                "Click to open"
            )
        ))
        setItem(SheepMergeManager.QUEST_MENU_OPEN_SLOT, questMenuOpenItem(player))
        setItem(SheepMergeManager.SHOP_MENU_OPEN_SLOT, MenuItemFactory.create(
            Material.SHEARS, "Shear Shop", listOf(
                "Shear level: ${SheepMergeManager.getShearPointGainUpgradeLevel(player)}",
                "Coin multiplier: x${SheepMergeManager.getShearPointMultiplier(player)}",
                "Click to open"
            )
        ))
        setItem(SheepMergeManager.COMBO_MENU_OPEN_SLOT, comboMenuOpenItem(player))
        setItem(SheepMergeManager.AUTOMATION_MENU_OPEN_SLOT, automationMenuOpenItem(player))
        setItem(SheepMergeManager.ACHIEVEMENTS_MENU_OPEN_SLOT, MenuItemFactory.create(
            Material.RED_BANNER, "Achievements", listOf("Track milestones and claim bonuses", "Click to open")
        ))
        setItem(SheepMergeManager.SACRIFICE_MENU_OPEN_SLOT, sacrificeMenuOpenItem(player))
        setItem(SheepMergeManager.REBIRTH_MENU_OPEN_SLOT, rebirthMenuOpenItem(player))
        setItem(SheepMergeManager.SOCIALS_MENU_OPEN_SLOT, socialsMenuOpenItem())
    }

    private fun questMenuOpenItem(player: Player) = MenuItemFactory.create(Material.BOOK, "Quests", listOf(
        "Quest points: ${SheepMergeManager.formatPoints(SheepMergeManager.getQuestPoints(player).toLong())}",
        "Next reset: ${SheepMergeManager.progressionFormatDuration(SheepMergeManager.progressionQuestResetRemainingMs(player))}",
        "Shear ${SheepQuestState.questShears()[player.uniqueId] ?: 0}/${SheepMergeManager.progressionQuestShearsTarget(player)}",
        "Spawn ${SheepQuestState.questSpawns()[player.uniqueId] ?: 0}/${SheepMergeManager.progressionQuestSpawnsTarget(player)}",
        "Merge ${SheepQuestState.questMerges()[player.uniqueId] ?: 0}/${SheepMergeManager.progressionQuestMergesTarget(player)}",
        "Click to open"
    ))

    private fun comboMenuOpenItem(player: Player) = MenuItemFactory.create(Material.BLAZE_POWDER, "Combo Upgrades", listOf(
        "Combo score: ${SheepMergeManager.progressionComboScoreDisplay(player)} / ${SheepMergeManager.progressionComboMaxScoreDisplay(player)}",
        "Coins x${SheepMergeManager.progressionComboMultiplierDisplay(player)}",
        "Click to open"
    ))

    private fun automationMenuOpenItem(player: Player) = MenuItemFactory.create(Material.REDSTONE, "Automation", listOf(
        "Automation points: ${SheepMergeManager.formatPoints(SheepMergeManager.getAutomationPoints(player).toLong())}",
        "Gain: +1 per ${SheepMergeManager.progressionAutomationPointInterval()}",
        "Click to open"
    ))

    private fun sacrificeMenuOpenItem(player: Player) = MenuItemFactory.create(Material.TOTEM_OF_UNDYING, "Sacrifice", listOf(
        "Sacrifice points: ${SheepMergeManager.formatPoints(SheepMergeManager.getSacrificePoints(player))}",
        "Unlocks bought: ${SheepMergeManager.getSacrificeUnlocksBought(player)} / ${SheepMergeManager.progressionSacrificeUnlockMax()}",
        "Click to open"
    ))

    private fun rebirthMenuOpenItem(player: Player): ItemStack {
        val level = SheepMergeManager.getRebirthLevel(player)
        val affordable = SheepMergeManager.getAffordableRebirthLevelsDisplay(player)
        val reward = SheepMergeManager.progressionRebirthRewardForLevels(level, affordable)
        return MenuItemFactory.create(Material.DRAGON_EGG, "Rebirth", listOf(
            "Rebirth level: $level",
            "Rebirth points: ${SheepMergeManager.formatPoints(SheepMergeManager.getRebirthPoints(player).toLong())}",
            "Next cost: ${SheepMergeManager.progressionRebirthCost(level)} prestige levels",
            "Consumes prestige levels and resets prestige progress",
            if (affordable > 0) "Buy now: +$affordable rebirth level(s), +${SheepMergeManager.formatPoints(reward.toLong())} rebirth points" else "Buy now: +0 rebirth level(s)",
            "Click to open"
        ))
    }

    private fun socialsMenuOpenItem(): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD, 1)
        val skullMeta = head.itemMeta as? SkullMeta ?: return MenuItemFactory.create(
            Material.PLAYER_HEAD,
            "${ChatColor.RESET}${ChatColor.YELLOW}Socials",
            listOf("${ChatColor.RED}${ChatColor.BOLD}Author:", "${ChatColor.GREEN}${ChatColor.ITALIC}0x208u16 (unknown)")
        )
        val author = SheepMergeManager.socialsAuthor()
        skullMeta.owningPlayer = author
        skullMeta.setDisplayName("${ChatColor.RESET}${ChatColor.YELLOW}Socials")
        skullMeta.lore = listOf(
            "${ChatColor.RED}${ChatColor.BOLD}Author:",
            "${ChatColor.GREEN}${ChatColor.ITALIC}${SheepMergeManager.socialsAuthorCredentialsText(author)}"
        )
        head.itemMeta = skullMeta
        return head
    }

    @JvmStatic
    fun handleUpgradeMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.LAYOUTS_MENU_OPEN_SLOT -> {
                SheepMergeManager.openUniversalLayoutMenu(player)
                return
            }
            SheepMergeManager.LIMIT_UPGRADE_SLOT -> {
                if (SheepMergeManager.progressionBlockRegularUpgradePurchase(player)) return refreshUpgradeMenu(player)
                if (SheepMergeManager.getPlayerLimit(player) >= SheepMergeManager.progressionMaxSheepLimit(player)) {
                    player.sendMessage(SheepMergeManager.warning("Sheep limit maxed."))
                } else if (SheepMergeManager.progressionUpgradeLimit(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Limit up: ${SheepMergeManager.getPlayerLimit(player)}"))
                    SheepMergeManager.progressionMarkTutorialRegularUpgradesIfComplete(player)
                } else player.sendMessage(SheepMergeManager.warning("Not enough Coins."))
            }
            SheepMergeManager.EGG_SPEED_UPGRADE_SLOT -> {
                if (SheepMergeManager.progressionBlockRegularUpgradePurchase(player)) return refreshUpgradeMenu(player)
                if (SheepMergeManager.progressionUpgradeEggSpeed(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Eggs: every ${SheepMergeManager.getEggIntervalSeconds(player)}s"))
                    SheepMergeManager.progressionMarkTutorialRegularUpgradesIfComplete(player)
                } else player.sendMessage(SheepMergeManager.warning("Not enough Coins."))
            }
            SheepMergeManager.WOOL_REGEN_UPGRADE_SLOT -> {
                if (SheepMergeManager.progressionBlockRegularUpgradePurchase(player)) return refreshUpgradeMenu(player)
                if (SheepMergeManager.progressionUpgradeWoolRegen(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Wool regen up"))
                    SheepMergeManager.progressionMarkTutorialRegularUpgradesIfComplete(player)
                } else player.sendMessage(SheepMergeManager.warning("Not enough Coins."))
            }
            SheepMergeManager.HIGHER_TIER_CHANCE_UPGRADE_SLOT -> {
                if (SheepMergeManager.progressionBlockRegularUpgradePurchase(player)) return refreshUpgradeMenu(player)
                if (SheepMergeManager.getHigherTierChanceLevel(player) >= SheepMergeManager.getHigherTierChanceMaxLevel(player)) {
                    player.sendMessage(SheepMergeManager.warning("Spawn chance maxed."))
                } else if (SheepMergeManager.progressionUpgradeHigherTierChance(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Spawn chance: ${SheepMergeManager.getHigherTierChancePercent(player)}%"))
                    SheepMergeManager.progressionMarkTutorialRegularUpgradesIfComplete(player)
                } else player.sendMessage(SheepMergeManager.warning("Not enough Coins."))
            }
            SheepMergeManager.PRESTIGE_MENU_OPEN_SLOT -> return SheepMergeManager.openPrestigeMenu(player)
            SheepMergeManager.QUEST_MENU_OPEN_SLOT -> return SheepMergeManager.openQuestMenu(player)
            SheepMergeManager.SHOP_MENU_OPEN_SLOT -> return SheepMergeManager.openShopMenu(player)
            SheepMergeManager.COMBO_MENU_OPEN_SLOT -> return SheepMergeManager.openComboShopMenu(player)
            SheepMergeManager.AUTOMATION_MENU_OPEN_SLOT -> return SheepMergeManager.openAutomationMenu(player)
            SheepMergeManager.ACHIEVEMENTS_MENU_OPEN_SLOT -> return SheepMergeManager.openAchievementsMenu(player)
            SheepMergeManager.SACRIFICE_MENU_OPEN_SLOT -> return SheepMergeManager.openSacrificeMenu(player)
            SheepMergeManager.REBIRTH_MENU_OPEN_SLOT -> return SheepMergeManager.openRebirthMenu(player)
            SheepMergeManager.SOCIALS_MENU_OPEN_SLOT -> return SheepMergeManager.openSocialsMenu(player)
            else -> return
        }
        refreshUpgradeMenu(player)
    }

    private fun refreshUpgradeMenu(player: Player) {
        SheepMergeManager.updatePointsScoreboard(player)
        openUpgradeMenu(player)
    }

    @JvmStatic
    fun openPrestigeMenu(player: Player?) {
        if (player == null) return
        SheepMergeManager.progressionMarkTutorialPrestigeOpened(player)
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.PRESTIGE_MENU_TITLE)
        renderPrestigeMenu(player, inventory::setItem, true)
        player.openInventory(inventory)
    }

    private fun refreshOpenPrestigeMenuItems(player: Player, inventory: Inventory) {
        renderPrestigeMenu(player, { slot, item -> setMenuItemIfChanged(inventory, slot, item) }, false)
    }

    private fun renderPrestigeMenu(player: Player, setItem: (Int, ItemStack) -> Unit, includeStaticItems: Boolean) {
        val prestigeLevel = SheepMergeManager.getPrestigeLevel(player)
        val affordable = SheepMergeManager.progressionAffordablePrestigeLevels(player)
        setItem(SheepMergeManager.PRESTIGE_UPGRADE_SLOT, MenuItemFactory.create(Material.NETHER_STAR, "Prestige Reset", listOf(
            "Current prestige: $prestigeLevel",
            if (affordable > 0) "Buy now: +$affordable prestige level(s)" else "Buy now: +0 prestige level(s)",
            if (affordable > 0) "Gain prestige points: +${SheepMergeManager.formatPoints(SheepMergeManager.progressionPrestigeRewardForLevels(player, affordable).toLong())}" else "Gain prestige points: +0",
            "Next prestige cost: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionPrestigeCost(player))} Coins",
            if (affordable > 0) "Total cost now: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionTotalPrestigeCost(player, affordable))} Coins" else "Need more Coins for next prestige",
            "Resets Coin upgrades",
            "Click to prestige multiple"
        )))
        val doubleLevel = SheepMergeManager.getPrestigeDoublePointsChanceLevel(player)
        val doubleMax = SheepMergeManager.progressionPrestigeDoublePointsMaxLevel()
        setItem(SheepMergeManager.PRESTIGE_DOUBLE_POINTS_SLOT, MenuItemFactory.create(Material.EMERALD, "Double Coins Chance", listOf(
            "Level: $doubleLevel / $doubleMax",
            "Chance: ${SheepMergeManager.getDoublePointsChancePercent(player)}%",
            if (doubleLevel >= doubleMax) "MAXED" else "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.getPrestigeDoublePointsCost(player).toLong())} prestige points",
            "Click to purchase"
        )))

        if (includeStaticItems) {
            val higherLevel = SheepMergeManager.getPrestigeHigherMaxLevel(player)
            setItem(SheepMergeManager.PRESTIGE_HIGHER_MAX_LEVEL_SLOT, MenuItemFactory.create(Material.ENCHANTED_BOOK, "Higher Maximum Levels", listOf(
                "Level: $higherLevel",
                "Current caps: Egg ${SheepMergeManager.getEggSpeedMaxLevel(player)}, Wool ${SheepMergeManager.getWoolRegenMaxLevel(player)}, Chance ${SheepMergeManager.getHigherTierChanceMaxLevel(player)}",
                "Next caps: ${SheepMergeManager.progressionPrestigeNextCaps(player)}",
                "Base caps: ${SheepMergeManager.progressionPrestigeBaseCaps()}",
                "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.getPrestigeHigherMaxLevelCost(player).toLong())} prestige points",
                "Click to purchase"
            )))
            setItem(SheepMergeManager.PRESTIGE_START_EGGS_SLOT, MenuItemFactory.create(Material.SHEEP_SPAWN_EGG, "Start With Extra Eggs", listOf(
                "Level: ${SheepMergeManager.getPrestigeStartEggsLevel(player)}",
                "Extra starting eggs: +${SheepMergeManager.getStartEggsBonus(player)}",
                "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.getPrestigeStartEggsCost(player).toLong())} prestige points",
                "Click to purchase"
            )))
            setItem(SheepMergeManager.PRESTIGE_EGG_CAP_SLOT, MenuItemFactory.create(Material.EGG, "Egg Capacity", listOf(
                "Level: ${SheepMergeManager.getPrestigeEggCapLevel(player)}",
                "Egg cap: ${SheepMergeManager.getEggCap(player)}",
                "Adds: +${SheepMergeManager.progressionPrestigeEggCapStep()} eggs per level",
                "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.getPrestigeEggCapCost(player).toLong())} prestige points",
                "Click to purchase"
            )))
            setItem(SheepMergeManager.PRESTIGE_BACK_TO_UPGRADES_SLOT, backItem())
        }

        val baseTierLevel = SheepMergeManager.getBaseSpawnTierLevel(player)
        setItem(SheepMergeManager.PRESTIGE_BASE_SPAWN_TIER_SLOT, MenuItemFactory.create(Material.SHEEP_SPAWN_EGG, "Higher Base Spawn Tier", listOf(
            "Level: $baseTierLevel / ${SheepTier.RAINBOW.level}",
            "Current base tier: ${SheepTier.byLevel(baseTierLevel).displayName}",
            if (baseTierLevel >= SheepTier.RAINBOW.level) "MAXED" else "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.getPrestigeBaseSpawnTierCost(player).toLong())} prestige points",
            "Click to purchase"
        )))
        val questRewardLevel = SheepMergeManager.getPrestigeQuestRewardLevel(player)
        setItem(SheepMergeManager.PRESTIGE_QUEST_REWARD_SLOT, MenuItemFactory.create(Material.BOOK, "Quest Reward Boost", listOf(
            "Level: $questRewardLevel",
            "Quest rewards: +${SheepMergeManager.progressionPrestigeQuestRewardPercent(player)}%",
            "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.getPrestigeQuestRewardCost(player).toLong())} prestige points",
            "Click to purchase"
        )))
        val refundRemaining = SheepMergeManager.getPrestigeRefundRemainingMs(player)
        setItem(SheepMergeManager.PRESTIGE_REFUND_SLOT, MenuItemFactory.create(Material.BARRIER, "Refund Prestige Upgrades", listOf(
            "Refund amount: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionPrestigeRefundAmount(player).toLong())} prestige points",
            if (refundRemaining > 0L) "Cooldown: ${SheepMergeManager.progressionFormatDuration(refundRemaining)}" else "Cooldown: ready",
            "Resets prestige shop upgrades",
            "Click to refund"
        )))
    }

    @JvmStatic
    fun handlePrestigeMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.PRESTIGE_UPGRADE_SLOT -> {
                if (SheepMergeManager.progressionBlockPrestigePurchase(player)) return refreshPrestigeMenu(player)
                val gained = SheepMergeManager.progressionPrestige(player)
                if (gained > 0) {
                    SheepMergeManager.progressionPlayPrestigeSound(player)
                    player.sendMessage(SheepMergeManager.action("Prestige +$gained"))
                } else player.sendMessage(SheepMergeManager.warning("Not enough Coins."))
            }
            SheepMergeManager.PRESTIGE_DOUBLE_POINTS_SLOT -> {
                if (SheepMergeManager.progressionBlockPrestigePurchase(player)) return refreshPrestigeMenu(player)
                if (SheepMergeManager.progressionPrestigeDoublePointsMaxed(player)) {
                    player.sendMessage(SheepMergeManager.warning("Double Coins chance is maxed."))
                } else if (SheepMergeManager.progressionUpgradePrestigeDoublePoints(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Double Coins: ${SheepMergeManager.getDoublePointsChancePercent(player)}%"))
                } else player.sendMessage(SheepMergeManager.warning("Not enough prestige points."))
            }
            SheepMergeManager.PRESTIGE_HIGHER_MAX_LEVEL_SLOT -> {
                if (SheepMergeManager.progressionBlockPrestigePurchase(player)) return refreshPrestigeMenu(player)
                if (SheepMergeManager.progressionUpgradePrestigeHigherMaxLevel(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Higher max levels up"))
                } else player.sendMessage(SheepMergeManager.warning("Not enough prestige points."))
            }
            SheepMergeManager.PRESTIGE_START_EGGS_SLOT -> {
                if (SheepMergeManager.progressionBlockPrestigePurchase(player)) return refreshPrestigeMenu(player)
                if (SheepMergeManager.progressionUpgradePrestigeStartEggs(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Start eggs up"))
                } else player.sendMessage(SheepMergeManager.warning("Not enough prestige points."))
            }
            SheepMergeManager.PRESTIGE_EGG_CAP_SLOT -> {
                if (SheepMergeManager.progressionBlockPrestigePurchase(player)) return refreshPrestigeMenu(player)
                if (SheepMergeManager.progressionUpgradePrestigeEggCap(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Egg cap: ${SheepMergeManager.getEggCap(player)}"))
                } else player.sendMessage(SheepMergeManager.warning("Not enough prestige points."))
            }
            SheepMergeManager.PRESTIGE_BASE_SPAWN_TIER_SLOT -> {
                if (SheepMergeManager.progressionBlockPrestigePurchase(player)) return refreshPrestigeMenu(player)
                if (SheepMergeManager.getBaseSpawnTierLevel(player) >= SheepTier.RAINBOW.level) {
                    player.sendMessage(SheepMergeManager.warning("Base spawn tier maxed."))
                } else if (SheepMergeManager.progressionUpgradePrestigeBaseSpawnTier(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Base spawn tier: ${SheepMergeManager.getBaseSpawnTier(player).displayName}"))
                } else player.sendMessage(SheepMergeManager.warning("Not enough prestige points."))
            }
            SheepMergeManager.PRESTIGE_QUEST_REWARD_SLOT -> {
                if (SheepMergeManager.progressionBlockPrestigePurchase(player)) return refreshPrestigeMenu(player)
                if (SheepMergeManager.progressionUpgradePrestigeQuestReward(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Quest rewards: +${SheepMergeManager.progressionPrestigeQuestRewardPercent(player)}%"))
                } else player.sendMessage(SheepMergeManager.warning("Not enough prestige points."))
            }
            SheepMergeManager.PRESTIGE_REFUND_SLOT -> {
                if (SheepMergeManager.progressionBlockPrestigePurchase(player)) return refreshPrestigeMenu(player)
                val remaining = SheepMergeManager.getPrestigeRefundRemainingMs(player)
                if (remaining > 0L) player.sendMessage(SheepMergeManager.warning("Refund cooldown: ${SheepMergeManager.progressionFormatDuration(remaining)}"))
                else {
                    val amount = SheepMergeManager.progressionPrestigeRefundAmount(player)
                    if (amount <= 0) player.sendMessage(SheepMergeManager.warning("No prestige upgrades to refund."))
                    else if (SheepMergeManager.progressionTryRefundPrestigePoints(player)) {
                        SheepMergeManager.progressionPlayUpgradeSound(player)
                        player.sendMessage(SheepMergeManager.action("Refunded ${SheepMergeManager.formatPoints(amount.toLong())} prestige points."))
                    } else player.sendMessage(SheepMergeManager.warning("Refund is not available right now."))
                }
            }
            SheepMergeManager.PRESTIGE_BACK_TO_UPGRADES_SLOT -> return SheepMergeManager.openUpgradeMenu(player)
            else -> return
        }
        refreshPrestigeMenu(player)
    }

    private fun refreshPrestigeMenu(player: Player) {
        SheepMergeManager.updatePointsScoreboard(player)
        openPrestigeMenu(player)
    }

    @JvmStatic
    fun openQuestMenu(player: Player?) {
        if (player == null) return
        SheepMergeManager.progressionMarkTutorialQuestOpened(player)
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.QUEST_MENU_TITLE)
        renderQuestMenu(player, inventory::setItem)
        player.openInventory(inventory)
    }

    private fun refreshOpenQuestMenuItems(player: Player, inventory: Inventory) {
        renderQuestMenu(player) { slot, item -> setMenuItemIfChanged(inventory, slot, item) }
    }

    private fun renderQuestMenu(player: Player, setItem: (Int, ItemStack) -> Unit) {
        val playerId = player.uniqueId
        val remaining = SheepMergeManager.progressionQuestResetRemainingMs(player)
        val questPoints = SheepMergeManager.getQuestPoints(player)
        val luckyCost = SheepMergeManager.progressionQuestLuckyBurstCost(player)
        val woolCost = SheepMergeManager.progressionQuestWoolRushCost(player)
        val jackpotCost = SheepMergeManager.progressionQuestJackpotCost(player)
        val autoMergeCost = SheepMergeManager.progressionQuestAutoMergeCost(player)
        val autoShearCost = SheepMergeManager.progressionQuestAutoShearCost(player)

        setItem(SheepMergeManager.QUEST_BOARD_SLOT, MenuItemFactory.create(Material.BOOK, "Quest Board", listOf(
            "Quest points: ${SheepMergeManager.formatPoints(questPoints.toLong())}",
            if (remaining > 0L) "Next reset: ${SheepMergeManager.progressionFormatDuration(remaining)}" else "Next reset: incoming",
            questLine("Shear", SheepQuestState.questShearsComplete()[playerId] ?: false, SheepQuestState.questShears()[playerId] ?: 0, SheepMergeManager.progressionQuestShearsTarget(player), SheepMergeManager.progressionQuestShearsReward(player)),
            questLine("Spawn", SheepQuestState.questSpawnsComplete()[playerId] ?: false, SheepQuestState.questSpawns()[playerId] ?: 0, SheepMergeManager.progressionQuestSpawnsTarget(player), SheepMergeManager.progressionQuestSpawnsReward(player)),
            questLine("Merge", SheepQuestState.questMergesComplete()[playerId] ?: false, SheepQuestState.questMerges()[playerId] ?: 0, SheepMergeManager.progressionQuestMergesTarget(player), SheepMergeManager.progressionQuestMergesReward(player))
        )))
        setItem(SheepMergeManager.QUEST_ABILITY_LUCKY_BURST_SLOT, MenuItemFactory.create(Material.ENDER_EYE, "Lucky Burst", listOf(
            "&6Cost: &f${SheepMergeManager.formatPoints(luckyCost.toLong())} qp",
            "&bBoost: &f+${SheepMergeManager.progressionQuestLuckyBurstBonusPercent()}% tier chance",
            "&7Uses: &f${SheepMergeManager.progressionQuestLuckyBurstUseCount(player)}",
            if (questPoints >= luckyCost) "&aReady to buy" else "&cNeed more quest points",
            SheepMergeManager.progressionQuestLuckyBurstStatus(player),
            SheepMergeManager.progressionQuestLuckyBurstAction(player)
        ), SheepMergeManager.progressionQuestLuckyBurstActive(player)))
        setItem(SheepMergeManager.QUEST_ABILITY_WOOL_RUSH_SLOT, MenuItemFactory.create(Material.WHITE_WOOL, "Wool Rush", listOf(
            "&6Cost: &f${SheepMergeManager.formatPoints(woolCost.toLong())} qp",
            "&bBoost: &fwool grows 90% faster",
            "&7Time: &f${SheepMergeManager.progressionQuestWoolRushDuration(player)}",
            if (questPoints >= woolCost) "&aReady to buy" else "&cNeed more quest points",
            SheepMergeManager.progressionQuestWoolRushStatus(player),
            if (SheepMergeManager.progressionQuestWoolRushActive(player)) "&eClick: Extend" else "&aClick: Activate"
        ), SheepMergeManager.progressionQuestWoolRushActive(player)))
        setItem(SheepMergeManager.QUEST_ABILITY_JACKPOT_SHEARS_SLOT, MenuItemFactory.create(Material.GOLD_INGOT, "Jackpot Shears", listOf(
            "&6Cost: &f${SheepMergeManager.formatPoints(jackpotCost.toLong())} qp",
            "&bBoost: &fx${2 + SheepMergeManager.getQuestUpgradePowerLevel(player)} shear Coins",
            "&7Time: &f${SheepMergeManager.progressionQuestJackpotDuration(player)}",
            if (questPoints >= jackpotCost) "&aReady to buy" else "&cNeed more quest points",
            SheepMergeManager.progressionQuestJackpotStatus(player),
            if (SheepMergeManager.progressionQuestJackpotActive(player)) "&eClick: Extend" else "&aClick: Activate"
        ), SheepMergeManager.progressionQuestJackpotActive(player)))
        setItem(SheepMergeManager.QUEST_ABILITY_AUTO_MERGE_SLOT, MenuItemFactory.create(Material.ANVIL, "Merge Assist", listOf(
            "&6Cost: &f${SheepMergeManager.formatPoints(autoMergeCost.toLong())} qp",
            "&bBoost: &fauto-merges carried sheep",
            "&7Uses: &f${SheepMergeManager.progressionQuestAutoMergeUseCount(player)}",
            if (questPoints >= autoMergeCost) "&aReady to buy" else "&cNeed more quest points",
            SheepMergeManager.progressionQuestAutoMergeStatus(player),
            SheepMergeManager.progressionQuestAutoMergeAction(player)
        ), SheepMergeManager.progressionQuestAutoMergeActive(player)))
        setItem(SheepMergeManager.QUEST_ABILITY_AUTO_SHEAR_SLOT, MenuItemFactory.create(Material.SHEARS, "Shear All Sheep", listOf(
            "&6Cost: &f${SheepMergeManager.formatPoints(autoShearCost.toLong())} qp",
            "&bBoost: &fshears every ready sheep",
            "&7Uses: &f${SheepMergeManager.progressionQuestAutoShearUseCount(player)}",
            if (questPoints >= autoShearCost) "&aReady to buy" else "&cNeed more quest points",
            SheepMergeManager.progressionQuestAutoShearStatus(player),
            SheepMergeManager.progressionQuestAutoShearAction(player)
        ), SheepMergeManager.progressionQuestAutoShearActive(player)))
        setItem(SheepMergeManager.QUEST_OPEN_UPGRADES_SLOT, MenuItemFactory.create(Material.ENCHANTED_BOOK, "Quest Upgrades", listOf(
            "&7Duration Lv: &e${SheepMergeManager.getQuestUpgradeDurationLevel(player)}",
            "&7Power Lv: &e${SheepMergeManager.getQuestUpgradePowerLevel(player)}",
            "&aClick: Open"
        )))
        setItem(SheepMergeManager.QUEST_BACK_TO_UPGRADES_SLOT, MenuItemFactory.create(Material.ARROW, "Back To Upgrades", listOf(
            "&7Quest Points: &e${SheepMergeManager.formatPoints(questPoints.toLong())}",
            if (remaining > 0L) "&7Reset: &b${SheepMergeManager.progressionFormatDuration(remaining)}" else "&7Reset: &bincoming",
            "&aClick: Back"
        )))
    }

    private fun questLine(name: String, complete: Boolean, progress: Int, target: Int, reward: Int): String =
        "${if (complete) "DONE" else "TODO"} $name $progress/$target (${SheepMergeManager.formatPoints(reward.toLong())} pts)"

    @JvmStatic
    fun handleQuestMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.QUEST_ABILITY_LUCKY_BURST_SLOT -> handleLuckyBurst(player)
            SheepMergeManager.QUEST_ABILITY_WOOL_RUSH_SLOT -> handleWoolRush(player)
            SheepMergeManager.QUEST_ABILITY_JACKPOT_SHEARS_SLOT -> handleJackpotShears(player)
            SheepMergeManager.QUEST_ABILITY_AUTO_MERGE_SLOT -> handleAutoMerge(player)
            SheepMergeManager.QUEST_ABILITY_AUTO_SHEAR_SLOT -> handleAutoShear(player)
            SheepMergeManager.QUEST_OPEN_UPGRADES_SLOT -> {
                SheepMergeManager.progressionMarkTutorialQuestUpgradesOpened(player)
                return SheepMergeManager.openQuestUpgradesMenu(player)
            }
            SheepMergeManager.QUEST_BACK_TO_UPGRADES_SLOT -> return SheepMergeManager.openUpgradeMenu(player)
            else -> return
        }
        openQuestMenu(player)
    }

    private fun handleLuckyBurst(player: Player) {
        if (SheepMergeManager.progressionToggleQuestLuckyBurst(player)) {
            val enabled = SheepQuestState.luckyBurstEnabled()[player.uniqueId] ?: true
            player.sendMessage(SheepMergeManager.action("Lucky Burst ${if (enabled) "enabled." else "disabled."}"))
            return
        }
        if (SheepMergeManager.progressionBlockQuestAbilityPurchase(player)) return
        if (!SheepMergeManager.progressionActivateQuestLuckyBurst(player)) return player.sendMessage(SheepMergeManager.warning("Not enough quest points."))
        SheepMergeManager.progressionMarkTutorialAbilityUsed(player)
        questEffect(player, Particle.TOTEM, 18, 0.45, 0.45, 0.45, 0.0, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.9f, 1.5f)
        player.sendMessage(SheepMergeManager.action("Lucky Burst active."))
    }

    private fun handleWoolRush(player: Player) {
        if (SheepMergeManager.progressionBlockQuestAbilityPurchase(player)) return
        val active = SheepMergeManager.progressionQuestWoolRushActive(player)
        if (!SheepMergeManager.progressionApplyQuestWoolRush(player, active)) return player.sendMessage(SheepMergeManager.warning("Not enough quest points."))
        SheepMergeManager.progressionMarkTutorialAbilityUsed(player)
        SheepMergeManager.progressionApplyWoolRushToShearedSheep(player)
        questEffect(player, Particle.SPORE_BLOSSOM_AIR, 28, 0.5, 0.35, 0.5, 0.01, Sound.BLOCK_MOSS_CARPET_PLACE, 1.0f, 0.8f)
        player.sendMessage(SheepMergeManager.action(if (active) "Wool Rush extended." else "Wool Rush active."))
    }

    private fun handleJackpotShears(player: Player) {
        if (SheepMergeManager.progressionBlockQuestAbilityPurchase(player)) return
        val active = SheepMergeManager.progressionQuestJackpotActive(player)
        if (!SheepMergeManager.progressionApplyQuestJackpot(player, active)) return player.sendMessage(SheepMergeManager.warning("Not enough quest points."))
        SheepMergeManager.progressionMarkTutorialAbilityUsed(player)
        questEffect(player, Particle.FIREWORKS_SPARK, 22, 0.45, 0.45, 0.45, 0.02, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.6f)
        player.sendMessage(SheepMergeManager.action(if (active) "Jackpot Shears extended." else "Jackpot Shears active."))
    }

    private fun handleAutoMerge(player: Player) {
        if (SheepMergeManager.progressionToggleQuestAutoMerge(player)) {
            val enabled = SheepQuestState.autoMergeEnabled()[player.uniqueId] ?: true
            player.sendMessage(SheepMergeManager.action("Merge Assist ${if (enabled) "enabled." else "disabled."}"))
            return
        }
        if (SheepMergeManager.progressionBlockQuestAbilityPurchase(player)) return
        if (!SheepMergeManager.progressionActivateQuestAutoMerge(player)) return player.sendMessage(SheepMergeManager.warning("Not enough quest points."))
        SheepMergeManager.progressionMarkTutorialAbilityUsed(player)
        SheepQuestState.nextAutoMergeAt()[player.uniqueId] = 0L
        questEffect(player, Particle.WAX_ON, 26, 0.5, 0.4, 0.5, 0.03, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.3f)
        player.sendMessage(SheepMergeManager.action("Merge Assist active."))
    }

    private fun handleAutoShear(player: Player) {
        if (SheepMergeManager.progressionToggleQuestAutoShear(player)) {
            val enabled = SheepQuestState.autoShearEnabled()[player.uniqueId] ?: true
            player.sendMessage(SheepMergeManager.action("Shear All Sheep ${if (enabled) "enabled." else "disabled."}"))
            return
        }
        if (SheepMergeManager.progressionBlockQuestAbilityPurchase(player)) return
        if (!SheepMergeManager.progressionActivateQuestAutoShear(player)) return player.sendMessage(SheepMergeManager.warning("Not enough quest points."))
        SheepMergeManager.progressionMarkTutorialAbilityUsed(player)
        SheepQuestState.nextAutoShearAt()[player.uniqueId] = 0L
        questEffect(player, Particle.WAX_OFF, 26, 0.5, 0.4, 0.5, 0.03, Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.4f)
        player.sendMessage(SheepMergeManager.action("Shear All Sheep active."))
    }

    private fun questEffect(player: Player, particle: Particle, count: Int, offsetX: Double, offsetY: Double, offsetZ: Double, extra: Double, sound: Sound, volume: Float, pitch: Float) {
        val height = if (particle == Particle.SPORE_BLOSSOM_AIR || particle == Particle.WAX_ON || particle == Particle.WAX_OFF) 2.0 else 2.1
        SheepMergeManager.progressionSpawnParticle(player, particle, player.location.add(0.0, height, 0.0), count, offsetX, offsetY, offsetZ, extra)
        SheepMergeManager.progressionPlaySound(player, sound, volume, pitch)
    }

    @JvmStatic
    fun openAutomationMenu(player: Player?) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.AUTOMATION_MENU_TITLE)
        renderAutomationMenu(player, inventory::setItem)
        player.openInventory(inventory)
    }

    private fun refreshOpenAutomationMenuItems(player: Player, inventory: Inventory) {
        renderAutomationMenu(player) { slot, item -> setMenuItemIfChanged(inventory, slot, item) }
    }

    private fun renderAutomationMenu(player: Player, setItem: (Int, ItemStack) -> Unit) {
        val autoBuyLevel = SheepMergeManager.getAutomationAutoBuyUpgradeLevel(player)
        val autoAbilityLevel = SheepMergeManager.getAutomationAutoAbilityUpgradeLevel(player)
        val autoMergeLevel = SheepMergeManager.getAutomationSlowAutoMergeUpgradeLevel(player)
        val autoShearLevel = SheepMergeManager.getAutomationSlowAutoShearUpgradeLevel(player)
        val autoPrestigeLevel = SheepMergeManager.getAutomationAutoPrestigeUpgradeLevel(player)
        val autoSpawnLevel = SheepMergeManager.getAutomationAutoSpawnUpgradeLevel(player)
        val autoBuyMax = SheepMergeManager.progressionAutomationAutoBuyMaxLevel()
        val autoAbilityMax = SheepMergeManager.progressionAutomationAutoAbilityMaxLevel()
        val autoMergeMax = SheepMergeManager.progressionAutomationSlowAutoMergeMaxLevel()
        val autoShearMax = SheepMergeManager.progressionAutomationSlowAutoShearMaxLevel()
        val autoSpawnMax = SheepMergeManager.progressionAutomationAutoSpawnMaxLevel()

        setItem(4, MenuItemFactory.create(Material.EXPERIENCE_BOTTLE, "Automation Points", listOf(
            "&7Current: &e${SheepMergeManager.formatPoints(SheepMergeManager.getAutomationPoints(player).toLong())}",
            "&bEarned while farming"
        )))
        setItem(SheepMergeManager.AUTOMATION_AUTO_BUY_SLOT, MenuItemFactory.create(Material.HOPPER, "Auto Buy Upgrades", listOf(
            "&7Level: &e$autoBuyLevel / $autoBuyMax",
            if (SheepMergeManager.isAutomationAutoBuyEnabled(player)) "&aStatus: ON" else "&cStatus: OFF",
            "&7Rate: &b${SheepMergeManager.progressionAutomationAutoBuyRate(player)}",
            if (autoBuyLevel >= autoBuyMax) "&6Cost: &aMAXED" else "&6Cost: &f${SheepMergeManager.formatPoints(SheepMergeManager.progressionAutomationAutoBuyUpgradeCost(player).toLong())} AP",
            "&fBuys cheap upgrades",
            if (autoBuyLevel >= autoBuyMax) "&aClick: Maxed" else "&aClick: Upgrade"
        )))
        setItem(SheepMergeManager.AUTOMATION_AUTO_ABILITY_SLOT, MenuItemFactory.create(Material.BREWING_STAND, "Auto Activate Abilities", listOf(
            "&7Level: &e$autoAbilityLevel / $autoAbilityMax",
            if (SheepMergeManager.isAutomationAutoAbilityEnabled(player)) "&aStatus: ON" else "&cStatus: OFF",
            "&7Rate: &b${if (autoAbilityLevel >= autoAbilityMax) "instant" else SheepMergeManager.progressionAutomationAutoAbilityRate()}",
            if (autoAbilityLevel >= autoAbilityMax) "&6Cost: &aMAXED" else "&6Cost: &f${SheepMergeManager.formatPoints(SheepMergeManager.progressionAutomationAutoAbilityUpgradeCost(player).toLong())} AP",
            if (autoAbilityLevel >= autoAbilityMax) "&fInstant ability refill" else if (autoAbilityLevel >= 2) "&fBuys every missing ability" else "&fBuys one missing ability",
            if (autoAbilityLevel >= autoAbilityMax) "&bFully automatic" else "&7Upgrade for instant refill",
            if (autoAbilityLevel >= autoAbilityMax) "&aClick: Maxed" else "&aClick: Upgrade"
        )))
        setItem(SheepMergeManager.AUTOMATION_SLOW_AUTO_MERGE_SLOT, MenuItemFactory.create(Material.ANVIL, "Auto Merge", listOf(
            "&7Level: &e$autoMergeLevel / $autoMergeMax",
            if (SheepMergeManager.isAutomationSlowAutoMergeEnabled(player)) "&aStatus: ON" else "&cStatus: OFF",
            "&7Rate: &b${SheepMergeManager.progressionAutomationSlowAutoMergeRate(player)}",
            if (autoMergeLevel >= autoMergeMax) "&6Cost: &aMAXED" else "&6Cost: &f${SheepMergeManager.formatPoints(SheepMergeManager.progressionAutomationSlowAutoMergeUpgradeCost(player).toLong())} AP",
            "&fMerges one pair each cycle",
            if (autoMergeLevel >= autoMergeMax) "&aClick: Maxed" else "&aClick: Upgrade"
        )))
        setItem(SheepMergeManager.AUTOMATION_AUTO_PRESTIGE_SLOT, MenuItemFactory.create(Material.NETHER_STAR, "Auto Prestige", listOf(
            "&7Level: &e$autoPrestigeLevel / 1",
            if (SheepMergeManager.isAutomationAutoPrestigeEnabled(player)) "&aStatus: ON" else "&cStatus: OFF",
            "&7Rate: &b${SheepMergeManager.progressionAutomationAutoPrestigeRate()}",
            if (autoPrestigeLevel > 0) "&6Cost: &aMAXED" else "&6Cost: &f${SheepMergeManager.formatPoints(SheepMergeManager.progressionAutomationAutoPrestigeUpgradeCost(player).toLong())} AP",
            "&fPrestiges when affordable",
            if (autoPrestigeLevel > 0) "&aClick: Maxed" else "&aClick: Unlock"
        )))
        setItem(SheepMergeManager.AUTOMATION_SLOW_AUTO_SHEAR_SLOT, MenuItemFactory.create(Material.SHEARS, "Auto Shear", listOf(
            "&7Level: &e$autoShearLevel / $autoShearMax",
            if (SheepMergeManager.isAutomationSlowAutoShearEnabled(player)) "&aStatus: ON" else "&cStatus: OFF",
            "&7Rate: &b${SheepMergeManager.progressionAutomationSlowAutoShearRate(player)}",
            if (autoShearLevel >= autoShearMax) "&6Cost: &aMAXED" else "&6Cost: &f${SheepMergeManager.formatPoints(SheepMergeManager.progressionAutomationSlowAutoShearUpgradeCost(player).toLong())} AP",
            "&fShears ready sheep each cycle",
            if (autoShearLevel >= autoShearMax) "&aClick: Maxed" else "&aClick: Upgrade"
        )))
        setItem(SheepMergeManager.AUTOMATION_AUTO_SPAWN_SLOT, MenuItemFactory.create(Material.SHEEP_SPAWN_EGG, "Auto Spawn Sheep", listOf(
            "&7Level: &e$autoSpawnLevel / $autoSpawnMax",
            if (SheepMergeManager.isAutomationAutoSpawnEnabled(player)) "&aStatus: ON" else "&cStatus: OFF",
            "&7Rate: &b${SheepMergeManager.progressionAutomationAutoSpawnRate(player)}",
            "&fDrops sheep from the sky",
            if (autoSpawnLevel >= autoSpawnMax) "&6Cost: &aMAXED" else "&6Cost: &f${SheepMergeManager.formatPoints(SheepMergeManager.progressionAutomationAutoSpawnUpgradeCost(player).toLong())} AP",
            "&7Uses eggs automatically",
            if (autoSpawnLevel >= autoSpawnMax) "&aClick: Maxed" else "&aClick: Upgrade"
        )))

        automationToggleItem(setItem, SheepMergeManager.AUTOMATION_AUTO_BUY_TOGGLE_SLOT, "Toggle Auto Buy", SheepMergeManager.isAutomationAutoBuyEnabled(player), autoBuyLevel)
        automationToggleItem(setItem, SheepMergeManager.AUTOMATION_AUTO_ABILITY_TOGGLE_SLOT, "Toggle Auto Ability", SheepMergeManager.isAutomationAutoAbilityEnabled(player), autoAbilityLevel)
        automationToggleItem(setItem, SheepMergeManager.AUTOMATION_AUTO_SPAWN_TOGGLE_SLOT, "Toggle Auto Spawn", SheepMergeManager.isAutomationAutoSpawnEnabled(player), autoSpawnLevel)
        automationToggleItem(setItem, SheepMergeManager.AUTOMATION_SLOW_AUTO_MERGE_TOGGLE_SLOT, "Toggle Auto Merge", SheepMergeManager.isAutomationSlowAutoMergeEnabled(player), autoMergeLevel)
        automationToggleItem(setItem, SheepMergeManager.AUTOMATION_SLOW_AUTO_SHEAR_TOGGLE_SLOT, "Toggle Auto Shear", SheepMergeManager.isAutomationSlowAutoShearEnabled(player), autoShearLevel)
        automationToggleItem(setItem, SheepMergeManager.AUTOMATION_AUTO_PRESTIGE_TOGGLE_SLOT, "Toggle Auto Prestige", SheepMergeManager.isAutomationAutoPrestigeEnabled(player), autoPrestigeLevel)

        val unlocked = SheepMergeManager.progressionUnlockedAutomationCount(player)
        setItem(SheepMergeManager.AUTOMATION_ENABLE_ALL_SLOT, MenuItemFactory.create(Material.LIME_DYE, "Enable All", listOf(
            "&7Unlocked: &e$unlocked / 6",
            if (unlocked > 0) "&aClick: Enable unlocked tracks" else "&cUnlock an automation first"
        )))
        setItem(SheepMergeManager.AUTOMATION_DISABLE_ALL_SLOT, MenuItemFactory.create(Material.GRAY_DYE, "Disable All", listOf(
            "&7Unlocked: &e$unlocked / 6",
            if (unlocked > 0) "&cClick: Disable unlocked tracks" else "&cUnlock an automation first"
        )))
        setItem(SheepMergeManager.AUTOMATION_BACK_TO_UPGRADES_SLOT, backItem())
    }

    private fun automationToggleItem(setItem: (Int, ItemStack) -> Unit, slot: Int, name: String, enabled: Boolean, level: Int) {
        setItem(slot, MenuItemFactory.create(Material.LEVER, name, listOf(
            if (enabled) "&aCurrent: ON" else "&cCurrent: OFF",
            if (level > 0) "&aClick: Toggle" else "&cBuy level 1 first"
        )))
    }

    @JvmStatic
    fun handleAutomationMenuClick(player: Player?, slot: Int, reopenMenu: Boolean = true) {
        if (player == null) return
        val message = when (slot) {
            SheepMergeManager.AUTOMATION_AUTO_BUY_SLOT -> automationUpgrade(player, SheepMergeManager.getAutomationAutoBuyUpgradeLevel(player) >= SheepMergeManager.progressionAutomationAutoBuyMaxLevel(), "Auto Buy", "upgraded") { SheepMergeManager.progressionUpgradeAutomationAutoBuy(player) }
            SheepMergeManager.AUTOMATION_AUTO_ABILITY_SLOT -> automationUpgrade(player, SheepMergeManager.getAutomationAutoAbilityUpgradeLevel(player) >= SheepMergeManager.progressionAutomationAutoAbilityMaxLevel(), "Auto Ability", "upgraded") { SheepMergeManager.progressionUpgradeAutomationAutoAbility(player) }
            SheepMergeManager.AUTOMATION_SLOW_AUTO_MERGE_SLOT -> automationUpgrade(player, SheepMergeManager.getAutomationSlowAutoMergeUpgradeLevel(player) >= SheepMergeManager.progressionAutomationSlowAutoMergeMaxLevel(), "Auto Merge", "upgraded") { SheepMergeManager.progressionUpgradeAutomationSlowAutoMerge(player) }
            SheepMergeManager.AUTOMATION_SLOW_AUTO_SHEAR_SLOT -> automationUpgrade(player, SheepMergeManager.getAutomationSlowAutoShearUpgradeLevel(player) >= SheepMergeManager.progressionAutomationSlowAutoShearMaxLevel(), "Auto Shear", "upgraded") { SheepMergeManager.progressionUpgradeAutomationSlowAutoShear(player) }
            SheepMergeManager.AUTOMATION_AUTO_PRESTIGE_SLOT -> automationUpgrade(player, SheepMergeManager.getAutomationAutoPrestigeUpgradeLevel(player) > 0, "Auto Prestige", "unlocked") { SheepMergeManager.progressionUpgradeAutomationAutoPrestige(player) }
            SheepMergeManager.AUTOMATION_AUTO_SPAWN_SLOT -> automationUpgrade(player, SheepMergeManager.getAutomationAutoSpawnUpgradeLevel(player) >= SheepMergeManager.progressionAutomationAutoSpawnMaxLevel(), "Auto Spawn", "upgraded") { SheepMergeManager.progressionUpgradeAutomationAutoSpawn(player) }
            SheepMergeManager.AUTOMATION_AUTO_BUY_TOGGLE_SLOT -> automationToggle(player, SheepMergeManager.getAutomationAutoBuyUpgradeLevel(player), "Auto Buy") { SheepMergeManager.progressionToggleAutomationAutoBuy(player) }
            SheepMergeManager.AUTOMATION_AUTO_ABILITY_TOGGLE_SLOT -> automationToggle(player, SheepMergeManager.getAutomationAutoAbilityUpgradeLevel(player), "Auto Ability") { SheepMergeManager.progressionToggleAutomationAutoAbility(player) }
            SheepMergeManager.AUTOMATION_SLOW_AUTO_MERGE_TOGGLE_SLOT -> automationToggle(player, SheepMergeManager.getAutomationSlowAutoMergeUpgradeLevel(player), "Auto Merge") { SheepMergeManager.progressionToggleAutomationSlowAutoMerge(player) }
            SheepMergeManager.AUTOMATION_SLOW_AUTO_SHEAR_TOGGLE_SLOT -> automationToggle(player, SheepMergeManager.getAutomationSlowAutoShearUpgradeLevel(player), "Auto Shear") { SheepMergeManager.progressionToggleAutomationSlowAutoShear(player) }
            SheepMergeManager.AUTOMATION_AUTO_PRESTIGE_TOGGLE_SLOT -> automationToggle(player, SheepMergeManager.getAutomationAutoPrestigeUpgradeLevel(player), "Auto Prestige") { SheepMergeManager.progressionToggleAutomationAutoPrestige(player) }
            SheepMergeManager.AUTOMATION_AUTO_SPAWN_TOGGLE_SLOT -> automationToggle(player, SheepMergeManager.getAutomationAutoSpawnUpgradeLevel(player), "Auto Spawn") { SheepMergeManager.progressionToggleAutomationAutoSpawn(player) }
            SheepMergeManager.AUTOMATION_ENABLE_ALL_SLOT -> automationSetAll(player, true)
            SheepMergeManager.AUTOMATION_DISABLE_ALL_SLOT -> automationSetAll(player, false)
            SheepMergeManager.AUTOMATION_BACK_TO_UPGRADES_SLOT -> return SheepMergeManager.openUpgradeMenu(player)
            else -> return
        }
        player.sendMessage(message)
        SheepMergeManager.updatePointsScoreboard(player)
        if (reopenMenu) openAutomationMenu(player)
    }

    private inline fun automationUpgrade(player: Player, maxed: Boolean, name: String, verb: String, upgrade: () -> Boolean): String {
        if (maxed) return SheepMergeManager.warning("$name is already maxed.")
        if (!upgrade()) return SheepMergeManager.warning("Not enough automation points.")
        SheepMergeManager.progressionPlayUpgradeSound(player)
        return SheepMergeManager.action("$name $verb.")
    }

    private inline fun automationToggle(player: Player, level: Int, name: String, toggle: () -> Boolean): String {
        if (level <= 0) return SheepMergeManager.warning("Buy $name${if (name == "Auto Prestige") "" else " level 1"} first.")
        val enabled = toggle()
        return SheepMergeManager.action("$name ${if (enabled) "enabled" else "disabled"}.")
    }

    private fun automationSetAll(player: Player, enabled: Boolean): String {
        if (SheepMergeManager.progressionUnlockedAutomationCount(player) <= 0) {
            return SheepMergeManager.warning("Unlock at least one automation first.")
        }
        val changed = SheepMergeManager.progressionSetAllAutomationsEnabled(player, enabled)
        val action = if (enabled) "Enabled" else "Disabled"
        return SheepMergeManager.action(if (changed > 0) "$action all unlocked automations." else "All unlocked automations are already ${if (enabled) "enabled" else "disabled"}.")
    }

    @JvmStatic
    fun tickOpenMenuStatRefresh(player: Player?) {
        if (player == null) return
        val view = player.openInventory ?: return
        val inventory = view.topInventory ?: return
        when (view.title) {
            SheepMergeManager.UPGRADE_MENU_TITLE -> refreshOpenUpgradeMenuItems(player, inventory)
            SheepMergeManager.PRESTIGE_MENU_TITLE -> refreshOpenPrestigeMenuItems(player, inventory)
            SheepMergeManager.QUEST_MENU_TITLE -> refreshOpenQuestMenuItems(player, inventory)
            SheepMergeManager.SHOP_MENU_TITLE -> refreshOpenShopMenuItems(player, inventory)
            SheepMergeManager.COMBO_SHOP_MENU_TITLE -> refreshOpenComboMenuItems(player, inventory)
            SheepMergeManager.AUTOMATION_MENU_TITLE -> refreshOpenAutomationMenuItems(player, inventory)
            SheepMergeManager.SOCIALS_MENU_TITLE -> SheepMergeManager.refreshOpenSocialsMenuItems(player, inventory)
        }
    }

    @JvmStatic
    fun openComboShopMenu(player: Player?) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.COMBO_SHOP_MENU_TITLE)
        renderComboMenu(player, inventory::setItem, false)
        player.openInventory(inventory)
    }

    @JvmStatic
    fun refreshOpenComboMenuItems(player: Player?, inventory: Inventory?) {
        if (player == null || inventory == null) return
        renderComboMenu(player, { slot, item -> setMenuItemIfChanged(inventory, slot, item) }, true)
    }

    private fun renderComboMenu(player: Player, setItem: (Int, ItemStack) -> Unit, refreshing: Boolean) {
        val decayLevel = SheepMergeManager.getComboDecayUpgradeLevel(player)
        val gainLevel = SheepMergeManager.getComboGainUpgradeLevel(player)
        val maxLevel = SheepMergeManager.getComboMaxUpgradeLevel(player)
        setItem(SheepMergeManager.COMBO_DECAY_SLOT, MenuItemFactory.create(
            Material.CLOCK, "Slower Combo Decay", listOf(
                "Level: $decayLevel / ${SheepMergeManager.progressionComboDecayMaxLevel()}",
                "Decay speed: ${Math.round(SheepMergeManager.progressionComboDecayMultiplier(player) * 100).toInt()}%",
                if (decayLevel >= SheepMergeManager.progressionComboDecayMaxLevel()) "MAXED"
                else "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionComboDecayUpgradeCost(player))} ${if (refreshing) "Coins" else "points"}",
                "Click to purchase"
            )
        ))
        setItem(SheepMergeManager.COMBO_MAX_SLOT, MenuItemFactory.create(
            Material.NETHER_STAR, "Maximum Combo", listOf(
                "Level: $maxLevel",
                "Max score: ${Math.floor(SheepMergeManager.progressionComboMaxScore(player)).toInt()}",
                "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionComboMaxUpgradePrestigeCost(player).toLong())} prestige points",
                "Click to purchase"
            )
        ))
        setItem(SheepMergeManager.COMBO_GAIN_SLOT, MenuItemFactory.create(
            Material.EMERALD, "Combo Gain Percentage", listOf(
                "Level: $gainLevel / ${SheepMergeManager.progressionComboGainMaxLevel()}",
                "Combo gain boost: +${Math.round(gainLevel * SheepMergeManager.progressionComboGainPercentPerLevel()).toInt()}%",
                if (gainLevel >= SheepMergeManager.progressionComboGainMaxLevel()) "MAXED"
                else "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionComboGainUpgradeCost(player))} ${if (refreshing) "Coins" else "points"}",
                "Click to purchase"
            )
        ))
        if (!refreshing) setItem(SheepMergeManager.COMBO_BACK_TO_UPGRADES_SLOT, backItem())
    }

    @JvmStatic
    fun handleComboShopMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.COMBO_DECAY_SLOT -> {
                if (SheepMergeManager.progressionUpgradeComboDecay(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Combo decay slowed."))
                } else player.sendMessage(SheepMergeManager.warning("Unable to buy combo decay upgrade."))
            }
            SheepMergeManager.COMBO_MAX_SLOT -> {
                if (SheepMergeManager.progressionUpgradeComboMax(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Combo max increased."))
                } else player.sendMessage(SheepMergeManager.warning("Unable to buy combo max upgrade."))
            }
            SheepMergeManager.COMBO_GAIN_SLOT -> {
                if (SheepMergeManager.progressionUpgradeComboGain(player)) {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Combo score gain increased."))
                } else player.sendMessage(SheepMergeManager.warning("Unable to buy combo gain upgrade."))
            }
            SheepMergeManager.COMBO_BACK_TO_UPGRADES_SLOT -> {
                SheepMergeManager.openUpgradeMenu(player)
                return
            }
            else -> return
        }
        SheepMergeManager.progressionUpdateComboBossBar(player)
        SheepMergeManager.updatePointsScoreboard(player)
        openComboShopMenu(player)
    }

    @JvmStatic
    fun openShopMenu(player: Player?) {
        if (player == null) return
        SheepMergeManager.progressionMarkTutorialShearShopOpened(player)
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.SHOP_MENU_TITLE)
        renderShopMenu(player, inventory::setItem)
        player.openInventory(inventory)
    }

    @JvmStatic
    fun refreshOpenShopMenuItems(player: Player?, inventory: Inventory?) {
        if (player == null || inventory == null) return
        renderShopMenu(player) { slot, item -> setMenuItemIfChanged(inventory, slot, item) }
    }

    @JvmStatic
    fun handleShopMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.SHOP_SHEAR_SLOT -> {
                if (!SheepMergeManager.progressionBlockShearUpgradePurchase(player)) {
                    if (SheepMergeManager.progressionUpgradeShearShop(player)) {
                        SheepMergeManager.progressionMarkTutorialShearUpgraded(player)
                        SheepMergeManager.progressionPlayUpgradeSound(player)
                        player.sendMessage(SheepMergeManager.action("Shear shop +1"))
                    } else player.sendMessage(SheepMergeManager.warning("Not enough Coins."))
                }
            }
            SheepMergeManager.SHOP_SHEAR_KEEP_WOOL_SLOT -> {
                if (!SheepMergeManager.progressionBlockShearUpgradePurchase(player)) {
                    if (SheepMergeManager.progressionUpgradeShearWoolSave(player)) {
                        SheepMergeManager.progressionMarkTutorialShearUpgraded(player)
                        SheepMergeManager.progressionPlayUpgradeSound(player)
                        player.sendMessage(SheepMergeManager.action("Wool Keeper: ${SheepMergeManager.getShearWoolSaveChancePercent(player)}%"))
                    } else player.sendMessage(SheepMergeManager.warning("Unable to buy Wool Keeper upgrade."))
                }
            }
            SheepMergeManager.SHOP_SHEAR_TIER_BOOST_SLOT -> {
                if (!SheepMergeManager.progressionBlockShearUpgradePurchase(player)) {
                    if (SheepMergeManager.progressionUpgradeShearTierBoost(player)) {
                        SheepMergeManager.progressionMarkTutorialShearUpgraded(player)
                        SheepMergeManager.progressionPlayUpgradeSound(player)
                        player.sendMessage(SheepMergeManager.action("Tier Booster: ${SheepMergeManager.getShearTierBoostChancePercent(player)}%"))
                    } else player.sendMessage(SheepMergeManager.warning("Unable to buy Tier Booster upgrade."))
                }
            }
            SheepMergeManager.SHOP_BACK_TO_UPGRADES_SLOT -> {
                SheepMergeManager.openUpgradeMenu(player)
                return
            }
            else -> return
        }
        SheepMergeManager.updatePointsScoreboard(player)
        openShopMenu(player)
    }

    @JvmStatic
    fun openQuestUpgradesMenu(player: Player?) {
        if (player == null) return
        SheepMergeManager.progressionMarkTutorialQuestUpgradesOpened(player)
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.QUEST_UPGRADES_MENU_TITLE)
        inventory.setItem(SheepMergeManager.QUEST_UPGRADE_DURATION_SLOT, MenuItemFactory.create(
            Material.CLOCK, "Extended Buff Duration", listOf(
                "Level: ${SheepMergeManager.getQuestUpgradeDurationLevel(player)}",
                "+30s ability duration per level",
                "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionQuestUpgradeDurationCost(player).toLong())} quest points",
                "Click: Upgrade"
            )
        ))
        inventory.setItem(SheepMergeManager.QUEST_UPGRADE_POWER_SLOT, MenuItemFactory.create(
            Material.BLAZE_POWDER, "Amplified Buff Power", listOf(
                "Level: ${SheepMergeManager.getQuestUpgradePowerLevel(player)}",
                "Jackpot Shears: +1x per level",
                "Quest ability costs: -1 per level",
                "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.progressionQuestUpgradePowerCost(player).toLong())} quest points",
                "Click: Upgrade"
            )
        ))
        inventory.setItem(SheepMergeManager.QUEST_UPGRADE_BACK_SLOT, MenuItemFactory.create(
            Material.ARROW, "Back To Quest Abilities", listOf("Click to go back")
        ))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleQuestUpgradeMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        SheepMergeManager.progressionMarkTutorialQuestUpgradesOpened(player)
        when (slot) {
            SheepMergeManager.QUEST_UPGRADE_DURATION_SLOT -> {
                if (!SheepMergeManager.progressionBlockQuestUpgradePurchase(player)) {
                    if (SheepMergeManager.progressionUpgradeQuestDuration(player)) {
                        SheepMergeManager.progressionPlayUpgradeSound(player)
                        player.sendMessage(SheepMergeManager.action("Quest duration upgrade purchased."))
                    } else player.sendMessage(SheepMergeManager.warning("Not enough quest points."))
                }
            }
            SheepMergeManager.QUEST_UPGRADE_POWER_SLOT -> {
                if (!SheepMergeManager.progressionBlockQuestUpgradePurchase(player)) {
                    if (SheepMergeManager.progressionUpgradeQuestPower(player)) {
                        SheepMergeManager.progressionPlayUpgradeSound(player)
                        player.sendMessage(SheepMergeManager.action("Quest power upgrade purchased."))
                    } else player.sendMessage(SheepMergeManager.warning("Not enough quest points."))
                }
            }
            SheepMergeManager.QUEST_UPGRADE_BACK_SLOT -> {
                SheepMergeManager.openQuestMenu(player)
                return
            }
            else -> return
        }
        openQuestUpgradesMenu(player)
    }

    private fun renderShopMenu(player: Player, setItem: (Int, ItemStack) -> Unit) {
        val woolSaveLevel = SheepMergeManager.getShearWoolSaveLevel(player)
        val tierBoostLevel = SheepMergeManager.getShearTierBoostLevel(player)
        setItem(SheepMergeManager.SHOP_SHEAR_SLOT, MenuItemFactory.create(
            Material.SHEARS, "Shear Value", listOf(
                "Level: ${SheepMergeManager.getShearPointGainUpgradeLevel(player)}",
                "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.getShearUpgradeCost(player))} Coins",
                "Coins: base x${SheepMergeManager.getShearPointMultiplier(player)}",
                "Wool reward scales with level",
                "Click to purchase"
            )
        ))
        setItem(SheepMergeManager.SHOP_SHEAR_KEEP_WOOL_SLOT, MenuItemFactory.create(
            Material.WHITE_WOOL, "Wool Keeper", listOf(
                "Level: $woolSaveLevel / ${SheepMergeManager.progressionShearWoolSaveMaxLevel()}",
                "Chance: ${SheepMergeManager.getShearWoolSaveChancePercent(player)}%",
                if (woolSaveLevel >= SheepMergeManager.progressionShearWoolSaveMaxLevel()) "MAXED"
                else "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.getShearWoolSaveUpgradeCost(player))} Coins",
                "Chance for sheep to keep wool when sheared"
            )
        ))
        setItem(SheepMergeManager.SHOP_SHEAR_TIER_BOOST_SLOT, MenuItemFactory.create(
            Material.GLOWSTONE_DUST, "Tier Booster", listOf(
                "Level: $tierBoostLevel / ${SheepMergeManager.progressionShearTierBoostMaxLevel()}",
                "Chance: ${SheepMergeManager.getShearTierBoostChancePercent(player)}%",
                if (tierBoostLevel >= SheepMergeManager.progressionShearTierBoostMaxLevel()) "MAXED"
                else "Cost: ${SheepMergeManager.formatPoints(SheepMergeManager.getShearTierBoostUpgradeCost(player))} Coins",
                "Chance for shearing to upgrade sheep by one tier"
            )
        ))
        setItem(SheepMergeManager.SHOP_BACK_TO_UPGRADES_SLOT, backItem())
    }

    private fun backItem() = MenuItemFactory.create(
        Material.ARROW, "Back To Upgrades", listOf("Click to go back")
    )

    private fun setMenuItemIfChanged(inventory: Inventory, slot: Int, next: ItemStack) {
        if (slot !in 0 until inventory.size) return
        val current = inventory.getItem(slot)
        if (current != null && current.isSimilar(next) && current.amount == next.amount) return
        inventory.setItem(slot, next)
    }
}