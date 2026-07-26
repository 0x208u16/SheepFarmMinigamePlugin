package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

internal object SheepCommandTabCompletion {
    private val rootSubcommands = listOf(
        "help",
        "-help",
        "upgrade",
        "prestige",
        "shop",
        "top",
        "visit",
        "kick",
        "status",
        "storm",
        "summon",
        "combofrenzy",
        "reload",
        "liveupdate",
        "leaderboard",
        "resetdata",
        "stats",
        "checkpoints",
        "checkquestpoints",
        "checkprestige",
        "givepoints",
        "giveautomationpoints",
        "givesacrificepoints",
        "setpoints",
        "givequestpoints",
        "setquestpoints",
        "setprestige",
        "completeachievement",
        "completeallachievements",
        "backup",
        "world"
    )
    private val worldSubcommands = listOf("help", "-help", "save", "load")
    private val backupSubcommands = listOf("help", "-help", "create", "list", "load", "delete", "recover")
    private val leaderboardSubcommands = listOf("help", "-help", "move", "remove", "clear")
    private val liveUpdateSubcommands = listOf(
        "help",
        "-help",
        "status",
        "check",
        "apply",
        "on",
        "off",
        "enable",
        "disable"
    )
    private val helpFlags = listOf("help", "-help")
    private val leaderboardCoordinateHints = listOf("<x>", "<y>", "<z>", "[world]")
    private val amountHints = listOf("<amount>", "0", "100")
    private val levelHints = listOf("<level>", "0", "1")
    private val adminAmountPlayerSubcommands = listOf(
        "givepoints",
        "giveautomationpoints",
        "givesacrificepoints",
        "setpoints",
        "givequestpoints",
        "setquestpoints",
        "setprestige"
    )
    private val adminAchievementSubcommands = listOf("completeachievement", "completeallachievements")
    private val adminPlayerTargetSubcommands = listOf("resetdata")
    private val adminStatCheckSubcommands = listOf("stats", "checkpoints", "checkquestpoints", "checkprestige")

    @JvmStatic
    fun rootSuggestions(prefix: String?): List<String> = filterSuggestions(rootSubcommands, prefix)

    @JvmStatic
    fun tabCompleteNone(sender: CommandSender, args: Array<String>): List<String> = emptyList()

    @JvmStatic
    fun tabCompleteLiveUpdate(sender: CommandSender, args: Array<String>): List<String> {
        if (!sender.isOp) return emptyList()
        return if (args.size == 2 && args[0].equals("liveupdate", ignoreCase = true)) {
            filterSuggestions(liveUpdateSubcommands, args[1])
        } else {
            emptyList()
        }
    }

    @JvmStatic
    fun tabCompleteWorld(sender: CommandSender, args: Array<String>): List<String> =
        if (args.size == 2 && args[0].equals("world", ignoreCase = true)) {
            filterSuggestions(worldSubcommands, args[1])
        } else {
            emptyList()
        }

    @JvmStatic
    fun tabCompleteBackup(sender: CommandSender, args: Array<String>): List<String> {
        if (!sender.isOp) return emptyList()
        if (args.size == 2 && args[0].equals("backup", ignoreCase = true)) {
            return filterSuggestions(backupSubcommands, args[1])
        }
        if (
            args.size == 3 &&
            args[0].equals("backup", ignoreCase = true) &&
            listOf("load", "delete", "recover").any { args[1].equals(it, ignoreCase = true) }
        ) {
            return filterSuggestions(SheepMergeManager.listBackups(), args[2])
        }
        return emptyList()
    }

    @JvmStatic
    fun tabCompleteSummon(sender: CommandSender, args: Array<String>): List<String> {
        if (!sender.isOp) return emptyList()
        return if (args.size == 2 && args[0].equals("summon", ignoreCase = true)) {
            filterSuggestions(
                listOf(
                    "<tier>",
                    "0",
                    "1",
                    SheepTier.RAINBOW.level.toString(),
                    (SheepTier.RAINBOW.level + 1).toString(),
                    (SheepTier.RAINBOW.level + 4).toString()
                ),
                args[1]
            )
        } else {
            emptyList()
        }
    }

    @JvmStatic
    fun tabCompleteVisit(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size != 2 || !args[0].equals("visit", ignoreCase = true)) return emptyList()
        val visitOptions = helpFlags.toMutableList()
        visitOptions.add("-toggle")
        Bukkit.getOnlinePlayers().forEach { online -> online.name?.let(visitOptions::add) }
        return filterSuggestions(visitOptions, args[1])
    }

    @JvmStatic
    fun tabCompleteLeaderboard(sender: CommandSender, args: Array<String>): List<String> {
        if (args.isEmpty() || !args[0].equals("leaderboard", ignoreCase = true)) return emptyList()
        return when (args.size) {
            2 -> filterSuggestions(leaderboardSubcommands + helpFlags + leaderboardCoordinateHints, args[1])
            3 -> filterSuggestions(appendHelpFlags(listOf("<y>", "remove"), args[2]), args[2])
            4 -> filterSuggestions(appendHelpFlags(listOf("<z>", "remove"), args[3]), args[3])
            5 -> filterSuggestions(appendHelpFlags(listOf("[world]", "remove"), args[4]), args[4])
            else -> emptyList()
        }
    }

    @JvmStatic
    fun tabCompleteTop(sender: CommandSender, args: Array<String>): List<String> =
        if (args.size == 2 && args[0].equals("top", ignoreCase = true)) {
            filterSuggestions(appendHelpFlags(listOf("<page>", "1", "2", "3"), args[1]), args[1])
        } else {
            emptyList()
        }

    @JvmStatic
    fun tabCompleteKick(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size != 2 || !args[0].equals("kick", ignoreCase = true) || sender !is Player) return emptyList()
        val kickTargets = Bukkit.getOnlinePlayers().filter { online ->
            online != sender && !online.isOp && online.world == sender.world
        }.mapNotNull(Player::getName)
        return filterSuggestions(kickTargets, args[1])
    }

    @JvmStatic
    fun tabCompleteAdminPlayerTarget(sender: CommandSender, args: Array<String>): List<String> {
        if (!sender.isOp) return emptyList()
        return if (args.size == 2 && matchesSubcommand(adminPlayerTargetSubcommands, args[0])) {
            appendHelpFlags(onlinePlayerNameSuggestions(args[1]), args[1])
        } else {
            emptyList()
        }
    }

    @JvmStatic
    fun tabCompleteAdminStatCheck(sender: CommandSender, args: Array<String>): List<String> {
        if (!sender.isOp) return emptyList()
        return if (args.size == 2 && matchesSubcommand(adminStatCheckSubcommands, args[0])) {
            appendHelpFlags(onlinePlayerNameSuggestions(args[1]), args[1])
        } else {
            emptyList()
        }
    }

    @JvmStatic
    fun tabCompleteAdminAmountPlayer(sender: CommandSender, args: Array<String>): List<String> {
        if (!sender.isOp) return emptyList()
        if (args.size == 2 && matchesSubcommand(adminAmountPlayerSubcommands, args[0])) {
            return filterSuggestions(helpFlags + amountHints, args[1])
        }
        if (args.size == 3 && matchesSubcommand(adminAmountPlayerSubcommands, args[0])) {
            return appendHelpFlags(onlinePlayerNameSuggestions(args[2]), args[2])
        }
        if (args.size == 2 && args[0].equals("setprestige", ignoreCase = true)) {
            return filterSuggestions(helpFlags + levelHints, args[1])
        }
        return emptyList()
    }

    @JvmStatic
    fun tabCompleteAdminAchievement(sender: CommandSender, args: Array<String>): List<String> {
        if (!sender.isOp) return emptyList()
        if (args.size == 2 && args[0].equals("completeachievement", ignoreCase = true)) {
            return filterSuggestions(helpFlags + "<id>" + "all" + SheepMergeManager.getAchievementIds(), args[1])
        }
        if (args.size == 3 && args[0].equals("completeachievement", ignoreCase = true)) {
            return appendHelpFlags(onlinePlayerNameSuggestions(args[2]), args[2])
        }
        if (args.size == 2 && args[0].equals("completeallachievements", ignoreCase = true)) {
            return appendHelpFlags(onlinePlayerNameSuggestions(args[1]), args[1])
        }
        return emptyList()
    }

    @JvmStatic
    fun isAdminAchievementSubcommand(candidate: String?): Boolean =
        matchesSubcommand(adminAchievementSubcommands, candidate)

    @JvmStatic
    fun matchesSubcommand(subcommands: List<String>, candidate: String?): Boolean =
        candidate != null && subcommands.any { it.equals(candidate, ignoreCase = true) }

    @JvmStatic
    fun onlinePlayerNameSuggestions(prefix: String): List<String> {
        val lowerPrefix = prefix.lowercase(Locale.getDefault())
        return Bukkit.getOnlinePlayers()
            .mapNotNull(Player::getName)
            .filter { it.lowercase(Locale.getDefault()).startsWith(lowerPrefix) }
    }

    @JvmStatic
    fun appendHelpFlags(suggestions: List<String>, prefix: String?): List<String> =
        suggestions + filterSuggestions(helpFlags, prefix)

    @JvmStatic
    fun filterSuggestions(suggestions: List<String>, prefix: String?): List<String> {
        if (prefix.isNullOrBlank()) return suggestions
        val lowerPrefix = prefix.lowercase(Locale.getDefault())
        return suggestions.filter { it.lowercase(Locale.getDefault()).startsWith(lowerPrefix) }
    }
}