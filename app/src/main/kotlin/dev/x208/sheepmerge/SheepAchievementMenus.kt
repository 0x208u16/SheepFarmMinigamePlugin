package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

internal object SheepAchievementMenus {

    private const val SECRET_AUTHOR_ONLINE_SLOT = 2
    private const val SECRET_OWNER_FARM_SLOT = 6

    @JvmStatic
    fun isAchievementsMenuTitle(title: String?): Boolean = SheepMergeManager.ACHIEVEMENTS_MENU_TITLE == title

    @JvmStatic
    fun isAchievementsViewMenuTitle(title: String?): Boolean = SheepMergeManager.ACHIEVEMENTS_VIEW_MENU_TITLE == title

    @JvmStatic
    fun isAchievementsUpgradesMenuTitle(title: String?): Boolean =
        SheepMergeManager.ACHIEVEMENTS_UPGRADES_MENU_TITLE == title

    @JvmStatic
    fun openAchievementsMenu(player: Player?) {
        if (player == null) return
        SheepMergeManager.achievementMenuEvaluateProgress(player)
        val entries = SheepMergeManager.achievementMenuEntries()
        val unlockedCount = SheepMergeManager.achievementMenuUnlockedIds(player).size
        val achievementPoints = SheepMergeManager.getAchievementPoints(player)
        val nextMilestoneTarget = SheepMergeManager.achievementMenuNextMilestoneTarget(achievementPoints)
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.ACHIEVEMENTS_MENU_TITLE)

        inventory.setItem(4, MenuItemFactory.create(Material.BOOK, "Achievements Hub", listOf(
            "Unlocked: $unlockedCount/${entries.size}",
            "Achievement points: $achievementPoints",
            "Milestone line: unlock bonuses automatically",
            if (nextMilestoneTarget > 0) "Next milestone: $nextMilestoneTarget AP" else "Milestone line complete"
        )))
        inventory.setItem(SheepMergeManager.ACHIEVEMENTS_VIEW_SLOT, MenuItemFactory.create(
            Material.MAP, "View Achievements", listOf("Browse goals and rewards", "Click to open")
        ))
        inventory.setItem(SheepMergeManager.ACHIEVEMENTS_UPGRADES_SLOT, MenuItemFactory.create(
            Material.ENCHANTED_BOOK,
            "Achievement Milestones",
            listOf("Unlock milestone bonuses with achievement points", "Click to open")
        ))
        inventory.setItem(SheepMergeManager.ACHIEVEMENTS_BACK_SLOT, MenuItemFactory.create(
            Material.ARROW, "Back", listOf("Click: Open upgrades")
        ))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun openAchievementsViewMenu(player: Player?) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 54, SheepMergeManager.ACHIEVEMENTS_VIEW_MENU_TITLE)
        val unlocked = SheepMergeManager.achievementMenuUnlockedIds(player)
        val entries = SheepMergeManager.achievementMenuEntries()
        var slotIndex = 0
        for (achievement in entries) {
            if (isSecretAchievementId(achievement.id())) continue
            if (slotIndex >= achievementGridSlots.size) break
            val unlockedAchievement = achievement.id() in unlocked
            val item = achievementItem(achievement, unlockedAchievement)
            if (unlockedAchievement && achievement.id() == "socials_explorer") setOwner(item)
            inventory.setItem(achievementGridSlots[slotIndex++], item)
        }

        for (achievement in entries) {
            if (!isSecretAchievementId(achievement.id()) || achievement.id() !in unlocked) continue
            val item = achievementItem(achievement, true)
            if (achievement.id() == "secret_owner_farm") setOwner(item)
            val slot = if (achievement.id() == "secret_author_online") SECRET_AUTHOR_ONLINE_SLOT else SECRET_OWNER_FARM_SLOT
            inventory.setItem(slot, item)
        }

        inventory.setItem(SheepMergeManager.ACHIEVEMENTS_VIEW_BACK_SLOT, MenuItemFactory.create(
            Material.ARROW, "Back", listOf("Click: Achievements hub")
        ))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun openAchievementsUpgradesMenu(player: Player?) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 54, SheepMergeManager.ACHIEVEMENTS_UPGRADES_MENU_TITLE)
        val unlocked = SheepMergeManager.achievementMenuUnlockedMilestoneIds(player)
        val entries = SheepMergeManager.achievementMilestoneMenuEntries()
        val achievementPoints = SheepMergeManager.getAchievementPoints(player)
        val nextTarget = SheepMergeManager.achievementMenuNextMilestoneTarget(achievementPoints)

        inventory.setItem(4, MenuItemFactory.create(Material.NETHER_STAR, "Achievement Milestones", listOf(
            "Current points: $achievementPoints",
            "Unlocked milestones: ${unlocked.size}/${entries.size}",
            if (nextTarget > 0) "Next unlock: $nextTarget AP" else "All milestones unlocked"
        )))
        for ((index, milestone) in entries.withIndex()) {
            if (index >= achievementMilestoneGridSlots.size) break
            val unlockedMilestone = milestone.id() in unlocked
            inventory.setItem(achievementMilestoneGridSlots[index], MenuItemFactory.create(
                milestone.material(),
                milestone.name(),
                listOf(
                    "Target: ${milestone.requiredPoints()} achievement points",
                    milestone.reward(),
                    "Status: ${if (unlockedMilestone) "UNLOCKED" else "LOCKED"}"
                ),
                unlockedMilestone
            ))
        }
        inventory.setItem(SheepMergeManager.ACHIEVEMENTS_UPGRADES_BACK_SLOT, MenuItemFactory.create(
            Material.ARROW, "Back", listOf("Click: Achievements hub")
        ))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleAchievementsMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.ACHIEVEMENTS_VIEW_SLOT -> openAchievementsViewMenu(player)
            SheepMergeManager.ACHIEVEMENTS_UPGRADES_SLOT -> openAchievementsUpgradesMenu(player)
            SheepMergeManager.ACHIEVEMENTS_BACK_SLOT -> SheepMergeManager.openUpgradeMenu(player)
        }
    }

    @JvmStatic
    fun handleAchievementsViewMenuClick(player: Player?, slot: Int) {
        if (player != null && slot == SheepMergeManager.ACHIEVEMENTS_VIEW_BACK_SLOT) openAchievementsMenu(player)
    }

    @JvmStatic
    fun handleAchievementsUpgradesMenuClick(player: Player?, slot: Int) {
        if (player != null && slot == SheepMergeManager.ACHIEVEMENTS_UPGRADES_BACK_SLOT) openAchievementsMenu(player)
    }

    private fun achievementItem(
        achievement: SheepMergeManager.AchievementMenuEntry,
        unlocked: Boolean
    ): ItemStack {
        val lore = listOf(
            "Objective: ${achievement.objective()}",
            achievement.reward(),
            "Achievement points: +${achievement.achievementPoints()}",
            "Status: ${if (unlocked) "UNLOCKED" else "LOCKED"}",
            "Key: ${achievement.id()}"
        )
        return if (achievement.id() == "wool_guardian") {
            MenuItemFactory.createShieldWithWhiteBanner(achievement.name(), lore, unlocked)
        } else {
            MenuItemFactory.create(achievement.material(), achievement.name(), lore, unlocked)
        }
    }

    private fun setOwner(item: ItemStack) {
        val skullMeta = item.itemMeta as? SkullMeta ?: return
        skullMeta.owningPlayer = SheepMergeManager.socialsAuthor()
        item.itemMeta = skullMeta
    }

    private fun isSecretAchievementId(id: String): Boolean =
        id == "secret_author_online" || id == "secret_owner_farm"

    private val achievementGridSlots = listOf(
        3, 4, 5,
        13, 12, 14, 11, 15, 10, 16,
        22, 21, 23, 20, 24, 19, 25,
        31, 30, 32, 29, 33, 28, 34,
        40, 39, 41, 38, 42, 37, 43
    )

    private val achievementMilestoneGridSlots = listOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        38, 39, 40, 41, 42
    )
}