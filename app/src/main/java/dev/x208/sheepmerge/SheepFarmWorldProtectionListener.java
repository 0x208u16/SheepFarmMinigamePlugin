package dev.x208.sheepmerge;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

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

    private boolean isPersonalWorld(World world) {
        return SheepMergeManager.isSheepFarmWorld(world);
    }
}
