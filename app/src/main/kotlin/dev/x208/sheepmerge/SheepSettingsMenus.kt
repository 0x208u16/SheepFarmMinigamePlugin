package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

internal object SheepSettingsMenus {

    data class QuickAccessOption(
        val id: String,
        val material: Material,
        val name: String,
        val description: String
    )

    @JvmStatic
    fun isScoreboardMenuTitle(title: String?): Boolean = SheepMergeManager.SCOREBOARD_MENU_TITLE == title

    @JvmStatic
    fun isUniversalLayoutMenuTitle(title: String?): Boolean = SheepMergeManager.SETTINGS_MENU_TITLE == title

    @JvmStatic
    fun isScoreboardLayoutMenuTitle(title: String?): Boolean = SheepMergeManager.SCOREBOARD_LAYOUT_MENU_TITLE == title

    @JvmStatic
    fun isInventoryLayoutMenuTitle(title: String?): Boolean = SheepMergeManager.INVENTORY_LAYOUT_MENU_TITLE == title

    @JvmStatic
    fun isSoundEffectsMenuTitle(title: String?): Boolean = SheepMergeManager.SOUND_EFFECTS_MENU_TITLE == title

    @JvmStatic
    fun isParticleEffectsMenuTitle(title: String?): Boolean = SheepMergeManager.PARTICLE_EFFECTS_MENU_TITLE == title

    @JvmStatic
    fun isVisitAccessMenuTitle(title: String?): Boolean = SheepMergeManager.VISIT_ACCESS_MENU_TITLE == title

    @JvmStatic
    fun openScoreboardMenu(player: Player?) {
        if (player == null) return
        val playerId = player.uniqueId
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.SCOREBOARD_MENU_TITLE)
        inventory.setItem(SheepMergeManager.SCOREBOARD_QUEST_POINTS_SLOT, scoreboardToggleItem(
            Material.BOOK, "Quest Points", SheepUiPreferences.shouldShowScoreboardQuestPoints(playerId)
        ))
        inventory.setItem(SheepMergeManager.SCOREBOARD_ACHIEVEMENT_POINTS_SLOT, scoreboardToggleItem(
            Material.ENCHANTED_BOOK, "Achievement Points",
            SheepUiPreferences.shouldShowScoreboardAchievementPoints(playerId)
        ))
        inventory.setItem(SheepMergeManager.SCOREBOARD_AUTOMATION_POINTS_SLOT, scoreboardToggleItem(
            Material.REDSTONE, "Automation Points", SheepUiPreferences.shouldShowScoreboardAutomationPoints(playerId)
        ))
        inventory.setItem(SheepMergeManager.SCOREBOARD_SACRIFICE_POINTS_SLOT, scoreboardToggleItem(
            Material.TOTEM_OF_UNDYING, "Sacrifice Points",
            SheepUiPreferences.shouldShowScoreboardSacrificePoints(playerId)
        ))
        inventory.setItem(SheepMergeManager.SCOREBOARD_PRESTIGE_STATS_SLOT, scoreboardToggleItem(
            Material.NETHER_STAR, "Prestige Stats", SheepUiPreferences.shouldShowScoreboardPrestigeStats(playerId)
        ))
        inventory.setItem(SheepMergeManager.SCOREBOARD_QUEST_PROGRESS_SLOT, scoreboardToggleItem(
            Material.MAP, "Quest Progress", SheepUiPreferences.shouldShowScoreboardQuestProgress(playerId)
        ))
        inventory.setItem(SheepMergeManager.SCOREBOARD_ABILITIES_SLOT, scoreboardToggleItem(
            Material.NETHER_STAR, "Ability Status", SheepUiPreferences.shouldShowScoreboardAbilityStatus(playerId)
        ))
        val layoutMode = SheepUiPreferences.getScoreboardLayoutMode(playerId)
        inventory.setItem(SheepMergeManager.SCOREBOARD_LAYOUT_SLOT, MenuItemFactory.create(
            Material.MAP,
            "Compact Layout",
            listOf(
                "Status: ${if (layoutMode == 1) "Enabled" else "Disabled"}",
                "Mode: ${if (layoutMode == 1) "Compact" else "Detailed"}",
                "Click: Toggle"
            )
        ))
        inventory.setItem(SheepMergeManager.SCOREBOARD_BACK_SLOT, backItem("Click: Open layouts"))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleScoreboardMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        val playerId = player.uniqueId
        when (slot) {
            SheepMergeManager.SCOREBOARD_ACHIEVEMENT_POINTS_SLOT ->
                SheepUiPreferences.setShowScoreboardAchievementPoints(
                    playerId, !SheepUiPreferences.shouldShowScoreboardAchievementPoints(playerId)
                )
            SheepMergeManager.SCOREBOARD_QUEST_POINTS_SLOT ->
                SheepUiPreferences.setShowScoreboardQuestPoints(
                    playerId, !SheepUiPreferences.shouldShowScoreboardQuestPoints(playerId)
                )
            SheepMergeManager.SCOREBOARD_AUTOMATION_POINTS_SLOT ->
                SheepUiPreferences.setShowScoreboardAutomationPoints(
                    playerId, !SheepUiPreferences.shouldShowScoreboardAutomationPoints(playerId)
                )
            SheepMergeManager.SCOREBOARD_SACRIFICE_POINTS_SLOT ->
                SheepUiPreferences.setShowScoreboardSacrificePoints(
                    playerId, !SheepUiPreferences.shouldShowScoreboardSacrificePoints(playerId)
                )
            SheepMergeManager.SCOREBOARD_PRESTIGE_STATS_SLOT ->
                SheepUiPreferences.setShowScoreboardPrestigeStats(
                    playerId, !SheepUiPreferences.shouldShowScoreboardPrestigeStats(playerId)
                )
            SheepMergeManager.SCOREBOARD_QUEST_PROGRESS_SLOT ->
                SheepUiPreferences.setShowScoreboardQuestProgress(
                    playerId, !SheepUiPreferences.shouldShowScoreboardQuestProgress(playerId)
                )
            SheepMergeManager.SCOREBOARD_ABILITIES_SLOT ->
                SheepUiPreferences.setShowScoreboardAbilityStatus(
                    playerId, !SheepUiPreferences.shouldShowScoreboardAbilityStatus(playerId)
                )
            SheepMergeManager.SCOREBOARD_LAYOUT_SLOT -> SheepUiPreferences.setScoreboardLayoutMode(
                playerId, if (SheepUiPreferences.getScoreboardLayoutMode(playerId) == 1) 0 else 1
            )
            SheepMergeManager.SCOREBOARD_BACK_SLOT -> {
                openUniversalLayoutMenu(player)
                return
            }
            else -> return
        }
        SheepMergeManager.saveData()
        SheepMergeManager.updatePointsScoreboard(player)
        openScoreboardMenu(player)
    }

    @JvmStatic
    fun openUniversalLayoutMenu(player: Player?) {
        if (player == null) return
        val playerId = player.uniqueId
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.SETTINGS_MENU_TITLE)
        inventory.setItem(4, MenuItemFactory.create(
            Material.NETHER_STAR,
            "Settings Hub",
            listOf("Inventory, scoreboard, visits, sound, and particles", "Use the items below to open a section")
        ))
        inventory.setItem(SheepMergeManager.UNIVERSAL_LAYOUT_INVENTORY_SLOT, MenuItemFactory.create(
            Material.CHEST,
            "Inventory Layout",
            listOf(
                "Quick access selected: ${SheepMergeManager.getSettingsQuickAccessActions(playerId).size} / " +
                    SheepMergeManager.getInventoryQuickAccessMaxItems(),
                "Click: Open"
            )
        ))
        inventory.setItem(SheepMergeManager.UNIVERSAL_LAYOUT_SCOREBOARD_SLOT, MenuItemFactory.create(
            Material.BOOK,
            "Scoreboard Settings",
            listOf(
                "Sections: points, quests, automation, sacrifice",
                "Layout: ${if (SheepUiPreferences.getScoreboardLayoutMode(playerId) == 0) "Detailed" else "Compact"}",
                "Click: Open"
            )
        ))
        inventory.setItem(SheepMergeManager.UNIVERSAL_LAYOUT_VISIT_SLOT, MenuItemFactory.create(
            Material.OAK_DOOR,
            "Visit Access & Blocks",
            listOf(
                "Visit access: ${if (SheepVisitAccessState.isFarmVisitable(playerId)) "Open" else "Closed"}",
                "Blocked visitors: ${SheepVisitAccessState.getBlockedFarmVisitorCount(playerId)}",
                "Click: Open"
            )
        ))
        inventory.setItem(SheepMergeManager.UNIVERSAL_LAYOUT_SOUND_SLOT, MenuItemFactory.create(
            Material.MUSIC_DISC_PIGSTEP,
            "Sound Effects",
            listOf(
                "Status: ${if (SheepEffectPreferences.areSoundEffectsEnabled(player)) "Enabled" else "Disabled"}",
                "Click: Open"
            )
        ))
        inventory.setItem(SheepMergeManager.UNIVERSAL_LAYOUT_PARTICLE_SLOT, MenuItemFactory.create(
            Material.FIRE_CHARGE,
            "Particle Effects",
            listOf(
                "Status: ${if (SheepEffectPreferences.areParticleEffectsEnabled(player)) "Enabled" else "Disabled"}",
                "Click: Open"
            )
        ))
        inventory.setItem(SheepMergeManager.UNIVERSAL_LAYOUT_BACK_SLOT, backItem("Click: Sheep Merge menu"))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleUniversalLayoutMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.UNIVERSAL_LAYOUT_SCOREBOARD_SLOT -> openScoreboardMenu(player)
            SheepMergeManager.UNIVERSAL_LAYOUT_INVENTORY_SLOT -> openInventoryLayoutMenu(player)
            SheepMergeManager.UNIVERSAL_LAYOUT_SOUND_SLOT -> openSoundEffectsMenu(player)
            SheepMergeManager.UNIVERSAL_LAYOUT_PARTICLE_SLOT -> openParticleEffectsMenu(player)
            SheepMergeManager.UNIVERSAL_LAYOUT_VISIT_SLOT -> openVisitAccessMenu(player)
            SheepMergeManager.UNIVERSAL_LAYOUT_BACK_SLOT -> SheepMergeManager.openUpgradeMenu(player)
        }
    }

    @JvmStatic
    fun openSoundEffectsMenu(player: Player?) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.SOUND_EFFECTS_MENU_TITLE)
        val enabled = SheepEffectPreferences.areSoundEffectsEnabled(player)
        inventory.setItem(SheepMergeManager.SOUND_EFFECTS_TOGGLE_SLOT, effectToggleItem(
            if (enabled) Material.LIME_DYE else Material.GRAY_DYE, "Sound Effects", enabled
        ))
        val sheepSoundsEnabled = SheepEffectPreferences.areSheepSoundsEnabled(player)
        inventory.setItem(SheepMergeManager.SHEEP_SOUNDS_TOGGLE_SLOT, effectToggleItem(
            if (sheepSoundsEnabled) Material.WHITE_WOOL else Material.GRAY_DYE,
            "Sheep Sounds",
            sheepSoundsEnabled
        ))
        inventory.setItem(SheepMergeManager.SOUND_EFFECTS_BACK_SLOT, backItem("Click: Settings"))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleSoundEffectsMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.SOUND_EFFECTS_TOGGLE_SLOT -> {
                val enabled = SheepMergeManager.toggleSoundEffects(player)
                player.sendMessage(SheepMergeManager.action("Sound effects ${if (enabled) "enabled." else "disabled."}"))
                openSoundEffectsMenu(player)
            }
            SheepMergeManager.SHEEP_SOUNDS_TOGGLE_SLOT -> {
                val enabled = SheepMergeManager.toggleSheepSounds(player)
                player.sendMessage(SheepMergeManager.action("Sheep sounds ${if (enabled) "enabled." else "disabled."}"))
                openSoundEffectsMenu(player)
            }
            SheepMergeManager.SOUND_EFFECTS_BACK_SLOT -> openUniversalLayoutMenu(player)
        }
    }

    @JvmStatic
    fun openParticleEffectsMenu(player: Player?) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.PARTICLE_EFFECTS_MENU_TITLE)
        val enabled = SheepEffectPreferences.areParticleEffectsEnabled(player)
        inventory.setItem(SheepMergeManager.PARTICLE_EFFECTS_TOGGLE_SLOT, effectToggleItem(
            if (enabled) Material.LIME_DYE else Material.GRAY_DYE, "Particle Effects", enabled
        ))
        inventory.setItem(SheepMergeManager.PARTICLE_EFFECTS_BACK_SLOT, backItem("Click: Settings"))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleParticleEffectsMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.PARTICLE_EFFECTS_TOGGLE_SLOT -> {
                val enabled = SheepMergeManager.toggleParticleEffects(player)
                player.sendMessage(
                    SheepMergeManager.action("Particle effects ${if (enabled) "enabled." else "disabled."}")
                )
                openParticleEffectsMenu(player)
            }
            SheepMergeManager.PARTICLE_EFFECTS_BACK_SLOT -> openUniversalLayoutMenu(player)
        }
    }

    @JvmStatic
    fun openVisitAccessMenu(player: Player?) = openVisitAccessMenu(player, 0)

    private fun openVisitAccessMenu(player: Player?, requestedPage: Int) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 54, SheepMergeManager.VISIT_ACCESS_MENU_TITLE)
        populateVisitAccessMenuItems(player, inventory, requestedPage)
        player.openInventory(inventory)
    }

    private fun populateVisitAccessMenuItems(player: Player, inventory: Inventory, requestedPage: Int) {
        val ownerId = player.uniqueId
        val managedPlayers = getManagedVisitPlayers(player)
        val totalPages = maxOf(1, kotlin.math.ceil(managedPlayers.size / VISIT_ACCESS_PAGE_SIZE.toDouble()).toInt())
        val page = requestedPage.coerceIn(0, totalPages - 1)
        SheepVisitAccessState.setVisitAccessPage(ownerId, page)
        val visitable = SheepVisitAccessState.isFarmVisitable(ownerId)
        setMenuItemIfChanged(inventory, SheepMergeManager.VISIT_ACCESS_TOGGLE_SLOT, MenuItemFactory.create(
            if (visitable) Material.OAK_DOOR else Material.IRON_DOOR,
            "Farm Visit Access",
            listOf(
                "Status: ${if (visitable) "Open" else "Closed"}",
                "Blocked visitors: ${SheepVisitAccessState.getBlockedFarmVisitorCount(ownerId)}",
                if (visitable) "Click: Close farm" else "Click: Open farm"
            )
        ))
        setMenuItemIfChanged(inventory, SheepMergeManager.VISIT_ACCESS_SUMMARY_SLOT, MenuItemFactory.create(
            Material.PLAYER_HEAD,
            "Blocked Visitors",
            listOf("Page ${page + 1} / $totalPages", "Click player heads to block or unblock")
        ))
        setMenuItemIfChanged(inventory, SheepMergeManager.VISIT_ACCESS_PREVIOUS_PAGE_SLOT, MenuItemFactory.create(
            Material.ARROW,
            "Previous Page",
            listOf("Page ${page + 1} / $totalPages", if (page > 0) "Click to go back" else "Already at first page")
        ))
        setMenuItemIfChanged(inventory, SheepMergeManager.VISIT_ACCESS_NEXT_PAGE_SLOT, MenuItemFactory.create(
            Material.ARROW,
            "Next Page",
            listOf(
                "Page ${page + 1} / $totalPages",
                if (page + 1 < totalPages) "Click to advance" else "Already at last page"
            )
        ))
        setMenuItemIfChanged(inventory, SheepMergeManager.VISIT_ACCESS_BACK_SLOT, backItem("Click: Settings"))
        visitDisplaySlots.forEach { inventory.setItem(it, null) }
        val startIndex = page * VISIT_ACCESS_PAGE_SIZE
        for (offset in 0 until VISIT_ACCESS_PAGE_SIZE) {
            val target = managedPlayers.getOrNull(startIndex + offset) ?: break
            val displaySlot = visitDisplaySlots.getOrNull(offset) ?: break
            setMenuItemIfChanged(inventory, displaySlot, createVisitAccessItem(player, target))
        }
    }

    private fun getManagedVisitPlayers(viewer: Player): List<Player> {
        val viewerId = viewer.uniqueId
        return Bukkit.getOnlinePlayers()
            .filter { it.isOnline && it.uniqueId != viewerId }
            .sortedWith { left, right ->
                val blockedOrder = SheepVisitAccessState.isFarmVisitorBlocked(viewerId, right.uniqueId)
                    .compareTo(SheepVisitAccessState.isFarmVisitorBlocked(viewerId, left.uniqueId))
                if (blockedOrder != 0) blockedOrder else left.name.compareTo(right.name, ignoreCase = true)
            }
    }

    private fun createVisitAccessItem(owner: Player, target: Player): ItemStack {
        val blocked = SheepVisitAccessState.isFarmVisitorBlocked(owner, target)
        val head = ItemStack(Material.PLAYER_HEAD, 1)
        val skullMeta = head.itemMeta as? SkullMeta ?: return MenuItemFactory.create(
            Material.PLAYER_HEAD,
            "${if (blocked) "Unblock " else "Block "}${target.name}",
            listOf("Status: ${if (blocked) "Blocked" else "Allowed"}", "Click to ${if (blocked) "allow" else "block"}")
        )
        skullMeta.owningPlayer = target
        skullMeta.setDisplayName("${if (blocked) "Unblock " else "Block "}${target.name}")
        skullMeta.lore = listOf(
            "Player: ${target.name}",
            "Status: ${if (blocked) "Blocked" else "Allowed"}",
            "Click to ${if (blocked) "allow visits" else "block visits"}"
        )
        socialVisitOwnerKey()?.let {
            skullMeta.persistentDataContainer.set(it, PersistentDataType.STRING, target.uniqueId.toString())
        }
        head.itemMeta = skullMeta
        return head
    }

    @JvmStatic
    fun handleVisitAccessMenuClick(player: Player?, slot: Int, clickedItem: ItemStack?) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.VISIT_ACCESS_TOGGLE_SLOT -> {
                val open = SheepMergeManager.toggleFarmVisitable(player)
                player.sendMessage(SheepMergeManager.action("Farm visit access ${if (open) "opened." else "closed."}"))
                openVisitAccessMenu(player, currentVisitAccessPage(player))
                return
            }
            SheepMergeManager.VISIT_ACCESS_PREVIOUS_PAGE_SLOT -> {
                openVisitAccessMenu(player, currentVisitAccessPage(player) - 1)
                return
            }
            SheepMergeManager.VISIT_ACCESS_NEXT_PAGE_SLOT -> {
                openVisitAccessMenu(player, currentVisitAccessPage(player) + 1)
                return
            }
            SheepMergeManager.VISIT_ACCESS_BACK_SLOT -> {
                openUniversalLayoutMenu(player)
                return
            }
        }
        val targetId = getSocialVisitOwnerId(clickedItem) ?: return
        if (targetId == player.uniqueId) {
            player.sendMessage(SheepMergeManager.hint("You cannot block yourself."))
            return
        }
        val blocked = SheepMergeManager.toggleFarmVisitorBlocked(player, targetId)
        val targetName = Bukkit.getOfflinePlayer(targetId).name ?: targetId.toString()
        player.sendMessage(SheepMergeManager.action("${if (blocked) "Blocked: " else "Unblocked: "}$targetName"))
        openVisitAccessMenu(player, currentVisitAccessPage(player))
    }

    @JvmStatic
    fun openScoreboardLayoutMenu(player: Player?) {
        if (player == null) return
        val layoutMode = SheepUiPreferences.getScoreboardLayoutMode(player.uniqueId)
        val inventory = Bukkit.createInventory(null, 27, SheepMergeManager.SCOREBOARD_LAYOUT_MENU_TITLE)
        inventory.setItem(SheepMergeManager.SCOREBOARD_LAYOUT_DETAILED_SLOT, MenuItemFactory.create(
            Material.BOOK,
            "Detailed Layout",
            listOf(
                "Status: ${if (layoutMode == 0) "Selected" else "Not selected"}",
                "Shows all sections",
                "Click: Select"
            )
        ))
        inventory.setItem(SheepMergeManager.SCOREBOARD_LAYOUT_COMPACT_SLOT, MenuItemFactory.create(
            Material.MAP,
            "Compact Layout",
            listOf(
                "Status: ${if (layoutMode == 1) "Selected" else "Not selected"}",
                "Shows summary sections",
                "Click: Select"
            )
        ))
        inventory.setItem(SheepMergeManager.SCOREBOARD_LAYOUT_BACK_SLOT, backItem("Click: Universal Layout"))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleScoreboardLayoutMenuClick(player: Player?, slot: Int) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.SCOREBOARD_LAYOUT_DETAILED_SLOT ->
                SheepUiPreferences.setScoreboardLayoutMode(player.uniqueId, 0)
            SheepMergeManager.SCOREBOARD_LAYOUT_COMPACT_SLOT ->
                SheepUiPreferences.setScoreboardLayoutMode(player.uniqueId, 1)
            SheepMergeManager.SCOREBOARD_LAYOUT_BACK_SLOT -> {
                openUniversalLayoutMenu(player)
                return
            }
            else -> return
        }
        SheepMergeManager.saveData()
        SheepMergeManager.updatePointsScoreboard(player)
        openScoreboardLayoutMenu(player)
    }

    @JvmStatic
    fun openInventoryLayoutMenu(player: Player?) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 54, SheepMergeManager.INVENTORY_LAYOUT_MENU_TITLE)
        val selected = SheepMergeManager.getSettingsQuickAccessActions(player.uniqueId)
        val castingEnabled = SheepMergeManager.isSettingsQuickAccessCastingEnabled(player)
        inventory.setItem(SheepMergeManager.INVENTORY_LAYOUT_SELECTED_SLOT, MenuItemFactory.create(
            Material.CHEST,
            "Quick Access Slots",
            listOf(
                "Selected: ${selected.size} / ${SheepMergeManager.getInventoryQuickAccessMaxItems()}",
                "Hotbar slots used: 1-6",
                "Casting: ${if (castingEnabled) "Enabled" else "Disabled"}",
                "Right-click quick item to cast"
            )
        ))
        inventory.setItem(SheepMergeManager.INVENTORY_LAYOUT_CASTING_TOGGLE_SLOT, MenuItemFactory.create(
            Material.LEVER,
            "Quick Access Casting",
            listOf(
                "Status: ${if (castingEnabled) "Enabled" else "Disabled"}",
                if (castingEnabled) "Click: Disable cast to inventory" else "Click: Enable cast to inventory"
            )
        ))
        var slot = 10
        for (option in SheepMergeManager.getSettingsQuickAccessOptions()) {
            if (slot >= 44) break
            val enabled = option.id in selected
            val item = MenuItemFactory.create(
                option.material,
                option.name,
                listOf(
                    option.description,
                    "Status: ${if (enabled) "Selected" else "Not selected"}",
                    if (enabled) "Click: Remove" else "Click: Add"
                )
            )
            val meta = item.itemMeta
            if (meta != null) {
                inventoryLayoutOptionKey()?.let {
                    meta.persistentDataContainer.set(it, PersistentDataType.STRING, option.id)
                }
                item.itemMeta = meta
            }
            inventory.setItem(slot, item)
            slot++
            if (slot % 9 == 8) slot += 2
        }
        inventory.setItem(SheepMergeManager.INVENTORY_LAYOUT_BACK_SLOT, backItem("Click: Universal Layout"))
        player.openInventory(inventory)
    }

    @JvmStatic
    fun handleInventoryLayoutMenuClick(player: Player?, slot: Int, clickedItem: ItemStack?) {
        if (player == null) return
        if (slot == SheepMergeManager.INVENTORY_LAYOUT_CASTING_TOGGLE_SLOT) {
            val enabled = SheepMergeManager.toggleSettingsQuickAccessCasting(player)
            player.sendMessage(
                SheepMergeManager.action("Quick-access inventory casting ${if (enabled) "enabled." else "disabled."}")
            )
            SheepMergeManager.enforceFarmLoadout(player)
            openInventoryLayoutMenu(player)
            return
        }
        if (slot == SheepMergeManager.INVENTORY_LAYOUT_BACK_SLOT) {
            openUniversalLayoutMenu(player)
            return
        }
        val actionId = getInventoryLayoutOptionId(clickedItem) ?: return
        val removing = actionId in SheepMergeManager.getSettingsQuickAccessActions(player.uniqueId)
        if (!SheepMergeManager.toggleSettingsQuickAccessAction(player, actionId)) {
            player.sendMessage(
                SheepMergeManager.warning(
                    "Quick access limit reached (${SheepMergeManager.getInventoryQuickAccessMaxItems()})."
                )
            )
            return
        }
        SheepMergeManager.getSettingsQuickAccessOption(actionId)?.let {
            player.sendMessage(SheepMergeManager.action("${if (removing) "Removed: " else "Added: "}${it.name}"))
        }
        SheepMergeManager.enforceFarmLoadout(player)
        openInventoryLayoutMenu(player)
    }

    private fun scoreboardToggleItem(material: Material, name: String, shown: Boolean): ItemStack =
        MenuItemFactory.create(
            material,
            name,
            listOf("Status: ${if (shown) "Shown" else "Hidden"}", if (shown) "Click: Hide" else "Click: Show")
        )

    private fun effectToggleItem(material: Material, name: String, enabled: Boolean): ItemStack =
        MenuItemFactory.create(
            material,
            name,
            listOf("Status: ${if (enabled) "Enabled" else "Disabled"}", if (enabled) "Click: Disable" else "Click: Enable")
        )

    private fun backItem(lore: String): ItemStack = MenuItemFactory.create(Material.ARROW, "Back", listOf(lore))

    private fun currentVisitAccessPage(player: Player): Int =
        SheepVisitAccessState.getVisitAccessPage(player.uniqueId)

    private fun setMenuItemIfChanged(inventory: Inventory, slot: Int, next: ItemStack?) {
        if (slot !in 0 until inventory.size) return
        val current = inventory.getItem(slot)
        if (current == null && next == null) return
        if (current != null && next != null && current.isSimilar(next) && current.amount == next.amount) return
        inventory.setItem(slot, next)
    }

    private fun socialVisitOwnerKey(): NamespacedKey? =
        SheepMergePlugin.instance?.let { NamespacedKey(it, "social-visit-owner") }

    private fun inventoryLayoutOptionKey(): NamespacedKey? =
        SheepMergePlugin.instance?.let { NamespacedKey(it, "inventory-layout-option") }

    private fun getSocialVisitOwnerId(itemStack: ItemStack?): UUID? {
        if (itemStack?.type != Material.PLAYER_HEAD) return null
        val key = socialVisitOwnerKey() ?: return null
        val raw = itemStack.itemMeta?.persistentDataContainer?.get(key, PersistentDataType.STRING) ?: return null
        return try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun getInventoryLayoutOptionId(itemStack: ItemStack?): String? {
        val key = inventoryLayoutOptionKey() ?: return null
        val actionId = itemStack?.itemMeta?.persistentDataContainer?.get(key, PersistentDataType.STRING) ?: return null
        return actionId.takeIf { SheepMergeManager.getSettingsQuickAccessOption(it) != null }
    }

    private val visitDisplaySlots: List<Int> = (10 until 44).filter { it % 9 != 8 }

    private const val VISIT_ACCESS_PAGE_SIZE = 31
}