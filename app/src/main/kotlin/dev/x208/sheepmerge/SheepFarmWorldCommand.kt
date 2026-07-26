package dev.x208.sheepmerge

import dev.x208.sheepmerge.commands.BackupCommandModule
import dev.x208.sheepmerge.commands.CheckPrestigeCommandModule
import dev.x208.sheepmerge.commands.CheckQuestPointsCommandModule
import dev.x208.sheepmerge.commands.CheckpointsCommandModule
import dev.x208.sheepmerge.commands.ComboFrenzyCommandModule
import dev.x208.sheepmerge.commands.CompleteAchievementCommandModule
import dev.x208.sheepmerge.commands.CompleteAllAchievementsCommandModule
import dev.x208.sheepmerge.commands.DashHelpCommandModule
import dev.x208.sheepmerge.commands.GiveAutomationPointsCommandModule
import dev.x208.sheepmerge.commands.GivePointsCommandModule
import dev.x208.sheepmerge.commands.GiveQuestPointsCommandModule
import dev.x208.sheepmerge.commands.GiveSacrificePointsCommandModule
import dev.x208.sheepmerge.commands.HelpCommandModule
import dev.x208.sheepmerge.commands.KickCommandModule
import dev.x208.sheepmerge.commands.LeaderboardCommandModule
import dev.x208.sheepmerge.commands.LiveUpdateCommandModule
import dev.x208.sheepmerge.commands.PrestigeCommandModule
import dev.x208.sheepmerge.commands.ReloadCommandModule
import dev.x208.sheepmerge.commands.ResetDataCommandModule
import dev.x208.sheepmerge.commands.SetPointsCommandModule
import dev.x208.sheepmerge.commands.SetPrestigeCommandModule
import dev.x208.sheepmerge.commands.SetQuestPointsCommandModule
import dev.x208.sheepmerge.commands.SheepMergeCommandModule
import dev.x208.sheepmerge.commands.ShopCommandModule
import dev.x208.sheepmerge.commands.StatsCommandModule
import dev.x208.sheepmerge.commands.StatusCommandModule
import dev.x208.sheepmerge.commands.StormCommandModule
import dev.x208.sheepmerge.commands.SummonCommandModule
import dev.x208.sheepmerge.commands.TopCommandModule
import dev.x208.sheepmerge.commands.UpgradeCommandModule
import dev.x208.sheepmerge.commands.VisitCommandModule
import dev.x208.sheepmerge.commands.WorldCommandModule
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.Locale
import java.util.UUID
import java.util.function.Consumer

class SheepFarmWorldCommand : CommandExecutor, TabCompleter {
    private val rootModules: List<SheepMergeCommandModule> = listOf(
        HelpCommandModule(::handleHelpRootCommand, ::tabCompleteNone),
        DashHelpCommandModule(::handleDashHelpRootCommand, ::tabCompleteNone),
        UpgradeCommandModule(SheepGameplayCommandHandlers::handleUpgradeCommand, ::tabCompleteNone),
        PrestigeCommandModule(SheepGameplayCommandHandlers::handlePrestigeCommand, ::tabCompleteNone),
        ShopCommandModule(SheepGameplayCommandHandlers::handleShopCommand, ::tabCompleteNone),
        TopCommandModule(SheepGameplayCommandHandlers::handleTopCommand, SheepCommandTabCompletion::tabCompleteTop),
        VisitCommandModule(SheepGameplayCommandHandlers::handleVisitCommand, SheepCommandTabCompletion::tabCompleteVisit),
        KickCommandModule(SheepGameplayCommandHandlers::handleKickCommand, SheepCommandTabCompletion::tabCompleteKick),
        StatusCommandModule(SheepGameplayCommandHandlers::handleStatusCommand, ::tabCompleteNone),
        StormCommandModule(SheepGameplayCommandHandlers::handleStormCommand, ::tabCompleteNone),
        SummonCommandModule(SheepGameplayCommandHandlers::handleSummonCommand, SheepCommandTabCompletion::tabCompleteSummon),
        ComboFrenzyCommandModule(SheepGameplayCommandHandlers::handleComboFrenzyCommand, ::tabCompleteNone),
        ReloadCommandModule(SheepGameplayCommandHandlers::handleReloadCommand, ::tabCompleteNone),
        LiveUpdateCommandModule(::handleLiveUpdateCommand, SheepCommandTabCompletion::tabCompleteLiveUpdate),
        LeaderboardCommandModule(SheepGameplayCommandHandlers::handleLeaderboardCommand, SheepCommandTabCompletion::tabCompleteLeaderboard),
        ResetDataCommandModule(SheepAdminCommandHandlers::handleResetDataCommand, SheepCommandTabCompletion::tabCompleteAdminPlayerTarget),
        StatsCommandModule(SheepAdminCommandHandlers::handleStatsCommand, SheepCommandTabCompletion::tabCompleteAdminStatCheck),
        CheckpointsCommandModule(SheepAdminCommandHandlers::handleCheckpointsCommand, SheepCommandTabCompletion::tabCompleteAdminStatCheck),
        CheckQuestPointsCommandModule(SheepAdminCommandHandlers::handleCheckQuestPointsCommand, SheepCommandTabCompletion::tabCompleteAdminStatCheck),
        CheckPrestigeCommandModule(SheepAdminCommandHandlers::handleCheckPrestigeCommand, SheepCommandTabCompletion::tabCompleteAdminStatCheck),
        GivePointsCommandModule(SheepAdminCommandHandlers::handleGivePointsCommand, SheepCommandTabCompletion::tabCompleteAdminAmountPlayer),
        GiveAutomationPointsCommandModule(SheepAdminCommandHandlers::handleGiveAutomationPointsCommand, SheepCommandTabCompletion::tabCompleteAdminAmountPlayer),
        GiveSacrificePointsCommandModule(SheepAdminCommandHandlers::handleGiveSacrificePointsCommand, SheepCommandTabCompletion::tabCompleteAdminAmountPlayer),
        SetPointsCommandModule(SheepAdminCommandHandlers::handleSetPointsCommand, SheepCommandTabCompletion::tabCompleteAdminAmountPlayer),
        GiveQuestPointsCommandModule(SheepAdminCommandHandlers::handleGiveQuestPointsCommand, SheepCommandTabCompletion::tabCompleteAdminAmountPlayer),
        SetQuestPointsCommandModule(SheepAdminCommandHandlers::handleSetQuestPointsCommand, SheepCommandTabCompletion::tabCompleteAdminAmountPlayer),
        SetPrestigeCommandModule(SheepAdminCommandHandlers::handleSetPrestigeCommand, SheepCommandTabCompletion::tabCompleteAdminAmountPlayer),
        CompleteAchievementCommandModule(SheepAdminCommandHandlers::handleCompleteAchievementCommand, SheepCommandTabCompletion::tabCompleteAdminAchievement),
        CompleteAllAchievementsCommandModule(SheepAdminCommandHandlers::handleCompleteAllAchievementsCommand, SheepCommandTabCompletion::tabCompleteAdminAchievement),
        BackupCommandModule(SheepAdminCommandHandlers::handleBackupCommand, SheepCommandTabCompletion::tabCompleteBackup),
        WorldCommandModule(SheepGameplayCommandHandlers::handleWorldCommand, SheepCommandTabCompletion::tabCompleteWorld)
    )

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command.")
            return true
        }
        if (handleHelpFlags(sender, args) || shouldBlockTutorialCommand(sender, args)) {
            return true
        }
        if (args.isNotEmpty() && dispatchRootCommand(sender, args)) {
            return true
        }
        if (args.isNotEmpty()) {
            sendInvalidCommandMessage(sender, args)
            return true
        }
        if (!SheepMergeManager.hasUnlockedFarm(sender)) {
            SheepMergeManager.startTutorial(sender, false)
            return true
        }

        val worldName = getWorldName(sender.uniqueId)
        if (!FarmWorldLifecycle.beginFarmLoadForPlayer(sender)) {
            sender.sendMessage("Your farm is already loading. Please wait.")
            return true
        }
        sender.sendMessage("Loading your sheep farm world...")
        FarmWorldLifecycle.ensureFarmWorldAsync(worldName) { world ->
            val playerId = sender.uniqueId
            if (world == null) {
                if (sender.isOnline) {
                    sender.sendMessage("Unable to create your sheep farm world right now.")
                }
                FarmWorldLifecycle.finishFarmLoadForPlayer(playerId, false)
                return@ensureFarmWorldAsync
            }
            if (!sender.isOnline) {
                FarmWorldLifecycle.finishFarmLoadForPlayer(playerId, false)
                return@ensureFarmWorldAsync
            }
            FarmWorldLifecycle.applyConfiguredSpawn(world)
            FarmWorldLifecycle.teleportPlayerToConfiguredSpawnAsync(
                sender,
                world,
                { sender.sendMessage("You were teleported to your sheep farm world.") },
                "Unable to teleport to your sheep farm world right now.",
                { FarmWorldLifecycle.finishFarmLoadForPlayer(playerId, false) }
            )
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (args.size == 1) {
            return SheepCommandTabCompletion.rootSuggestions(args[0])
        }
        if (args.isNotEmpty()) {
            return findRootModule(args[0])?.tabComplete(sender, args) ?: emptyList()
        }
        return emptyList()
    }

    private fun handleHelpFlags(player: Player, args: Array<String>): Boolean {
        val helpTopic = args.firstOrNull { !isHelpFlag(it) }
        if (args.any(::isHelpFlag)) {
            sendCommandHelp(player, helpTopic)
            return true
        }
        return false
    }

    private fun dispatchRootCommand(player: Player, args: Array<String>): Boolean {
        return findRootModule(args[0])?.execute(player, args) == true
    }

    private fun findRootModule(root: String?): SheepMergeCommandModule? {
        return root?.let { value -> rootModules.firstOrNull { it.root().equals(value, ignoreCase = true) } }
    }

    private fun handleHelpRootCommand(player: Player, args: Array<String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("help", ignoreCase = true)) {
            sendCommandHelp(player, args.getOrNull(1))
            return true
        }
        return false
    }

    private fun handleDashHelpRootCommand(player: Player, args: Array<String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("-help", ignoreCase = true)) {
            sendCommandHelp(player, args.getOrNull(1))
            return true
        }
        return false
    }

    private fun handleLiveUpdateCommand(player: Player, args: Array<String>): Boolean {
        return SheepGameplayCommandHandlers.handleLiveUpdateCommand(player, args) { topic ->
            sendCommandHelp(player, topic)
        }
    }

    private fun tabCompleteNone(sender: CommandSender, args: Array<String>): List<String> {
        return SheepCommandTabCompletion.tabCompleteNone(sender, args)
    }

    private fun sendCommandHelp(player: Player, topic: String?) {
        player.sendMessage(SheepCommandPresentation.adminHeader("SheepMerge Help"))
        sendHelpLine(player, "/sheepmerge", "return to your sheep farm world")
        sendHelpLine(player, "/sheepmerge help", "show this help page")
        sendHelpLine(player, "/sheepmerge upgrade", "open upgrade menu")
        sendHelpLine(player, "/sheepmerge prestige", "open prestige menu")
        sendHelpLine(player, "/sheepmerge shop", "open shop menu")
        sendHelpLine(player, "/sheepmerge top [page]", "show top players by Coins (10 per page)")
        sendHelpLine(player, "/sheepmerge status", "view your current stats")
        sendHelpLine(player, "/sheepmerge visit <player>", "visit another open farm")
        sendHelpLine(player, "/sheepmerge visit -toggle [player]", "toggle farm visit access")
        sendHelpLine(player, "/sheepmerge kick <player>", "remove a visitor from your farm")
        sendHelpLine(player, "/sheepmerge leaderboard", "move the leaderboard display to you")
        sendHelpLine(player, "/sheepmerge leaderboard <x> <y> <z> [world]", "place the leaderboard at coordinates")
        sendHelpLine(player, "/sheepmerge leaderboard remove", "remove the leaderboard display")
        sendHelpLine(player, "/sheepmerge storm", "trigger a sheep storm")
        sendHelpLine(player, "/sheepmerge summon [tier]", "operator summon a sheep (optional tier level)")
        sendHelpLine(player, "/sheepmerge combofrenzy", "trigger combo frenzy")
        sendHelpLine(player, "/sheepmerge reload", "reload plugin configuration values live")
        sendHelpLine(player, "/sheepmerge liveupdate", "check staged update status and live-update controls")
        sendHelpLine(player, "/sheepmerge world", "travel to the shared farm build world")
        sendHelpLine(player, "/sheepmerge world save|load", "save/load shared farm chunks for all farms")
        sendHelpLine(player, "/sheepmerge resetdata [player]", "admin reset a player")
        sendHelpLine(player, "/sheepmerge stats [player]", "admin stats view")
        sendHelpLine(player, "/sheepmerge checkpoints [player]", "admin coin check")
        sendHelpLine(player, "/sheepmerge checkquestpoints [player]", "admin quest points check")
        sendHelpLine(player, "/sheepmerge checkprestige [player]", "admin prestige check")
        sendHelpLine(player, "/sheepmerge givepoints <amount> [player]", "admin give Coins")
        sendHelpLine(player, "/sheepmerge giveautomationpoints <amount> [player]", "admin give automation points")
        sendHelpLine(player, "/sheepmerge givesacrificepoints <amount> [player]", "admin give sacrifice points")
        sendHelpLine(player, "/sheepmerge setpoints <amount> [player]", "admin set Coins")
        sendHelpLine(player, "/sheepmerge givequestpoints <amount> [player]", "admin give quest points")
        sendHelpLine(player, "/sheepmerge setquestpoints <amount> [player]", "admin set quest points")
        sendHelpLine(player, "/sheepmerge setprestige <level> [player]", "admin set prestige level")
        sendHelpLine(player, "/sheepmerge completeachievement <id> [player]", "admin complete one achievement by id")
        sendHelpLine(player, "/sheepmerge completeallachievements [player]", "admin complete all achievements")
        sendHelpLine(player, "/sheepmerge backup", "create a permanent compressed backup")
        sendHelpLine(player, "/sheepmerge backup delete <file>", "mark a backup for deletion (removed on a future restart after 24h)")
        sendHelpLine(player, "/sheepmerge backup recover <file>", "unmark a backup that was marked for deletion")
        sendHelpLine(player, "/sheepmerge backup load <file>", "restore backup data and create a post-load permanent backup")

        if (topic.isNullOrBlank()) {
            return
        }
        when {
            topic.equals("visit", ignoreCase = true) -> {
                sendHelpHeading(player, "Visit hints:")
                sendHelpLine(player, "/sheepmerge visit <player>", "visit another open farm")
                sendHelpLine(player, "/sheepmerge visit -toggle", "toggle your farm visit access")
                sendHelpLine(player, "/sheepmerge visit -toggle <player>", "operator toggle for another player")
            }
            topic.equals("leaderboard", ignoreCase = true) -> {
                sendHelpHeading(player, "Leaderboard hints:")
                sendHelpLine(player, "/sheepmerge leaderboard", "move the leaderboard display to your position")
                sendHelpLine(player, "/sheepmerge leaderboard <x> <y> <z> [world]", "move the leaderboard to explicit coordinates")
                sendHelpLine(player, "/sheepmerge leaderboard remove", "remove the leaderboard display and clear the saved location")
            }
            topic.equals("top", ignoreCase = true) -> {
                sendHelpHeading(player, "Top hints:")
                sendHelpLine(player, "/sheepmerge top", "view top players by sheep merge Coins")
            }
            topic.equals("world", ignoreCase = true) -> {
                sendHelpHeading(player, "World hints:")
                sendHelpLine(player, "/sheepmerge world", "travel to the shared farm build world")
                sendHelpLine(player, "/sheepmerge world save", "save the shared farm build world")
                sendHelpLine(player, "/sheepmerge world load", "commit the shared build world into all loaded farm worlds")
            }
            topic.equals("backup", ignoreCase = true) -> {
                sendHelpHeading(player, "Backup hints:")
                sendHelpLine(player, "/sheepmerge backup", "create a permanent compressed backup now")
                sendHelpLine(player, "/sheepmerge backup list", "show recent backup archive names")
                sendHelpLine(player, "/sheepmerge backup delete <file>", "mark one backup for deferred deletion")
                sendHelpLine(player, "/sheepmerge backup recover <file>", "cancel deferred deletion mark")
                sendHelpLine(player, "/sheepmerge backup load <file>", "restore one backup archive")
            }
            topic.equals("liveupdate", ignoreCase = true) -> {
                sendHelpHeading(player, "Live update hints:")
                sendHelpLine(player, "/sheepmerge liveupdate status", "show current live-update toggle and staged release status")
                sendHelpLine(player, "/sheepmerge liveupdate check", "query GitHub Releases and stage the latest update")
                sendHelpLine(player, "/sheepmerge liveupdate apply", "apply a staged live-safe migration manifest now")
                sendHelpLine(player, "/sheepmerge liveupdate on|off", "enable or disable automatic live update checks")
            }
            topic.equals("summon", ignoreCase = true) -> {
                sendHelpHeading(player, "Summon hints:")
                sendHelpLine(player, "/sheepmerge summon", "spawn a sheep using normal egg tier roll logic")
                sendHelpLine(player, "/sheepmerge summon <tier>", "spawn an exact tier (0+; ${SheepTier.RAINBOW.level + 1}+ means higher rainbow tiers)")
            }
            topic.equals("completeachievement", ignoreCase = true) ||
                topic.equals("completeallachievements", ignoreCase = true) -> {
                sendHelpHeading(player, "Achievement admin hints:")
                sendHelpLine(player, "/sheepmerge completeachievement <id> [player]", "complete one achievement by id")
                sendHelpLine(player, "/sheepmerge completeachievement all [player]", "complete all achievements (alias)")
                sendHelpLine(player, "/sheepmerge completeallachievements [player]", "complete all achievements")
            }
            topic.equals("resetdata", ignoreCase = true) -> {
                sendHelpHeading(player, "Reset hints:")
                sendHelpLine(player, "/sheepmerge resetdata", "reset your own data")
                sendHelpLine(player, "/sheepmerge resetdata <player>", "reset a specific online player")
            }
            ADMIN_STAT_TOPICS.any { it.equals(topic, ignoreCase = true) } -> {
                sendHelpHeading(player, "Admin stat check hints:")
                sendHelpLine(player, "/sheepmerge $topic", "inspect your own stats if you are checking yourself")
                sendHelpLine(player, "/sheepmerge $topic <player>", "inspect another online player")
            }
            ADMIN_VALUE_TOPICS.any { it.equals(topic, ignoreCase = true) } -> {
                sendHelpHeading(player, "Admin value hints:")
                val amountLabel = if (topic.equals("setprestige", ignoreCase = true)) "<level>" else "<amount>"
                sendHelpLine(player, "/sheepmerge $topic $amountLabel", "affect your own account if you are the target")
                sendHelpLine(player, "/sheepmerge $topic $amountLabel [player]", "affect a specific online player")
            }
        }
    }

    private fun sendHelpHeading(player: Player, text: String) {
        player.sendMessage(ChatColor.DARK_AQUA.toString() + text)
    }

    private fun sendHelpLine(player: Player, command: String, description: String) {
        player.sendMessage(ChatColor.GRAY.toString() + "- " + SheepCommandPresentation.label(command) + ": " + description)
    }

    private fun sendInvalidCommandMessage(player: Player, args: Array<String>) {
        val root = args[0].lowercase(Locale.ROOT)
        when (root) {
            "leaderboard" -> invalidWithHelp(player, "Invalid leaderboard command. Use /sheepmerge leaderboard, /sheepmerge leaderboard remove, or /sheepmerge leaderboard <x> <y> <z> [world].", "leaderboard")
            "topdisplay" -> invalidWithHelp(player, "/sheepmerge topdisplay was removed. Use /sheepmerge leaderboard instead.", "leaderboard")
            "world" -> invalidWithHelp(player, "Invalid world command. Use /sheepmerge world, /sheepmerge world save, or /sheepmerge world load.", "world")
            "backup" -> invalidWithHelp(player, "Invalid backup command. Use /sheepmerge backup, /sheepmerge backup list, /sheepmerge backup load <file>, /sheepmerge backup delete <file>, or /sheepmerge backup recover <file>.", "backup")
            "mapsave" -> invalidWithHelp(player, "/sheepmerge mapsave was removed. Use /sheepmerge world save instead.", "world")
            "mapload" -> invalidWithHelp(player, "/sheepmerge mapload was removed. Use /sheepmerge world load instead.", "world")
            "layout" -> invalidWithHelp(player, "/sheepmerge layout was removed. Use /sheepmerge world save|load instead.", "world")
            "visit" -> invalidWithHelp(player, "Invalid visit command. Use /sheepmerge visit <player> or /sheepmerge visit -toggle [player].", "visit")
            "tutorial" -> player.sendMessage(SheepCommandPresentation.error("/sheepmerge tutorial was removed. The tutorial starts automatically until you unlock your farm."))
            in ADMIN_SYNTAX_ROOTS -> sendInvalidAdminSyntax(player, root)
            else -> {
                if (SheepCommandTabCompletion.isAdminAchievementSubcommand(root)) {
                    sendInvalidAdminSyntax(player, root)
                    return
                }
                player.sendMessage(SheepCommandPresentation.error("Unknown SheepMerge command: /sheepmerge ${args.joinToString(" ")}"))
                player.sendMessage(ChatColor.GRAY.toString() + "Use /sheepmerge help or /sheepmerge -help for command hints.")
            }
        }
    }

    private fun invalidWithHelp(player: Player, message: String, topic: String) {
        player.sendMessage(SheepCommandPresentation.error(message))
        sendCommandHelp(player, topic)
    }

    private fun sendInvalidAdminSyntax(player: Player, root: String) {
        player.sendMessage(SheepCommandPresentation.error("Invalid admin command syntax for /sheepmerge $root. Use /sheepmerge help -help for command hints."))
        sendCommandHelp(player, root)
    }

    private fun shouldBlockTutorialCommand(player: Player, args: Array<String>): Boolean {
        if (!SheepMergeManager.shouldRestrictTutorialActions(player)) {
            return false
        }
        return when (args.firstOrNull()?.lowercase(Locale.ROOT)) {
            "upgrade" -> SheepMergeManager.blockTutorialAction(player, SheepMergeManager.TutorialAction.OPEN_UPGRADE_COMMAND, "open upgrades now")
            "shop" -> SheepMergeManager.blockTutorialAction(player, SheepMergeManager.TutorialAction.OPEN_SHOP_COMMAND, "open the shear shop now")
            "prestige" -> SheepMergeManager.blockTutorialAction(player, SheepMergeManager.TutorialAction.OPEN_PRESTIGE_COMMAND, "open prestige now")
            else -> SheepMergeManager.blockTutorialAction(player, SheepMergeManager.TutorialAction.OTHER_COMMAND, "use that command")
        }
    }

    companion object {
        private val ADMIN_STAT_TOPICS = setOf("stats", "checkpoints", "checkquestpoints", "checkprestige")
        private val ADMIN_VALUE_TOPICS = setOf("givepoints", "giveautomationpoints", "setpoints", "givequestpoints", "setquestpoints", "setprestige")
        private val ADMIN_SYNTAX_ROOTS = setOf(
            "resetdata", "stats", "checkpoints", "checkquestpoints", "checkprestige", "givepoints",
            "setpoints", "givequestpoints", "setquestpoints", "givesacrificepoints", "reload", "liveupdate",
            "setprestige", "summon"
        )

        private fun isHelpFlag(value: String?): Boolean {
            return value != null && (value.equals("help", ignoreCase = true) || value.equals("-help", ignoreCase = true))
        }

        @JvmStatic
        fun getWorldName(playerId: UUID): String = FarmWorldLifecycle.getWorldName(playerId)

        @JvmStatic
        fun ensureFarmWorld(worldName: String): World? = FarmWorldLifecycle.ensureFarmWorld(worldName)

        @JvmStatic
        fun ensureFarmWorldAsync(worldName: String, callback: Consumer<World?>) {
            FarmWorldLifecycle.ensureFarmWorldAsync(worldName, callback)
        }

        @JvmStatic
        fun ensureFarmBuildWorld(): World? = FarmWorldLifecycle.ensureFarmBuildWorld()

        @JvmStatic
        fun invalidateManagedWorldInitialization(worldName: String) {
            FarmWorldLifecycle.invalidateManagedWorldInitialization(worldName)
        }

        @JvmStatic
        fun applyFarmRulesToLoadedWorlds() {
            FarmWorldLifecycle.applyFarmRulesToLoadedWorlds()
        }

        @JvmStatic
        fun ensureTutorialWorld(playerId: UUID): World? = FarmWorldLifecycle.ensureTutorialWorld(playerId)

        @JvmStatic
        fun teleportToFarmWorld(player: Player): Boolean = FarmWorldLifecycle.teleportToFarmWorld(player)

        @JvmStatic
        fun teleportToTutorialWorld(player: Player): Boolean = FarmWorldLifecycle.teleportToTutorialWorld(player)
    }
}
