package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration

object SheepLeaderboardLocationState {
    private const val WORLD_KEY = "topPointsDisplay.world"
    private const val X_KEY = "topPointsDisplay.x"
    private const val Y_KEY = "topPointsDisplay.y"
    private const val Z_KEY = "topPointsDisplay.z"
    private const val YAW_KEY = "topPointsDisplay.yaw"
    private const val PITCH_KEY = "topPointsDisplay.pitch"

    @JvmStatic
    fun save(configuration: FileConfiguration, location: Location) {
        val world = location.world ?: return
        configuration.set(WORLD_KEY, world.name)
        configuration.set(X_KEY, location.x)
        configuration.set(Y_KEY, location.y)
        configuration.set(Z_KEY, location.z)
        configuration.set(YAW_KEY, location.yaw)
        configuration.set(PITCH_KEY, location.pitch)
    }

    @JvmStatic
    fun clear(configuration: FileConfiguration?) {
        if (configuration == null) {
            return
        }
        persistedKeys.forEach { configuration.set(it, null) }
    }

    @JvmStatic
    fun hasLocation(configuration: FileConfiguration?): Boolean =
        configuration?.contains(WORLD_KEY) == true

    @JvmStatic
    fun getWorldName(configuration: FileConfiguration?): String? =
        configuration?.getString(WORLD_KEY)?.takeIf { it.isNotBlank() }

    @JvmStatic
    fun load(configuration: FileConfiguration?): Location? {
        val worldName = getWorldName(configuration) ?: return null
        val world = Bukkit.getWorld(worldName) ?: return null
        return Location(
            world,
            configuration!!.getDouble(X_KEY, 0.0),
            configuration.getDouble(Y_KEY, 0.0),
            configuration.getDouble(Z_KEY, 0.0),
            configuration.getDouble(YAW_KEY, 0.0).toFloat(),
            configuration.getDouble(PITCH_KEY, 0.0).toFloat(),
        )
    }

    @JvmStatic
    fun isSavedChunk(configuration: FileConfiguration?, world: World?, chunkX: Int, chunkZ: Int): Boolean {
        if (world == null || getWorldName(configuration) != world.name) {
            return false
        }
        val savedChunkX = kotlin.math.floor(configuration!!.getDouble(X_KEY, 0.0) / 16.0).toInt()
        val savedChunkZ = kotlin.math.floor(configuration.getDouble(Z_KEY, 0.0) / 16.0).toInt()
        return savedChunkX == chunkX && savedChunkZ == chunkZ
    }

    private val persistedKeys = listOf(WORLD_KEY, X_KEY, Y_KEY, Z_KEY, YAW_KEY, PITCH_KEY)
}