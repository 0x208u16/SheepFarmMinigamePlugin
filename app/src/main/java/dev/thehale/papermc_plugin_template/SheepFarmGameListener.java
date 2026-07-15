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
        SheepMergeManager.setSheepTier(sheep, SheepTier.WHITE);
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

        SheepTier tier = SheepMergeManager.getSheepTier(sheep);
        int points = tier.getPointsOnShear();
        SheepMergeManager.addPoints(event.getPlayer(), points);
        SheepMergeManager.updatePointsScoreboard(event.getPlayer());
        event.getPlayer()
                .sendMessage("You sheared a " + tier.getDisplayName() + " and earned " + points + " points. Total: "
                        + SheepMergeManager.getPlayerPoints(event.getPlayer()));
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Sheep sheep)) {
            return;
        }

        Block block = event.getBlock();
        if (block.getType() != Material.GRASS_BLOCK) {
            return;
        }

        if (!SheepMergeManager.isSheepFarmWorld(block.getWorld())) {
            return;
        }

        SheepTier tier = SheepMergeManager.getSheepTier(sheep);
        long now = System.currentTimeMillis();
        if (now < SheepMergeManager.getNextEatTimestamp(sheep)) {
            event.setCancelled(true);
            return;
        }

        if (SheepMergeManager.shouldDelayGrassEat(tier)) {
            SheepMergeManager.setNextEatTimestamp(sheep, now + SheepMergeManager.getEatCooldownSeconds(tier) * 1000L);
            SheepMergeManager.updateSheepName(sheep);
            event.setCancelled(true);
            return;
        }

        SheepMergeManager.setNextEatTimestamp(sheep, now + SheepMergeManager.getEatCooldownSeconds(tier) * 1000L);
        SheepMergeManager.updateSheepName(sheep);
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
            player.sendMessage(
                    "You have reached the sheep limit for this world. Spend points with /sheepmerge upgrade to raise it.");
            event.setCancelled(true);
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
            player.sendMessage("You picked up a " + tier.getDisplayName()
                    + " above your head. Sneak-right-click another sheep of the same tier to merge, or right-click empty air to drop it.");
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
            player.sendMessage("You can only throw a sheep at another sheep of the same tier.");
            return;
        }

        if (!carriedTier.hasNext()) {
            player.sendMessage("That sheep is already the highest tier and cannot be merged further.");
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

        player.sendMessage("You merged two " + carriedTier.getDisplayName() + " sheep into a "
                + mergedTier.getDisplayName() + "!");
        SheepMergeManager.clearPickedUpSheep(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SheepMergeManager.saveData();
        SheepMergeManager.clearPickedUpSheep(event.getPlayer());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        SheepMergeManager.saveData();
        SheepMergeManager.clearPickedUpSheep(event.getPlayer());
    }
}
