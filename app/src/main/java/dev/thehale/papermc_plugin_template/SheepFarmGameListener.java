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
import org.bukkit.event.entity.SheepRegrowWoolEvent;
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

        if (SheepMergeManager.blockTutorialAction(
                event.getPlayer(),
                SheepMergeManager.TutorialAction.SHEAR_SHEEP,
                "shear sheep")) {
            event.setCancelled(true);
            return;
        }

        if (event.getPlayer().isSneaking()) {
            // Sneak-right-click is used for pickup, so do not shear in that case.
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        SheepMergeManager.shearSheepForPlayer(event.getPlayer(), sheep);
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
            sheep.setSheared(true);
            SheepMergeManager.updateSheepName(sheep);
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSheepRegrowWool(SheepRegrowWoolEvent event) {
        Sheep sheep = event.getEntity();
        if (!SheepMergeManager.isSheepFarmWorld(sheep.getWorld())) {
            return;
        }

        long nextEatAt = SheepMergeManager.getNextEatTimestamp(sheep);
        if (nextEatAt <= 0L) {
            return;
        }

        if (System.currentTimeMillis() < nextEatAt) {
            event.setCancelled(true);
            sheep.setSheared(true);
            SheepMergeManager.updateSheepName(sheep);
        }
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

        if (SheepMergeManager.blockTutorialAction(
                player,
                SheepMergeManager.TutorialAction.SPAWN_SHEEP,
                "spawn sheep")) {
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

        if (!(event.getRightClicked() instanceof Sheep)) {
            return;
        }
        Sheep targetSheep = (Sheep) event.getRightClicked();

        Player player = event.getPlayer();
        if (!SheepMergeManager.isSheepFarmWorld(targetSheep.getWorld())) {
            return;
        }

        if (!SheepMergeManager.isFarmOwner(player, targetSheep.getWorld())) {
            event.setCancelled(true);
            player.sendMessage(SheepMergeManager.warning("Visitors cannot merge sheep here."));
            return;
        }

        if (SheepMergeManager.blockTutorialAction(
                player,
                SheepMergeManager.TutorialAction.MERGE_SHEEP,
                "merge sheep")) {
            event.setCancelled(true);
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
            Sheep retargetedSheep = findMergeTargetInSight(player, pickedSheep);
            if (retargetedSheep == null) {
                event.setCancelled(true);
                player.sendMessage(SheepMergeManager.hint("Aim at another sheep to merge."));
                return;
            }
            targetSheep = retargetedSheep;
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
            if (carriedTier != SheepTier.RAINBOW) {
                player.sendMessage(SheepMergeManager.warning("Top tier. No merge."));
                return;
            }

            int pickedCount = SheepMergeManager.getRainbowMergedCount(pickedSheep);
            int targetCount = SheepMergeManager.getRainbowMergedCount(targetSheep);
            long totalCount = (long) pickedCount + targetCount;
            int mergedCount = totalCount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalCount;
            long mergedWoolRegenMs = Math.max(
                    SheepMergeManager.getRemainingWoolRegenMs(pickedSheep),
                    SheepMergeManager.getRemainingWoolRegenMs(targetSheep));

            World world = targetSheep.getWorld();
            org.bukkit.Location spawnLocation = targetSheep.getLocation();
            targetSheep.remove();
            pickedSheep.remove();

            Sheep mergedSheep = world.spawn(spawnLocation, Sheep.class);
            SheepMergeManager.setSheepTier(mergedSheep, SheepTier.RAINBOW);
            SheepMergeManager.setRainbowMergedCount(mergedSheep, mergedCount);
            SheepMergeManager.initializeMergedSheepAfterMerge(mergedSheep, SheepTier.RAINBOW, mergedWoolRegenMs);

            Vector velocity = player.getLocation().getDirection().multiply(0.4).setY(0.2);
            mergedSheep.setVelocity(velocity);
            world.spawnParticle(Particle.VILLAGER_HAPPY, spawnLocation.add(0, 0.5, 0), 15, 0.3, 0.3, 0.3, 0.05);

            SheepMergeManager.recordSheepMerge(player, carriedTier, 0);
            SheepMergeManager.recordQuestMerge(player);
            SheepMergeManager.recordTutorialMerge(player);
            SheepMergeManager.showOverlay(player, SheepMergeManager.action(
                    carriedTier.getDisplayName() + " " + SheepMergeManager.formatSheepMergedCount(pickedCount)
                            + " + " + carriedTier.getDisplayName() + " "
                            + SheepMergeManager.formatSheepMergedCount(targetCount)
                            + " -> " + carriedTier.getDisplayName() + " "
                            + SheepMergeManager.formatSheepMergedCount(mergedCount)));
            SheepMergeManager.clearPickedUpSheep(player);
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
        long combinedWoolRegenMs = SheepMergeManager.getCombinedRemainingWoolRegenMs(pickedSheep, targetSheep);
        World world = targetSheep.getWorld();
        org.bukkit.Location spawnLocation = targetSheep.getLocation();

        // Remove both source sheep and spawn the merged result.
        targetSheep.remove();
        pickedSheep.remove();

        Sheep mergedSheep = world.spawn(spawnLocation, Sheep.class);
        SheepMergeManager.setSheepTier(mergedSheep, mergedTier);
        SheepMergeManager.initializeMergedSheepAfterMerge(mergedSheep, mergedTier, combinedWoolRegenMs);
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

    private Sheep findMergeTargetInSight(Player player, Sheep carriedSheep) {
        if (player == null || carriedSheep == null || player.getWorld() == null) {
            return null;
        }
        org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                5.5,
                0.2,
                entity -> entity instanceof Sheep && !entity.getUniqueId().equals(carriedSheep.getUniqueId()));
        if (result == null || !(result.getHitEntity() instanceof Sheep sheep)) {
            return null;
        }
        return sheep;
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
