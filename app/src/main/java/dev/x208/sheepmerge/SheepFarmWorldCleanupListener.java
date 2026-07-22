package dev.x208.sheepmerge;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SheepFarmWorldCleanupListener implements Listener {

    private static final long WORLD_CLEANUP_DELAY_TICKS = 5L * 60L * 20L;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (SheepMergeManager.isFarmBuildWorld(event.getPlayer().getWorld())) {
            scheduleBuildWorldSaveCheck();
        }
        scheduleDeleteWorldForPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        String reason = event.getReason();
        if (reason != null && reason.toLowerCase().contains("another location")) {
            return;
        }
        if (SheepMergeManager.isFarmBuildWorld(event.getPlayer().getWorld())) {
            scheduleBuildWorldSaveCheck();
        }
        scheduleDeleteWorldForPlayer(event.getPlayer().getUniqueId());
    }

    public static void scheduleDeleteWorldForPlayer(UUID playerId) {
        if (playerId == null || SheepMergePlugin.instance == null) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(SheepMergePlugin.instance, () -> {
            Player stillOnline = Bukkit.getPlayer(playerId);
            if (stillOnline != null && stillOnline.isOnline()) {
                return;
            }

            deleteTransientWorldsForPlayer(playerId, true);
        }, WORLD_CLEANUP_DELAY_TICKS);
    }

    public static void cleanupFarmWorldsOnStartup() {
        File[] worldFolders = Bukkit.getWorldContainer().listFiles(File::isDirectory);
        if (worldFolders == null || worldFolders.length == 0) {
            return;
        }

        for (File worldFolder : worldFolders) {
            String worldName = worldFolder.getName();
            if (!isTransientPlayerWorldName(worldName)) {
                continue;
            }
            unloadWorld(worldName);
            deleteWorldFolder(worldName, worldFolder);
        }
    }

    public static void cleanupFarmWorldsOnShutdown() {
        SheepMergeManager.saveBuildWorldIfIdle();
        boolean capturedSnapshot = false;
        for (World world : Bukkit.getWorlds()) {
            if (!SheepMergeManager.isSheepFarmWorld(world)) {
                continue;
            }
            SheepMergeManager.saveSheepSnapshotForWorld(world);
            capturedSnapshot = true;
            unloadWorld(world.getName());
        }

        if (capturedSnapshot) {
            SheepMergeManager.saveData();
        }

        File[] worldFolders = Bukkit.getWorldContainer().listFiles(File::isDirectory);
        if (worldFolders == null || worldFolders.length == 0) {
            return;
        }

        for (File worldFolder : worldFolders) {
            String worldName = worldFolder.getName();
            if (!isTransientPlayerWorldName(worldName)) {
                continue;
            }
            deleteWorldFolder(worldName, worldFolder);
        }
    }

    public static void deleteTransientWorldsForPlayer(UUID playerId, boolean asyncDelete) {
        if (playerId == null) {
            return;
        }
        deleteWorldByName(SheepFarmWorldCommand.getWorldName(playerId), asyncDelete, true);
        deleteWorldByName(SheepMergeManager.getTutorialWorldName(playerId), asyncDelete, true);
    }

    public static void deleteWorldByName(String worldName, boolean asyncDelete, boolean saveSheepState) {
        if (worldName == null || worldName.isBlank() || SheepMergeManager.getFarmBuildWorldName().equals(worldName)) {
            return;
        }
        World loadedWorld = Bukkit.getWorld(worldName);
        boolean snapshotCaptured = false;
        if (saveSheepState && loadedWorld != null && SheepMergeManager.isSheepFarmWorld(loadedWorld)) {
            SheepMergeManager.saveSheepSnapshotForWorld(loadedWorld);
            snapshotCaptured = true;
        }
        if (snapshotCaptured) {
            SheepMergeManager.saveData();
        }
        unloadWorld(worldName);

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!asyncDelete || SheepMergePlugin.instance == null) {
            deleteWorldFolder(worldName, worldFolder);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SheepMergePlugin.instance,
                () -> deleteWorldFolder(worldName, worldFolder));
    }

    private static boolean isTransientPlayerWorldName(String worldName) {
        if (worldName == null || SheepMergeManager.getFarmBuildWorldName().equals(worldName)) {
            return false;
        }
        return worldName.startsWith("sheepfarm_") || worldName.startsWith("sheeptutorial_");
    }

    private static UUID getTutorialOwnerId(String worldName) {
        if (worldName == null || !worldName.startsWith("sheeptutorial_")) {
            return null;
        }
        String rawId = worldName.substring("sheeptutorial_".length());
        if (rawId.length() != 32) {
            return null;
        }
        StringBuilder builder = new StringBuilder(rawId);
        builder.insert(8, '-');
        builder.insert(13, '-');
        builder.insert(18, '-');
        builder.insert(23, '-');
        try {
            return UUID.fromString(builder.toString());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static void unloadWorld(String worldName) {
        SheepFarmWorldCommand.invalidateManagedWorldInitialization(worldName);
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }
    }

    private static void scheduleBuildWorldSaveCheck() {
        if (SheepMergePlugin.instance == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(SheepMergePlugin.instance, SheepMergeManager::saveBuildWorldIfIdle, 1L);
    }

    private static void deleteWorldFolder(String worldName, File worldFolder) {
        if (worldFolder.exists()) {
            try {
                deleteDirectory(worldFolder);
            } catch (IOException exception) {
                SheepMergePlugin.log.warning(
                        "Could not delete temporary sheep farm world '" + worldName + "': " + exception.getMessage());
            }
        }
    }

    private static void deleteDirectory(File directory) throws IOException {
        Path path = directory.toPath();
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(currentPath -> {
                        try {
                            Files.deleteIfExists(currentPath);
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }
}
