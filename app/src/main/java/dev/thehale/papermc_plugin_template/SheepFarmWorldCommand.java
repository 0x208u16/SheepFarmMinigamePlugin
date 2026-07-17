package dev.thehale.papermc_plugin_template;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
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
import java.io.File;
import java.util.List;

public class SheepFarmWorldCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "help",
            "-help",
            "--help",
            "upgrade",
            "prestige",
            "shop",
            "tutorial",
            "visit",
            "kick",
            "status",
            "storm",
            "combofrenzy",
            "leaderboard",
            "topdisplay",
            "resetdata",
            "stats",
            "checkpoints",
            "checkquestpoints",
            "checkprestige",
            "givepoints",
            "setpoints",
            "givequestpoints",
            "setquestpoints",
            "setprestige",
            "layout",
            "mapsave",
            "mapload",
            "world");

    private static final List<String> WORLD_SUBCOMMANDS = List.of("help", "-help", "--help", "save", "load");
    private static final List<String> LEADERBOARD_SUBCOMMANDS = List.of(
            "help",
            "-help",
            "--help",
            "move",
            "remove",
            "clear");
    private static final List<String> LAYOUT_SUBCOMMANDS = List.of("help", "-help", "--help", "save", "load");
    private static final List<String> HELP_FLAGS = List.of("help", "-help", "--help");
    private static final List<String> ADMIN_AMOUNT_PLAYER_SUBCOMMANDS = List.of(
            "givepoints",
            "setpoints",
            "givequestpoints",
            "setquestpoints",
            "setprestige");
    private static final List<String> ADMIN_PLAYER_TARGET_SUBCOMMANDS = List.of("resetdata");
    private static final List<String> ADMIN_STAT_CHECK_SUBCOMMANDS = List.of(
            "stats",
            "checkpoints",
            "checkquestpoints",
            "checkprestige");

    public static String getWorldName(java.util.UUID playerId) {
        return "sheepfarm_" + playerId.toString().replace("-", "");
    }

    private static boolean isHelpFlag(String value) {
        return value != null && (value.equalsIgnoreCase("help")
                || value.equalsIgnoreCase("-help")
                || value.equalsIgnoreCase("--help"));
    }

    private void sendCommandHelp(Player player, String topic) {
        if (player == null) {
            return;
        }

        player.sendMessage(adminHeader("SheepMerge Help"));
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge") + ": return to your sheep farm world");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge help") + ": show this help page");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge upgrade") + ": open upgrade menu");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge prestige") + ": open prestige menu");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge shop") + ": open shop menu");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge tutorial") + ": open the tutorial world");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge status") + ": view your current stats");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge visit <player>") + ": visit another open farm");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge visit -toggle [player]") + ": toggle farm visit access");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge kick <player>") + ": remove a visitor from your farm");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard")
                + ": move the leaderboard display to you");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard <x> <y> <z> [world] [yaw] [pitch]")
                + ": place the leaderboard at coordinates");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard remove")
                + ": remove the leaderboard display");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge topdisplay")
                + ": legacy alias for leaderboard");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge storm") + ": trigger a sheep storm");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge combofrenzy") + ": trigger combo frenzy");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge layout save")
                + ": save the current farm layout");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge layout load")
                + ": load the saved farm layout");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge mapsave") + ": legacy alias for layout save");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge mapload") + ": legacy alias for layout load");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge world save|load") + ": save or load the farm layout");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge resetdata [player]") + ": admin reset a player");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge stats [player]") + ": admin stats view");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge checkpoints [player]") + ": admin points check");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge checkquestpoints [player]") + ": admin quest points check");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge checkprestige [player]") + ": admin prestige check");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge givepoints <amount> [player]") + ": admin give points");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge setpoints <amount> [player]") + ": admin set points");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge givequestpoints <amount> [player]")
                + ": admin give quest points");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge setquestpoints <amount> [player]")
                + ": admin set quest points");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge setprestige <level> [player]")
                + ": admin set prestige level");

        if (topic == null || topic.isBlank()) {
            return;
        }

        if (topic.equalsIgnoreCase("visit")) {
            player.sendMessage(ChatColor.DARK_AQUA + "Visit hints:");
            player.sendMessage(
                    ChatColor.GRAY + "- " + label("/sheepmerge visit <player>") + ": visit another open farm");
            player.sendMessage(
                    ChatColor.GRAY + "- " + label("/sheepmerge visit -toggle") + ": toggle your farm visit access");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge visit -toggle <player>")
                    + ": operator toggle for another player");
            return;
        }

        if (topic.equalsIgnoreCase("topdisplay")) {
            player.sendMessage(ChatColor.DARK_AQUA + "Leaderboard hints:");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard")
                    + ": move the leaderboard display to your position");
            player.sendMessage(
                    ChatColor.GRAY + "- " + label("/sheepmerge leaderboard <x> <y> <z> [world] [yaw] [pitch]")
                            + ": move the leaderboard to explicit coordinates");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard remove")
                    + ": remove the leaderboard display and clear the saved location");
            return;
        }

        if (topic.equalsIgnoreCase("leaderboard")) {
            player.sendMessage(ChatColor.DARK_AQUA + "Leaderboard hints:");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard")
                    + ": move the leaderboard display to your position");
            player.sendMessage(
                    ChatColor.GRAY + "- " + label("/sheepmerge leaderboard <x> <y> <z> [world] [yaw] [pitch]")
                            + ": move the leaderboard to explicit coordinates");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard remove")
                    + ": remove the leaderboard display and clear the saved location");
            return;
        }

        if (topic.equalsIgnoreCase("world")) {
            player.sendMessage(ChatColor.DARK_AQUA + "World hints:");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge world save")
                    + ": save the current farm layout and apply it to all farm worlds");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge world load")
                    + ": load the saved farm layout into all farm worlds");
            return;
        }

        if (topic.equalsIgnoreCase("resetdata")) {
            player.sendMessage(ChatColor.DARK_AQUA + "Reset hints:");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge resetdata")
                    + ": reset your own data");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge resetdata <player>")
                    + ": reset a specific online player");
            return;
        }

        if (topic.equalsIgnoreCase("stats") || topic.equalsIgnoreCase("checkpoints")
                || topic.equalsIgnoreCase("checkquestpoints") || topic.equalsIgnoreCase("checkprestige")) {
            player.sendMessage(ChatColor.DARK_AQUA + "Admin stat check hints:");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge " + topic)
                    + ": inspect your own stats if you are checking yourself");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge " + topic + " <player>")
                    + ": inspect another online player");
            return;
        }

        if (topic.equalsIgnoreCase("givepoints") || topic.equalsIgnoreCase("setpoints")
                || topic.equalsIgnoreCase("givequestpoints") || topic.equalsIgnoreCase("setquestpoints")
                || topic.equalsIgnoreCase("setprestige")) {
            player.sendMessage(ChatColor.DARK_AQUA + "Admin value hints:");
            String amountLabel = topic.equalsIgnoreCase("setprestige") ? "<level>" : "<amount>";
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge " + topic + " " + amountLabel)
                    + ": affect your own account if you are the target");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge " + topic + " " + amountLabel + " [player]")
                    + ": affect a specific online player");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        String helpTopic = null;
        for (String arg : args) {
            if (isHelpFlag(arg)) {
                continue;
            }
            if (helpTopic == null) {
                helpTopic = arg;
            }
        }
        for (String arg : args) {
            if (isHelpFlag(arg)) {
                sendCommandHelp(player, helpTopic);
                return true;
            }
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("upgrade")) {
            SheepMergeManager.openUpgradeMenu(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("shop")) {
            SheepMergeManager.openShopMenu(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("prestige")) {
            SheepMergeManager.openPrestigeMenu(player);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("tutorial")) {
            if (SheepMergeManager.hasUnlockedFarm(player)) {
                player.sendMessage("Tutorial has already been completed. Use /sheepmerge to return to your farm.");
            } else {
                SheepMergeManager.startTutorial(player, false);
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            sendDetailedStats(player, player, "Status");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("leaderboard")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            boolean createdOrMoved = SheepMergeManager.spawnOrMoveTopPointsDisplay(player);
            if (createdOrMoved) {
                player.sendMessage("Leaderboard moved to your position.");
            } else {
                player.sendMessage("Unable to move leaderboard right now.");
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("stats")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 1);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            sendDetailedStats(player, target, "Admin Stats");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("checkpoints")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 1);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            player.sendMessage(adminHeader("Stat Check")
                    + " " + label("Player") + ": " + value(target.getName())
                    + ChatColor.DARK_GRAY + " | "
                    + label("Points") + ": " + value(String.valueOf(SheepMergeManager.getPlayerPoints(target))));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("checkquestpoints")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 1);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            player.sendMessage(adminHeader("Stat Check")
                    + " " + label("Player") + ": " + value(target.getName())
                    + ChatColor.DARK_GRAY + " | "
                    + label("Quest Points") + ": " + value(String.valueOf(SheepMergeManager.getQuestPoints(target))));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("checkprestige")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 1);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            player.sendMessage(adminHeader("Stat Check")
                    + " " + label("Player") + ": " + value(target.getName())
                    + ChatColor.DARK_GRAY + " | "
                    + label("Prestige") + ": " + value(String.valueOf(SheepMergeManager.getPrestigeLevel(target)))
                    + ChatColor.DARK_GRAY + " | "
                    + label("Prestige Points") + ": "
                    + value(String.valueOf(SheepMergeManager.getPrestigePoints(target))));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("visit")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("-toggle")) {
                Player target = player;
                if (args.length >= 3) {
                    if (!player.isOp()) {
                        player.sendMessage("Only server operators can toggle another player's farm access.");
                        return true;
                    }
                    target = resolveTargetPlayer(player, args, 2);
                    if (target == null) {
                        player.sendMessage("That player is not online.");
                        return true;
                    }
                }

                boolean nowOpen = SheepMergeManager.toggleFarmVisitable(target);
                player.sendMessage(
                        target.getName() + "'s farm is now " + (nowOpen ? "open" : "closed") + " to visitors.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("Usage: /sheepmerge visit <player> or /sheepmerge visit -toggle [player]");
                return true;
            }

            Player owner = Bukkit.getPlayerExact(args[1]);
            if (owner == null) {
                player.sendMessage("That player is not online.");
                return true;
            }

            java.util.UUID ownerId = owner.getUniqueId();
            if (!player.isOp()
                    && !ownerId.equals(player.getUniqueId())
                    && !SheepMergeManager.isFarmVisitable(ownerId)) {
                player.sendMessage("That farm is closed to visitors.");
                return true;
            }

            String ownerWorldName = getWorldName(ownerId);
            World ownerWorld = ensureFarmWorld(ownerWorldName);
            if (ownerWorld == null) {
                player.sendMessage("Unable to open that farm world right now.");
                return true;
            }

            ownerWorld.setSpawnLocation(0, 101, 0);
            player.teleport(new Location(ownerWorld, 0.5, 101, 0.5));
            player.sendMessage("You were teleported to " + owner.getName() + "'s sheep farm.");
            player.sendMessage("Use /sheepmerge to return to your own farm.");
            player.sendTitle(
                    SheepMergeManager.color("&eVisiting " + owner.getName()),
                    SheepMergeManager.color("&7Use /sheepmerge to return home"),
                    10,
                    60,
                    10);
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("kick")) {
            if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())
                    || !SheepMergeManager.isFarmOwner(player, player.getWorld())) {
                player.sendMessage("You can only use this in your own sheep farm world.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("Usage: /sheepmerge kick <player>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !target.getWorld().equals(player.getWorld())) {
                player.sendMessage("That player is not in your farm right now.");
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage("You cannot kick yourself.");
                return true;
            }
            if (target.isOp()) {
                player.sendMessage("You cannot kick operators from your farm.");
                return true;
            }

            World fallbackWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (fallbackWorld == null) {
                player.sendMessage("No safe world is available to move that player.");
                return true;
            }

            Location spawn = fallbackWorld.getSpawnLocation().clone().add(0.5, 0, 0.5);
            target.teleport(spawn);
            target.sendMessage("You were removed from " + player.getName() + "'s sheep farm.");
            player.sendMessage("You removed " + target.getName() + " from your farm.");
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

        if (args.length == 3 && args[0].equalsIgnoreCase("leaderboard") && args[1].equalsIgnoreCase("remove")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            boolean removed = SheepMergeManager.removeTopPointsDisplay();
            player.sendMessage(removed ? "Leaderboard removed." : "No leaderboard display was found.");
            return true;
        }

        if (args.length >= 4 && (args[0].equalsIgnoreCase("topdisplay") || args[0].equalsIgnoreCase("leaderboard"))
                && isCoordinate(args[1]) && isCoordinate(args[2]) && isCoordinate(args[3])) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }

            World targetWorld = player.getWorld();
            int extraArgIndex = 4;
            if (args.length >= 5) {
                World byName = Bukkit.getWorld(args[4]);
                if (byName != null) {
                    targetWorld = byName;
                    extraArgIndex = 5;
                }
            }

            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                Location location = new Location(targetWorld, x, y, z);
                if (args.length > extraArgIndex) {
                    location.setYaw(Float.parseFloat(args[extraArgIndex]));
                }
                if (args.length > extraArgIndex + 1) {
                    location.setPitch(Float.parseFloat(args[extraArgIndex + 1]));
                }

                boolean createdOrMoved = SheepMergeManager.spawnOrMoveTopPointsDisplay(location);
                if (createdOrMoved) {
                    player.sendMessage("Leaderboard moved to coordinates " + x + ", " + y + ", " + z
                            + " in " + targetWorld.getName() + ".");
                } else {
                    player.sendMessage("Unable to move leaderboard right now.");
                }
                return true;
            } catch (NumberFormatException exception) {
                player.sendMessage(
                        "Invalid coordinates. Usage: /sheepmerge leaderboard <x> <y> <z> [world] [yaw] [pitch]");
                return true;
            }
        }

        if ((args.length == 2 || args.length == 3) && (args[0].equalsIgnoreCase("topdisplay")
                || args[0].equalsIgnoreCase("leaderboard")) && args[1].equalsIgnoreCase("remove")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            boolean removed = SheepMergeManager.removeTopPointsDisplay();
            if (removed) {
                player.sendMessage("Leaderboard removed.");
            } else {
                player.sendMessage("No leaderboard display was found.");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("storm")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            if (SheepMergeManager.triggerSheepStormEvent()) {
                player.sendMessage("Sheep storm triggered.");
            } else {
                player.sendMessage("A sheep storm is already active or could not be started.");
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("combofrenzy")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            if (SheepMergeManager.triggerComboFrenzyEvent()) {
                player.sendMessage("Combo frenzy triggered.");
            } else {
                player.sendMessage("A combo frenzy is already active or could not be started.");
            }
            return true;
        }

        if ((args.length == 1 && args[0].equalsIgnoreCase("mapsave"))
                || (args.length == 2 && args[0].equalsIgnoreCase("layout") && args[1].equalsIgnoreCase("save"))
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
                || (args.length == 2 && args[0].equalsIgnoreCase("layout") && args[1].equalsIgnoreCase("load"))
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
            SheepMergeManager.startTutorial(target, false);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("givepoints")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(error("Invalid amount. Usage: /sheepmerge givepoints <amount> [player]"));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 2);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            int previous = SheepMergeManager.getPlayerPoints(target);
            SheepMergeManager.adminGivePoints(target, amount);
            int updated = SheepMergeManager.getPlayerPoints(target);
            SheepMergeManager.updatePointsScoreboard(target);
            player.sendMessage(statUpdateMessage(
                    "Points Updated",
                    target,
                    "Points",
                    previous,
                    updated));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("setpoints")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(error("Invalid amount. Usage: /sheepmerge setpoints <amount> [player]"));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 2);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            int previous = SheepMergeManager.getPlayerPoints(target);
            SheepMergeManager.adminSetPoints(target, amount);
            int updated = SheepMergeManager.getPlayerPoints(target);
            SheepMergeManager.updatePointsScoreboard(target);
            player.sendMessage(statUpdateMessage(
                    "Points Updated",
                    target,
                    "Points",
                    previous,
                    updated));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("givequestpoints")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(error("Invalid amount. Usage: /sheepmerge givequestpoints <amount> [player]"));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 2);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            int previous = SheepMergeManager.getQuestPoints(target);
            SheepMergeManager.adminGiveQuestPoints(target, amount);
            int updated = SheepMergeManager.getQuestPoints(target);
            player.sendMessage(statUpdateMessage(
                    "Quest Points Updated",
                    target,
                    "Quest Points",
                    previous,
                    updated));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("setquestpoints")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(error("Invalid amount. Usage: /sheepmerge setquestpoints <amount> [player]"));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 2);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            int previous = SheepMergeManager.getQuestPoints(target);
            SheepMergeManager.adminSetQuestPoints(target, amount);
            int updated = SheepMergeManager.getQuestPoints(target);
            player.sendMessage(statUpdateMessage(
                    "Quest Points Updated",
                    target,
                    "Quest Points",
                    previous,
                    updated));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("setprestige")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            int level;
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(error("Invalid level. Usage: /sheepmerge setprestige <level> [player]"));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 2);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            int previous = SheepMergeManager.getPrestigeLevel(target);
            if (!SheepMergeManager.adminSetPrestigeLevel(target, level)) {
                player.sendMessage(error("Invalid prestige level. Use a value between 0 and "
                        + SheepMergeManager.getPrestigeMaxLevel() + "."));
                return true;
            }
            int updated = SheepMergeManager.getPrestigeLevel(target);
            SheepMergeManager.updatePointsScoreboard(target);
            player.sendMessage(statUpdateMessage(
                    "Prestige Updated",
                    target,
                    "Prestige",
                    previous,
                    updated));
            return true;
        }

        if (!SheepMergeManager.hasUnlockedFarm(player)) {
            SheepMergeManager.startTutorial(player, false);
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

        if (args.length == 2 && args[0].equalsIgnoreCase("layout")) {
            return filterSuggestions(LAYOUT_SUBCOMMANDS, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("visit")) {
            List<String> visitOptions = new ArrayList<>(HELP_FLAGS);
            visitOptions.add("-toggle");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName() != null) {
                    visitOptions.add(online.getName());
                }
            }
            return filterSuggestions(visitOptions, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("topdisplay")) {
            List<String> suggestions = new ArrayList<>(LEADERBOARD_SUBCOMMANDS);
            suggestions.addAll(HELP_FLAGS);
            return filterSuggestions(suggestions, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
            List<String> suggestions = new ArrayList<>(LEADERBOARD_SUBCOMMANDS);
            suggestions.addAll(HELP_FLAGS);
            return filterSuggestions(suggestions, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("kick") && sender instanceof Player player) {
            List<String> kickTargets = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player) || online.isOp()) {
                    continue;
                }
                if (online.getWorld().equals(player.getWorld()) && online.getName() != null) {
                    kickTargets.add(online.getName());
                }
            }
            return filterSuggestions(kickTargets, args[1]);
        }

        if (!sender.isOp()) {
            return List.of();
        }

        if (args.length == 2 && matchesSubcommand(ADMIN_PLAYER_TARGET_SUBCOMMANDS, args[0])) {
            return appendHelpFlags(onlinePlayerNameSuggestions(args[1]), args[1]);
        }

        if (args.length == 2 && matchesSubcommand(ADMIN_STAT_CHECK_SUBCOMMANDS, args[0])) {
            return appendHelpFlags(onlinePlayerNameSuggestions(args[1]), args[1]);
        }

        if (args.length == 2 && matchesSubcommand(ADMIN_AMOUNT_PLAYER_SUBCOMMANDS, args[0])) {
            return filterSuggestions(HELP_FLAGS, args[1]);
        }

        if (args.length == 3 && matchesSubcommand(ADMIN_AMOUNT_PLAYER_SUBCOMMANDS, args[0])) {
            return appendHelpFlags(onlinePlayerNameSuggestions(args[2]), args[2]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("leaderboard")) {
            return filterSuggestions(appendHelpFlags(List.of("remove"), args[2]), args[2]);
        }

        return List.of();
    }

    private boolean matchesSubcommand(List<String> subcommands, String candidate) {
        if (candidate == null) {
            return false;
        }
        for (String subcommand : subcommands) {
            if (subcommand.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private List<String> onlinePlayerNameSuggestions(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(player -> player == null ? null : player.getName())
                .filter(name -> name != null && name.toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();
    }

    private List<String> appendHelpFlags(List<String> suggestions, String prefix) {
        List<String> combined = new ArrayList<>(suggestions);
        combined.addAll(filterSuggestions(HELP_FLAGS, prefix));
        return combined;
    }

    private boolean isCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
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

    private Player resolveTargetPlayer(Player sender, String[] args, int playerArgIndex) {
        if (args.length <= playerArgIndex) {
            return sender;
        }
        return Bukkit.getPlayerExact(args[playerArgIndex]);
    }

    private void sendDetailedStats(Player sender, Player target, String title) {
        sender.sendMessage(adminHeader(title)
                + " " + label("Player") + ": " + value(target.getName()));
        sender.sendMessage(ChatColor.GRAY + "- " + label("Points") + ": "
                + value(String.valueOf(SheepMergeManager.getPlayerPoints(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Quest Points") + ": " + value(String.valueOf(SheepMergeManager.getQuestPoints(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Prestige") + ": " + value(String.valueOf(SheepMergeManager.getPrestigeLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Prestige Points") + ": " + value(String.valueOf(SheepMergeManager.getPrestigePoints(target))));
        sender.sendMessage(ChatColor.GRAY + "- " + label("Sheep Limit") + ": "
                + value(String.valueOf(SheepMergeManager.getPlayerLimit(target)))
                + ChatColor.GRAY + " (Lv." + value(String.valueOf(SheepMergeManager.getLimitUpgradeLevel(target)))
                + ChatColor.GRAY + ")"
                + ChatColor.DARK_GRAY + " | "
                + label("Egg Interval") + ": " + value(String.valueOf(SheepMergeManager.getEggIntervalSeconds(target)))
                + ChatColor.GRAY + "s"
                + ChatColor.GRAY + " (Lv." + value(String.valueOf(SheepMergeManager.getEggSpeedLevel(target)))
                + ChatColor.GRAY + ")");
        sender.sendMessage(ChatColor.GRAY + "- " + label("Wool Regen Lv") + ": "
                + value(String.valueOf(SheepMergeManager.getWoolRegenLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Higher-Tier Chance") + ": "
                + value(String.valueOf(SheepMergeManager.getHigherTierChancePercent(target))) + ChatColor.GRAY + "%"
                + ChatColor.GRAY + " (Lv." + value(String.valueOf(SheepMergeManager.getHigherTierChanceLevel(target)))
                + ChatColor.GRAY + ")"
                + ChatColor.DARK_GRAY + " | "
                + label("Egg Cap") + ": " + value(String.valueOf(SheepMergeManager.getEggCap(target)))
                + ChatColor.GRAY + " (Lv." + value(String.valueOf(SheepMergeManager.getPrestigeEggCapLevel(target)))
                + ChatColor.GRAY + ")");
        sender.sendMessage(ChatColor.GRAY + "- " + label("Farm Visit Access") + ": "
                + (SheepMergeManager.isFarmVisitable(target.getUniqueId())
                        ? ChatColor.GREEN + "open"
                        : ChatColor.RED + "closed"));
    }

    private String statUpdateMessage(String importance, Player target, String statLabel, int fromValue, int toValue) {
        int delta = toValue - fromValue;
        ChatColor deltaColor = delta >= 0 ? ChatColor.GREEN : ChatColor.RED;
        String signedDelta = (delta >= 0 ? "+" : "") + delta;
        return adminHeader(importance)
                + " " + label("Player") + ": " + value(target.getName())
                + ChatColor.DARK_GRAY + " | "
                + label("Stat") + ": " + value(statLabel)
                + ChatColor.DARK_GRAY + " | "
                + label("From") + ": " + value(String.valueOf(fromValue))
                + ChatColor.DARK_GRAY + " -> "
                + label("To") + ": " + value(String.valueOf(toValue))
                + ChatColor.DARK_GRAY + " | "
                + label("Change") + ": " + deltaColor + signedDelta;
    }

    private String adminHeader(String text) {
        return ChatColor.DARK_AQUA + "[SheepMerge] " + ChatColor.GOLD + text;
    }

    private String label(String text) {
        return ChatColor.YELLOW + text;
    }

    private String value(String text) {
        return ChatColor.AQUA + text;
    }

    private String error(String text) {
        return ChatColor.RED + text;
    }

    public static World ensureFarmWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            applyFarmWorldRules(world);
            ensureWorldStorageFolders(world);
            return world;
        }
        return createFlatWorld(worldName);
    }

    public static void applyFarmRulesToLoadedWorlds() {
        for (World world : Bukkit.getWorlds()) {
            applyFarmWorldRules(world);
        }
    }

    public static World ensureTutorialWorld(java.util.UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return ensureFarmWorld(SheepMergeManager.getTutorialWorldName(playerId));
    }

    public static boolean teleportToFarmWorld(Player player) {
        if (player == null) {
            return false;
        }
        World world = ensureFarmWorld(getWorldName(player.getUniqueId()));
        if (world == null) {
            return false;
        }
        world.setSpawnLocation(0, 101, 0);
        player.teleport(new Location(world, 0.5, 101, 0.5));
        return true;
    }

    public static boolean teleportToTutorialWorld(Player player) {
        if (player == null) {
            return false;
        }
        World world = ensureTutorialWorld(player.getUniqueId());
        if (world == null) {
            return false;
        }
        world.setSpawnLocation(0, 101, 0);
        player.teleport(new Location(world, 0.5, 101, 0.5));
        return true;
    }

    private static World createFlatWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            applyFarmWorldRules(world);
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

        applyFarmWorldRules(world);
        ensureWorldStorageFolders(world);
        SheepMergeManager.applyFarmLayout(world);
        return world;
    }

    private static void ensureWorldStorageFolders(World world) {
        if (world == null || world.getWorldFolder() == null) {
            return;
        }
        File worldFolder = world.getWorldFolder();
        File dataFolder = new File(worldFolder, "data");
        File regionFolder = new File(worldFolder, "region");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!regionFolder.exists()) {
            regionFolder.mkdirs();
        }
    }

    private static void applyFarmWorldRules(World world) {
        if (world == null || !SheepMergeManager.isSheepFarmWorld(world)) {
            return;
        }
        world.setPVP(false);
        world.setDifficulty(Difficulty.PEACEFUL);
    }
}
