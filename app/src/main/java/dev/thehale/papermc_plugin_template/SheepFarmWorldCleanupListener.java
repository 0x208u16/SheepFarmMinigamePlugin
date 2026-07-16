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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        scheduleDeleteWorldForPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        scheduleDeleteWorldForPlayer(event.getPlayer().getUniqueId());
    }

    private void scheduleDeleteWorldForPlayer(UUID playerId) {
        if (playerId == null || SheepMergePlugin.instance == null) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(SheepMergePlugin.instance, () -> {
            Player stillOnline = Bukkit.getPlayer(playerId);
            if (stillOnline != null && stillOnline.isOnline()) {
                return;
            }

            String worldName = SheepFarmWorldCommand.getWorldName(playerId);
            deleteWorld(worldName);
        }, 40L);
    }

    private void deleteWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            try {
                deleteDirectory(worldFolder);
            } catch (IOException exception) {
                SheepMergePlugin.log.warning(
                        "Could not delete temporary sheep farm world '" + worldName + "': " + exception.getMessage());
            }
        }
    }

    private void deleteDirectory(File directory) throws IOException {
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
