package dev.thehale.papermc_plugin_template;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SheepMergeWorldListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (SheepMergeManager.isFarmBuildWorld(player.getWorld()) && !player.isOp()) {
            World fallbackWorld = player.getServer().getWorlds().isEmpty() ? null
                    : player.getServer().getWorlds().get(0);
            if (fallbackWorld != null) {
                player.teleport(fallbackWorld.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D));
            }
            player.sendMessage(SheepMergeManager.warning("Only operators may enter the farm build world."));
            return;
        }
        SheepMergeManager.restoreSavedStateOutsideFarm(player);
        if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            return;
        }
        SheepMergeManager.upgradeSheepBelowMinimumSpawnTier(player.getWorld());
        SheepMergeManager.enforceFarmLoadout(player);
        SheepMergeManager.applyFarmSaturation(player);
        SheepMergeManager.showPointsScoreboard(player);
        SheepMergeManager.updatePointsScoreboard(player);
        SheepMergeManager.resetEggTimer(player);
        SheepMergeManager.resetMergeReminder(player);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        if (world == null) {
            return;
        }
        SheepMergeManager.restoreTopPointsDisplayAfterRestart(world);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (SheepMergeManager.isFarmBuildWorld(player.getWorld()) && !player.isOp()) {
            World fallbackWorld = player.getServer().getWorlds().isEmpty() ? null
                    : player.getServer().getWorlds().get(0);
            if (fallbackWorld != null) {
                player.teleport(fallbackWorld.getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D));
            }
            player.sendMessage(SheepMergeManager.warning("Only operators may enter the farm build world."));
            return;
        }
        boolean fromManagedWorld = SheepMergeManager.isSheepFarmWorld(event.getFrom());
        boolean toManagedWorld = SheepMergeManager.isSheepFarmWorld(player.getWorld());
        boolean fromTutorialWorld = SheepMergeManager.isTutorialWorld(event.getFrom());
        boolean toTutorialWorld = SheepMergeManager.isTutorialWorld(player.getWorld());

        if (SheepMergeManager.isFarmBuildWorld(event.getFrom())
                && !SheepMergeManager.isFarmBuildWorld(player.getWorld())) {
            player.getServer().getScheduler().runTaskLater(SheepMergePlugin.instance,
                    SheepMergeManager::saveBuildWorldIfIdle, 1L);
        }

        if (!fromManagedWorld && toManagedWorld) {
            SheepMergeManager.savePlayerInventory(player);
            player.getInventory().clear();
            SheepMergeManager.upgradeSheepBelowMinimumSpawnTier(player.getWorld());
            SheepMergeManager.enforceFarmLoadout(player);
            SheepMergeManager.applyFarmSaturation(player);
            SheepMergeManager.showPointsScoreboard(player);
            SheepMergeManager.updatePointsScoreboard(player);
            SheepMergeManager.resetEggTimer(player);
            SheepMergeManager.resetMergeReminder(player);
            if (!SheepMergeManager.isFarmOwner(player, player.getWorld())) {
                player.sendMessage(SheepMergeManager
                        .hint("You are visiting another farm. Use /sheepmerge to return to your own farm."));
                player.sendTitle(
                        SheepMergeManager.color("&eVisiting a farm"),
                        SheepMergeManager.color("&7Use /sheepmerge to return home"),
                        10,
                        50,
                        10);
            }
        } else if (fromTutorialWorld && toManagedWorld && !toTutorialWorld) {
            player.getInventory().clear();
            SheepMergeManager.clearPickedUpSheep(player);
            SheepMergeManager.clearEggTimer(player);
            SheepMergeManager.enforceFarmLoadout(player);
            SheepMergeManager.applyFarmSaturation(player);
            SheepMergeManager.showPointsScoreboard(player);
            SheepMergeManager.updatePointsScoreboard(player);
            SheepMergeManager.resetEggTimer(player);
            SheepMergeManager.resetMergeReminder(player);
        } else if (fromManagedWorld && !toManagedWorld) {
            SheepMergeManager.saveData();
            SheepMergeManager.restorePlayerInventory(player);
            SheepMergeManager.restorePlayerScoreboard(player);
            SheepMergeManager.updateTabListPointsVisibility(player);
            SheepMergeManager.clearEggTimer(player);
            SheepMergeManager.clearPickedUpSheep(player);
            SheepMergeManager.clearMergeReminder(player);
            SheepMergeManager.clearPrestigeReminder(player);
            SheepMergeManager.clearComboRuntime(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory != null && clickedInventory.equals(player.getInventory())) {
                ItemStack currentItem = event.getCurrentItem();
                ItemStack cursorItem = event.getCursor();
                if (SheepMergeManager.isForcedFarmLoadoutItem(currentItem)
                        || SheepMergeManager.isForcedFarmLoadoutItem(cursorItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (!SheepMergeManager.isUpgradeMenuTitle(title)
                && !SheepMergeManager.isPrestigeMenuTitle(title)
                && !SheepMergeManager.isQuestMenuTitle(title)
                && !SheepMergeManager.isQuestUpgradesMenuTitle(title)
                && !SheepMergeManager.isShopMenuTitle(title)
                && !SheepMergeManager.isComboShopMenuTitle(title)
                && !SheepMergeManager.isAutomationMenuTitle(title)) {
            return;
        }

        event.setCancelled(true);
        if (SheepMergeManager.isUpgradeMenuTitle(title)) {
            SheepMergeManager.handleUpgradeMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isPrestigeMenuTitle(title)) {
            SheepMergeManager.handlePrestigeMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isQuestMenuTitle(title)) {
            SheepMergeManager.handleQuestMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isQuestUpgradesMenuTitle(title)) {
            SheepMergeManager.handleQuestUpgradeMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isShopMenuTitle(title)) {
            SheepMergeManager.handleShopMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isAutomationMenuTitle(title)) {
            SheepMergeManager.handleAutomationMenuClick(player, event.getRawSlot());
            return;
        }
        SheepMergeManager.handleComboShopMenuClick(player, event.getRawSlot());
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        World world = player.getWorld();
        if (!SheepMergeManager.isSheepFarmWorld(world) || SheepMergeManager.isFarmBuildWorld(world)) {
            return;
        }

        ItemStack pickedItem = event.getItem() == null ? null : event.getItem().getItemStack();
        if (SheepMergeManager.isForcedFarmLoadoutItem(pickedItem) && pickedItem.getAmount() == 1) {
            return;
        }

        event.setCancelled(true);
        if (event.getItem() != null && event.getItem().isValid()) {
            event.getItem().remove();
        }
        player.getInventory().clear();
        SheepMergeManager.enforceFarmLoadout(player);
        player.sendMessage(SheepMergeManager.warning("You picked up an invalid item for this world. Inventory reset."));
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (SheepMergeManager.isSheepFarmWorld(event.getPlayer().getWorld())) {
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

        SheepMergeManager.applyFarmSaturation(player);

        ItemStack item = player.getInventory().getItemInMainHand();
        if (SheepMergeManager.isSheepMergeUpgradeCommandItem(item)) {
            event.setCancelled(true);
            player.performCommand("sheepmerge upgrade");
            return;
        }

        if (!SheepMergeManager.hasPickedUpSheep(player)) {
            return;
        }

        Sheep carriedSheep = SheepMergeManager.getPickedUpSheep(player);
        if (carriedSheep == null) {
            return;
        }

        org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                5,
                0.2,
                entity -> entity instanceof Sheep && !entity.getUniqueId().equals(carriedSheep.getUniqueId()));

        if (result != null && result.getHitEntity() instanceof Sheep) {
            return;
        }

        if (SheepMergeManager.dropPickedUpSheep(player)) {
            event.setCancelled(true);
        }
    }
}
