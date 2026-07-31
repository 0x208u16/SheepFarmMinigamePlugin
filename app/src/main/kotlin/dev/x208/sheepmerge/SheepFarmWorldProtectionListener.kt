package dev.x208.sheepmerge

import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntityExplodeEvent

class SheepFarmWorldProtectionListener : Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player: Player = event.player
        val world: World = event.block.world
        if (!isProtectedWorld(world)) {
            return
        }
        if (SheepMergeManager.isFarmBuildWorld(world) && !player.isOp) {
            event.isCancelled = true
            player.sendMessage("Only operators can edit the farm build world.")
            return
        }
        if (isPersonalWorld(world) && !player.isOp) {
            event.isCancelled = true
            player.sendMessage("You cannot break blocks in your personal world.")
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player: Player = event.player
        val world: World = event.block.world
        if (!isProtectedWorld(world)) {
            return
        }
        if (SheepMergeManager.isFarmBuildWorld(world) && !player.isOp) {
            event.isCancelled = true
            player.sendMessage("Only operators can edit the farm build world.")
            return
        }
        if (isPersonalWorld(world) && !player.isOp) {
            event.isCancelled = true
            player.sendMessage("You cannot place blocks in your personal world.")
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        val world = event.block?.world
        if (isProtectedWorld(world)) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockIgnite(event: BlockIgniteEvent) {
        val world = event.block?.world
        if (isProtectedWorld(world)) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockSpread(event: BlockSpreadEvent) {
        val world = event.block?.world
        if (isProtectedWorld(world)) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        val world = event.block?.world
        if (!isProtectedWorld(world)) {
            return
        }
        event.blockList().clear()
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        val world = event.location?.world
        if (!isProtectedWorld(world)) {
            return
        }
        event.blockList().clear()
    }

    private fun isPersonalWorld(world: World?): Boolean {
        return SheepMergeManager.isSheepFarmWorld(world)
    }

    private fun isProtectedWorld(world: World?): Boolean {
        return world != null && (
            SheepMergeManager.isFarmBuildWorld(world)
                || SheepMergeManager.isSheepFarmWorld(world)
                || SheepMergeManager.isTutorialWorld(world)
            )
    }
}
