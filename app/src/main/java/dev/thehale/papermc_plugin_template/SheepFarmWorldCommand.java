package dev.thehale.papermc_plugin_template;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SheepFarmWorldCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "upgrade",
            "shop",
            "status",
            "topdisplay",
            "resetdata",
            "givepoints",
            "mapsave",
            "mapload",
            "world");

    private static final List<String> WORLD_SUBCOMMANDS = List.of("save", "load");

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
                    + ", Egg cap: " + SheepMergeManager.getEggCap(player)
                    + " (Lv." + SheepMergeManager.getPrestigeEggCapLevel(player) + ")"
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

        if ((args.length == 1 && args[0].equalsIgnoreCase("mapsave"))
                || (args.length == 2 && args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("save"))) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
                player.sendMessage("Use this command while standing in a sheep farm world.");
                return true;
            }
            if (!SheepMergeManager.saveSharedFarmLayoutFromWorld(player.getWorld())) {
                player.sendMessage("Unable to save farm layout right now.");
                return true;
            }
            int updated = SheepMergeManager.applySharedFarmLayoutToAllFarmWorlds();
            player.sendMessage("Saved farm layout and applied it to " + updated + " farm world(s).");
            return true;
        }

        if ((args.length == 1 && args[0].equalsIgnoreCase("mapload"))
                || (args.length == 2 && args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("load"))) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            if (!SheepMergeManager.hasSavedFarmLayout()) {
                player.sendMessage("No saved farm layout found yet. Use /sheepmerge mapsave first.");
                return true;
            }
            int updated = SheepMergeManager.applySharedFarmLayoutToAllFarmWorlds();
            player.sendMessage("Loaded saved farm layout into " + updated + " farm world(s).");
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

        String worldName = getWorldName(player.getUniqueId());
        World world = ensureFarmWorld(worldName);

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterSuggestions(ROOT_SUBCOMMANDS, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("world")) {
            return filterSuggestions(WORLD_SUBCOMMANDS, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("givepoints") && sender.isOp()) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .filter(name -> name != null && name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("resetdata")
                || args[0].equalsIgnoreCase("givepoints"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(player -> player.getName())
                    .filter(name -> name != null && name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return List.of();
    }

    private List<String> filterSuggestions(List<String> suggestions, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return suggestions;
        }
        String lowerPrefix = prefix.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(lowerPrefix)) {
                matches.add(suggestion);
            }
        }
        return matches;
    }

    private World ensureFarmWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }
        return createFlatWorld(worldName);
    }

    private World createFlatWorld(String worldName) {
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

        SheepMergeManager.applyFarmLayout(world);
        return world;
    }
}
