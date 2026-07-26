package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import java.util.Locale
import java.util.function.Consumer
import kotlin.math.max

internal object SheepGameplayCommandHandlers {
    @JvmStatic
    fun handleUpgradeCommand(player: Player, args: Array<String>): Boolean {
        if (args.size == 1 && args[0].equals("upgrade", ignoreCase = true)) {
            SheepMergeManager.openUpgradeMenu(player)
            return true
        }
        return false
    }

    @JvmStatic
    fun handleShopCommand(player: Player, args: Array<String>): Boolean {
        if (args.size == 1 && args[0].equals("shop", ignoreCase = true)) {
            SheepMergeManager.openShopMenu(player)
            return true
        }
        return false
    }

    @JvmStatic
    fun handleTopCommand(player: Player, args: Array<String>): Boolean {
        if (args.isEmpty() || !args[0].equals("top", ignoreCase = true)) return false

        val pageSize = 10
        var page = 1
        if (args.size >= 2) {
            if (isHelpFlag(args[1])) {
                player.sendMessage(SheepCommandPresentation.error("Usage: /sheepmerge top [page]"))
                return true
            }
            page = try {
                args[1].toInt()
            } catch (_: NumberFormatException) {
                player.sendMessage(SheepCommandPresentation.error("Invalid page number. Usage: /sheepmerge top [page]"))
                return true
            }
        }
        if (args.size > 2) {
            player.sendMessage(SheepCommandPresentation.error("Usage: /sheepmerge top [page]"))
            return true
        }
        if (page < 1) {
            player.sendMessage(SheepCommandPresentation.error("Page number must be 1 or higher."))
            return true
        }

        val totalPages = SheepMergeManager.getTopPointsPageCount(pageSize)
        if (page > totalPages) {
            player.sendMessage(SheepCommandPresentation.error("Page out of range. Available pages: 1-$totalPages."))
            return true
        }

        player.sendMessage(
            SheepCommandPresentation.adminHeader("Top Players") + ChatColor.DARK_GRAY + " (" +
                ChatColor.GRAY + "Page $page/$totalPages" + ChatColor.DARK_GRAY + ")"
        )
        SheepMergeManager.getTopPointsLines(pageSize, page).forEach { line ->
            player.sendMessage(ChatColor.GRAY.toString() + line)
        }
        return true
    }

    @JvmStatic
    fun handlePrestigeCommand(player: Player, args: Array<String>): Boolean {
        if (args.size == 1 && args[0].equals("prestige", ignoreCase = true)) {
            SheepMergeManager.openPrestigeMenu(player)
            return true
        }
        return false
    }

    @JvmStatic
    fun handleStatusCommand(player: Player, args: Array<String>): Boolean {
        if (args.size == 1 && args[0].equals("status", ignoreCase = true)) {
            SheepCommandPresentation.sendDetailedStats(player, player, "Status")
            return true
        }
        return false
    }

    @JvmStatic
    fun handleVisitCommand(player: Player, args: Array<String>): Boolean {
        if (args.isEmpty() || !args[0].equals("visit", ignoreCase = true)) return false
        if (args.size >= 2 && args[1].equals("-toggle", ignoreCase = true)) {
            var target = player
            if (args.size >= 3) {
                if (!player.isOp) {
                    player.sendMessage("Only server operators can toggle another player's farm access.")
                    return true
                }
                target = SheepAdminCommandHandlers.resolveTargetPlayer(player, args, 2) ?: run {
                    player.sendMessage("That player is not online.")
                    return true
                }
            }

            val nowOpen = SheepMergeManager.toggleFarmVisitable(target)
            player.sendMessage(target.name + "'s farm is now " + (if (nowOpen) "open" else "closed") + " to visitors.")
            return true
        }
        if (args.size < 2) {
            player.sendMessage("Usage: /sheepmerge visit <player> or /sheepmerge visit -toggle [player]")
            return true
        }

        val owner = Bukkit.getPlayerExact(args[1])
        if (owner == null) {
            player.sendMessage("That player is not online.")
            return true
        }
        val ownerId = owner.uniqueId
        if (!player.isOp && ownerId != player.uniqueId && !SheepMergeManager.isFarmVisitable(ownerId)) {
            player.sendMessage("That farm is closed to visitors.")
            return true
        }
        if (!player.isOp && SheepMergeManager.isFarmVisitorBlocked(ownerId, player.uniqueId)) {
            player.sendMessage("That farm has blocked your visits.")
            return true
        }

        val ownerWorldName = FarmWorldLifecycle.getWorldName(ownerId)
        if (!FarmWorldLifecycle.beginFarmLoadForPlayer(player)) {
            player.sendMessage("A farm is already loading for you. Please wait.")
            return true
        }
        player.sendMessage("Loading ${owner.name}'s sheep farm...")
        FarmWorldLifecycle.ensureFarmWorldAsync(ownerWorldName) { ownerWorld ->
            val playerId = player.uniqueId
            if (ownerWorld == null) {
                if (player.isOnline) player.sendMessage("Unable to open that farm world right now.")
                FarmWorldLifecycle.finishFarmLoadForPlayer(playerId, false)
                return@ensureFarmWorldAsync
            }
            if (!player.isOnline) {
                FarmWorldLifecycle.finishFarmLoadForPlayer(playerId, false)
                return@ensureFarmWorldAsync
            }

            FarmWorldLifecycle.applyConfiguredSpawn(ownerWorld)
            FarmWorldLifecycle.teleportPlayerToConfiguredSpawnAsync(
                player,
                ownerWorld,
                {
                    SheepMergeManager.recordVisitedOtherFarm(player, ownerId)
                    player.sendMessage("You were teleported to ${owner.name}'s sheep farm.")
                    player.sendMessage("Use /sheepmerge to return to your own farm.")
                    val plugin = SheepMergePlugin.instance
                    if (plugin != null) {
                        Bukkit.getScheduler().runTaskLater(
                            plugin,
                            Runnable { SheepMergeManager.updateVisitFarmBossBar(player) },
                            2L
                        )
                    }
                    player.sendTitle(
                        SheepMergeManager.color("&eVisiting ${owner.name}"),
                        SheepMergeManager.color("&7Use /sheepmerge to return home"),
                        10,
                        60,
                        10
                    )
                },
                "Unable to teleport to that farm world right now.",
                { FarmWorldLifecycle.finishFarmLoadForPlayer(playerId, false) }
            )
        }
        return true
    }

    @JvmStatic
    fun handleKickCommand(player: Player, args: Array<String>): Boolean {
        if (args.isEmpty() || !args[0].equals("kick", ignoreCase = true)) return false
        if (!SheepMergeManager.isSheepFarmWorld(player.world) || !SheepMergeManager.isFarmOwner(player, player.world)) {
            player.sendMessage("You can only use this in your own sheep farm world.")
            return true
        }
        if (args.size < 2) {
            player.sendMessage("Usage: /sheepmerge kick <player>")
            return true
        }

        val target = Bukkit.getPlayerExact(args[1])
        if (target == null || target.world != player.world) {
            player.sendMessage("That player is not in your farm right now.")
            return true
        }
        if (target == player) {
            player.sendMessage("You cannot kick yourself.")
            return true
        }
        if (target.isOp) {
            player.sendMessage("You cannot kick operators from your farm.")
            return true
        }

        val fallbackWorld = Bukkit.getWorlds().firstOrNull()
        if (fallbackWorld == null) {
            player.sendMessage("No safe world is available to move that player.")
            return true
        }
        val spawn = fallbackWorld.spawnLocation.clone().add(0.5, 0.0, 0.5)
        target.teleport(spawn)
        target.sendMessage("You were removed from ${player.name}'s sheep farm.")
        player.sendMessage("You removed ${target.name} from your farm.")
        return true
    }

    @JvmStatic
    fun handleLeaderboardCommand(player: Player, args: Array<String>): Boolean {
        if (args.size == 1 && args[0].equals("leaderboard", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage("Only server operators can use this command.")
                return true
            }
            val createdOrMoved = SheepMergeManager.spawnOrMoveTopPointsDisplay(player)
            player.sendMessage(if (createdOrMoved) "Leaderboard moved to your position." else "Unable to move leaderboard right now.")
            return true
        }
        if (args.size >= 2 && args[0].equals("leaderboard", ignoreCase = true) &&
            args[1].equals("remove", ignoreCase = true)
        ) {
            if (!player.isOp) {
                player.sendMessage("Only server operators can use this command.")
                return true
            }
            val removed = SheepMergeManager.removeTopPointsDisplay()
            player.sendMessage(if (removed) "Leaderboard removed." else "No leaderboard display was found.")
            return true
        }
        if ((args.size == 4 || args.size == 5) && args[0].equals("leaderboard", ignoreCase = true) &&
            isCoordinate(args[1]) && isCoordinate(args[2]) && isCoordinate(args[3])
        ) {
            if (!player.isOp) {
                player.sendMessage("Only server operators can use this command.")
                return true
            }
            var targetWorld = player.world
            if (args.size == 5) {
                targetWorld = Bukkit.getWorld(args[4]) ?: run {
                    player.sendMessage("Unknown world. Usage: /sheepmerge leaderboard <x> <y> <z> [world]")
                    return true
                }
            }
            try {
                val x = args[1].toDouble()
                val y = args[2].toDouble()
                val z = args[3].toDouble()
                val createdOrMoved = SheepMergeManager.spawnOrMoveTopPointsDisplay(Location(targetWorld, x, y, z))
                if (createdOrMoved) {
                    player.sendMessage("Leaderboard moved to coordinates $x, $y, $z in ${targetWorld.name}.")
                } else {
                    player.sendMessage("Unable to move leaderboard right now.")
                }
                return true
            } catch (_: NumberFormatException) {
                player.sendMessage("Invalid coordinates. Usage: /sheepmerge leaderboard <x> <y> <z> [world]")
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun handleStormCommand(player: Player, args: Array<String>): Boolean {
        if (args.size == 1 && args[0].equals("storm", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage("Only server operators can use this command.")
                return true
            }
            player.sendMessage(
                if (SheepMergeManager.triggerSheepStormEvent()) "Sheep storm triggered."
                else "A sheep storm is already active or could not be started."
            )
            return true
        }
        return false
    }

    @JvmStatic
    fun handleSummonCommand(player: Player, args: Array<String>): Boolean {
        if (args.isEmpty() || !args[0].equals("summon", ignoreCase = true)) return false
        if (!player.isOp) {
            player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
            return true
        }
        if (!SheepMergeManager.isSheepFarmWorld(player.world) || SheepMergeManager.isFarmBuildWorld(player.world)) {
            player.sendMessage(SheepCommandPresentation.error("Use this command in a farm or tutorial world (not the build world)."))
            return true
        }
        if (args.size > 2) {
            player.sendMessage(SheepCommandPresentation.error("Usage: /sheepmerge summon [tier]"))
            return true
        }
        if (SheepMergeManager.isWorldAtLimit(player.world)) {
            player.sendMessage(SheepCommandPresentation.error("Farm full. Merge sheep or increase the sheep limit first."))
            return true
        }

        var rainbowTier = 1
        var requestedLevel = -1
        val autoRolled = args.size == 1
        val tier = if (autoRolled) {
            SheepMergeManager.rollSpawnTier(player.world)
        } else {
            requestedLevel = try {
                args[1].toInt()
            } catch (_: NumberFormatException) {
                player.sendMessage(SheepCommandPresentation.error("Invalid tier. Usage: /sheepmerge summon [tier]"))
                return true
            }
            if (requestedLevel < 0) {
                player.sendMessage(SheepCommandPresentation.error("Tier must be 0 or higher."))
                return true
            }
            if (requestedLevel <= SheepTier.RAINBOW.level) {
                SheepTier.byLevel(requestedLevel)
            } else {
                rainbowTier = requestedLevel - SheepTier.RAINBOW.level + 1
                SheepTier.RAINBOW
            }
        }
        if (tier == SheepTier.RAINBOW && requestedLevel <= SheepTier.RAINBOW.level) rainbowTier = 1

        val spawned = player.world.spawn(player.location.clone().add(0.0, 0.15, 0.0), Sheep::class.java)
        SheepMergeManager.setSheepTier(spawned, tier)
        if (tier == SheepTier.RAINBOW) SheepMergeManager.setRainbowTier(spawned, rainbowTier)

        val tierSummary = if (tier == SheepTier.RAINBOW) {
            val effectiveLevel = SheepTier.RAINBOW.level + max(1, rainbowTier) - 1
            tier.displayName + " " + SheepMergeManager.formatRainbowTier(rainbowTier) + " (effective level $effectiveLevel)"
        } else {
            tier.displayName + " (tier level ${tier.level})"
        }
        player.sendMessage(
            SheepCommandPresentation.adminHeader("Summon") + " " + SheepCommandPresentation.value("Spawned ") +
                SheepCommandPresentation.label(tierSummary) +
                if (autoRolled) SheepCommandPresentation.value(" using egg roll logic.")
                else SheepCommandPresentation.value(" from request $requestedLevel.")
        )
        return true
    }

    @JvmStatic
    fun handleComboFrenzyCommand(player: Player, args: Array<String>): Boolean {
        if (args.size == 1 && args[0].equals("combofrenzy", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage("Only server operators can use this command.")
                return true
            }
            player.sendMessage(
                if (SheepMergeManager.triggerComboFrenzyEvent()) "Combo frenzy triggered."
                else "A combo frenzy is already active or could not be started."
            )
            return true
        }
        return false
    }

    @JvmStatic
    fun handleReloadCommand(player: Player, args: Array<String>): Boolean {
        if (args.size == 1 && args[0].equals("reload", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage(SheepCommandPresentation.error("Only server operators can use this command."))
                return true
            }
            val plugin = SheepMergePlugin.instance
            if (plugin == null) {
                player.sendMessage(SheepCommandPresentation.error("Plugin instance not available."))
                return true
            }
            plugin.reloadConfig()
            SheepMergeConfiguration.initialize(plugin)
            SheepMergeManager.applyConfiguration(SheepMergeConfiguration.get())
            player.sendMessage(SheepCommandPresentation.adminHeader("Config") + " " + SheepCommandPresentation.value("Configuration reloaded."))
            return true
        }
        return false
    }

    @JvmStatic
    fun handleLiveUpdateCommand(player: Player, args: Array<String>, sendHelp: Consumer<String>): Boolean {
        if (args.isEmpty() || !args[0].equals("liveupdate", ignoreCase = true)) return false
        if (!player.isOp) {
            player.sendMessage(SheepCommandPresentation.error("Only operators can use live update controls."))
            return true
        }
        if (args.size == 1 || isHelpFlag(args[1]) || args[1].equals("status", ignoreCase = true)) {
            player.sendMessage(SheepCommandPresentation.adminHeader("Live Update Status"))
            SheepMergeManager.getLiveUpdateStatusLines().forEach(player::sendMessage)
            return true
        }

        when (args[1].lowercase(Locale.ROOT)) {
            "on", "enable" -> {
                SheepMergeManager.setLiveUpdateEnabled(true)
                player.sendMessage(SheepMergeManager.action("Live updates enabled."))
            }
            "off", "disable" -> {
                SheepMergeManager.setLiveUpdateEnabled(false)
                player.sendMessage(SheepMergeManager.action("Live updates disabled."))
            }
            "check" -> {
                player.sendMessage(SheepMergeManager.action("Checking GitHub Releases for an update..."))
                LiveUpdateCoordinator.checkForUpdatesNow(player)
            }
            "apply" -> LiveUpdateCoordinator.applyStagedUpdateNow(player)
            else -> {
                player.sendMessage(SheepCommandPresentation.error("Invalid liveupdate command. Use /sheepmerge liveupdate status|check|apply|on|off."))
                sendHelp.accept("liveupdate")
            }
        }
        return true
    }

    @JvmStatic
    fun handleWorldCommand(player: Player, args: Array<String>): Boolean {
        if (args.size == 1 && args[0].equals("world", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage("Only server operators can use this command.")
                return true
            }
            val buildWorld = FarmWorldLifecycle.ensureFarmBuildWorld()
            if (buildWorld == null) {
                player.sendMessage("Unable to open the farm build world right now.")
                return true
            }
            FarmWorldLifecycle.applyConfiguredSpawn(buildWorld)
            player.teleportAsync(FarmWorldLifecycle.getConfiguredFarmTeleportLocation(buildWorld))
            player.sendMessage("Teleported to the shared farm build world.")
            return true
        }
        if (args.size == 2 && args[0].equals("world", ignoreCase = true) && args[1].equals("save", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage("Only server operators can use this command.")
                return true
            }
            if (FarmWorldLifecycle.ensureFarmBuildWorld() == null) {
                player.sendMessage("Unable to open the farm build world right now.")
                return true
            }
            player.sendMessage(
                if (SheepMergeManager.saveBuildWorldToLayoutFile()) "Saved the shared farm build world and layout snapshot."
                else "Saved the shared farm build world, but layout snapshot save failed."
            )
            return true
        }
        if (args.size == 2 && args[0].equals("world", ignoreCase = true) && args[1].equals("load", ignoreCase = true)) {
            if (!player.isOp) {
                player.sendMessage("Only server operators can use this command.")
                return true
            }
            if (FarmWorldLifecycle.ensureFarmBuildWorld() == null) {
                player.sendMessage("Unable to open the farm build world right now.")
                return true
            }
            if (SheepMergeManager.isFarmBuildCommitInProgress()) {
                player.sendMessage("A farm refresh is already in progress.")
                return true
            }
            val updated = SheepMergeManager.startLoadSavedFarmLayoutToBuildAndLoadedFarms(player)
            if (updated < 0) {
                player.sendMessage("Unable to load the saved shared farm layout right now.")
                return true
            }
            if (updated == 0) {
                player.sendMessage("Loaded the saved shared farm layout. No farm worlds are currently loaded.")
                return true
            }
            player.sendMessage("Refreshing $updated loaded farm world(s). Players will be able to return shortly.")
            return true
        }
        return false
    }

    @JvmStatic
    fun isCoordinate(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return try {
            value.toDouble()
            true
        } catch (_: NumberFormatException) {
            false
        }
    }

    private fun isHelpFlag(value: String?): Boolean {
        return value != null && (value.equals("help", ignoreCase = true) || value.equals("-help", ignoreCase = true))
    }
}