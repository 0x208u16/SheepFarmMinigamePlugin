package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Fence
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

object SheepFarmLayoutManager {
    private const val BUILD_WORLD = "sheepfarm_build"
    private const val CHUNK_SPAN = 4
    private const val CHUNK_HALF_SPAN = CHUNK_SPAN / 2
    private const val MIN_XZ = -5
    private const val MAX_XZ = 6
    private const val BASE_Y = 100
    private const val MIN_Y = BASE_Y - 1
    private const val MAX_Y = BASE_Y + 4
    private const val BLOCKS_PER_TICK = 2400

    private var plugin: SheepMergePlugin? = null
    private var layoutFile: File? = null
    private var config: FileConfiguration? = null

    private class ChunkCursor(
        val path: String,
        val x: Int,
        val z: Int,
        val minY: Int,
        val maxY: Int,
        private val palette: List<BlockData>,
        private val tokens: List<String>?,
        val legacy: Boolean,
    ) {
        var blockIndex = 0
        private var tokenIndex = 0
        private var remaining = 0
        private var paletteIndex = -1

        fun complete() = blockIndex >= (maxY - minY).coerceAtLeast(0) * 256

        fun next(): BlockData? {
            if (legacy) return null
            while (remaining <= 0) {
                if (tokens == null || tokenIndex >= tokens.size) return Bukkit.createBlockData(Material.AIR)
                val parts = tokens[tokenIndex++].split("*", limit = 2)
                val nextIndex = parseBase36(parts.firstOrNull(), -1)
                val nextLength = if (parts.size > 1) parseBase36(parts[1], 1) else 1
                if (nextIndex !in palette.indices || nextLength <= 0) continue
                paletteIndex = nextIndex
                remaining = nextLength
            }
            remaining--
            return palette.getOrNull(paletteIndex) ?: Bukkit.createBlockData(Material.AIR)
        }
    }

    @JvmStatic
    fun initialize(plugin: SheepMergePlugin) {
        this.plugin = plugin
        layoutFile = File(plugin.dataFolder, "farm-layout.yml")
    }

    @JvmStatic
    fun load() {
        config = layoutFile?.let(YamlConfiguration::loadConfiguration) ?: return
        if (pruneChunks()) save()
    }

    @JvmStatic
    fun save(): Boolean {
        val activePlugin = plugin ?: return false
        val file = layoutFile ?: return false
        val configuration = config ?: return false
        return try {
            if (!activePlugin.dataFolder.exists() && !activePlugin.dataFolder.mkdirs()) false
            else configuration.save(file).let { true }
        } catch (exception: IOException) {
            activePlugin.logger.warning("Unable to save farm layout: ${exception.message}")
            false
        }
    }

    @JvmStatic
    fun hasSavedLayout(): Boolean {
        val configuration = config ?: return false
        return configuration.getConfigurationSection("chunks")?.getKeys(false)?.isNotEmpty() == true ||
            configuration.getConfigurationSection("blocks")?.getKeys(false)?.isNotEmpty() == true
    }

    @JvmStatic
    fun saveBuildWorldToLayoutFile(): Boolean {
        val world = Bukkit.getWorld(BUILD_WORLD) ?: return false
        if (!SheepMergeManager.isFarmBuildWorld(world)) return false
        world.save()
        val saved = capture(world)
        if (saved) SheepMergeManager.refreshFarmWorldStructureCache()
        return saved
    }

    @JvmStatic
    fun capture(world: World?): Boolean {
        if (world == null || (!SheepMergeManager.isSheepFarmWorld(world) && !SheepMergeManager.isFarmBuildWorld(world))) return false
        val configuration = config ?: YamlConfiguration().also { config = it }
        val loaded = world.loadedChunks
        val allowedMin = -CHUNK_HALF_SPAN
        val allowedMax = allowedMin + CHUNK_SPAN - 1
        var minChunkX = loaded.minOfOrNull { it.x }?.coerceAtLeast(allowedMin) ?: allowedMin
        var maxChunkX = loaded.maxOfOrNull { it.x }?.coerceAtMost(allowedMax) ?: allowedMax
        var minChunkZ = loaded.minOfOrNull { it.z }?.coerceAtLeast(allowedMin) ?: allowedMin
        var maxChunkZ = loaded.maxOfOrNull { it.z }?.coerceAtMost(allowedMax) ?: allowedMax
        if (minChunkX > maxChunkX || minChunkZ > maxChunkZ) {
            minChunkX = allowedMin; maxChunkX = allowedMax; minChunkZ = allowedMin; maxChunkZ = allowedMax
        }
        configuration.set("version", 3)
        configuration.set("world.minY", world.minHeight)
        configuration.set("world.maxY", world.maxHeight)
        configuration.set("world.minX", minChunkX shl 4)
        configuration.set("world.maxX", (maxChunkX shl 4) + 15)
        configuration.set("world.minZ", minChunkZ shl 4)
        configuration.set("world.maxZ", (maxChunkZ shl 4) + 15)
        configuration.set("world.name", world.name)
        configuration.set("world.savedAt", System.currentTimeMillis())
        configuration.set("chunks", null)
        configuration.set("blocks", null)
        for (chunkX in minChunkX..maxChunkX) for (chunkZ in minChunkZ..maxChunkZ) {
            val path = "chunks.$chunkX,$chunkZ"
            configuration.set("$path.x", chunkX)
            configuration.set("$path.z", chunkZ)
            val palette = mutableListOf<String>()
            val indices = mutableMapOf<String, Int>()
            val encoded = StringBuilder()
            var previous = -1
            var runLength = 0
            for (y in world.minHeight until world.maxHeight) for (localX in 0..15) for (localZ in 0..15) {
                val value = world.getBlockAt((chunkX shl 4) + localX, y, (chunkZ shl 4) + localZ).blockData.asString
                val index = indices.getOrPut(value) { palette.add(value); palette.lastIndex }
                if (index == previous) runLength++ else {
                    appendRun(encoded, previous, runLength)
                    previous = index
                    runLength = 1
                }
            }
            appendRun(encoded, previous, runLength)
            configuration.set("$path.format", "rle-v1")
            configuration.set("$path.palette", palette)
            configuration.set("$path.data", encoded.toString())
            configuration.set("$path.height", world.maxHeight - world.minHeight)
        }
        return save()
    }

    @JvmStatic
    fun applyToAllFarmWorlds(): Int {
        val worlds = plugin?.server?.worlds ?: return 0
        return worlds.count { world ->
            if (!SheepMergeManager.isSheepFarmWorld(world)) false else { apply(world); true }
        }
    }

    @JvmStatic
    fun apply(world: World?) {
        if (world == null) return
        clearBounds(world)
        if (hasSavedLayout()) applySaved(world) else {
            applyDefault(world)
            enforcePerimeter(world)
        }
        repairBase(world)
    }

    @JvmStatic
    fun applyAsync(world: World?, complete: Runnable?) {
        if (world == null) { complete?.run(); return }
        if (plugin == null) { apply(world); complete?.run(); return }
        clearBounds(world)
        val finish = Runnable { enforcePerimeter(world); repairBase(world); complete?.run() }
        when {
            hasSavedLayout() && config?.isConfigurationSection("chunks") == true -> applyChunksAsync(world, finish)
            hasSavedLayout() -> applyBlocksAsync(world, finish)
            else -> { applyDefault(world); finish.run() }
        }
    }

    @JvmStatic
    fun needsBootstrap(world: World?): Boolean = world != null &&
        (SheepMergeManager.isSheepFarmWorld(world) || SheepMergeManager.isFarmBuildWorld(world)) &&
        (world.getBlockAt(0, MIN_Y, 0).type.isAir || world.getBlockAt(0, BASE_Y, 0).type.isAir)

    private fun applySaved(world: World) {
        val configuration = config ?: return
        if (configuration.isConfigurationSection("chunks")) { applyChunks(world); return }
        val minX = configuration.getInt("world.minX", MIN_XZ)
        val maxX = configuration.getInt("world.maxX", MAX_XZ)
        val minZ = configuration.getInt("world.minZ", MIN_XZ)
        val maxZ = configuration.getInt("world.maxZ", MAX_XZ)
        val minY = maxOf(world.minHeight, configuration.getInt("world.minY", world.minHeight))
        val maxY = minOf(world.maxHeight, configuration.getInt("world.maxY", world.maxHeight))
        for (x in minX..maxX) for (y in minY until maxY) for (z in minZ..maxZ) {
            val value = configuration.getString("blocks.$x,$y,$z")
            world.getBlockAt(x, y, z).setBlockData(value?.takeIf(String::isNotBlank)?.let(::parse) ?: Bukkit.createBlockData(defaultAt(x, y, z)), false)
        }
    }

    private fun applyChunks(world: World) {
        val configuration = config ?: return
        val section = configuration.getConfigurationSection("chunks") ?: return
        val minY = maxOf(world.minHeight, configuration.getInt("world.minY", world.minHeight))
        val maxY = minOf(world.maxHeight, configuration.getInt("world.maxY", world.maxHeight))
        for (key in section.getKeys(false)) {
            val path = "chunks.$key"
            val chunkX = coordinate(key, "$path.x", 0)
            val chunkZ = coordinate(key, "$path.z", 1)
            if (chunkX == Int.MIN_VALUE || chunkZ == Int.MIN_VALUE) continue
            val palette = configuration.getStringList("$path.palette")
            val data = configuration.getString("$path.data", "")
            if (palette.isNotEmpty() && !data.isNullOrBlank()) {
                var index = 0
                val total = (maxY - minY) * 256
                for (token in data.split(";")) {
                    val parts = token.split("*", limit = 2)
                    val paletteIndex = parseBase36(parts.firstOrNull(), -1)
                    val length = if (parts.size > 1) parseBase36(parts[1], 1) else 1
                    if (paletteIndex !in palette.indices || length <= 0) continue
                    val blockData = parse(palette[paletteIndex])
                    repeat(minOf(length, total - index)) { setChunkBlock(world, chunkX, chunkZ, minY, index++, blockData) }
                }
            } else {
                var index = 0
                for (y in minY until maxY) for (localX in 0..15) for (localZ in 0..15) {
                    val value = configuration.getString("$path.blocks.${index++}")
                    world.getBlockAt((chunkX shl 4) + localX, y, (chunkZ shl 4) + localZ)
                        .setBlockData(value?.takeIf(String::isNotBlank)?.let(::parse) ?: Bukkit.createBlockData(Material.AIR), false)
                }
            }
        }
    }

    private fun applyBlocksAsync(world: World, complete: Runnable) {
        val activePlugin = plugin ?: return applySaved(world).also { complete.run() }
        val configuration = config ?: return applySaved(world).also { complete.run() }
        val state = intArrayOf(MIN_XZ, MIN_Y, MIN_XZ)
        Bukkit.getScheduler().runTaskTimer(activePlugin, { task ->
            if (Bukkit.getWorld(world.uid) == null) { task.cancel(); complete.run(); return@runTaskTimer }
            var processed = 0
            while (state[1] <= MAX_Y && processed++ < BLOCKS_PER_TICK) {
                val (x, y, z) = state
                val value = configuration.getString("blocks.$x,$y,$z")
                world.getBlockAt(x, y, z).setBlockData(value?.takeIf(String::isNotBlank)?.let(::parse) ?: Bukkit.createBlockData(defaultAt(x, y, z)), false)
                if (++state[2] > MAX_XZ) { state[2] = MIN_XZ; if (++state[0] > MAX_XZ) { state[0] = MIN_XZ; state[1]++ } }
            }
            if (state[1] > MAX_Y) { task.cancel(); complete.run() }
        }, 1L, 1L)
    }

    private fun applyChunksAsync(world: World, complete: Runnable) {
        val activePlugin = plugin ?: return applyChunks(world).also { complete.run() }
        val configuration = config ?: return applyChunks(world).also { complete.run() }
        val section = configuration.getConfigurationSection("chunks") ?: return complete.run()
        val minY = maxOf(world.minHeight, configuration.getInt("world.minY", world.minHeight))
        val maxY = minOf(world.maxHeight, configuration.getInt("world.maxY", world.maxHeight))
        val cursors = section.getKeys(false).mapNotNull { key ->
            val path = "chunks.$key"
            val x = coordinate(key, "$path.x", 0); val z = coordinate(key, "$path.z", 1)
            if (x == Int.MIN_VALUE || z == Int.MIN_VALUE) null else {
                val palette = configuration.getStringList("$path.palette").map(::parse)
                val data = configuration.getString("$path.data", "")
                ChunkCursor(path, x, z, minY, maxY, palette, data?.takeIf(String::isNotBlank)?.split(";"), data.isNullOrBlank() || palette.isEmpty())
            }
        }
        if (cursors.isEmpty()) return complete.run()
        var cursorIndex = 0
        Bukkit.getScheduler().runTaskTimer(activePlugin, { task ->
            if (Bukkit.getWorld(world.uid) == null) { task.cancel(); complete.run(); return@runTaskTimer }
            var processed = 0
            while (processed < BLOCKS_PER_TICK && cursorIndex < cursors.size) {
                val cursor = cursors[cursorIndex]
                if (cursor.complete()) { cursorIndex++; continue }
                val index = cursor.blockIndex++
                val blockData = if (cursor.legacy) configuration.getString("${cursor.path}.blocks.$index")?.let(::parse) ?: Bukkit.createBlockData(Material.AIR) else cursor.next()!!
                setChunkBlock(world, cursor.x, cursor.z, cursor.minY, index, blockData)
                processed++
            }
            if (cursorIndex >= cursors.size) { task.cancel(); complete.run() }
        }, 1L, 1L)
    }

    private fun setChunkBlock(world: World, chunkX: Int, chunkZ: Int, minY: Int, index: Int, data: BlockData) {
        val layerIndex = index % 256
        world.getBlockAt((chunkX shl 4) + layerIndex / 16, minY + index / 256, (chunkZ shl 4) + layerIndex % 16).setBlockData(data, false)
    }

    private fun clearBounds(world: World) {
        for (x in MIN_XZ..MAX_XZ) for (y in MIN_Y..MAX_Y) for (z in MIN_XZ..MAX_XZ)
            world.getBlockAt(x, y, z).setBlockData(Bukkit.createBlockData(Material.AIR), false)
    }

    private fun applyDefault(world: World) {
        for (x in MIN_XZ..MAX_XZ) for (y in MIN_Y..MAX_Y) for (z in MIN_XZ..MAX_XZ)
            world.getBlockAt(x, y, z).setBlockData(Bukkit.createBlockData(defaultAt(x, y, z)), false)
    }

    private fun enforcePerimeter(world: World) {
        for (x in MIN_XZ..MAX_XZ) for (z in MIN_XZ..MAX_XZ) if (perimeter(x, z)) {
            val fence = Bukkit.createBlockData(Material.OAK_FENCE) as Fence
            if (z == MIN_XZ || z == MAX_XZ) { if (x > MIN_XZ) fence.setFace(BlockFace.WEST, true); if (x < MAX_XZ) fence.setFace(BlockFace.EAST, true) }
            if (x == MIN_XZ || x == MAX_XZ) { if (z > MIN_XZ) fence.setFace(BlockFace.NORTH, true); if (z < MAX_XZ) fence.setFace(BlockFace.SOUTH, true) }
            world.getBlockAt(x, BASE_Y + 1, z).setBlockData(fence, false)
            world.getBlockAt(x, BASE_Y + 2, z).setBlockData(Bukkit.createBlockData(Material.WHITE_CARPET), false)
        }
    }

    private fun repairBase(world: World) {
        if (!SheepMergeManager.isSheepFarmWorld(world)) return
        for (x in MIN_XZ..MAX_XZ) for (z in MIN_XZ..MAX_XZ) {
            if (world.getBlockAt(x, MIN_Y, z).type.isAir) world.getBlockAt(x, MIN_Y, z).setBlockData(Bukkit.createBlockData(Material.DIRT), false)
            if (world.getBlockAt(x, BASE_Y, z).type.isAir) world.getBlockAt(x, BASE_Y, z).setBlockData(Bukkit.createBlockData(Material.GRASS_BLOCK), false)
        }
    }

    private fun defaultAt(x: Int, y: Int, z: Int) = when {
        y == MIN_Y -> Material.DIRT
        y == BASE_Y -> Material.GRASS_BLOCK
        y == BASE_Y + 1 && perimeter(x, z) -> Material.OAK_FENCE
        y == BASE_Y + 2 && perimeter(x, z) -> Material.WHITE_CARPET
        else -> Material.AIR
    }

    private fun perimeter(x: Int, z: Int) = x == MIN_XZ || x == MAX_XZ || z == MIN_XZ || z == MAX_XZ
    private fun parse(value: String): BlockData = try { Bukkit.createBlockData(value) } catch (_: IllegalArgumentException) { Bukkit.createBlockData(Material.matchMaterial(value) ?: Material.AIR) }
    private fun parseBase36(value: String?, fallback: Int) = value?.takeIf(String::isNotBlank)?.toIntOrNull(36) ?: fallback
    private fun coordinate(key: String, path: String, axis: Int): Int = config?.getInt(path, Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }
        ?: key.split(",", limit = 2).getOrNull(if (axis <= 0) 0 else 1)?.trim()?.toIntOrNull() ?: Int.MIN_VALUE

    private fun appendRun(target: StringBuilder, index: Int, length: Int) {
        if (index < 0 || length <= 0) return
        if (target.isNotEmpty()) target.append(';')
        target.append(index.toString(36))
        if (length > 1) target.append('*').append(length.toString(36))
    }

    private fun pruneChunks(): Boolean {
        val configuration = config ?: return false
        val section = configuration.getConfigurationSection("chunks") ?: return false
        val min = -CHUNK_HALF_SPAN
        val max = min + CHUNK_SPAN - 1
        var changed = false
        for (key in section.getKeys(false).toList()) {
            val path = "chunks.$key"
            val x = coordinate(key, "$path.x", 0); val z = coordinate(key, "$path.z", 1)
            if (x !in min..max || z !in min..max) { configuration.set(path, null); changed = true }
        }
        if (changed) {
            configuration.set("world.minX", MIN_XZ); configuration.set("world.maxX", MAX_XZ)
            configuration.set("world.minZ", MIN_XZ); configuration.set("world.maxZ", MAX_XZ)
        }
        return changed
    }
}