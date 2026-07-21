package dev.x208.sheepmerge;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

final class SacrificeUnlockState {

    private final Map<UUID, Integer> unlocksBoughtByPlayer = new HashMap<>();
    private final Map<UUID, Integer> unlockMaskByPlayer = new HashMap<>();
    private final Map<UUID, Integer> pendingMaskByPlayer = new HashMap<>();

    int getUnlocksBought(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        if (unlockMaskByPlayer.containsKey(playerId)) {
            return Integer.bitCount(getUnlockMask(playerId));
        }
        return Math.max(0, Math.min(SheepMergeManager.SACRIFICE_UNLOCK_MAX,
                unlocksBoughtByPlayer.getOrDefault(playerId, 0)));
    }

    int getUnlockMask(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        return normalizeMask(unlockMaskByPlayer.getOrDefault(playerId, 0));
    }

    int getPendingMask(UUID playerId) {
        if (playerId == null) {
            return 0;
        }
        return normalizeMask(pendingMaskByPlayer.getOrDefault(playerId, 0));
    }

    boolean hasUnlock(UUID playerId, int unlockId) {
        if (playerId == null || unlockId <= 0) {
            return false;
        }
        int unlockBit = getUnlockBit(unlockId);
        if (unlockBit == 0) {
            return false;
        }
        if (unlockMaskByPlayer.containsKey(playerId)) {
            return (getUnlockMask(playerId) & unlockBit) != 0;
        }
        return getUnlocksBought(playerId) >= unlockId;
    }

    boolean isPending(UUID playerId, int unlockId) {
        int unlockBit = getUnlockBit(unlockId);
        return playerId != null && unlockBit != 0 && (getPendingMask(playerId) & unlockBit) != 0;
    }

    boolean isActive(UUID playerId, int unlockId) {
        return hasUnlock(playerId, unlockId);
    }

    String statusLine(Player player, int unlockId) {
        if (player == null) {
            return "LOCKED";
        }
        UUID playerId = player.getUniqueId();
        if (!hasUnlock(playerId, unlockId)) {
            return "LOCKED";
        }
        return "ACTIVE";
    }

    void recordPurchase(UUID playerId, int unlockId) {
        int unlockBit = getUnlockBit(unlockId);
        if (playerId == null || unlockBit == 0) {
            return;
        }
        unlocksBoughtByPlayer.put(playerId, getUnlocksBought(playerId) + 1);
        unlockMaskByPlayer.put(playerId, getUnlockMask(playerId) | unlockBit);
    }

    void clearPending(UUID playerId) {
        if (playerId == null) {
            return;
        }
        pendingMaskByPlayer.remove(playerId);
    }

    boolean refund(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        int current = getUnlocksBought(playerId);
        if (current <= 0) {
            return false;
        }
        unlocksBoughtByPlayer.remove(playerId);
        unlockMaskByPlayer.remove(playerId);
        pendingMaskByPlayer.remove(playerId);
        return true;
    }

    void remove(UUID playerId) {
        if (playerId == null) {
            return;
        }
        unlocksBoughtByPlayer.remove(playerId);
        unlockMaskByPlayer.remove(playerId);
        pendingMaskByPlayer.remove(playerId);
    }

    void clear() {
        unlocksBoughtByPlayer.clear();
        unlockMaskByPlayer.clear();
        pendingMaskByPlayer.clear();
    }

    Set<UUID> getTrackedPlayerIds() {
        Set<UUID> playerIds = new HashSet<>();
        playerIds.addAll(unlocksBoughtByPlayer.keySet());
        playerIds.addAll(unlockMaskByPlayer.keySet());
        playerIds.addAll(pendingMaskByPlayer.keySet());
        return playerIds;
    }

    void saveTo(FileConfiguration dataConfig) {
        if (dataConfig == null) {
            return;
        }
        dataConfig.set("sacrificeUnlocksBought", null);
        dataConfig.set("sacrificeUnlockMask", null);
        dataConfig.set("sacrificeUnlockPendingMask", null);
        for (Map.Entry<UUID, Integer> entry : unlocksBoughtByPlayer.entrySet()) {
            dataConfig.set("sacrificeUnlocksBought." + entry.getKey(),
                    Math.max(0, Math.min(SheepMergeManager.SACRIFICE_UNLOCK_MAX, entry.getValue())));
        }
        for (Map.Entry<UUID, Integer> entry : unlockMaskByPlayer.entrySet()) {
            dataConfig.set("sacrificeUnlockMask." + entry.getKey(), normalizeMask(entry.getValue()));
        }
        for (Map.Entry<UUID, Integer> entry : pendingMaskByPlayer.entrySet()) {
            dataConfig.set("sacrificeUnlockPendingMask." + entry.getKey(), normalizeMask(entry.getValue()));
        }
    }

    void loadFrom(FileConfiguration dataConfig) {
        clear();
        if (dataConfig == null) {
            return;
        }
        if (dataConfig.isConfigurationSection("sacrificeUnlocksBought")) {
            dataConfig.getConfigurationSection("sacrificeUnlocksBought").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    unlocksBoughtByPlayer.put(uuid,
                            Math.max(0, Math.min(SheepMergeManager.SACRIFICE_UNLOCK_MAX,
                                    dataConfig.getInt("sacrificeUnlocksBought." + key, 0))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("sacrificeUnlockMask")) {
            dataConfig.getConfigurationSection("sacrificeUnlockMask").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    int mask = normalizeMask(dataConfig.getInt("sacrificeUnlockMask." + key, 0));
                    unlockMaskByPlayer.put(uuid, mask);
                    unlocksBoughtByPlayer.put(uuid, Integer.bitCount(mask));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        if (dataConfig.isConfigurationSection("sacrificeUnlockPendingMask")) {
            dataConfig.getConfigurationSection("sacrificeUnlockPendingMask").getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    pendingMaskByPlayer.put(uuid,
                            normalizeMask(dataConfig.getInt("sacrificeUnlockPendingMask." + key, 0)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid UUIDs.
                }
            });
        }
        for (Map.Entry<UUID, Integer> entry : unlocksBoughtByPlayer.entrySet()) {
            if (!unlockMaskByPlayer.containsKey(entry.getKey())) {
                unlockMaskByPlayer.put(entry.getKey(), firstUnlockBits(entry.getValue()));
            }
        }
        pendingMaskByPlayer.clear();
    }

    private static int getUnlockBit(int unlockId) {
        if (unlockId <= 0 || unlockId > SheepMergeManager.SACRIFICE_UNLOCK_MAX) {
            return 0;
        }
        return 1 << (unlockId - 1);
    }

    private static int normalizeMask(int mask) {
        int allUnlocksMask = (1 << SheepMergeManager.SACRIFICE_UNLOCK_MAX) - 1;
        return mask & allUnlocksMask;
    }

    private static int firstUnlockBits(int unlocksBought) {
        int clamped = Math.max(0, Math.min(SheepMergeManager.SACRIFICE_UNLOCK_MAX, unlocksBought));
        if (clamped <= 0) {
            return 0;
        }
        return (1 << clamped) - 1;
    }
}