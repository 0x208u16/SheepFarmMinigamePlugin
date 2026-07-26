package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

internal object SheepSacrificeMenus {
    private const val KEEP_REGULAR_UPGRADES = 1
    private const val KEEP_COMBO_UPGRADES = 2
    private const val KEEP_SHEAR_UPGRADES = 3
    private const val EGG_COOLDOWN_CAP = 4
    private const val MAX_SHEEP_BONUS = 5

    @JvmStatic
    fun isSacrificeMenuTitle(title: String?): Boolean = SheepMergeManager.SACRIFICE_MENU_TITLE == title

    @JvmStatic
    fun openSacrificeMenu(player: Player?) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.SACRIFICE_MENU_TITLE)
        val bought = SheepMergeManager.getSacrificeUnlocksBought(player)
        val eggUnlocked = SheepMergeManager.sacrificeMenuHasUnlock(player, EGG_COOLDOWN_CAP)
        val maxSheepUnlocked = SheepMergeManager.sacrificeMenuHasUnlock(player, MAX_SHEEP_BONUS)
        inventory.setItem(SheepMergeManager.SACRIFICE_POINTS_SLOT, MenuItemFactory.create(
            Material.TOTEM_OF_UNDYING, "Sacrifice Progress", listOf(
                "Sacrifice points: ${SheepMergeManager.formatPoints(SheepMergeManager.getSacrificePoints(player))}",
                "Unlocks bought: $bought / ${SheepMergeManager.SACRIFICE_UNLOCK_MAX}",
                if (bought >= SheepMergeManager.SACRIFICE_UNLOCK_MAX) "Next unlock cost: MAXED"
                else "Next unlock cost: ${SheepMergeManager.formatPoints(SheepMergeManager.sacrificeMenuUnlockCost(player))}"
            )
        ))
        inventory.setItem(SheepMergeManager.SACRIFICE_ALL_SHEEP_SLOT, MenuItemFactory.create(
            Material.IRON_SWORD, "Sacrifice All Sheep", listOf(
                "Converts all current farm sheep", "into sacrifice points instantly",
                "Per sheep value: 2^(tierIndex)", "Click to sacrifice now"
            )
        ))
        inventory.setItem(SheepMergeManager.SACRIFICE_UNLOCK_REGULAR_RESETS_SLOT,
            unlockItem(player, KEEP_REGULAR_UPGRADES, Material.BARRIER, "Unlock 1: Keep Regular Upgrades",
                "Keeps regular upgrades on prestige"))
        inventory.setItem(SheepMergeManager.SACRIFICE_UNLOCK_COMBO_RESETS_SLOT,
            unlockItem(player, KEEP_COMBO_UPGRADES, Material.BLAZE_POWDER, "Unlock 2: Keep Combo Upgrades",
                "Keeps combo upgrades on prestige"))
        inventory.setItem(SheepMergeManager.SACRIFICE_UNLOCK_SHEAR_RESETS_SLOT,
            unlockItem(player, KEEP_SHEAR_UPGRADES, Material.SHEARS, "Unlock 3: Keep Shear Upgrades",
                "Keeps shear shop on prestige"))
        inventory.setItem(SheepMergeManager.SACRIFICE_UNLOCK_EGG_COOLDOWN_SLOT, MenuItemFactory.create(
            Material.CLOCK, "Unlock 4: 1s Egg Cooldown Cap", listOf(
                "Status: ${if (eggUnlocked) "UNLOCKED" else "LOCKED"}",
                if (eggUnlocked) "MAXED" else "Not unlocked",
                "Adds +1 egg speed max level", "Allows 1 egg per second"
            ), eggUnlocked
        ))
        inventory.setItem(SheepMergeManager.SACRIFICE_UNLOCK_MAX_SHEEP_SLOT, MenuItemFactory.create(
            Material.OAK_FENCE, "Unlock 5: +50 Sheep Cap", listOf(
                "Status: ${if (maxSheepUnlocked) "UNLOCKED" else "LOCKED"}",
                if (maxSheepUnlocked) "MAXED" else "Not unlocked", "Raises max sheep limit by +50"
            ), maxSheepUnlocked
        ))
        inventory.setItem(SheepMergeManager.SACRIFICE_BACK_TO_UPGRADES_SLOT,
            MenuItemFactory.create(Material.ARROW, "Back To Upgrades", listOf("Click to go back")))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleSacrificeMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        if (slot == SheepMergeManager.SACRIFICE_BACK_TO_UPGRADES_SLOT) {
            SheepMergeManager.openUpgradeMenu(player)
            return
        }
        if (slot == SheepMergeManager.SACRIFICE_ALL_SHEEP_SLOT) {
            val gained = SheepMergeManager.sacrificeMenuAllSheep(player)
            if (gained.signum() > 0) {
                SheepMergeManager.progressionPlayUpgradeSound(player)
                player.sendMessage(SheepMergeManager.action(
                    "Sacrificed all sheep for ${SheepMergeManager.formatPoints(gained)} sacrifice points."
                ))
            } else player.sendMessage(SheepMergeManager.warning("No sheep available to sacrifice."))
        } else {
            val unlockId = unlockIdForSlot(slot) ?: return
            val bought = SheepMergeManager.getSacrificeUnlocksBought(player)
            when {
                bought >= SheepMergeManager.SACRIFICE_UNLOCK_MAX ->
                    player.sendMessage(SheepMergeManager.warning("All sacrifice unlocks are already purchased."))
                SheepMergeManager.sacrificeMenuHasUnlock(player, unlockId) ->
                    player.sendMessage(SheepMergeManager.warning("That sacrifice unlock is already purchased."))
                unlockId != bought + 1 -> player.sendMessage(SheepMergeManager.warning(
                    "Buy sacrifice unlocks in order. Next unlock: ${bought + 1} / ${SheepMergeManager.SACRIFICE_UNLOCK_MAX}."
                ))
                SheepMergeManager.sacrificeMenuTryBuyUnlock(player, unlockId) -> {
                    SheepMergeManager.progressionPlayUpgradeSound(player)
                    player.sendMessage(SheepMergeManager.action("Sacrifice unlock purchased."))
                }
                else -> player.sendMessage(SheepMergeManager.warning("Not enough sacrifice points."))
            }
        }
        SheepMergeManager.updatePointsScoreboard(player)
        openSacrificeMenu(player)
    }

    private fun unlockItem(player: Player, id: Int, material: Material, name: String, effect: String) =
        MenuItemFactory.create(material, name, listOf(
            "Status: ${SheepMergeManager.sacrificeMenuUnlockStatus(player, id)}", effect, "Applies immediately"
        ), SheepMergeManager.sacrificeMenuHasUnlock(player, id))

    private fun unlockIdForSlot(slot: Int): Int? = when (slot) {
        SheepMergeManager.SACRIFICE_UNLOCK_REGULAR_RESETS_SLOT -> KEEP_REGULAR_UPGRADES
        SheepMergeManager.SACRIFICE_UNLOCK_COMBO_RESETS_SLOT -> KEEP_COMBO_UPGRADES
        SheepMergeManager.SACRIFICE_UNLOCK_SHEAR_RESETS_SLOT -> KEEP_SHEAR_UPGRADES
        SheepMergeManager.SACRIFICE_UNLOCK_EGG_COOLDOWN_SLOT -> EGG_COOLDOWN_CAP
        SheepMergeManager.SACRIFICE_UNLOCK_MAX_SHEEP_SLOT -> MAX_SHEEP_BONUS
        else -> null
    }
}