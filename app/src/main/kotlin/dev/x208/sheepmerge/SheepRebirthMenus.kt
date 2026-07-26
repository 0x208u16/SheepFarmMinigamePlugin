package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

internal object SheepRebirthMenus {
    @JvmStatic
    fun isRebirthMenuTitle(title: String?): Boolean = SheepMergeManager.REBIRTH_MENU_TITLE == title

    @JvmStatic
    fun isRebirthTreeMenuTitle(title: String?): Boolean = SheepMergeManager.REBIRTH_TREE_MENU_TITLE == title

    @JvmStatic
    fun openRebirthMenu(player: Player?) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.REBIRTH_MENU_TITLE)
        val level = SheepMergeManager.getRebirthLevel(player)
        val points = SheepMergeManager.getRebirthPoints(player)
        val unspent = SheepMergeManager.rebirthMenuUnspentPoints(player)
        val affordable = SheepMergeManager.getAffordableRebirthLevelsDisplay(player)
        val nextCost = SheepMergeManager.progressionRebirthCost(level)
        val reward = SheepMergeManager.progressionRebirthRewardForLevels(level, affordable)
        inventory.setItem(SheepMergeManager.REBIRTH_PROGRESS_SLOT, MenuItemFactory.create(
            Material.DRAGON_EGG, "Rebirth Progress", listOf(
                "&bLevel: &f$level", "&dRebirth points: &f${SheepMergeManager.formatPoints(points.toLong())}",
                "&aUnspent: &f${SheepMergeManager.formatPoints(unspent.toLong())}",
                "&6Next cost: &f$nextCost prestige levels"
            )
        ))
        inventory.setItem(SheepMergeManager.REBIRTH_ACTION_SLOT, MenuItemFactory.create(
            Material.NETHER_STAR, "Rebirth Reset", listOf(
                if (affordable > 0) "&aReady: +$affordable rebirth level(s)" else "&cReady: +0 rebirth level(s)",
                if (affordable > 0) "&dReward: +${SheepMergeManager.formatPoints(reward.toLong())} rebirth points"
                else "&dReward: +0 rebirth points",
                "&6Required prestige levels: &f$nextCost", "&7Cost scaling: +10 prestige levels per rebirth",
                "&cConsumes prestige levels and prestige points", "&cResets prestige upgrades",
                if (SheepMergeManager.rebirthMenuKeepsSacrifice(player)) "&aKeeps sacrifice points"
                else "&cSacrifice points reset", "&aClick: Rebirth"
            )
        ))
        inventory.setItem(SheepMergeManager.REBIRTH_OPEN_TREE_SLOT, MenuItemFactory.create(
            Material.ENCHANTED_BOOK, "Rebirth Skill Tree", listOf(
                "&7Spend rebirth points here", "&aUnlocks apply immediately",
                "&aUnlocks stay active until refunded", "&7Cost starts at 1 RP",
                "&7Builds upward from the root", "&aClick: Open"
            )
        ))
        inventory.setItem(SheepMergeManager.REBIRTH_BACK_TO_UPGRADES_SLOT,
            MenuItemFactory.create(Material.ARROW, "Back To Upgrades", listOf("Click to go back")))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleRebirthMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.REBIRTH_ACTION_SLOT -> {
                val gained = SheepMergeManager.rebirthMenuRebirth(player)
                if (gained > 0) {
                    SheepMergeManager.progressionPlayPrestigeSound(player)
                    player.sendMessage(SheepMergeManager.action("Rebirth +$gained"))
                } else player.sendMessage(SheepMergeManager.warning("Not enough prestige levels."))
            }
            SheepMergeManager.REBIRTH_OPEN_TREE_SLOT -> return openRebirthTreeMenu(player)
            SheepMergeManager.REBIRTH_BACK_TO_UPGRADES_SLOT -> return SheepMergeManager.openUpgradeMenu(player)
            else -> return
        }
        SheepMergeManager.updatePointsScoreboard(player)
        openRebirthMenu(player)
    }

    @JvmStatic
    fun openRebirthTreeMenu(player: Player?) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 54, SheepMergeManager.REBIRTH_TREE_MENU_TITLE)
        val unspent = SheepMergeManager.rebirthMenuUnspentPoints(player)
        val entries = SheepMergeManager.rebirthMenuSkillEntries()
        val byId = entries.associateBy { it.id() }
        val refund = SheepMergeManager.rebirthMenuRespecRefundAmount(player)
        val remaining = SheepMergeManager.getRebirthRespecRemainingMs(player)
        inventory.setItem(4, MenuItemFactory.create(Material.BOOK, "Tree Overview", listOf(
            "&bLevel: &f${SheepMergeManager.getRebirthLevel(player)}",
            "&dRebirth points: &f${SheepMergeManager.formatPoints(SheepMergeManager.getRebirthPoints(player).toLong())}",
            "&aUnspent: &f${SheepMergeManager.formatPoints(unspent.toLong())}",
            "&aAll unlocked skills are active immediately", "&7Respec available every 30 minutes",
            "&7Root starts at the bottom", "&7Unlock nodes upward from the root"
        )))
        for (node in entries) {
            val unlocked = SheepMergeManager.rebirthMenuHasSkill(player, node.id())
            val parentUnlocked = node.parentId() <= 0 || SheepMergeManager.rebirthMenuHasSkill(player, node.parentId())
            val ready = !unlocked && parentUnlocked && unspent >= node.cost()
            val lore = mutableListOf("&6Cost: &f${node.cost()} RP", "&e${node.effectLine()}")
            if (node.parentId() > 0) lore += "&7Requires: ${byId[node.parentId()]?.name() ?: "Root"}"
            when {
                unlocked -> lore += listOf("&aUnlocked", "&aActive now", "&aPermanent unlock")
                !parentUnlocked -> lore += "&cLocked: need parent first"
                !ready -> lore += "&cNeed ${node.cost() - unspent} more RP"
                else -> lore += listOf("&aReady", "&aClick to unlock")
            }
            inventory.setItem(node.slot(), MenuItemFactory.create(node.material(), node.name(), lore))
        }
        inventory.setItem(SheepMergeManager.REBIRTH_TREE_RESPEC_SLOT, MenuItemFactory.create(
            Material.BARRIER, "Respec Rebirth Skills", listOf(
                "&dRefund amount: &f${SheepMergeManager.formatPoints(refund.toLong())} rebirth points",
                if (remaining > 0L) "&7Cooldown: &f${SheepMergeManager.progressionFormatDuration(remaining)}"
                else "&7Cooldown: &aready", "&cResets all rebirth skill unlocks",
                "&7Rebirth level and rebirth points are kept", "&aClick to respec"
            )
        ))
        inventory.setItem(SheepMergeManager.REBIRTH_TREE_BACK_SLOT,
            MenuItemFactory.create(Material.ARROW, "Back To Rebirth", listOf("Click to go back")))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleRebirthTreeMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        if (slot == SheepMergeManager.REBIRTH_TREE_BACK_SLOT) return openRebirthMenu(player)
        if (slot == SheepMergeManager.REBIRTH_TREE_RESPEC_SLOT) {
            val remaining = SheepMergeManager.getRebirthRespecRemainingMs(player)
            if (remaining > 0L) {
                player.sendMessage(SheepMergeManager.warning(
                    "Respec cooldown: ${SheepMergeManager.progressionFormatDuration(remaining)}"
                ))
            } else {
                val refund = SheepMergeManager.rebirthMenuRespecRefundAmount(player)
                when {
                    refund <= 0 -> player.sendMessage(SheepMergeManager.warning("No rebirth skills to respec."))
                    SheepMergeManager.rebirthMenuTryRespec(player) -> {
                        SheepMergeManager.progressionPlayUpgradeSound(player)
                        player.sendMessage(SheepMergeManager.action(
                            "Refunded ${SheepMergeManager.formatPoints(refund.toLong())} rebirth points."
                        ))
                    }
                    else -> player.sendMessage(SheepMergeManager.warning("Respec is not available right now."))
                }
            }
            return openRebirthTreeMenu(player)
        }
        val clicked = SheepMergeManager.rebirthMenuSkillEntries().firstOrNull { it.slot() == slot } ?: return
        if (SheepMergeManager.rebirthMenuHasSkill(player, clicked.id())) {
            player.sendMessage(SheepMergeManager.warning("That rebirth skill is already unlocked permanently."))
        } else if (SheepMergeManager.rebirthMenuTryUnlock(player, clicked.id())) {
            SheepMergeManager.progressionPlayUpgradeSound(player)
            player.sendMessage(SheepMergeManager.action("Rebirth skill unlocked."))
        } else player.sendMessage(SheepMergeManager.warning("Cannot unlock this skill yet."))
        openRebirthTreeMenu(player)
    }
}