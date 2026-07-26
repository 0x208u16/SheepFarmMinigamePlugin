package dev.x208.sheepmerge

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.SheepRegrowWoolEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerShearEntityEvent
import org.bukkit.event.weather.ThunderChangeEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector

class SheepFarmGameListener : Listener {

    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        val world = event.location.world
        if (world != null
            && SheepMergeManager.isSheepFarmWorld(world)
            && !SheepMergeManager.isTutorialWorld(world)
            && event.spawnReason == CreatureSpawnEvent.SpawnReason.NATURAL
        ) {
            event.isCancelled = true
            return
        }

        if (event.entityType != EntityType.SHEEP) {
            return
        }

        if (SheepMergeManager.isFarmBuildWorld(world)
            && world != null
            && (world.hasStorm() || world.isThundering)
        ) {
            event.isCancelled = true
            return
        }

        if (!SheepMergeManager.isSheepFarmWorld(world)) {
            return
        }

        val sheep = event.entity as Sheep
        SheepMergeManager.setSheepTier(sheep, SheepMergeManager.rollSpawnTier(world))
    }

    @EventHandler(ignoreCancelled = true)
    fun onWeatherChange(event: WeatherChangeEvent) {
        val world = event.world
        if (!SheepMergeManager.isSheepFarmWorld(world) && !SheepMergeManager.isFarmBuildWorld(world)) {
            return
        }
        if (SheepMergeManager.isSheepFarmWorld(world)
            && SheepMergeManager.isSheepStormActive()
            && event.toWeatherState()
        ) {
            return
        }
        if (event.toWeatherState()) {
            event.isCancelled = true
            world.setStorm(false)
            world.weatherDuration = 0
            world.clearWeatherDuration = Int.MAX_VALUE
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onThunderChange(event: ThunderChangeEvent) {
        val world = event.world
        if (!SheepMergeManager.isSheepFarmWorld(world) && !SheepMergeManager.isFarmBuildWorld(world)) {
            return
        }
        if (SheepMergeManager.isSheepFarmWorld(world)
            && SheepMergeManager.isSheepStormActive()
            && event.toThunderState()
        ) {
            return
        }
        if (event.toThunderState()) {
            event.isCancelled = true
            world.setThundering(false)
        }
    }

    @EventHandler
    fun onPlayerShearEntity(event: PlayerShearEntityEvent) {
        val sheep = event.entity as? Sheep ?: return

        if (!SheepMergeManager.isSheepFarmWorld(sheep.world)) {
            return
        }

        if (!SheepMergeManager.isFarmOwner(event.player, sheep.world)) {
            event.isCancelled = true
            event.player.sendMessage(SheepMergeManager.warning("Visitors cannot shear sheep here."))
            return
        }

        if (SheepMergeManager.blockTutorialAction(
                event.player,
                SheepMergeManager.TutorialAction.SHEAR_SHEEP,
                "shear sheep"
            )
        ) {
            event.isCancelled = true
            return
        }

        // Sneak-right-click is used for pickup, so do not shear in that case.
        if (event.player.isSneaking) {
            event.isCancelled = true
            return
        }

        event.isCancelled = true
        SheepMergeManager.shearSheepForPlayer(event.player, sheep)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        val sheep = event.entity as? Sheep ?: return

        val block = event.block
        if (block.type != Material.GRASS_BLOCK && event.to != Material.DIRT) {
            return
        }

        if (!SheepMergeManager.isSheepFarmWorld(block.world)) {
            return
        }

        if (sheep.isSheared) {
            sheep.isSheared = true
            SheepMergeManager.updateSheepName(sheep)
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onSheepRegrowWool(event: SheepRegrowWoolEvent) {
        val sheep = event.entity
        if (!SheepMergeManager.isSheepFarmWorld(sheep.world)) {
            return
        }

        val nextEatAt = SheepMergeManager.getNextEatTimestamp(sheep)
        if (nextEatAt <= 0L) {
            return
        }

        if (System.currentTimeMillis() < nextEatAt) {
            event.isCancelled = true
            sheep.isSheared = true
            SheepMergeManager.updateSheepName(sheep)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onSheepDamage(event: EntityDamageEvent) {
        val sheep = event.entity as? Sheep ?: return
        if (!SheepMergeManager.isSheepFarmWorld(sheep.world)) {
            return
        }
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val item = event.item
        if (!SheepMergeManager.isSheepMergeEggItem(item)) {
            return
        }

        val player = event.player
        val world = player.world
        if (!SheepMergeManager.isSheepFarmWorld(world)
            && !SheepMergeManager.isTutorialWorld(world)
            && !SheepMergeManager.isFarmBuildWorld(world)
        ) {
            return
        }

        event.isCancelled = true
        if (!SheepMergeManager.isSheepFarmWorld(world)) {
            return
        }

        if (!SheepMergeManager.isFarmOwner(player, world)) {
            player.sendMessage(SheepMergeManager.warning("Visitors cannot spawn sheep here."))
            event.isCancelled = true
            return
        }

        if (SheepMergeManager.blockTutorialAction(
                player,
                SheepMergeManager.TutorialAction.SPAWN_SHEEP,
                "spawn sheep"
            )
        ) {
            return
        }

        if (SheepMergeManager.isWorldAtLimit(world)) {
            if (SheepMergeManager.shouldNotifySpawnLimit(player)) {
                player.sendMessage(SheepMergeManager.warning("Farm full. Use /sheepmerge upgrade or merge sheep."))
            }
            return
        }

        val clickedBlock = event.clickedBlock
        val blockFace: BlockFace? = event.blockFace
        if (clickedBlock == null || blockFace == null) {
            return
        }

        val spawnLocation = clickedBlock.getRelative(blockFace).location.add(0.5, 0.0, 0.5)
        if (!SheepMergeManager.spawnSheepFromEgg(player, spawnLocation)) {
            if (SheepMergeManager.shouldNotifyOutOfEggs(player)) {
                player.sendMessage(SheepMergeManager.warning("No eggs available. Wait for your egg timer."))
            }
            return
        }

        SheepMergeManager.recordQuestSpawn(player)
        SheepMergeManager.recordTutorialSpawn(player)
        SheepMergeManager.updatePointsScoreboard(player)
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }

        if (event.rightClicked !is Sheep) {
            return
        }
        var targetSheep = event.rightClicked as Sheep

        val player = event.player
        if (!SheepMergeManager.isSheepFarmWorld(targetSheep.world)) {
            return
        }

        val item = player.inventory.itemInMainHand
        val attemptingShearOnly = item.type == Material.SHEARS
            && !SheepMergeManager.hasPickedUpSheep(player)
        if (attemptingShearOnly) {
            return
        }

        if (!SheepMergeManager.isFarmOwner(player, targetSheep.world)) {
            event.isCancelled = true
            player.sendMessage(SheepMergeManager.warning("Visitors cannot merge sheep here."))
            return
        }

        if (SheepMergeManager.blockTutorialAction(
                player,
                SheepMergeManager.TutorialAction.MERGE_SHEEP,
                "merge sheep"
            )
        ) {
            event.isCancelled = true
            return
        }

        if (SheepMergeManager.isSheepMergeEggItem(item)) {
            event.isCancelled = true
            return
        }

        if (player.isSneaking && !SheepMergeManager.hasPickedUpSheep(player)) {
            if (SheepMergeManager.tryAutoMergeOnPickup(player, targetSheep)) {
                event.isCancelled = true
                return
            }
            SheepMergeManager.storePickedUpSheep(player, targetSheep)
            return
        }

        if (!SheepMergeManager.hasPickedUpSheep(player)) {
            return
        }

        val pickedSheep = SheepMergeManager.getPickedUpSheep(player) ?: return

        if (pickedSheep.uniqueId == targetSheep.uniqueId) {
            val retargetedSheep = findMergeTargetInSight(player, pickedSheep)
            if (retargetedSheep == null) {
                event.isCancelled = true
                player.sendMessage(SheepMergeManager.hint("Aim at another sheep to merge."))
                return
            }
            targetSheep = retargetedSheep
        }

        val carriedTier = SheepMergeManager.getSheepTier(pickedSheep)
        val targetTier = SheepMergeManager.getSheepTier(targetSheep)

        if (carriedTier == null) {
            SheepMergeManager.clearPickedUpSheep(player)
            return
        }

        if (carriedTier != targetTier) {
            SheepMergeManager.dropPickedUpSheep(player)
            return
        }

        if (!carriedTier.hasNext()) {
            if (carriedTier != SheepTier.RAINBOW) {
                player.sendMessage(SheepMergeManager.warning("Top tier. No merge."))
                return
            }

            val pickedRainbowTier = SheepMergeManager.getRainbowTier(pickedSheep)
            val targetRainbowTier = SheepMergeManager.getRainbowTier(targetSheep)
            if (pickedRainbowTier != targetRainbowTier) {
                event.isCancelled = true
                SheepMergeManager.dropPickedUpSheep(player)
                player.sendMessage(
                    SheepMergeManager.warning(
                        "Rainbow sheep tiers must match to merge ("
                            + SheepMergeManager.formatRainbowTier(pickedRainbowTier)
                            + " vs " + SheepMergeManager.formatRainbowTier(targetRainbowTier) + ")."
                    )
                )
                return
            }
            val mergedRainbowTier = maxOf(1, pickedRainbowTier + 1)
            val mergedWoolRegenMs = SheepMergeManager.getCombinedRemainingWoolRegenMs(pickedSheep, targetSheep)

            val world = targetSheep.world
            val spawnLocation = targetSheep.location
            targetSheep.remove()
            pickedSheep.remove()

            val mergedSheep = world.spawn(spawnLocation, Sheep::class.java)
            SheepMergeManager.setSheepTier(mergedSheep, SheepTier.RAINBOW)
            SheepMergeManager.setRainbowTier(mergedSheep, mergedRainbowTier)
            SheepMergeManager.initializeMergedSheepAfterMerge(mergedSheep, SheepTier.RAINBOW, mergedWoolRegenMs)

            val velocity = player.location.direction.multiply(0.4).setY(0.2)
            mergedSheep.velocity = velocity
            world.spawnParticle(Particle.VILLAGER_HAPPY, spawnLocation.add(0.0, 0.5, 0.0), 15, 0.3, 0.3, 0.3, 0.05)

            if (pickedRainbowTier == 1 && SheepMergeManager.shouldAnnounceTierUnlock(player, SheepTier.RAINBOW)) {
                SheepMergeManager.announceTierUnlock(player, SheepTier.RAINBOW)
                SheepMergeManager.markTierUnlockAnnounced(player, SheepTier.RAINBOW)
            }
            SheepMergeManager.recordSheepMerge(player, carriedTier, 0)
            SheepMergeManager.recordQuestMerge(player)
            SheepMergeManager.recordTutorialMerge(player)
            SheepMergeManager.showOverlay(
                player,
                SheepMergeManager.action(
                    carriedTier.displayName + " " + SheepMergeManager.formatRainbowTier(pickedRainbowTier)
                        + " + " + carriedTier.displayName + " "
                        + SheepMergeManager.formatRainbowTier(targetRainbowTier)
                        + " -> " + carriedTier.displayName + " "
                        + SheepMergeManager.formatRainbowTier(mergedRainbowTier)
                )
            )
            SheepMergeManager.clearPickedUpSheep(player)
            return
        }

        val mergedTier = carriedTier.next()
        var woolReadyCount = 0
        if (!pickedSheep.isSheared) {
            woolReadyCount++
        }
        if (!targetSheep.isSheared) {
            woolReadyCount++
        }
        val combinedWoolRegenMs = SheepMergeManager.getCombinedRemainingWoolRegenMs(pickedSheep, targetSheep)
        val world = targetSheep.world
        val spawnLocation = targetSheep.location

        // Remove both source sheep and spawn the merged result.
        targetSheep.remove()
        pickedSheep.remove()

        val mergedSheep = world.spawn(spawnLocation, Sheep::class.java)
        SheepMergeManager.setSheepTier(mergedSheep, mergedTier)
        SheepMergeManager.initializeMergedSheepAfterMerge(mergedSheep, mergedTier, combinedWoolRegenMs)
        val velocity = player.location.direction.multiply(0.4).setY(0.2)
        mergedSheep.velocity = velocity
        world.spawnParticle(Particle.VILLAGER_HAPPY, spawnLocation.add(0.0, 0.5, 0.0), 15, 0.3, 0.3, 0.3, 0.05)

        if (SheepMergeManager.shouldAnnounceTierUnlock(player, mergedTier)) {
            SheepMergeManager.announceTierUnlock(player, mergedTier)
            SheepMergeManager.markTierUnlockAnnounced(player, mergedTier)
        }
        SheepMergeManager.recordSheepMerge(player, carriedTier, woolReadyCount)
        SheepMergeManager.recordQuestMerge(player)
        SheepMergeManager.recordTutorialMerge(player)
        SheepMergeManager.showOverlay(
            player,
            SheepMergeManager.action(
                carriedTier.displayName + " + " + carriedTier.displayName + " -> "
                    + mergedTier.displayName
            )
        )
        SheepMergeManager.clearPickedUpSheep(player)
    }

    private fun findMergeTargetInSight(player: Player, carriedSheep: Sheep): Sheep? {
        val result = player.world.rayTraceEntities(
            player.eyeLocation,
            player.location.direction,
            5.5,
            0.2
        ) { entity -> entity is Sheep && entity.uniqueId != carriedSheep.uniqueId }
        val hitEntity = result?.hitEntity
        return if (hitEntity is Sheep) hitEntity else null
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        SheepMergeManager.clearEggTimer(player)
        SheepMergeManager.clearPickedUpSheep(player)
        SheepMergeManager.clearMergeReminder(player)
        SheepMergeManager.clearPrestigeReminder(player)
        SheepMergeManager.clearRebirthReminder(player)
        SheepMergeManager.clearComboRuntime(player)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        val player = event.player
        val reason = event.reason
        val duplicateLoginKick = reason.contains("another location", ignoreCase = true)
        SheepMergeManager.clearEggTimer(player)
        if (duplicateLoginKick) {
            SheepMergeManager.clearPickedUpSheep(player)
            SheepMergeManager.clearMergeReminder(player)
            SheepMergeManager.clearPrestigeReminder(player)
            SheepMergeManager.clearRebirthReminder(player)
            SheepMergeManager.clearComboRuntime(player)
            return
        }
        SheepMergeManager.clearPickedUpSheep(player)
        SheepMergeManager.clearMergeReminder(player)
        SheepMergeManager.clearPrestigeReminder(player)
        SheepMergeManager.clearRebirthReminder(player)
        SheepMergeManager.clearComboRuntime(player)
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!SheepMergeManager.isSheepFarmWorld(event.entity.world)) {
            return
        }
        event.keepInventory = true
        event.keepLevel = true
        event.drops.clear()
        event.droppedExp = 0
    }

    @EventHandler
    fun onSheepDeath(event: EntityDeathEvent) {
        val sheep = event.entity as? Sheep ?: return
        if (!SheepMergeManager.isSheepFarmWorld(sheep.world)) {
            return
        }
        event.drops.clear()
        event.droppedExp = 0
    }
}
