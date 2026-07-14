package dev.thehale.papermc_plugin_template;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SheepMergeWorldListener implements Listener {

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!SheepMergeManager.isSheepFarmWorld(event.getFrom())
                && SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            SheepMergeManager.savePlayerInventory(player);
            player.getInventory().clear();
            player.getInventory().setItemInMainHand(SheepMergeManager.getSheepMergeShears());
            SheepMergeManager.showPointsScoreboard(player);
            SheepMergeManager.updatePointsScoreboard(player);
        } else if (SheepMergeManager.isSheepFarmWorld(event.getFrom())
                && !SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            SheepMergeManager.saveData();
            SheepMergeManager.restorePlayerInventory(player);
            SheepMergeManager.restorePlayerScoreboard(player);
            SheepMergeManager.clearPickedUpSheep(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            return;
        }

        if (!SheepMergeManager.hasPickedUpSheep(player)) {
            return;
        }

        org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                5,
                0.2,
                entity -> entity instanceof Sheep);

        if (result != null && result.getHitEntity() instanceof Sheep) {
            return;
        }

        if (SheepMergeManager.dropPickedUpSheep(player)) {
            player.sendMessage("You dropped the sheep above your head.");
            event.setCancelled(true);
        }
    }
}
