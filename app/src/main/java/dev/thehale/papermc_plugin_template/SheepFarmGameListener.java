package dev.thehale.papermc_plugin_template;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class SheepFarmGameListener implements Listener {

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.SHEEP) {
            return;
        }

        World world = event.getLocation().getWorld();
        if (!SheepMergeManager.isSheepFarmWorld(world)) {
            return;
        }

        Sheep sheep = (Sheep) event.getEntity();
        SheepMergeManager.setSheepTier(sheep, SheepMergeManager.rollSpawnTier(world));
    }

    @EventHandler
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        if (!(event.getEntity() instanceof Sheep sheep)) {
            return;
        }

        if (!SheepMergeManager.isSheepFarmWorld(sheep.getWorld())) {
            return;
        }

        if (!SheepMergeManager.isFarmOwner(event.getPlayer(), sheep.getWorld())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(SheepMergeManager.warning("Visitors cannot shear sheep here."));
            return;
        }

        if (event.getPlayer().isSneaking()) {
            // Sneak-right-click is used for pickup, so do not shear in that case.
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        sheep.setSheared(true);
        sheep.setAI(true);
        SheepTier tier = SheepMergeManager.getSheepTier(sheep);
        SheepMergeManager.setNextEatTimestamp(sheep,
                System.currentTimeMillis() + SheepMergeManager.getEatCooldownSeconds(sheep, tier) * 1000L);

        int points = SheepMergeManager.calculateShearPoints(event.getPlayer(), tier);
        SheepMergeManager.addPoints(event.getPlayer(), points);

        SheepMergeManager.tryTriggerShearWoolSave(event.getPlayer(), sheep);
        SheepMergeManager.tryTriggerShearTierBoost(event.getPlayer(), sheep);
        SheepMergeManager.updateSheepName(sheep);

        SheepMergeManager.recordQuestShear(event.getPlayer());
        SheepMergeManager.recordTutorialShear(event.getPlayer());
        SheepMergeManager.updatePointsScoreboard(event.getPlayer());
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Sheep sheep)) {
            return;
        }

        Block block = event.getBlock();
        if (block.getType() != Material.GRASS_BLOCK && event.getTo() != Material.DIRT) {
            return;
        }

        if (!SheepMergeManager.isSheepFarmWorld(block.getWorld())) {
            return;
        }

        if (sheep.isSheared()) {
            long now = System.currentTimeMillis();
            long nextEat = SheepMergeManager.getNextEatTimestamp(sheep);
            if (now >= nextEat && nextEat > 0L) {
                SheepMergeManager.setNextEatTimestamp(sheep, 0L);
                sheep.setSheared(false);
                SheepMergeManager.updateSheepName(sheep);
                event.setCancelled(true);
                return;
            }
            sheep.setSheared(true);
            SheepMergeManager.updateSheepName(sheep);
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSheepDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Sheep sheep)) {
            return;
        }
        if (!SheepMergeManager.isSheepFarmWorld(sheep.getWorld())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!SheepMergeManager.isSheepMergeEggItem(item)) {
            return;
        }

        Player player = event.getPlayer();
        event.setCancelled(true);
        if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            return;
        }

        if (!SheepMergeManager.isFarmOwner(player, player.getWorld())) {
            player.sendMessage(SheepMergeManager.warning("Visitors cannot spawn sheep here."));
            event.setCancelled(true);
            return;
        }

        if (SheepMergeManager.isWorldAtLimit(player.getWorld())) {
            if (SheepMergeManager.shouldNotifySpawnLimit(player)) {
                player.sendMessage(SheepMergeManager.warning("Farm full. Use /sheepmerge upgrade or merge sheep."));
            }
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        BlockFace blockFace = event.getBlockFace();
        if (clickedBlock == null || blockFace == null) {
            return;
        }

        org.bukkit.Location spawnLocation = clickedBlock.getRelative(blockFace).getLocation().add(0.5D, 0.0D, 0.5D);
        if (!SheepMergeManager.spawnSheepFromEgg(player, spawnLocation)) {
            player.sendMessage(SheepMergeManager.warning("No eggs available. Wait for your egg timer."));
            return;
        }

        SheepMergeManager.recordQuestSpawn(player);
        SheepMergeManager.recordTutorialSpawn(player);
        SheepMergeManager.updatePointsScoreboard(player);

    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Sheep targetSheep)) {
            return;
        }

        Player player = event.getPlayer();
        if (!SheepMergeManager.isSheepFarmWorld(targetSheep.getWorld())) {
            return;
        }

        if (!SheepMergeManager.isFarmOwner(player, targetSheep.getWorld())) {
            event.setCancelled(true);
            player.sendMessage(SheepMergeManager.warning("Visitors cannot merge sheep here."));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (SheepMergeManager.isSheepMergeEggItem(item)) {
            event.setCancelled(true);
            return;
        }

        if (player.isSneaking() && !SheepMergeManager.hasPickedUpSheep(player)) {
            SheepMergeManager.storePickedUpSheep(player, targetSheep);
            return;
        }

        if (!SheepMergeManager.hasPickedUpSheep(player)) {
            return;
        }

        Sheep pickedSheep = SheepMergeManager.getPickedUpSheep(player);
        if (pickedSheep == null) {
            return;
        }

        if (pickedSheep.getUniqueId().equals(targetSheep.getUniqueId())) {
            return;
        }

        SheepTier carriedTier = SheepMergeManager.getSheepTier(pickedSheep);
        SheepTier targetTier = SheepMergeManager.getSheepTier(targetSheep);

        if (carriedTier == null) {
            SheepMergeManager.clearPickedUpSheep(player);
            return;
        }

        if (!carriedTier.equals(targetTier)) {
            SheepMergeManager.dropPickedUpSheep(player);
            return;
        }

        if (!carriedTier.hasNext()) {
            player.sendMessage(SheepMergeManager.warning("Top tier. No merge."));
            return;
        }

        SheepTier mergedTier = carriedTier.next();
        int woolReadyCount = 0;
        if (!pickedSheep.isSheared()) {
            woolReadyCount++;
        }
        if (!targetSheep.isSheared()) {
            woolReadyCount++;
        }
        World world = targetSheep.getWorld();
        org.bukkit.Location spawnLocation = targetSheep.getLocation();

        // Remove both source sheep and spawn the merged result.
        targetSheep.remove();
        pickedSheep.remove();

        Sheep mergedSheep = world.spawn(spawnLocation, Sheep.class);
        SheepMergeManager.setSheepTier(mergedSheep, mergedTier);
        SheepMergeManager.initializeMergedSheepAsSheared(mergedSheep, mergedTier);
        Vector velocity = player.getLocation().getDirection().multiply(0.4).setY(0.2);
        mergedSheep.setVelocity(velocity);
        world.spawnParticle(Particle.VILLAGER_HAPPY, spawnLocation.add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.05);

        if (SheepMergeManager.shouldAnnounceTierUnlock(player, mergedTier)) {
            SheepMergeManager.announceTierUnlock(player, mergedTier);
            SheepMergeManager.markTierUnlockAnnounced(player, mergedTier);
        }
        SheepMergeManager.recordSheepMerge(player, carriedTier, woolReadyCount);
        SheepMergeManager.recordQuestMerge(player);
        SheepMergeManager.recordTutorialMerge(player);
        SheepMergeManager.showOverlay(player, SheepMergeManager.action(
                carriedTier.getDisplayName() + " + " + carriedTier.getDisplayName() + " -> "
                        + mergedTier.getDisplayName()));
        SheepMergeManager.clearPickedUpSheep(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SheepMergeManager.clearEggTimer(player);
        SheepMergeManager.clearPickedUpSheep(player);
        SheepMergeManager.clearMergeReminder(player);
        SheepMergeManager.clearPrestigeReminder(player);
        SheepMergeManager.clearComboRuntime(player);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        String reason = event.getReason();
        boolean duplicateLoginKick = reason != null && reason.toLowerCase().contains("another location");
        SheepMergeManager.clearEggTimer(player);
        if (duplicateLoginKick) {
            SheepMergeManager.clearPickedUpSheep(player);
            SheepMergeManager.clearMergeReminder(player);
            SheepMergeManager.clearPrestigeReminder(player);
            SheepMergeManager.clearComboRuntime(player);
            return;
        }
        SheepMergeManager.clearPickedUpSheep(player);
        SheepMergeManager.clearMergeReminder(player);
        SheepMergeManager.clearPrestigeReminder(player);
        SheepMergeManager.clearComboRuntime(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!SheepMergeManager.isSheepFarmWorld(event.getEntity().getWorld())) {
            return;
        }
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler
    public void onSheepDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Sheep sheep)) {
            return;
        }
        if (!SheepMergeManager.isSheepFarmWorld(sheep.getWorld())) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
    }
}
