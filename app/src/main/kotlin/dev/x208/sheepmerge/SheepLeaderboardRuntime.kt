package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import java.math.BigInteger
import java.util.Locale
import java.util.UUID

object SheepLeaderboardRuntime {
    private const val DISPLAY_MARKER_KEY = "top-points-display"
    private const val DISPLAY_ENTRY_LIMIT = 10
    private const val DISPLAY_HEADER = "Top Sheep Merge Coins"

    @JvmStatic
    fun buildTopPointsText(maxEntries: Int): String {
        val pointsSnapshot = SheepEconomyState.getPointsSnapshot()
        return buildTopPointsText(pointsSnapshot, snapshotPlayerNames(pointsSnapshot.keys), maxEntries)
    }

    private fun buildTopPointsText(
        pointsSnapshot: Map<UUID, BigInteger>,
        nameSnapshot: Map<UUID, String>,
        maxEntries: Int,
    ): String {
        val builder = StringBuilder(DISPLAY_HEADER)
        sortedEntries(pointsSnapshot, nameSnapshot).take(maxEntries.coerceAtLeast(1)).forEach { (playerId, points) ->
            builder.append('\n').append(safeName(playerId, nameSnapshot)).append(": ")
                .append(SheepMergeManager.formatPoints(points))
        }
        if (builder.toString() == DISPLAY_HEADER) builder.append("\nNo scores yet")
        return builder.toString()
    }

    private fun snapshotPlayerNames(playerIds: Collection<UUID>?): Map<UUID, String> {
        val names = HashMap<UUID, String>()
        val plugin = SheepMergeManager.leaderboardPlugin()
        if (plugin?.server == null) return names
        Bukkit.getOnlinePlayers().forEach { online ->
            online.name.takeIf(String::isNotBlank)?.let { names[online.uniqueId] = it }
        }
        playerIds?.forEach { playerId ->
            if (!names.containsKey(playerId)) {
                Bukkit.getOfflinePlayer(playerId).name?.takeIf(String::isNotBlank)?.let { names[playerId] = it }
            }
        }
        return names
    }

    private fun safeName(playerId: UUID?, names: Map<UUID, String>?): String {
        if (playerId == null) return "unknown"
        return names?.get(playerId)?.takeIf(String::isNotBlank) ?: playerId.toString().substring(0, 8)
    }

    private fun sortedEntries(points: Map<UUID, BigInteger>, names: Map<UUID, String>) =
        points.entries.sortedWith { left, right ->
            val pointsCompare = right.value.compareTo(left.value)
            if (pointsCompare != 0) pointsCompare else {
                val nameCompare = safeName(left.key, names).compareTo(safeName(right.key, names), ignoreCase = true)
                if (nameCompare != 0) nameCompare else left.key.compareTo(right.key)
            }
        }

    @JvmStatic
    fun getTopPointsLines(maxEntries: Int): List<String> {
        val points = SheepEconomyState.getPointsSnapshot()
        val names = snapshotPlayerNames(points.keys)
        return sortedEntries(points, names).take(maxEntries.coerceAtLeast(1)).mapIndexed { index, entry ->
            "${index + 1}. ${safeName(entry.key, names)} - ${SheepMergeManager.formatPoints(entry.value)}"
        }.ifEmpty { listOf("No scores yet.") }
    }

    @JvmStatic
    fun getTopPointsPageCount(pageSize: Int): Int {
        val safePageSize = pageSize.coerceAtLeast(1)
        val totalEntries = SheepEconomyState.getPointsSnapshot().size
        return if (totalEntries <= 0) 1 else kotlin.math.ceil(totalEntries / safePageSize.toDouble()).toInt()
    }

    @JvmStatic
    fun getTopPointsLines(pageSize: Int, pageNumber: Int): List<String> {
        val safePageSize = pageSize.coerceAtLeast(1)
        val safePageNumber = pageNumber.coerceAtLeast(1)
        val points = SheepEconomyState.getPointsSnapshot()
        val names = snapshotPlayerNames(points.keys)
        val entries = sortedEntries(points, names)
        if (entries.isEmpty()) return listOf("No scores yet.")
        val startIndex = (safePageNumber - 1) * safePageSize
        if (startIndex >= entries.size) return listOf("No scores on this page.")
        val endIndex = (startIndex + safePageSize).coerceAtMost(entries.size)
        return (startIndex until endIndex).map { index ->
            val entry = entries[index]
            "${index + 1}. ${safeName(entry.key, names)} - ${SheepMergeManager.formatPoints(entry.value)}"
        }
    }

    @JvmStatic
    fun spawnOrMoveTopPointsDisplay(player: Player?): Boolean =
        player?.world?.let { spawnOrMoveTopPointsDisplay(player.location.clone().add(0.0, 2.2, 0.0)) } ?: false

    @JvmStatic
    fun spawnOrMoveTopPointsDisplay(location: Location?): Boolean {
        if (location?.world == null) return false
        removeNearbyUnmarkedDisplays(location)
        removeLegacyArmorStands(location)
        saveDisplayLocation(location)
        configureDisplay(ensureDisplay(location))
        refreshTopPointsDisplays()
        return true
    }

    @JvmStatic
    fun removeTopPointsDisplay(): Boolean {
        val displays = findDisplays()
        if (displays.isEmpty() && !SheepLeaderboardLocationState.hasLocation(SheepMergeManager.leaderboardDataConfig())) {
            return false
        }
        displays.forEach(TextDisplay::remove)
        SheepLeaderboardLocationState.clear(SheepMergeManager.leaderboardDataConfig())
        SheepMergeManager.saveData()
        return true
    }

    @JvmStatic
    fun restoreTopPointsDisplayIfPossible() {
        val location = savedDisplayLocation() ?: return
        if (location.world == null) return
        ensureDisplay(location)
        refreshTopPointsDisplays()
    }

    @JvmStatic
    fun restoreTopPointsDisplayAfterRestart(loadedWorld: World?) {
        val plugin = SheepMergeManager.leaderboardPlugin() ?: return
        val configuration = SheepMergeManager.leaderboardDataConfig() ?: return
        val savedWorldName = SheepLeaderboardLocationState.getWorldName(configuration) ?: return
        if (loadedWorld != null && savedWorldName != loadedWorld.name) return
        val targetWorld = Bukkit.getWorld(savedWorldName) ?: return
        val location = savedDisplayLocation() ?: return
        removeDisplaysAtSavedLocation(location)
        findDisplays().forEach(TextDisplay::remove)
        val restored = targetWorld.spawn(location, TextDisplay::class.java)
        restored.persistentDataContainer.set(markerKey(plugin), PersistentDataType.BYTE, 1.toByte())
        configureDisplay(restored)
        restored.text = buildTopPointsText(DISPLAY_ENTRY_LIMIT)
    }

    @JvmStatic
    fun reconcileTopPointsDisplayForChunk(world: World?, chunkX: Int, chunkZ: Int) {
        if (!SheepLeaderboardLocationState.isSavedChunk(
                SheepMergeManager.leaderboardDataConfig(), world, chunkX, chunkZ,
            )) return
        restoreTopPointsDisplayAfterRestart(world)
    }

    @JvmStatic
    fun reconcileTopPointsDisplayForLocation(location: Location?) {
        val world = location?.world ?: return
        reconcileTopPointsDisplayForChunk(world, location.chunk.x, location.chunk.z)
    }

    @JvmStatic
    fun refreshTopPointsDisplays() {
        val plugin = SheepMergeManager.leaderboardPlugin() ?: return
        if (plugin.server == null) return
        val points = SheepEconomyState.getPointsSnapshot()
        val names = snapshotPlayerNames(points.keys)
        val location = savedDisplayLocation()
        val requestVersion = SheepLeaderboardRefreshState.nextRequestVersion()
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val text = buildTopPointsText(points, names, DISPLAY_ENTRY_LIMIT)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (SheepLeaderboardRefreshState.isLatestRequest(requestVersion)) applyText(location, text)
            })
        })
    }

    private fun applyText(location: Location?, text: String) {
        if (location?.world != null) removeNearbyUnmarkedDisplays(location)
        val displays = findDisplays()
        if (displays.isEmpty()) ensureDisplay(location)?.let(displays::add)
        displays.forEach { configureDisplay(it); it.text = text }
    }

    private fun configureDisplay(display: TextDisplay?) {
        display ?: return
        display.billboard = Display.Billboard.CENTER
        display.isSeeThrough = true
        display.isDefaultBackground = false
        display.isShadowed = true
        display.lineWidth = 260
    }

    private fun ensureDisplay(location: Location?): TextDisplay? {
        val world = location?.world ?: return null
        val displays = findDisplays()
        val display = displays.firstOrNull()?.also { it.teleport(location) } ?: run {
            val plugin = SheepMergeManager.leaderboardPlugin() ?: return null
            world.spawn(location, TextDisplay::class.java).also {
                it.persistentDataContainer.set(markerKey(plugin), PersistentDataType.BYTE, 1.toByte())
            }
        }
        displays.drop(1).forEach(TextDisplay::remove)
        return display
    }

    private fun findDisplays(): MutableList<TextDisplay> {
        val plugin = SheepMergeManager.leaderboardPlugin() ?: return mutableListOf()
        val key = markerKey(plugin)
        return plugin.server.worlds.flatMapTo(mutableListOf()) { world ->
            world.getEntitiesByClass(TextDisplay::class.java).filter {
                it.persistentDataContainer.get(key, PersistentDataType.BYTE) == 1.toByte()
            }
        }
    }

    private fun removeDisplaysAtSavedLocation(location: Location) {
        val world = location.world ?: return
        val chunk = world.getChunkAt(location)
        if (!chunk.isLoaded) chunk.load()
        world.getEntitiesByClass(TextDisplay::class.java)
            .filter { it.isValid && isNear(it.location, location) }.forEach(TextDisplay::remove)
        chunk.entities.filterIsInstance<TextDisplay>()
            .filter { it.isValid && isNear(it.location, location) }.forEach(TextDisplay::remove)
        removeLegacyArmorStands(location)
    }

    private fun isNear(candidate: Location, saved: Location): Boolean =
        candidate.world == saved.world &&
            kotlin.math.abs(candidate.x - saved.x) <= 1.25 &&
            kotlin.math.abs(candidate.y - saved.y) <= 6.0 &&
            kotlin.math.abs(candidate.z - saved.z) <= 1.25

    private fun removeNearbyUnmarkedDisplays(location: Location) {
        val world = location.world ?: return
        val plugin = SheepMergeManager.leaderboardPlugin() ?: return
        val key = markerKey(plugin)
        world.getNearbyEntities(location, 1.25, 6.0, 1.25).forEach { entity ->
            val display = entity as? TextDisplay ?: return@forEach
            if (!display.isValid || display.persistentDataContainer.get(key, PersistentDataType.BYTE) == 1.toByte()) {
                return@forEach
            }
            val stripped = display.text?.let(ChatColor::stripColor)
            if (stripped?.lowercase(Locale.ROOT)?.contains(DISPLAY_HEADER.lowercase(Locale.ROOT)) == true) display.remove()
        }
    }

    private fun removeLegacyArmorStands(location: Location) {
        val world = location.world ?: return
        val nearby = world.getNearbyEntities(location, 1.25, 6.0, 1.25)
        val hasHeader = nearby.filterIsInstance<ArmorStand>().any {
            it.customName?.let(ChatColor::stripColor)?.lowercase(Locale.ROOT)
                ?.contains(DISPLAY_HEADER.lowercase(Locale.ROOT)) == true
        }
        if (!hasHeader) return
        nearby.filterIsInstance<ArmorStand>().filter { !it.customName.isNullOrBlank() }.forEach(ArmorStand::remove)
    }

    private fun saveDisplayLocation(location: Location) {
        if (location.world == null || SheepMergeManager.leaderboardPlugin() == null) return
        val configuration = SheepMergeManager.ensureLeaderboardDataConfig() ?: return
        SheepLeaderboardLocationState.save(configuration, location)
        SheepMergeManager.saveData()
    }

    private fun savedDisplayLocation(): Location? {
        if (SheepMergeManager.leaderboardPlugin() == null) return null
        return SheepLeaderboardLocationState.load(SheepMergeManager.leaderboardDataConfig())
    }

    private fun markerKey(plugin: SheepMergePlugin) = NamespacedKey(plugin, DISPLAY_MARKER_KEY)
}