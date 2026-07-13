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
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        world.getBlockAt(x, 100, z).setType(Material.STONE);
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
