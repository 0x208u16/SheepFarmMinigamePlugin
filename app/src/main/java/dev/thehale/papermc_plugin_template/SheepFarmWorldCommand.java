package dev.thehale.papermc_plugin_template;

import dev.thehale.papermc_plugin_template.commands.CheckPrestigeCommandModule;
import dev.thehale.papermc_plugin_template.commands.CheckQuestPointsCommandModule;
import dev.thehale.papermc_plugin_template.commands.CheckpointsCommandModule;
import dev.thehale.papermc_plugin_template.commands.ComboFrenzyCommandModule;
import dev.thehale.papermc_plugin_template.commands.DashHelpCommandModule;
import dev.thehale.papermc_plugin_template.commands.BackupCommandModule;
import dev.thehale.papermc_plugin_template.commands.GiveAutomationPointsCommandModule;
import dev.thehale.papermc_plugin_template.commands.GivePointsCommandModule;
import dev.thehale.papermc_plugin_template.commands.GiveQuestPointsCommandModule;
import dev.thehale.papermc_plugin_template.commands.HelpCommandModule;
import dev.thehale.papermc_plugin_template.commands.KickCommandModule;
import dev.thehale.papermc_plugin_template.commands.LeaderboardCommandModule;
import dev.thehale.papermc_plugin_template.commands.PrestigeCommandModule;
import dev.thehale.papermc_plugin_template.commands.ReloadCommandModule;
import dev.thehale.papermc_plugin_template.commands.ResetDataCommandModule;
import dev.thehale.papermc_plugin_template.commands.SetPointsCommandModule;
import dev.thehale.papermc_plugin_template.commands.SetPrestigeCommandModule;
import dev.thehale.papermc_plugin_template.commands.SetQuestPointsCommandModule;
import dev.thehale.papermc_plugin_template.commands.SheepMergeCommandModule;
import dev.thehale.papermc_plugin_template.commands.ShopCommandModule;
import dev.thehale.papermc_plugin_template.commands.StatsCommandModule;
import dev.thehale.papermc_plugin_template.commands.StatusCommandModule;
import dev.thehale.papermc_plugin_template.commands.StormCommandModule;
import dev.thehale.papermc_plugin_template.commands.SummonCommandModule;
import dev.thehale.papermc_plugin_template.commands.UpgradeCommandModule;
import dev.thehale.papermc_plugin_template.commands.VisitCommandModule;
import dev.thehale.papermc_plugin_template.commands.WorldCommandModule;

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
import org.bukkit.entity.Sheep;

import java.util.ArrayList;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SheepFarmWorldCommand implements CommandExecutor, TabCompleter {

    private static final Set<String> initializedManagedWorldNames = new HashSet<>();

    private static final List<String> ROOT_SUBCOMMANDS = List.of(
            "help",
            "-help",
            "upgrade",
            "prestige",
            "shop",
            "visit",
            "kick",
            "status",
            "storm",
            "summon",
            "combofrenzy",
            "reload",
            "leaderboard",
            "resetdata",
            "stats",
            "checkpoints",
            "checkquestpoints",
            "checkprestige",
            "givepoints",
            "giveautomationpoints",
            "setpoints",
            "givequestpoints",
            "setquestpoints",
            "setprestige",
            "backup",
            "world");

    private static final List<String> WORLD_SUBCOMMANDS = List.of("help", "-help", "save", "load");
    private static final List<String> BACKUP_SUBCOMMANDS = List.of(
            "help",
            "-help",
            "create",
            "list",
            "load",
            "delete",
            "recover");
    private static final List<String> LEADERBOARD_SUBCOMMANDS = List.of(
            "help",
            "-help",
            "move",
            "remove",
            "clear");
    private static final List<String> HELP_FLAGS = List.of("help", "-help");
    private static final List<String> LEADERBOARD_COORDINATE_HINTS = List.of("<x>", "<y>", "<z>", "[world]");
    private static final List<String> AMOUNT_HINTS = List.of("<amount>", "0", "100");
    private static final List<String> LEVEL_HINTS = List.of("<level>", "0", "1");
    private static final List<String> ADMIN_AMOUNT_PLAYER_SUBCOMMANDS = List.of(
            "givepoints",
            "giveautomationpoints",
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

    private final List<SheepMergeCommandModule> rootModules = List.of(
            new HelpCommandModule(this::handleHelpRootCommand, this::tabCompleteNone),
            new DashHelpCommandModule(this::handleDashHelpRootCommand, this::tabCompleteNone),
            new UpgradeCommandModule(this::handleUpgradeCommand, this::tabCompleteNone),
            new PrestigeCommandModule(this::handlePrestigeCommand, this::tabCompleteNone),
            new ShopCommandModule(this::handleShopCommand, this::tabCompleteNone),
            new VisitCommandModule(this::handleVisitCommand, this::tabCompleteVisit),
            new KickCommandModule(this::handleKickCommand, this::tabCompleteKick),
            new StatusCommandModule(this::handleStatusCommand, this::tabCompleteNone),
            new StormCommandModule(this::handleStormCommand, this::tabCompleteNone),
            new SummonCommandModule(this::handleSummonCommand, this::tabCompleteSummon),
            new ComboFrenzyCommandModule(this::handleComboFrenzyCommand, this::tabCompleteNone),
            new ReloadCommandModule(this::handleReloadCommand, this::tabCompleteNone),
            new LeaderboardCommandModule(this::handleLeaderboardCommand, this::tabCompleteLeaderboard),
            new ResetDataCommandModule(this::handleResetDataCommand, this::tabCompleteAdminPlayerTarget),
            new StatsCommandModule(this::handleStatsCommand, this::tabCompleteAdminStatCheck),
            new CheckpointsCommandModule(this::handleCheckpointsCommand, this::tabCompleteAdminStatCheck),
            new CheckQuestPointsCommandModule(this::handleCheckQuestPointsCommand, this::tabCompleteAdminStatCheck),
            new CheckPrestigeCommandModule(this::handleCheckPrestigeCommand, this::tabCompleteAdminStatCheck),
            new GivePointsCommandModule(this::handleGivePointsCommand, this::tabCompleteAdminAmountPlayer),
            new GiveAutomationPointsCommandModule(this::handleGiveAutomationPointsCommand,
                    this::tabCompleteAdminAmountPlayer),
            new SetPointsCommandModule(this::handleSetPointsCommand, this::tabCompleteAdminAmountPlayer),
            new GiveQuestPointsCommandModule(this::handleGiveQuestPointsCommand, this::tabCompleteAdminAmountPlayer),
            new SetQuestPointsCommandModule(this::handleSetQuestPointsCommand, this::tabCompleteAdminAmountPlayer),
            new SetPrestigeCommandModule(this::handleSetPrestigeCommand, this::tabCompleteAdminAmountPlayer),
            new BackupCommandModule(this::handleBackupCommand, this::tabCompleteBackup),
            new WorldCommandModule(this::handleWorldCommand, this::tabCompleteWorld));

    public static String getWorldName(java.util.UUID playerId) {
        return "sheepfarm_" + playerId.toString().replace("-", "");
    }

    private static boolean isHelpFlag(String value) {
        return value != null && (value.equalsIgnoreCase("help")
                || value.equalsIgnoreCase("-help"));
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
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge status") + ": view your current stats");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge visit <player>") + ": visit another open farm");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge visit -toggle [player]") + ": toggle farm visit access");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge kick <player>") + ": remove a visitor from your farm");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard")
                + ": move the leaderboard display to you");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard <x> <y> <z> [world]")
                + ": place the leaderboard at coordinates");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard remove")
                + ": remove the leaderboard display");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge storm") + ": trigger a sheep storm");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge summon [tier]")
                + ": operator summon a sheep (optional tier level)");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge combofrenzy") + ": trigger combo frenzy");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge reload")
                + ": reload plugin configuration values live");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge world") + ": travel to the shared farm build world");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge world save|load")
                        + ": save/load shared farm chunks for all farms");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge resetdata [player]") + ": admin reset a player");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge stats [player]") + ": admin stats view");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge checkpoints [player]") + ": admin points check");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge checkquestpoints [player]") + ": admin quest points check");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge checkprestige [player]") + ": admin prestige check");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge givepoints <amount> [player]") + ": admin give points");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge giveautomationpoints <amount> [player]")
                + ": admin give automation points");
        player.sendMessage(
                ChatColor.GRAY + "- " + label("/sheepmerge setpoints <amount> [player]") + ": admin set points");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge givequestpoints <amount> [player]")
                + ": admin give quest points");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge setquestpoints <amount> [player]")
                + ": admin set quest points");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge setprestige <level> [player]")
                + ": admin set prestige level");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge backup")
                + ": create a permanent compressed backup");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge backup delete <file>")
                + ": mark a backup for deletion (removed on a future restart after 24h)");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge backup recover <file>")
                + ": unmark a backup that was marked for deletion");
        player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge backup load <file>")
                + ": restore backup data and create a post-load permanent backup");

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

        if (topic.equalsIgnoreCase("leaderboard")) {
            player.sendMessage(ChatColor.DARK_AQUA + "Leaderboard hints:");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard")
                    + ": move the leaderboard display to your position");
            player.sendMessage(
                    ChatColor.GRAY + "- " + label("/sheepmerge leaderboard <x> <y> <z> [world]")
                            + ": move the leaderboard to explicit coordinates");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge leaderboard remove")
                    + ": remove the leaderboard display and clear the saved location");
            return;
        }

        if (topic.equalsIgnoreCase("world")) {
            player.sendMessage(ChatColor.DARK_AQUA + "World hints:");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge world")
                    + ": travel to the shared farm build world");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge world save")
                    + ": save the shared farm build world");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge world load")
                    + ": commit the shared build world into all loaded farm worlds");
            return;
        }

        if (topic.equalsIgnoreCase("backup")) {
            player.sendMessage(ChatColor.DARK_AQUA + "Backup hints:");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge backup")
                    + ": create a permanent compressed backup now");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge backup list")
                    + ": show recent backup archive names");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge backup delete <file>")
                    + ": mark one backup for deferred deletion");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge backup recover <file>")
                    + ": cancel deferred deletion mark");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge backup load <file>")
                    + ": restore one backup archive");
            return;
        }

        if (topic.equalsIgnoreCase("summon")) {
            player.sendMessage(ChatColor.DARK_AQUA + "Summon hints:");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge summon")
                    + ": spawn a sheep using normal egg tier roll logic");
            player.sendMessage(ChatColor.GRAY + "- " + label("/sheepmerge summon <tier>")
                    + ": spawn an exact tier (0-" + SheepTier.RAINBOW.getLevel() + ")");
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

        if (topic.equalsIgnoreCase("givepoints") || topic.equalsIgnoreCase("giveautomationpoints")
                || topic.equalsIgnoreCase("setpoints")
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

        if (handleHelpFlags(player, args)) {
            return true;
        }

        if (shouldBlockTutorialCommand(player, args)) {
            return true;
        }

        if (args.length > 0 && dispatchRootCommand(player, args)) {
            return true;
        }

        if (args.length > 0) {
            sendInvalidCommandMessage(player, args);
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

        applyConfiguredSpawn(world);
        player.teleportAsync(getConfiguredFarmTeleportLocation(world));
        player.sendMessage("You were teleported to your sheep farm world.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterSuggestions(ROOT_SUBCOMMANDS, args[0]);
        }

        if (args.length > 0) {
            SheepMergeCommandModule module = findRootModule(args[0]);
            if (module != null) {
                return module.tabComplete(sender, args);
            }
        }

        return List.of();
    }

    private boolean handleHelpFlags(Player player, String[] args) {
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
        return false;
    }

    private boolean dispatchRootCommand(Player player, String[] args) {
        SheepMergeCommandModule module = findRootModule(args[0]);
        return module != null && module.execute(player, args);
    }

    private SheepMergeCommandModule findRootModule(String root) {
        if (root == null) {
            return null;
        }
        for (SheepMergeCommandModule module : rootModules) {
            if (module.root().equalsIgnoreCase(root)) {
                return module;
            }
        }
        return null;
    }

    private boolean handleHelpRootCommand(Player player, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("help")) {
            sendCommandHelp(player, args.length >= 2 ? args[1] : null);
            return true;
        }
        return false;
    }

    private boolean handleDashHelpRootCommand(Player player, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("-help")) {
            sendCommandHelp(player, args.length >= 2 ? args[1] : null);
            return true;
        }
        return false;
    }

    private boolean handleUpgradeCommand(Player player, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("upgrade")) {
            SheepMergeManager.openUpgradeMenu(player);
            return true;
        }
        return false;
    }

    private boolean handleShopCommand(Player player, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("shop")) {
            SheepMergeManager.openShopMenu(player);
            return true;
        }
        return false;
    }

    private boolean handlePrestigeCommand(Player player, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("prestige")) {
            SheepMergeManager.openPrestigeMenu(player);
            return true;
        }
        return false;
    }

    private boolean handleStatusCommand(Player player, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            sendDetailedStats(player, player, "Status");
            return true;
        }
        return false;
    }

    private boolean handleStatsCommand(Player player, String[] args) {
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
        return false;
    }

    private boolean handleCheckpointsCommand(Player player, String[] args) {
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
            sendDetailedStats(player, target, "Checkpoints");
            return true;
        }
        return false;
    }

    private boolean handleCheckQuestPointsCommand(Player player, String[] args) {
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
            sendDetailedStats(player, target, "Check Quest Points");
            return true;
        }
        return false;
    }

    private boolean handleCheckPrestigeCommand(Player player, String[] args) {
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
            sendDetailedStats(player, target, "Check Prestige");
            return true;
        }
        return false;
    }

    private boolean handleVisitCommand(Player player, String[] args) {
        if (!(args.length >= 1 && args[0].equalsIgnoreCase("visit"))) {
            return false;
        }
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

        applyConfiguredSpawn(ownerWorld);
        player.teleportAsync(getConfiguredFarmTeleportLocation(ownerWorld));
        player.sendMessage("You were teleported to " + owner.getName() + "'s sheep farm.");
        player.sendMessage("Use /sheepmerge to return to your own farm.");
        Bukkit.getScheduler().runTaskLater(SheepMergePlugin.instance,
                () -> SheepMergeManager.updateVisitFarmBossBar(player),
                2L);
        player.sendTitle(
                SheepMergeManager.color("&eVisiting " + owner.getName()),
                SheepMergeManager.color("&7Use /sheepmerge to return home"),
                10,
                60,
                10);
        return true;
    }

    private boolean handleKickCommand(Player player, String[] args) {
        if (!(args.length >= 1 && args[0].equalsIgnoreCase("kick"))) {
            return false;
        }
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

    private boolean handleLeaderboardCommand(Player player, String[] args) {
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

        if (args.length >= 2 && args[0].equalsIgnoreCase("leaderboard")
                && args[1].equalsIgnoreCase("remove")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            boolean removed = SheepMergeManager.removeTopPointsDisplay();
            player.sendMessage(removed ? "Leaderboard removed." : "No leaderboard display was found.");
            return true;
        }

        if ((args.length == 4 || args.length == 5) && args[0].equalsIgnoreCase("leaderboard")
                && isCoordinate(args[1]) && isCoordinate(args[2]) && isCoordinate(args[3])) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }

            World targetWorld = player.getWorld();
            if (args.length == 5) {
                World byName = Bukkit.getWorld(args[4]);
                if (byName == null) {
                    player.sendMessage("Unknown world. Usage: /sheepmerge leaderboard <x> <y> <z> [world]");
                    return true;
                }
                targetWorld = byName;
            }

            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                Location location = new Location(targetWorld, x, y, z);

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
                        "Invalid coordinates. Usage: /sheepmerge leaderboard <x> <y> <z> [world]");
                return true;
            }
        }
        return false;
    }

    private boolean handleStormCommand(Player player, String[] args) {
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
        return false;
    }

    private boolean handleSummonCommand(Player player, String[] args) {
        if (!(args.length >= 1 && args[0].equalsIgnoreCase("summon"))) {
            return false;
        }

        if (!player.isOp()) {
            player.sendMessage(error("Only server operators can use this command."));
            return true;
        }

        if (!SheepMergeManager.isSheepFarmWorld(player.getWorld())
                || SheepMergeManager.isFarmBuildWorld(player.getWorld())) {
            player.sendMessage(error("Use this command in a farm or tutorial world (not the build world)."));
            return true;
        }

        if (args.length > 2) {
            player.sendMessage(error("Usage: /sheepmerge summon [tier]"));
            return true;
        }

        if (SheepMergeManager.isWorldAtLimit(player.getWorld())) {
            player.sendMessage(error("Farm full. Merge sheep or increase the sheep limit first."));
            return true;
        }

        SheepTier tier;
        boolean autoRolled = args.length == 1;
        if (autoRolled) {
            tier = SheepMergeManager.rollSpawnTier(player.getWorld());
        } else {
            int requestedLevel;
            try {
                requestedLevel = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(error("Invalid tier. Usage: /sheepmerge summon [tier]"));
                return true;
            }

            if (requestedLevel < 0 || requestedLevel > SheepTier.RAINBOW.getLevel()) {
                player.sendMessage(error("Tier must be between 0 and " + SheepTier.RAINBOW.getLevel() + "."));
                return true;
            }
            tier = SheepTier.byLevel(requestedLevel);
        }

        Sheep spawned = player.getWorld().spawn(player.getLocation().clone().add(0.0D, 0.15D, 0.0D), Sheep.class);
        SheepMergeManager.setSheepTier(spawned, tier);
        player.sendMessage(adminHeader("Summon") + " " + value("Spawned ") + label(tier.getDisplayName())
                + (autoRolled ? value(" using egg roll logic.") : value(" at tier level " + tier.getLevel() + ".")));
        return true;
    }

    private boolean handleComboFrenzyCommand(Player player, String[] args) {
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
        return false;
    }

    private boolean handleReloadCommand(Player player, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }

            SheepMergePlugin plugin = SheepMergePlugin.instance;
            if (plugin == null) {
                player.sendMessage(error("Plugin instance not available."));
                return true;
            }

            plugin.reloadConfig();
            SheepMergeConfiguration.initialize(plugin);
            SheepMergeManager.applyConfiguration(SheepMergeConfiguration.get());
            player.sendMessage(adminHeader("Config") + " " + value("Configuration reloaded."));
            return true;
        }
        return false;
    }

    private boolean handleWorldCommand(Player player, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("world")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            World buildWorld = ensureFarmBuildWorld();
            if (buildWorld == null) {
                player.sendMessage("Unable to open the farm build world right now.");
                return true;
            }
            applyConfiguredSpawn(buildWorld);
            player.teleportAsync(getConfiguredFarmTeleportLocation(buildWorld));
            player.sendMessage("Teleported to the shared farm build world.");
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("save")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            if (!SheepMergeManager.isFarmBuildWorld(player.getWorld())) {
                player.sendMessage(
                        "Use this command while standing in the shared farm build world. Use /sheepmerge world to travel there.");
                return true;
            }
            player.getWorld().save();
            if (SheepMergeManager.saveSharedFarmLayoutFromWorld(player.getWorld())) {
                player.sendMessage("Saved the shared farm build world and layout snapshot.");
            } else {
                player.sendMessage("Saved the shared farm build world, but layout snapshot save failed.");
            }
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("load")) {
            if (!player.isOp()) {
                player.sendMessage("Only server operators can use this command.");
                return true;
            }
            if (!SheepMergeManager.isFarmBuildWorld(player.getWorld())) {
                player.sendMessage("Use this command while standing in the shared farm build world.");
                return true;
            }
            if (SheepMergeManager.isFarmBuildCommitInProgress()) {
                player.sendMessage("A farm refresh is already in progress.");
                return true;
            }
            int updated = SheepMergeManager.startCommitFarmBuildWorldToLoadedFarms(player);
            if (updated <= 0) {
                player.sendMessage("Unable to commit the shared farm build world right now.");
                return true;
            }
            player.sendMessage(
                    "Refreshing " + updated + " loaded farm world(s). Players will be able to return shortly.");
            return true;
        }

        return false;
    }

    private boolean handleBackupCommand(Player player, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("backup")) {
            return false;
        }
        if (!player.isOp()) {
            player.sendMessage(error("Only server operators can use this command."));
            return true;
        }

        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("create"))) {
            File created = SheepMergeManager.createManualBackup();
            if (created == null) {
                player.sendMessage(error("Unable to create backup."));
                return true;
            }
            player.sendMessage(adminHeader("Backup") + " " + value("Created ") + label(created.getName()));
            return true;
        }

        if (args.length == 2 && args[1].equalsIgnoreCase("list")) {
            List<String> backups = SheepMergeManager.listBackups();
            if (backups.isEmpty()) {
                player.sendMessage(error("No backups found."));
                return true;
            }
            player.sendMessage(adminHeader("Backups") + " " + value("Available archives:"));
            for (int i = 0; i < Math.min(15, backups.size()); i++) {
                String backupName = backups.get(i);
                boolean marked = SheepMergeManager.isBackupMarkedForDeletion(backupName);
                player.sendMessage(ChatColor.GRAY + "- " + backupName
                        + (marked ? ChatColor.RED + " [marked for deletion]" : ""));
            }
            return true;
        }

        if (args.length >= 3 && args[1].equalsIgnoreCase("load")) {
            String backupName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            File postLoadBackup = SheepMergeManager.loadBackup(backupName);
            if (postLoadBackup == null) {
                player.sendMessage(error("Unable to load backup '" + backupName + "'."));
                return true;
            }
            player.sendMessage(adminHeader("Backup") + " " + value("Loaded backup '") + label(backupName)
                    + value("'."));
            player.sendMessage(adminHeader("Backup") + " " + value("Created post-load permanent backup '")
                    + label(postLoadBackup.getName()) + value("'."));
            return true;
        }

        if (args.length >= 3 && args[1].equalsIgnoreCase("delete")) {
            String backupName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            if (!SheepMergeManager.markBackupForDeletion(backupName)) {
                player.sendMessage(error("Unable to mark backup '" + backupName + "' for deletion."));
                return true;
            }
            player.sendMessage(adminHeader("Backup") + " " + value("Marked '") + label(backupName)
                    + value("' for deletion after 24h on a future restart."));
            return true;
        }

        if (args.length >= 3 && args[1].equalsIgnoreCase("recover")) {
            String backupName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            if (!SheepMergeManager.recoverBackupMarkedForDeletion(backupName)) {
                player.sendMessage(error("Backup '" + backupName + "' is not marked for deletion."));
                return true;
            }
            player.sendMessage(adminHeader("Backup") + " " + value("Recovered '") + label(backupName)
                    + value("' from deletion queue."));
            return true;
        }

        player.sendMessage(error("Usage: /sheepmerge backup [create|list|load|delete|recover <file>]."));
        return true;
    }

    private boolean handleResetDataCommand(Player player, String[] args) {
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
        return false;
    }

    private boolean handleGivePointsCommand(Player player, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("givepoints")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            long amount;
            try {
                amount = Long.parseLong(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(error("Invalid amount. Usage: /sheepmerge givepoints <amount> [player]"));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 2);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            long previous = SheepMergeManager.getPlayerPoints(target);
            SheepMergeManager.adminGivePoints(target, amount);
            long updated = SheepMergeManager.getPlayerPoints(target);
            SheepMergeManager.updatePointsScoreboard(target);
            player.sendMessage(statUpdateMessage(
                    "Points Updated",
                    target,
                    "Points",
                    previous,
                    updated));
            return true;
        }
        return false;
    }

    private boolean handleSetPointsCommand(Player player, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("setpoints")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            long amount;
            try {
                amount = Long.parseLong(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(error("Invalid amount. Usage: /sheepmerge setpoints <amount> [player]"));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 2);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            long previous = SheepMergeManager.getPlayerPoints(target);
            SheepMergeManager.adminSetPoints(target, amount);
            long updated = SheepMergeManager.getPlayerPoints(target);
            SheepMergeManager.updatePointsScoreboard(target);
            player.sendMessage(statUpdateMessage(
                    "Points Updated",
                    target,
                    "Points",
                    previous,
                    updated));
            return true;
        }
        return false;
    }

    private boolean handleGiveAutomationPointsCommand(Player player, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("giveautomationpoints")) {
            if (!player.isOp()) {
                player.sendMessage(error("Only server operators can use this command."));
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(error("Invalid amount. Usage: /sheepmerge giveautomationpoints <amount> [player]"));
                return true;
            }
            Player target = resolveTargetPlayer(player, args, 2);
            if (target == null) {
                player.sendMessage(error("That player is not online."));
                return true;
            }
            int previous = SheepMergeManager.getAutomationPoints(target);
            SheepMergeManager.adminGiveAutomationPoints(target, amount);
            int updated = SheepMergeManager.getAutomationPoints(target);
            player.sendMessage(statUpdateMessage(
                    "Automation Points Updated",
                    target,
                    "Automation Points",
                    previous,
                    updated));
            return true;
        }
        return false;
    }

    private boolean handleGiveQuestPointsCommand(Player player, String[] args) {
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
        return false;
    }

    private boolean handleSetQuestPointsCommand(Player player, String[] args) {
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
        return false;
    }

    private boolean handleSetPrestigeCommand(Player player, String[] args) {
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
                player.sendMessage(error("Invalid prestige level. Use a value of 0 or higher (unlimited)."));
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
        return false;
    }

    private List<String> tabCompleteNone(CommandSender sender, String[] args) {
        return List.of();
    }

    private List<String> tabCompleteWorld(CommandSender sender, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("world")) {
            return filterSuggestions(WORLD_SUBCOMMANDS, args[1]);
        }
        return List.of();
    }

    private List<String> tabCompleteBackup(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            return List.of();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("backup")) {
            return filterSuggestions(BACKUP_SUBCOMMANDS, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("backup") && args[1].equalsIgnoreCase("load")) {
            return filterSuggestions(SheepMergeManager.listBackups(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("backup") && args[1].equalsIgnoreCase("delete")) {
            return filterSuggestions(SheepMergeManager.listBackups(), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("backup") && args[1].equalsIgnoreCase("recover")) {
            return filterSuggestions(SheepMergeManager.listBackups(), args[2]);
        }
        return List.of();
    }

    private List<String> tabCompleteSummon(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            return List.of();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("summon")) {
            return filterSuggestions(List.of(
                    "<tier>",
                    "0",
                    "1",
                    String.valueOf(SheepTier.RAINBOW.getLevel())), args[1]);
        }
        return List.of();
    }

    private List<String> tabCompleteVisit(CommandSender sender, String[] args) {
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
        return List.of();
    }

    private List<String> tabCompleteLeaderboard(CommandSender sender, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
            List<String> suggestions = new ArrayList<>(LEADERBOARD_SUBCOMMANDS);
            suggestions.addAll(HELP_FLAGS);
            suggestions.addAll(LEADERBOARD_COORDINATE_HINTS);
            return filterSuggestions(suggestions, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("leaderboard")) {
            return filterSuggestions(appendHelpFlags(List.of("<y>", "remove"), args[2]), args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("leaderboard")) {
            return filterSuggestions(appendHelpFlags(List.of("<z>", "remove"), args[3]), args[3]);
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("leaderboard")) {
            return filterSuggestions(appendHelpFlags(List.of("[world]", "remove"), args[4]), args[4]);
        }

        return List.of();
    }

    private List<String> tabCompleteKick(CommandSender sender, String[] args) {
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
        return List.of();
    }

    private List<String> tabCompleteAdminPlayerTarget(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            return List.of();
        }
        if (args.length == 2 && matchesSubcommand(ADMIN_PLAYER_TARGET_SUBCOMMANDS, args[0])) {
            return appendHelpFlags(onlinePlayerNameSuggestions(args[1]), args[1]);
        }
        return List.of();
    }

    private List<String> tabCompleteAdminStatCheck(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            return List.of();
        }
        if (args.length == 2 && matchesSubcommand(ADMIN_STAT_CHECK_SUBCOMMANDS, args[0])) {
            return appendHelpFlags(onlinePlayerNameSuggestions(args[1]), args[1]);
        }
        return List.of();
    }

    private List<String> tabCompleteAdminAmountPlayer(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            return List.of();
        }
        if (args.length == 2 && matchesSubcommand(ADMIN_AMOUNT_PLAYER_SUBCOMMANDS, args[0])) {
            List<String> suggestions = new ArrayList<>(HELP_FLAGS);
            suggestions.addAll(AMOUNT_HINTS);
            return filterSuggestions(suggestions, args[1]);
        }

        if (args.length == 3 && matchesSubcommand(ADMIN_AMOUNT_PLAYER_SUBCOMMANDS, args[0])) {
            return appendHelpFlags(onlinePlayerNameSuggestions(args[2]), args[2]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setprestige")) {
            List<String> suggestions = new ArrayList<>(HELP_FLAGS);
            suggestions.addAll(LEVEL_HINTS);
            return filterSuggestions(suggestions, args[1]);
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

    private void sendInvalidCommandMessage(Player player, String[] args) {
        if (player == null || args == null || args.length == 0) {
            return;
        }

        String root = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
        if (root.equals("leaderboard")) {
            player.sendMessage(error(
                    "Invalid leaderboard command. Use /sheepmerge leaderboard, /sheepmerge leaderboard remove, or /sheepmerge leaderboard <x> <y> <z> [world]."));
            sendCommandHelp(player, "leaderboard");
            return;
        }

        if (root.equals("topdisplay")) {
            player.sendMessage(error("/sheepmerge topdisplay was removed. Use /sheepmerge leaderboard instead."));
            sendCommandHelp(player, "leaderboard");
            return;
        }

        if (root.equals("world")) {
            player.sendMessage(error(
                    "Invalid world command. Use /sheepmerge world, /sheepmerge world save, or /sheepmerge world load."));
            sendCommandHelp(player, "world");
            return;
        }

        if (root.equals("backup")) {
            player.sendMessage(error(
                    "Invalid backup command. Use /sheepmerge backup, /sheepmerge backup list, /sheepmerge backup load <file>, /sheepmerge backup delete <file>, or /sheepmerge backup recover <file>."));
            sendCommandHelp(player, "backup");
            return;
        }

        if (root.equals("mapsave")) {
            player.sendMessage(error("/sheepmerge mapsave was removed. Use /sheepmerge world save instead."));
            sendCommandHelp(player, "world");
            return;
        }

        if (root.equals("mapload")) {
            player.sendMessage(error("/sheepmerge mapload was removed. Use /sheepmerge world load instead."));
            sendCommandHelp(player, "world");
            return;
        }

        if (root.equals("layout")) {
            player.sendMessage(error("/sheepmerge layout was removed. Use /sheepmerge world save|load instead."));
            sendCommandHelp(player, "world");
            return;
        }

        if (root.equals("visit")) {
            player.sendMessage(error(
                    "Invalid visit command. Use /sheepmerge visit <player> or /sheepmerge visit -toggle [player]."));
            sendCommandHelp(player, "visit");
            return;
        }

        if (root.equals("tutorial")) {
            player.sendMessage(error(
                    "/sheepmerge tutorial was removed. The tutorial starts automatically until you unlock your farm."));
            return;
        }

        if (root.equals("resetdata") || root.equals("stats") || root.equals("checkpoints")
                || root.equals("checkquestpoints") || root.equals("checkprestige") || root.equals("givepoints")
                || root.equals("setpoints") || root.equals("givequestpoints") || root.equals("setquestpoints")
                || root.equals("reload")
                || root.equals("setprestige")
                || root.equals("summon")) {
            player.sendMessage(error("Invalid admin command syntax for /sheepmerge " + root
                    + ". Use /sheepmerge help -help for command hints."));
            sendCommandHelp(player, root);
            return;
        }

        player.sendMessage(error("Unknown SheepMerge command: /sheepmerge " + String.join(" ", args)));
        player.sendMessage(ChatColor.GRAY + "Use /sheepmerge help or /sheepmerge -help for command hints.");
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

    private boolean shouldBlockTutorialCommand(Player player, String[] args) {
        if (!SheepMergeManager.shouldRestrictTutorialActions(player)) {
            return false;
        }
        if (args == null || args.length == 0) {
            return SheepMergeManager.blockTutorialAction(
                    player,
                    SheepMergeManager.TutorialAction.OTHER_COMMAND,
                    "use that command");
        }

        String root = args[0].toLowerCase(Locale.ROOT);
        return switch (root) {
            case "upgrade" -> SheepMergeManager.blockTutorialAction(
                    player,
                    SheepMergeManager.TutorialAction.OPEN_UPGRADE_COMMAND,
                    "open upgrades now");
            case "shop" -> SheepMergeManager.blockTutorialAction(
                    player,
                    SheepMergeManager.TutorialAction.OPEN_SHOP_COMMAND,
                    "open the shear shop now");
            case "prestige" -> SheepMergeManager.blockTutorialAction(
                    player,
                    SheepMergeManager.TutorialAction.OPEN_PRESTIGE_COMMAND,
                    "open prestige now");
            default -> SheepMergeManager.blockTutorialAction(
                    player,
                    SheepMergeManager.TutorialAction.OTHER_COMMAND,
                    "use that command");
        };
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
                + value(SheepMergeManager.formatPoints(SheepMergeManager.getPlayerPoints(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Quest Points") + ": "
                + value(SheepMergeManager.formatPoints(SheepMergeManager.getQuestPoints(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Prestige") + ": " + value(String.valueOf(SheepMergeManager.getPrestigeLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Prestige Points") + ": "
                + value(SheepMergeManager.formatPoints(SheepMergeManager.getPrestigePoints(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Automation Points") + ": "
                + value(SheepMergeManager.formatPoints(SheepMergeManager.getAutomationPoints(target))));
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
        sender.sendMessage(ChatColor.GRAY + "- " + label("Shear Shop") + ": Lv."
                + value(String.valueOf(SheepMergeManager.getShearShopLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Wool Keeper") + ": Lv."
                + value(String.valueOf(SheepMergeManager.getShearWoolSaveLevel(target)))
                + ChatColor.GRAY + " (" + value(String.valueOf(SheepMergeManager.getShearWoolSaveChancePercent(target)))
                + ChatColor.GRAY + "%)"
                + ChatColor.DARK_GRAY + " | "
                + label("Tier Booster") + ": Lv."
                + value(String.valueOf(SheepMergeManager.getShearTierBoostLevel(target)))
                + ChatColor.GRAY + " ("
                + value(String.valueOf(SheepMergeManager.getShearTierBoostChancePercent(target)))
                + ChatColor.GRAY + "%)");
        sender.sendMessage(ChatColor.GRAY + "- " + label("Quest Upgrades") + ": Duration Lv."
                + value(String.valueOf(SheepMergeManager.getQuestUpgradeDurationLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Power Lv") + ": "
                + value(String.valueOf(SheepMergeManager.getQuestUpgradePowerLevel(target))));
        sender.sendMessage(ChatColor.GRAY + "- " + label("Combo Upgrades") + ": Decay Lv."
                + value(String.valueOf(SheepMergeManager.getComboDecayUpgradeLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Gain Lv") + ": " + value(String.valueOf(SheepMergeManager.getComboGainUpgradeLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Max Lv") + ": " + value(String.valueOf(SheepMergeManager.getComboMaxUpgradeLevel(target))));
        sender.sendMessage(ChatColor.GRAY + "- " + label("Prestige Upgrades") + ": Double Points Lv."
                + value(String.valueOf(SheepMergeManager.getPrestigeDoublePointsChanceLevel(target)))
                + ChatColor.GRAY + " (" + value(String.valueOf(SheepMergeManager.getDoublePointsChancePercent(target)))
                + ChatColor.GRAY + "%)"
                + ChatColor.DARK_GRAY + " | "
                + label("Higher Max Lv") + ": "
                + value(String.valueOf(SheepMergeManager.getPrestigeHigherMaxLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Start Eggs Lv") + ": "
                + value(String.valueOf(SheepMergeManager.getPrestigeStartEggsLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Base Tier Lv") + ": " + value(String.valueOf(SheepMergeManager.getBaseSpawnTierLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Quest Reward Lv") + ": "
                + value(String.valueOf(SheepMergeManager.getPrestigeQuestRewardLevel(target))));
        sender.sendMessage(ChatColor.GRAY + "- " + label("Automation Upgrades") + ": Auto Buy Lv."
                + value(String.valueOf(SheepMergeManager.getAutomationAutoBuyUpgradeLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Auto Ability Lv") + ": "
                + value(String.valueOf(SheepMergeManager.getAutomationAutoAbilityUpgradeLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Slow Merge Lv") + ": "
                + value(String.valueOf(SheepMergeManager.getAutomationSlowAutoMergeUpgradeLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Slow Shear Lv") + ": "
                + value(String.valueOf(SheepMergeManager.getAutomationSlowAutoShearUpgradeLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Auto Spawn Lv") + ": "
                + value(String.valueOf(SheepMergeManager.getAutomationAutoSpawnUpgradeLevel(target)))
                + ChatColor.DARK_GRAY + " | "
                + label("Auto Prestige Lv") + ": "
                + value(String.valueOf(SheepMergeManager.getAutomationAutoPrestigeUpgradeLevel(target))));
        sender.sendMessage(ChatColor.GRAY + "- " + label("Automation Enabled") + ": "
                + label("Auto Buy") + "="
                + (SheepMergeManager.isAutomationAutoBuyEnabled(target) ? ChatColor.GREEN + "ON"
                        : ChatColor.RED + "OFF")
                + ChatColor.DARK_GRAY + " | "
                + label("Auto Ability") + "="
                + (SheepMergeManager.isAutomationAutoAbilityEnabled(target) ? ChatColor.GREEN + "ON"
                        : ChatColor.RED + "OFF")
                + ChatColor.DARK_GRAY + " | "
                + label("Slow Merge") + "="
                + (SheepMergeManager.isAutomationSlowAutoMergeEnabled(target) ? ChatColor.GREEN + "ON"
                        : ChatColor.RED + "OFF")
                + ChatColor.DARK_GRAY + " | "
                + label("Slow Shear") + "="
                + (SheepMergeManager.isAutomationSlowAutoShearEnabled(target) ? ChatColor.GREEN + "ON"
                        : ChatColor.RED + "OFF")
                + ChatColor.DARK_GRAY + " | "
                + label("Auto Spawn") + "="
                + (SheepMergeManager.isAutomationAutoSpawnEnabled(target) ? ChatColor.GREEN + "ON"
                        : ChatColor.RED + "OFF")
                + ChatColor.DARK_GRAY + " | "
                + label("Auto Prestige") + "="
                + (SheepMergeManager.isAutomationAutoPrestigeEnabled(target) ? ChatColor.GREEN + "ON"
                        : ChatColor.RED + "OFF"));
        sender.sendMessage(ChatColor.GRAY + "- " + label("Farm Visit Access") + ": "
                + (SheepMergeManager.isFarmVisitable(target.getUniqueId())
                        ? ChatColor.GREEN + "open"
                        : ChatColor.RED + "closed"));
    }

    private String statUpdateMessage(String importance, Player target, String statLabel, long fromValue, long toValue) {
        long delta = toValue - fromValue;
        ChatColor deltaColor = delta >= 0 ? ChatColor.GREEN : ChatColor.RED;
        String formattedFrom = shouldFormatPointStat(statLabel) ? SheepMergeManager.formatPoints(fromValue)
                : String.valueOf(fromValue);
        String formattedTo = shouldFormatPointStat(statLabel) ? SheepMergeManager.formatPoints(toValue)
                : String.valueOf(toValue);
        String signedDelta = (delta >= 0 ? "+" : "")
                + (shouldFormatPointStat(statLabel) ? SheepMergeManager.formatPoints(Math.abs(delta))
                        : String.valueOf(Math.abs(delta)));
        return adminHeader(importance)
                + " " + label("Player") + ": " + value(target.getName())
                + ChatColor.DARK_GRAY + " | "
                + label("Stat") + ": " + value(statLabel)
                + ChatColor.DARK_GRAY + " | "
                + label("From") + ": " + value(formattedFrom)
                + ChatColor.DARK_GRAY + " -> "
                + label("To") + ": " + value(formattedTo)
                + ChatColor.DARK_GRAY + " | "
                + label("Change") + ": " + deltaColor + signedDelta;
    }

    private boolean shouldFormatPointStat(String statLabel) {
        return "Points".equals(statLabel) || "Quest Points".equals(statLabel) || "Prestige Points".equals(statLabel);
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
            initializeManagedWorldState(world, false);
            return world;
        }
        return createFlatWorld(worldName);
    }

    public static World ensureFarmBuildWorld() {
        World world = Bukkit.getWorld(SheepMergeManager.getFarmBuildWorldName());
        if (world != null) {
            initializeManagedWorldState(world, false);
            return world;
        }
        return createFlatWorld(SheepMergeManager.getFarmBuildWorldName());
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
        applyConfiguredSpawn(world);
        player.teleportAsync(getConfiguredFarmTeleportLocation(world));
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
        applyConfiguredSpawn(world);
        player.teleportAsync(getConfiguredFarmTeleportLocation(world));
        return true;
    }

    private static World createFlatWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            initializeManagedWorldState(world, false);
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

        initializeManagedWorldState(world, true);
        return world;
    }

    private static void initializeManagedWorldState(World world, boolean applyDefaultLayoutWhenMissing) {
        if (world == null) {
            return;
        }

        applyFarmWorldRules(world);
        ensureWorldStorageFolders(world);

        String managedWorldName = world.getName();
        if (managedWorldName == null || managedWorldName.isBlank()) {
            return;
        }
        if (initializedManagedWorldNames.contains(managedWorldName) && !applyDefaultLayoutWhenMissing) {
            return;
        }

        if (SheepMergeManager.isSheepFarmWorld(world)) {
            if (SheepMergeManager.hasSavedFarmLayout() || applyDefaultLayoutWhenMissing) {
                SheepMergeManager.applyFarmLayout(world);
            }
            if (world.getEntitiesByClass(org.bukkit.entity.Sheep.class).isEmpty()) {
                SheepMergeManager.restoreSavedSheepForWorld(world);
            }
        } else if (SheepMergeManager.isFarmBuildWorld(world)) {
            if (SheepMergeManager.hasSavedFarmLayout() || applyDefaultLayoutWhenMissing) {
                SheepMergeManager.applyFarmLayout(world);
            }
        }

        initializedManagedWorldNames.add(managedWorldName);
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
        if (world == null
                || (!SheepMergeManager.isSheepFarmWorld(world) && !SheepMergeManager.isFarmBuildWorld(world))) {
            return;
        }
        world.setPVP(false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(0);
        world.setClearWeatherDuration(Integer.MAX_VALUE);
    }

    private static Location getConfiguredFarmTeleportLocation(World world) {
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        double x = configuration == null ? 0.5D : configuration.getFarmTeleportX();
        double y = configuration == null ? 101.0D : configuration.getFarmTeleportY();
        double z = configuration == null ? 0.5D : configuration.getFarmTeleportZ();
        return new Location(world, x, y, z);
    }

    private static void applyConfiguredSpawn(World world) {
        if (world == null) {
            return;
        }
        SheepMergeConfiguration configuration = SheepMergeConfiguration.get();
        double x = configuration == null ? 0.5D : configuration.getFarmTeleportX();
        double y = configuration == null ? 101.0D : configuration.getFarmTeleportY();
        double z = configuration == null ? 0.5D : configuration.getFarmTeleportZ();
        world.setSpawnLocation((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }
}
