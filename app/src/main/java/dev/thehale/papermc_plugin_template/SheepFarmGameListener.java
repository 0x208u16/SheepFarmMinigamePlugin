package dev.thehale.papermc_plugin_template;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
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

        event.setCancelled(true);
        sheep.setSheared(true);
        sheep.setAI(false);
        SheepTier tier = SheepMergeManager.getSheepTier(sheep);
        SheepMergeManager.setNextEatTimestamp(sheep,
                System.currentTimeMillis() + SheepMergeManager.getEatCooldownSeconds(sheep, tier) * 1000L);
        SheepMergeManager.updateSheepName(sheep);

        int points = SheepMergeManager.calculateShearPoints(event.getPlayer(), tier);
        SheepMergeManager.addPoints(event.getPlayer(), points);
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
                sheep.setSheared(false);
                SheepMergeManager.setNextEatTimestamp(sheep, 0L);
            } else {
                sheep.setSheared(true);
            }
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
        if (item == null || item.getType() != Material.SHEEP_SPAWN_EGG) {
            return;
        }

        Player player = event.getPlayer();
        if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            return;
        }

        if (SheepMergeManager.isWorldAtLimit(player.getWorld())) {
            player.sendMessage(SheepMergeManager.warning("Farm full. Use /sheepmerge upgrade or merge sheep."));
            event.setCancelled(true);
            return;
        }

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

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType() == Material.SHEEP_SPAWN_EGG) {
            event.setCancelled(true);
            return;
        }

        if (player.isSneaking() && !SheepMergeManager.hasPickedUpSheep(player)) {
            SheepTier tier = SheepMergeManager.getSheepTier(targetSheep);
            SheepMergeManager.storePickedUpSheep(player, targetSheep);
            SheepMergeManager.showOverlay(player,
                    SheepMergeManager.action("Held: " + tier.getDisplayName() + " | sneak-right-click same tier"));
            return;
        }

        if (!SheepMergeManager.hasPickedUpSheep(player)) {
            return;
        }

        Sheep pickedSheep = SheepMergeManager.getPickedUpSheep(player);
        if (pickedSheep == null) {
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
            player.sendMessage(SheepMergeManager.warning("Tier mismatch. Sheep dropped."));
            return;
        }

        if (!carriedTier.hasNext()) {
            player.sendMessage(SheepMergeManager.warning("Top tier. No merge."));
            return;
        }

        SheepTier mergedTier = carriedTier.next();
        World world = targetSheep.getWorld();
        org.bukkit.Location spawnLocation = targetSheep.getLocation();

        // Remove both source sheep and spawn the merged result.
        targetSheep.remove();
        pickedSheep.remove();

        Sheep mergedSheep = world.spawn(spawnLocation, Sheep.class);
        SheepMergeManager.setSheepTier(mergedSheep, mergedTier);
        Vector velocity = player.getLocation().getDirection().multiply(0.4).setY(0.2);
        mergedSheep.setVelocity(velocity);
        world.spawnParticle(Particle.VILLAGER_HAPPY, spawnLocation.add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.05);

        if (mergedTier.getLevel() > SheepMergeManager.getUnlockedTierCap(world)) {
            SheepMergeManager.announceTierUnlock(player, mergedTier);
        }
        SheepMergeManager.recordSheepMerge(player);
        SheepMergeManager.showOverlay(player, SheepMergeManager.action(
                carriedTier.getDisplayName() + " + " + carriedTier.getDisplayName() + " -> "
                        + mergedTier.getDisplayName()));
        SheepMergeManager.clearPickedUpSheep(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SheepMergeManager.saveData();
        if (SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            SheepMergeManager.restorePlayerInventory(player);
            SheepMergeManager.restorePlayerScoreboard(player);
        }
        SheepMergeManager.clearPickedUpSheep(player);
        SheepMergeManager.clearMergeReminder(player);
        SheepMergeManager.clearPrestigeReminder(player);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        SheepMergeManager.saveData();
        if (SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            SheepMergeManager.restorePlayerInventory(player);
            SheepMergeManager.restorePlayerScoreboard(player);
        }
        SheepMergeManager.clearPickedUpSheep(player);
        SheepMergeManager.clearMergeReminder(player);
        SheepMergeManager.clearPrestigeReminder(player);
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
