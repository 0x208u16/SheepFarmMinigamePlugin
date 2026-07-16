package dev.thehale.papermc_plugin_template;

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

    private static final long WORLD_CLEANUP_DELAY_TICKS = 200L;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        scheduleDeleteWorldForPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        String reason = event.getReason();
        if (reason != null && reason.toLowerCase().contains("another location")) {
            return;
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

            deleteFarmWorldForPlayer(playerId, true);
        }, WORLD_CLEANUP_DELAY_TICKS);
    }

    public static void cleanupFarmWorldsOnStartup() {
        File[] worldFolders = Bukkit.getWorldContainer().listFiles(File::isDirectory);
        if (worldFolders == null || worldFolders.length == 0) {
            return;
        }

        for (File worldFolder : worldFolders) {
            String worldName = worldFolder.getName();
            if (!isTemporaryFarmWorldName(worldName)) {
                continue;
            }
            unloadWorld(worldName);
            deleteWorldFolder(worldName, worldFolder);
        }
    }

    public static void cleanupFarmWorldsOnShutdown() {
        for (World world : Bukkit.getWorlds()) {
            if (!SheepMergeManager.isSheepFarmWorld(world)) {
                continue;
            }
            unloadWorld(world.getName());
        }

        File[] worldFolders = Bukkit.getWorldContainer().listFiles(File::isDirectory);
        if (worldFolders == null || worldFolders.length == 0) {
            return;
        }

        for (File worldFolder : worldFolders) {
            String worldName = worldFolder.getName();
            if (!isTemporaryFarmWorldName(worldName)) {
                continue;
            }
            deleteWorldFolder(worldName, worldFolder);
        }
    }

    private static void deleteFarmWorldForPlayer(UUID playerId, boolean asyncDelete) {
        String worldName = SheepFarmWorldCommand.getWorldName(playerId);
        deleteFarmWorld(worldName, asyncDelete);

        String tutorialWorldName = SheepMergeManager.getTutorialWorldName(playerId);
        deleteFarmWorld(tutorialWorldName, asyncDelete);
    }

    private static void deleteFarmWorld(String worldName, boolean asyncDelete) {
        unloadWorld(worldName);

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!asyncDelete || SheepMergePlugin.instance == null) {
            deleteWorldFolder(worldName, worldFolder);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SheepMergePlugin.instance,
                () -> deleteWorldFolder(worldName, worldFolder));
    }

    private static boolean isTemporaryFarmWorldName(String worldName) {
        return worldName != null
                && (worldName.startsWith("sheepfarm_") || worldName.startsWith("sheeptutorial_"));
    }

    private static void unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }
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
