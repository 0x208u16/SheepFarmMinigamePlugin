package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.math.BigInteger

internal object SheepAdminCommandHandlers {
    @JvmStatic
    fun handleStatsCommand(player: Player, args: Array<String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("stats", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val target = resolveTargetPlayer(player, args, 1)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            SheepCommandPresentation.sendDetailedStats(player, target, "Admin Stats")
            return true
        }
        return false
    }

    @JvmStatic
    fun handleCheckpointsCommand(player: Player, args: Array<String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("checkpoints", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val target = resolveTargetPlayer(player, args, 1)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            SheepCommandPresentation.sendDetailedStats(player, target, "Coins")
            return true
        }
        return false
    }

    @JvmStatic
    fun handleCheckQuestPointsCommand(player: Player, args: Array<String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("checkquestpoints", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val target = resolveTargetPlayer(player, args, 1)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            SheepCommandPresentation.sendDetailedStats(player, target, "Check Quest Points")
            return true
        }
        return false
    }

    @JvmStatic
    fun handleCheckPrestigeCommand(player: Player, args: Array<String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("checkprestige", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val target = resolveTargetPlayer(player, args, 1)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            SheepCommandPresentation.sendDetailedStats(player, target, "Check Prestige")
            return true
        }
        return false
    }

    @JvmStatic
    fun handleBackupCommand(player: Player, args: Array<String>): Boolean {
        if (args.isEmpty() || !args[0].equals("backup", ignoreCase = true)) {
            return false
        }
        if (!player.isOp) {
            player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
            return true
        }

        if (args.size == 1 || (args.size == 2 && args[1].equals("create", ignoreCase = true))) {
            val created = SheepMergeManager.createManualBackup()
            if (created == null) {
                player.sendMessage(SheepCommandPresentation.error("Unable to create backup."))
                return true
            }
            player.sendMessage(
                SheepCommandPresentation.adminHeader("Backup") + " " +
                    SheepCommandPresentation.value("Created ") + SheepCommandPresentation.label(created.name)
            )
            return true
        }

        if (args.size == 2 && args[1].equals("list", ignoreCase = true)) {
            val backups = SheepMergeManager.listBackups()
            if (backups.isEmpty()) {
                player.sendMessage(SheepCommandPresentation.error("No backups found."))
                return true
            }
            player.sendMessage(
                SheepCommandPresentation.adminHeader("Backups") + " " +
                    SheepCommandPresentation.value("Available archives:")
            )
            for (index in 0 until minOf(15, backups.size)) {
                val backupName = backups[index]
                val marked = SheepMergeManager.isBackupMarkedForDeletion(backupName)
                player.sendMessage(
                    ChatColor.GRAY.toString() + "- " + backupName +
                        if (marked) ChatColor.RED.toString() + " [temporary: queued for deletion]" else ""
                )
            }
            return true
        }

        if (args.size >= 3 && args[1].equals("load", ignoreCase = true)) {
            val backupName = args.copyOfRange(2, args.size).joinToString(" ")
            val postLoadBackup = SheepMergeManager.loadBackup(backupName)
            if (postLoadBackup == null) {
                player.sendMessage(SheepCommandPresentation.error("Unable to load backup '$backupName'."))
                return true
            }
            player.sendMessage(
                SheepCommandPresentation.adminHeader("Backup") + " " +
                    SheepCommandPresentation.value("Loaded backup '") + SheepCommandPresentation.label(backupName) +
                    SheepCommandPresentation.value("'.")
            )
            player.sendMessage(
                SheepCommandPresentation.adminHeader("Backup") + " " +
                    SheepCommandPresentation.value("Created post-load permanent backup '") +
                    SheepCommandPresentation.label(postLoadBackup.name) + SheepCommandPresentation.value("'.")
            )
            return true
        }

        if (args.size >= 3 && args[1].equals("delete", ignoreCase = true)) {
            val backupName = args.copyOfRange(2, args.size).joinToString(" ")
            if (!SheepMergeManager.markBackupForDeletion(backupName)) {
                player.sendMessage(
                    SheepCommandPresentation.error("Unable to mark backup '$backupName' for deletion.")
                )
                return true
            }
            player.sendMessage(
                SheepCommandPresentation.adminHeader("Backup") + " " +
                    SheepCommandPresentation.value("Marked '") + SheepCommandPresentation.label(backupName) +
                    SheepCommandPresentation.value("' for deletion after 24h on a future restart.")
            )
            return true
        }

        if (args.size >= 3 && args[1].equals("recover", ignoreCase = true)) {
            val backupName = args.copyOfRange(2, args.size).joinToString(" ")
            if (!SheepMergeManager.recoverBackupMarkedForDeletion(backupName)) {
                player.sendMessage(
                    SheepCommandPresentation.error("Backup '$backupName' is not marked for deletion.")
                )
                return true
            }
            player.sendMessage(
                SheepCommandPresentation.adminHeader("Backup") + " " +
                    SheepCommandPresentation.value("Recovered '") + SheepCommandPresentation.label(backupName) +
                    SheepCommandPresentation.value("' from deletion queue.")
            )
            return true
        }

        player.sendMessage(
            SheepCommandPresentation.error("Usage: /sheepmerge backup [create|list|load|delete|recover <file>].")
        )
        return true
    }

    @JvmStatic
    fun handleResetDataCommand(player: Player, args: Array<String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("resetdata", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage("Only server operators can use this command.")
                return true
            }
            var target = player
            if (args.size >= 2) {
                Bukkit.getPlayerExact(args[1])?.let { target = it }
            }
            SheepMergeManager.adminResetPlayer(target)
            player.sendMessage("Reset data for ${target.name}.")
            SheepMergeManager.startTutorial(target, false)
            return true
        }
        return false
    }

    @JvmStatic
    fun handleGivePointsCommand(player: Player, args: Array<String>): Boolean {
        if (args.size >= 2 && args[0].equals("givepoints", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val amount = parseLong(player, args[1], "Invalid amount. Usage: /sheepmerge givepoints <amount> [player]")
                ?: return true
            val target = resolveTargetPlayer(player, args, 2)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            val previous = SheepMergeManager.getPlayerPoints(target)
            SheepMergeManager.adminGivePoints(target, amount)
            val updated = SheepMergeManager.getPlayerPoints(target)
            SheepMergeManager.updatePointsScoreboard(target)
            player.sendMessage(
                SheepCommandPresentation.statUpdateMessage("Coins Updated", target, "Coins", previous, updated)
            )
            return true
        }
        return false
    }

    @JvmStatic
    fun handleSetPointsCommand(player: Player, args: Array<String>): Boolean {
        if (args.size >= 2 && args[0].equals("setpoints", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val amount = parseLong(player, args[1], "Invalid amount. Usage: /sheepmerge setpoints <amount> [player]")
                ?: return true
            val target = resolveTargetPlayer(player, args, 2)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            val previous = SheepMergeManager.getPlayerPoints(target)
            SheepMergeManager.adminSetPoints(target, amount)
            val updated = SheepMergeManager.getPlayerPoints(target)
            SheepMergeManager.updatePointsScoreboard(target)
            player.sendMessage(
                SheepCommandPresentation.statUpdateMessage("Coins Updated", target, "Coins", previous, updated)
            )
            return true
        }
        return false
    }

    @JvmStatic
    fun handleGiveAutomationPointsCommand(player: Player, args: Array<String>): Boolean {
        if (args.size >= 2 && args[0].equals("giveautomationpoints", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val amount = parseInt(
                player,
                args[1],
                "Invalid amount. Usage: /sheepmerge giveautomationpoints <amount> [player]"
            ) ?: return true
            val target = resolveTargetPlayer(player, args, 2)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            val previous = SheepMergeManager.getAutomationPoints(target)
            SheepMergeManager.adminGiveAutomationPoints(target, amount)
            val updated = SheepMergeManager.getAutomationPoints(target)
            player.sendMessage(
                SheepCommandPresentation.statUpdateMessage(
                    "Automation Points Updated",
                    target,
                    "Automation Points",
                    previous.toLong(),
                    updated.toLong()
                )
            )
            return true
        }
        return false
    }

    @JvmStatic
    fun handleGiveQuestPointsCommand(player: Player, args: Array<String>): Boolean {
        if (args.size >= 2 && args[0].equals("givequestpoints", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val amount = parseInt(
                player,
                args[1],
                "Invalid amount. Usage: /sheepmerge givequestpoints <amount> [player]"
            ) ?: return true
            val target = resolveTargetPlayer(player, args, 2)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            val previous = SheepMergeManager.getQuestPoints(target)
            SheepMergeManager.adminGiveQuestPoints(target, amount)
            val updated = SheepMergeManager.getQuestPoints(target)
            player.sendMessage(
                SheepCommandPresentation.statUpdateMessage(
                    "Quest Points Updated",
                    target,
                    "Quest Points",
                    previous.toLong(),
                    updated.toLong()
                )
            )
            return true
        }
        return false
    }

    @JvmStatic
    fun handleSetQuestPointsCommand(player: Player, args: Array<String>): Boolean {
        if (args.size >= 2 && args[0].equals("setquestpoints", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val amount = parseInt(
                player,
                args[1],
                "Invalid amount. Usage: /sheepmerge setquestpoints <amount> [player]"
            ) ?: return true
            val target = resolveTargetPlayer(player, args, 2)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            val previous = SheepMergeManager.getQuestPoints(target)
            SheepMergeManager.adminSetQuestPoints(target, amount)
            val updated = SheepMergeManager.getQuestPoints(target)
            player.sendMessage(
                SheepCommandPresentation.statUpdateMessage(
                    "Quest Points Updated",
                    target,
                    "Quest Points",
                    previous.toLong(),
                    updated.toLong()
                )
            )
            return true
        }
        return false
    }

    @JvmStatic
    fun handleSetPrestigeCommand(player: Player, args: Array<String>): Boolean {
        if (args.size >= 2 && args[0].equals("setprestige", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val level = parseInt(
                player,
                args[1],
                "Invalid level. Usage: /sheepmerge setprestige <level> [player]"
            ) ?: return true
            val target = resolveTargetPlayer(player, args, 2)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            val previous = SheepMergeManager.getPrestigeLevel(target)
            if (!SheepMergeManager.adminSetPrestigeLevel(target, level)) {
                player.sendMessage(
                    SheepCommandPresentation.error("Invalid prestige level. Use a value of 0 or higher (unlimited).")
                )
                return true
            }
            val updated = SheepMergeManager.getPrestigeLevel(target)
            SheepMergeManager.updatePointsScoreboard(target)
            player.sendMessage(
                SheepCommandPresentation.statUpdateMessage(
                    "Prestige Updated",
                    target,
                    "Prestige",
                    previous.toLong(),
                    updated.toLong()
                )
            )
            return true
        }
        return false
    }

    @JvmStatic
    fun handleGiveSacrificePointsCommand(player: Player, args: Array<String>): Boolean {
        if (args.size >= 2 && args[0].equals("givesacrificepoints", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val amount = try {
                BigInteger(args[1])
            } catch (_: NumberFormatException) {
                player.sendMessage(
                    SheepCommandPresentation.error(
                        "Invalid amount. Usage: /sheepmerge givesacrificepoints <amount> [player]"
                    )
                )
                return true
            }
            val target = resolveTargetPlayer(player, args, 2)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            val previous = SheepMergeManager.getSacrificePoints(target)
            SheepMergeManager.adminGiveSacrificePoints(target, amount)
            val updated = SheepMergeManager.getSacrificePoints(target)
            SheepMergeManager.updatePointsScoreboard(target)
            player.sendMessage(
                SheepCommandPresentation.bigIntegerStatUpdateMessage(
                    "Sacrifice Points Updated",
                    target,
                    "Sacrifice Points",
                    previous,
                    updated
                )
            )
            return true
        }
        return false
    }

    @JvmStatic
    fun handleCompleteAchievementCommand(player: Player, args: Array<String>): Boolean {
        if (args.size >= 2 && args[0].equals("completeachievement", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            if (isHelpFlag(args[1])) {
                player.sendMessage(
                    SheepCommandPresentation.error("Usage: /sheepmerge completeachievement <id|all> [player]")
                )
                return true
            }
            val target = resolveTargetPlayer(player, args, 2)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }

            if (args[1].equals("all", ignoreCase = true)) {
                val unlockedNow = SheepMergeManager.adminCompleteAllAchievements(target, true)
                SheepMergeManager.updatePointsScoreboard(target)
                val total = SheepMergeManager.getAchievementIds().size
                sendAllAchievementsResult(player, target, unlockedNow, total)
                return true
            }

            val alreadyUnlocked = SheepMergeManager.isAchievementUnlocked(target, args[1])
            if (!SheepMergeManager.adminCompleteAchievement(target, args[1], true)) {
                player.sendMessage(SheepCommandPresentation.error("Unknown achievement id: ${args[1]}."))
                return true
            }
            SheepMergeManager.updatePointsScoreboard(target)
            var achievementName = SheepMergeManager.getAchievementDisplayName(args[1])
            if (achievementName.isNullOrBlank()) {
                achievementName = args[1]
            }
            if (alreadyUnlocked) {
                player.sendMessage(
                    SheepCommandPresentation.adminHeader("Achievements") + " " +
                        SheepCommandPresentation.label("Player") + ": " +
                        SheepCommandPresentation.value(target.name) + ChatColor.DARK_GRAY + " | " +
                        SheepCommandPresentation.label("Achievement") + ": " +
                        SheepCommandPresentation.value(achievementName) +
                        ChatColor.GRAY + " was already unlocked."
                )
            } else {
                player.sendMessage(
                    SheepCommandPresentation.adminHeader("Achievements") + " " +
                        SheepCommandPresentation.label("Player") + ": " +
                        SheepCommandPresentation.value(target.name) + ChatColor.DARK_GRAY + " | " +
                        SheepCommandPresentation.label("Completed") + ": " +
                        SheepCommandPresentation.value(achievementName)
                )
            }
            return true
        }
        return false
    }

    @JvmStatic
    fun handleCompleteAllAchievementsCommand(player: Player, args: Array<String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("completeallachievements", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            if (args.size > 2 || (args.size == 2 && isHelpFlag(args[1]))) {
                player.sendMessage(
                    SheepCommandPresentation.error("Usage: /sheepmerge completeallachievements [player]")
                )
                return true
            }
            val target = resolveTargetPlayer(player, args, 1)
            if (target == null) {
                player.sendMessage(SheepCommandPresentation.error("That player is not online."))
                return true
            }
            val unlockedNow = SheepMergeManager.adminCompleteAllAchievements(target, true)
            SheepMergeManager.updatePointsScoreboard(target)
            val total = SheepMergeManager.getAchievementIds().size
            sendAllAchievementsResult(player, target, unlockedNow, total)
            return true
        }
        return false
    }

    @JvmStatic
    fun resolveTargetPlayer(sender: Player, args: Array<String>, playerArgIndex: Int): Player? {
        if (args.size <= playerArgIndex) {
            return sender
        }
        return Bukkit.getPlayerExact(args[playerArgIndex])
    }

    private fun parseLong(player: Player, value: String, errorMessage: String): Long? {
        return try {
            value.toLong()
        } catch (_: NumberFormatException) {
            player.sendMessage(SheepCommandPresentation.error(errorMessage))
            null
        }
    }

    private fun parseInt(player: Player, value: String, errorMessage: String): Int? {
        return try {
            value.toInt()
        } catch (_: NumberFormatException) {
            player.sendMessage(SheepCommandPresentation.error(errorMessage))
            null
        }
    }

    private fun isHelpFlag(value: String?): Boolean {
        return value != null && (value.equals("help", ignoreCase = true) || value.equals("-help", ignoreCase = true))
    }

    private fun sendAllAchievementsResult(player: Player, target: Player, unlockedNow: Int, total: Int) {
        player.sendMessage(
            SheepCommandPresentation.adminHeader("Achievements") + " " +
                SheepCommandPresentation.label("Player") + ": " + SheepCommandPresentation.value(target.name) +
                ChatColor.DARK_GRAY + " | " + SheepCommandPresentation.label("Unlocked") + ": " +
                SheepCommandPresentation.value(unlockedNow.toString()) +
                ChatColor.GRAY + " newly completed ($total total available)."
        )
    }
}