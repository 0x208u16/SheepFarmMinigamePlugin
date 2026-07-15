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
import org.bukkit.entity.Player;

public class SheepFarmWorldCommand implements CommandExecutor {

    public static String getWorldName(java.util.UUID playerId) {
        return "sheepfarm_" + playerId.toString().replace("-", "");
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

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            player.sendMessage("Points: " + SheepMergeManager.getPlayerPoints(player)
                    + ", Sheep limit: " + SheepMergeManager.getPlayerLimit(player)
                    + " (Lv." + SheepMergeManager.getLimitUpgradeLevel(player) + ")"
                    + ", Spawn eggs every " + SheepMergeManager.getEggIntervalSeconds(player) + " seconds"
                    + " (Lv." + SheepMergeManager.getEggSpeedLevel(player) + ")"
                    + ", Wool regen level: " + SheepMergeManager.getWoolRegenLevel(player)
                    + ", Higher-tier spawn chance: " + SheepMergeManager.getHigherTierChancePercent(player) + "%"
                    + " (Lv." + SheepMergeManager.getHigherTierChanceLevel(player) + ")");
            return true;
        }

        String worldName = getWorldName(player.getUniqueId());
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            creator.generatorSettings("{" +
                    "\"layers\": [{\"block\": \"minecraft:air\", \"height\": 1}]," +
                    "\"biome\": \"minecraft:plains\"" +
                    "}");
            world = Bukkit.createWorld(creator);

            if (world != null) {
                int radius = 5;
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
}
