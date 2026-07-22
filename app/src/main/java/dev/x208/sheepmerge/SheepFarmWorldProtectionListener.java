package dev.x208.sheepmerge;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class SheepFarmWorldProtectionListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (SheepMergeManager.isFarmBuildWorld(world) && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage("Only operators can edit the farm build world.");
            return;
        }
        if (isPersonalWorld(world) && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage("You cannot break blocks in your personal world.");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (SheepMergeManager.isFarmBuildWorld(world) && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage("Only operators can edit the farm build world.");
            return;
        }
        if (isPersonalWorld(world) && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage("You cannot place blocks in your personal world.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        World world = event.getBlock() == null ? null : event.getBlock().getWorld();
        if (isProtectedWorld(world)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        World world = event.getBlock() == null ? null : event.getBlock().getWorld();
        if (isProtectedWorld(world)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        World world = event.getBlock() == null ? null : event.getBlock().getWorld();
        if (isProtectedWorld(world)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        World world = event.getBlock() == null ? null : event.getBlock().getWorld();
        if (!isProtectedWorld(world)) {
            return;
        }
        event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        World world = event.getLocation() == null ? null : event.getLocation().getWorld();
        if (!isProtectedWorld(world)) {
            return;
        }
        event.blockList().clear();
    }

    private boolean isPersonalWorld(World world) {
        return SheepMergeManager.isSheepFarmWorld(world);
    }

    private boolean isProtectedWorld(World world) {
        return SheepMergeManager.isFarmBuildWorld(world)
                || SheepMergeManager.isSheepFarmWorld(world)
                || SheepMergeManager.isTutorialWorld(world);
    }
}
