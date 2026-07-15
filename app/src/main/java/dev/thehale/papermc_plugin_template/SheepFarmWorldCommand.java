package dev.thehale.papermc_plugin_template;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SheepFarmWorldCommand implements CommandExecutor {

    public static String getWorldName(java.util.UUID playerId) {
        return "sheepfarm_" + playerId.toString().replace("-", "");
    }

    public static String getTutorialWorldName(java.util.UUID playerId) {
        return SheepMergeManager.getTutorialWorldName(playerId);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("upgrade")) {
            SheepMergeManager.openUpgradeMenu(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("shop")) {
            SheepMergeManager.openShopMenu(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            player.sendMessage("Points: " + SheepMergeManager.getPlayerPoints(player)
                    + ", Sheep limit: " + SheepMergeManager.getPlayerLimit(player)
                    + " (Lv." + SheepMergeManager.getLimitUpgradeLevel(player) + ")"
                    + ", Spawn eggs every " + SheepMergeManager.getEggIntervalSeconds(player) + " seconds"
                    + " (Lv." + SheepMergeManager.getEggSpeedLevel(player) + ")"
                    + ", Wool regen level: " + SheepMergeManager.getWoolRegenLevel(player)
                    + ", Higher-tier spawn chance: " + SheepMergeManager.getHigherTierChancePercent(player) + "%"
                    + " (Lv." + SheepMergeManager.getHigherTierChanceLevel(player) + ")"
                    + ", Prestige: " + SheepMergeManager.getPrestigeLevel(player)
                    + ", Prestige points: " + SheepMergeManager.getPrestigePoints(player));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("topdisplay")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            boolean createdOrMoved = SheepMergeManager.spawnOrMoveTopPointsDisplay(player);
            if (createdOrMoved) {
                player.sendMessage("Top points display created or moved to your position.");
            } else {
                player.sendMessage("Unable to create top points display right now.");
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("resetdata")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            Player target = player;
            if (args.length >= 2) {
                Player byName = Bukkit.getPlayerExact(args[1]);
                if (byName != null) {
                    target = byName;
                }
            }
            SheepMergeManager.adminResetPlayer(target);
            player.sendMessage("Reset data for " + target.getName() + ".");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("givepoints")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage("Invalid amount. Usage: /sheepmerge givepoints <amount> [player]");
                return true;
            }
            Player target = player;
            if (args.length >= 3) {
                Player byName = Bukkit.getPlayerExact(args[2]);
                if (byName != null) {
                    target = byName;
                }
            }
            SheepMergeManager.adminGivePoints(target, amount);
            SheepMergeManager.updatePointsScoreboard(target);
            player.sendMessage("Updated points for " + target.getName() + ".");
            return true;
        }

        if (!SheepMergeManager.isTutorialCompleted(player)) {
            World tutorial = ensureTutorialWorld(player);
            if (tutorial == null) {
                player.sendMessage("Unable to create your tutorial world right now.");
                return true;
            }
            tutorial.setSpawnLocation(0, 101, 0);
            player.teleport(new Location(tutorial, 0.5, 101, 0.5));
            player.sendMessage("Complete the tutorial before entering your personal farm world.");
            player.sendMessage(SheepMergeManager.getTutorialProgressLine(player));
            return true;
        }

        String worldName = getWorldName(player.getUniqueId());
        World world = ensureFarmWorld(player, worldName);

        if (world == null) {
            player.sendMessage("Unable to create your sheep farm world right now.");
            return true;
        }

        world.setSpawnLocation(0, 101, 0);
        Location teleportLocation = new Location(world, 0.5, 101, 0.5);
        player.teleport(teleportLocation);
        player.sendMessage("You were teleported to your sheep farm world.");
        return true;
    }

    private World ensureTutorialWorld(Player player) {
        String worldName = getTutorialWorldName(player.getUniqueId());
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }
        world = createFlatWorld(worldName, 4);
        if (world != null && world.getEntitiesByClass(org.bukkit.entity.Sheep.class).isEmpty()) {
            world.spawnEntity(new Location(world, 0.5, 101, 0.5), EntityType.SHEEP);
            world.spawnEntity(new Location(world, 1.5, 101, 0.5), EntityType.SHEEP);
        }
        return world;
    }

    private World ensureFarmWorld(Player player, String worldName) {
        int radius = 5 + SheepMergeManager.getPrestigeExpandFarmLevel(player) * 2;
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            applyFarmLayout(world, radius);
            return world;
        }
        return createFlatWorld(worldName, radius);
    }

    private World createFlatWorld(String worldName, int radius) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }
        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        creator.generatorSettings("{" +
                "\"layers\": [{\"block\": \"minecraft:air\", \"height\": 1}]," +
                "\"biome\": \"minecraft:plains\"" +
                "}");
        world = Bukkit.createWorld(creator);
        if (world == null) {
            return null;
        }

        applyFarmLayout(world, radius);
        return world;
    }

    private void applyFarmLayout(World world, int radius) {
        int platformY = 100;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                world.getBlockAt(x, platformY, z).setType(Material.GRASS_BLOCK);
                world.getBlockAt(x, platformY - 1, z).setType(Material.DIRT);
            }
        }

        Material fence = Material.OAK_FENCE;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (Math.abs(x) == radius || Math.abs(z) == radius) {
                    world.getBlockAt(x, platformY + 1, z).setType(fence);
                }
            }
        }
    }
}
