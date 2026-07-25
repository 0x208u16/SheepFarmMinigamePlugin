package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.UUID
import java.util.stream.Stream

class SheepFarmWorldCleanupListener : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (SheepMergeManager.isFarmBuildWorld(event.player.world)) {
            scheduleBuildWorldSaveCheck()
        }
        scheduleDeleteWorldForPlayer(event.player.uniqueId)
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        val reason = event.reason
        if (reason.lowercase().contains("another location")) {
            return
        }
        if (SheepMergeManager.isFarmBuildWorld(event.player.world)) {
            scheduleBuildWorldSaveCheck()
        }
        scheduleDeleteWorldForPlayer(event.player.uniqueId)
    }

    companion object {
        private const val WORLD_CLEANUP_DELAY_TICKS: Long = 5L * 60L * 20L

        @JvmStatic
        fun scheduleDeleteWorldForPlayer(playerId: UUID?) {
            val plugin = SheepMergePlugin.instance
            if (playerId == null || plugin == null) {
                return
            }

            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                val stillOnline: Player? = Bukkit.getPlayer(playerId)
                if (stillOnline != null && stillOnline.isOnline) {
                    return@Runnable
                }

                deleteTransientWorldsForPlayer(playerId, true)
            }, WORLD_CLEANUP_DELAY_TICKS)
        }

        @JvmStatic
        fun cleanupFarmWorldsOnStartup() {
            val worldFolders = Bukkit.getWorldContainer().listFiles(File::isDirectory)
            if (worldFolders.isNullOrEmpty()) {
                return
            }

            for (worldFolder in worldFolders) {
                val worldName = worldFolder.name
                if (!isTransientPlayerWorldName(worldName)) {
                    continue
                }
                unloadWorld(worldName)
                deleteWorldFolder(worldName, worldFolder)
            }
        }

        @JvmStatic
        fun cleanupFarmWorldsOnShutdown() {
            SheepMergeManager.saveBuildWorldIfIdle()
            var capturedSnapshot = false
            for (world in Bukkit.getWorlds()) {
                if (!SheepMergeManager.isSheepFarmWorld(world)) {
                    continue
                }
                SheepMergeManager.saveSheepSnapshotForWorld(world)
                capturedSnapshot = true
                unloadWorld(world.name)
            }

            if (capturedSnapshot) {
                SheepMergeManager.saveData()
            }

            val worldFolders = Bukkit.getWorldContainer().listFiles(File::isDirectory)
            if (worldFolders.isNullOrEmpty()) {
                return
            }

            for (worldFolder in worldFolders) {
                val worldName = worldFolder.name
                if (!isTransientPlayerWorldName(worldName)) {
                    continue
                }
                deleteWorldFolder(worldName, worldFolder)
            }
        }

        @JvmStatic
        fun deleteTransientWorldsForPlayer(playerId: UUID?, asyncDelete: Boolean) {
            if (playerId == null) {
                return
            }
            deleteWorldByName(SheepFarmWorldCommand.getWorldName(playerId), asyncDelete, true)
            deleteWorldByName(SheepMergeManager.getTutorialWorldName(playerId), asyncDelete, true)
        }

        @JvmStatic
        fun deleteWorldByName(worldName: String?, asyncDelete: Boolean, saveSheepState: Boolean) {
            if (worldName.isNullOrBlank() || SheepMergeManager.getFarmBuildWorldName() == worldName) {
                return
            }
            val loadedWorld: World? = Bukkit.getWorld(worldName)
            var snapshotCaptured = false
            if (saveSheepState && loadedWorld != null && SheepMergeManager.isSheepFarmWorld(loadedWorld)) {
                SheepMergeManager.saveSheepSnapshotForWorld(loadedWorld)
                snapshotCaptured = true
            }
            if (snapshotCaptured) {
                SheepMergeManager.saveData()
            }
            unloadWorld(worldName)

            val worldFolder = File(Bukkit.getWorldContainer(), worldName)
            val plugin = SheepMergePlugin.instance
            if (!asyncDelete || plugin == null) {
                deleteWorldFolder(worldName, worldFolder)
                return
            }

            Bukkit.getScheduler().runTaskAsynchronously(
                plugin,
                Runnable { deleteWorldFolder(worldName, worldFolder) }
            )
        }

        private fun isTransientPlayerWorldName(worldName: String?): Boolean {
            if (worldName == null || SheepMergeManager.getFarmBuildWorldName() == worldName) {
                return false
            }
            return worldName.startsWith("sheepfarm_") || worldName.startsWith("sheeptutorial_")
        }

        private fun unloadWorld(worldName: String) {
            SheepFarmWorldCommand.invalidateManagedWorldInitialization(worldName)
            val world = Bukkit.getWorld(worldName)
            if (world != null) {
                Bukkit.unloadWorld(world, false)
            }
        }

        private fun scheduleBuildWorldSaveCheck() {
            val plugin = SheepMergePlugin.instance
            if (plugin == null) {
                return
            }
            Bukkit.getScheduler().runTaskLater(plugin, SheepMergeManager::saveBuildWorldIfIdle, 1L)
        }

        private fun deleteWorldFolder(worldName: String, worldFolder: File) {
            if (worldFolder.exists()) {
                try {
                    deleteDirectory(worldFolder)
                } catch (exception: IOException) {
                    SheepMergePlugin.log.warning(
                        "Could not delete temporary sheep farm world '" + worldName + "': " + exception.message
                    )
                }
            }
        }

        @Throws(IOException::class)
        private fun deleteDirectory(directory: File) {
            val path: Path = directory.toPath()
            if (!Files.exists(path)) {
                return
            }

            try {
                Files.walk(path).use { paths: Stream<Path> ->
                    paths.sorted(Comparator.reverseOrder())
                        .forEach { currentPath ->
                            try {
                                Files.deleteIfExists(currentPath)
                            } catch (exception: IOException) {
                                throw UncheckedIOException(exception)
                            }
                        }
                }
            } catch (exception: UncheckedIOException) {
                throw exception.cause ?: exception
            }
        }
    }
}
