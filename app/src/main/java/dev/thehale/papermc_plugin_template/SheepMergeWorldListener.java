package dev.thehale.papermc_plugin_template;

import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SheepMergeWorldListener implements Listener {

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!SheepMergeManager.isSheepFarmWorld(event.getFrom())
                && SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            SheepMergeManager.savePlayerInventory(player);
            player.getInventory().clear();
            player.getInventory().setItemInMainHand(SheepMergeManager.getSheepMergeShears());
            int extraEggs = SheepMergeManager.getStartEggsBonus(player);
            if (extraEggs > 0) {
                player.getInventory()
                        .addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SHEEP_SPAWN_EGG, extraEggs));
            }
            SheepMergeManager.showPointsScoreboard(player);
            SheepMergeManager.updatePointsScoreboard(player);
            SheepMergeManager.resetEggTimer(player);
        } else if (SheepMergeManager.isSheepFarmWorld(event.getFrom())
                && !SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            SheepMergeManager.saveData();
            SheepMergeManager.restorePlayerInventory(player);
            SheepMergeManager.restorePlayerScoreboard(player);
            SheepMergeManager.clearEggTimer(player);
            SheepMergeManager.clearPickedUpSheep(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!SheepMergeManager.isUpgradeMenuTitle(title)
                && !SheepMergeManager.isPrestigeMenuTitle(title)
                && !SheepMergeManager.isShopMenuTitle(title)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (SheepMergeManager.isUpgradeMenuTitle(title)) {
            SheepMergeManager.handleUpgradeMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isPrestigeMenuTitle(title)) {
            SheepMergeManager.handlePrestigeMenuClick(player, event.getRawSlot());
            return;
        }
        SheepMergeManager.handleShopMenuClick(player, event.getRawSlot());
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            event.setCancelled(true);
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
