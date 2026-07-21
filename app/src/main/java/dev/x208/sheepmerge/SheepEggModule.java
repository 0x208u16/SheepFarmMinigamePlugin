package dev.x208.sheepmerge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

final class SheepEggModule {

    private final Map<UUID, Long> nextEggTimestampByPlayer = new HashMap<>();
    private final Map<UUID, Integer> eggCountByPlayer = new HashMap<>();
    private final Map<UUID, Integer> savedLevels = new HashMap<>();
    private final Map<UUID, Float> savedExpProgress = new HashMap<>();

    void tickEggDistribution(Player player) {
        if (player == null || !SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            return;
        }

        ensureEggCountInitialized(player);
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long next = nextEggTimestampByPlayer.computeIfAbsent(
                playerId,
                key -> now + SheepMergeManager.getEggIntervalSeconds(player) * 1000L);

        if (now < next) {
            updateEggHud(player);
            return;
        }

        if (getEggCount(player) >= SheepMergeManager.getEggCap(player)) {
            nextEggTimestampByPlayer.put(playerId, now + 2000L);
            updateEggHud(player);
            return;
        }

        addEggsInternal(player, 1, false);
        nextEggTimestampByPlayer.put(playerId, now + SheepMergeManager.getEggIntervalSeconds(player) * 1000L);
        updateEggHud(player);
    }

    void addEggs(Player player, int amount) {
        addEggsInternal(player, amount, true);
    }

    void resetEggTimer(Player player) {
        if (player == null) {
            return;
        }
        ensureEggCountInitialized(player);
        nextEggTimestampByPlayer.put(player.getUniqueId(),
                System.currentTimeMillis() + SheepMergeManager.getEggIntervalSeconds(player) * 1000L);
        updateEggHud(player);
    }

    void clearEggTimer(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        nextEggTimestampByPlayer.remove(playerId);
        eggCountByPlayer.remove(playerId);
        restoreSavedExperience(player);
    }

    void clearRuntimeState(UUID playerId) {
        if (playerId == null) {
            return;
        }
        nextEggTimestampByPlayer.remove(playerId);
        eggCountByPlayer.remove(playerId);
    }

    void clearSavedExperienceCache() {
        savedLevels.clear();
        savedExpProgress.clear();
    }

    private void addEggsInternal(Player player, int amount, boolean updateHud) {
        if (player == null || amount <= 0) {
            return;
        }
        ensureEggCountInitialized(player);
        UUID playerId = player.getUniqueId();
        int capped = Math.min(SheepMergeManager.getEggCap(player), getEggCount(player) + amount);
        eggCountByPlayer.put(playerId, capped);
        if (updateHud) {
            updateEggHud(player);
        }
    }

    boolean tryConsumeEgg(Player player) {
        if (player == null) {
            return false;
        }
        ensureEggCountInitialized(player);
        int current = getEggCount(player);
        if (current <= 0) {
            return false;
        }
        eggCountByPlayer.put(player.getUniqueId(), current - 1);
        updateEggHud(player);
        return true;
    }

    private int getEggCount(Player player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, eggCountByPlayer.getOrDefault(player.getUniqueId(), 0));
    }

    private void ensureEggCountInitialized(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (eggCountByPlayer.containsKey(playerId)) {
            return;
        }
        int initialEggs = Math.min(SheepMergeManager.getEggCap(player),
                Math.max(0, SheepMergeManager.getStartEggsBonus(player)));
        eggCountByPlayer.put(playerId, initialEggs);
    }

    private void updateEggHud(Player player) {
        if (player == null || !SheepMergeManager.isSheepFarmWorld(player.getWorld())) {
            return;
        }

        saveExperienceStateIfNeeded(player);
        int eggCount = getEggCount(player);
        int eggCap = SheepMergeManager.getEggCap(player);
        player.setLevel(eggCount);

        if (eggCount >= eggCap) {
            player.setExp(1.0f);
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMs = Math.max(1000L, SheepMergeManager.getEggIntervalSeconds(player) * 1000L);
        long next = nextEggTimestampByPlayer.getOrDefault(player.getUniqueId(), now + intervalMs);
        long remainingMs = Math.max(0L, next - now);
        float progress = 1.0f - Math.min(1.0f, remainingMs / (float) intervalMs);
        player.setExp(Math.max(0.0f, Math.min(1.0f, progress)));
    }

    private void saveExperienceStateIfNeeded(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (savedLevels.containsKey(playerId)) {
            return;
        }
        savedLevels.put(playerId, player.getLevel());
        savedExpProgress.put(playerId, player.getExp());
    }

    private void restoreSavedExperience(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        Integer savedLevel = savedLevels.remove(playerId);
        Float savedExp = savedExpProgress.remove(playerId);
        if (savedLevel == null || savedExp == null) {
            return;
        }
        player.setLevel(savedLevel);
        player.setExp(savedExp);
    }
}