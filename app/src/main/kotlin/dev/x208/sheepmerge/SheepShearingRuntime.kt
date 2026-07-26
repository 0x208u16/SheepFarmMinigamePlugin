package dev.x208.sheepmerge

import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import org.bukkit.Particle
import org.bukkit.Sound
import java.math.BigInteger

object SheepShearingRuntime {
    @JvmStatic
    fun calculatePoints(player: Player?, tier: SheepTier?, sheep: Sheep?): BigInteger {
        val base = if (tier == SheepTier.RAINBOW) {
            val rainbowTier = sheep?.let(SheepEntityRuntime::getRainbowTier) ?: 1
            val effectiveLevel = (SheepTier.RAINBOW.level + rainbowTier - 1).coerceAtLeast(0)
            BigInteger.valueOf(4L).pow(effectiveLevel)
        } else {
            BigInteger.valueOf(4L).pow((tier?.level ?: 0).coerceAtLeast(0))
        }
        val combinedMultiplier = SheepMergeManager.getShearPointMultiplier(player).coerceAtLeast(1).toLong() *
            SheepMergeManager.entityGetAchievementPointMultiplier(player).coerceAtLeast(1)
        var points = base.multiply(BigInteger.valueOf(combinedMultiplier.coerceAtLeast(1L)))
        if (SheepMergeManager.entityIsJackpotShearsActive(player)) {
            points = points.multiply(BigInteger.valueOf(2L + SheepMergeManager.getQuestUpgradePowerLevel(player)))
        }
        if (SheepMergeManager.entityRandomNextInt(100) < SheepMergeManager.getDoublePointsChancePercent(player)) {
            points = points.multiply(BigInteger.TWO)
        }
        return points.max(BigInteger.ONE)
    }

    @JvmStatic
    fun shearForPlayer(player: Player?, sheep: Sheep?): Boolean {
        if (player == null || sheep?.world == null || !SheepMergeManager.isSheepFarmWorld(sheep.world)) return false
        if (SheepMergeManager.entityIsAutoShearActive(player)) {
            return SheepMergeManager.entityQueueShearAllEligibleSheep(player, sheep)
        }
        return shearSingle(player, sheep)
    }

    @JvmStatic
    fun shearSingle(player: Player?, sheep: Sheep?): Boolean {
        if (player == null || sheep?.world == null || !SheepMergeManager.isSheepFarmWorld(sheep.world)) return false
        if (!sheep.isAdult) return false
        val now = System.currentTimeMillis()
        val nextEatAt = SheepEntityRuntime.getNextEatTimestamp(sheep)
        if (nextEatAt > now) {
            sheep.isSheared = true
            SheepEntityRuntime.updateName(sheep)
            return false
        }
        if (sheep.isSheared) return false

        sheep.isSheared = true
        sheep.setAI(true)
        SheepAchievementRuntime.recordShear(player)
        val tier = SheepEntityRuntime.getTier(sheep)
        SheepEntityRuntime.setNextEatTimestamp(
            sheep,
            System.currentTimeMillis() + SheepMergeManager.getEatCooldownSeconds(sheep, tier) * 1000L
        )
        SheepMergeManager.addPoints(player, calculatePoints(player, tier, sheep))
        SheepMergeManager.tryTriggerShearWoolSave(player, sheep)
        SheepMergeManager.tryTriggerShearTierBoost(player, sheep)
        SheepEntityRuntime.updateName(sheep)
        SheepMergeManager.recordQuestShear(player)
        SheepMergeManager.recordTutorialShear(player)
        SheepMergeManager.updatePointsScoreboard(player)
        return true
    }

    @JvmStatic
    fun tryWoolSave(player: Player?, sheep: Sheep?): Boolean {
        if (player == null || sheep == null) return false
        val chance = SheepMergeManager.getShearWoolSaveChancePercent(player)
        if (chance <= 0 || SheepMergeManager.entityRandomNextInt(100) >= chance) return false
        sheep.isSheared = false
        SheepEntityRuntime.setNextEatTimestamp(sheep, 0L)
        SheepEntityRuntime.updateName(sheep)
        SheepMergeManager.showOverlay(player, SheepMergeManager.accent("Wool Keeper triggered: wool preserved"))
        SheepMergeManager.entityPlaySound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.35f)
        return true
    }

    @JvmStatic
    fun tryTierBoost(player: Player?, sheep: Sheep?): Boolean {
        if (player == null || sheep == null) return false
        val chance = SheepMergeManager.getShearTierBoostChancePercent(player)
        if (chance <= 0 || SheepMergeManager.entityRandomNextInt(100) >= chance) return false
        val currentTier = SheepEntityRuntime.getTier(sheep)
        if (!currentTier.hasNext()) return false
        val upgradedTier = currentTier.next()
        SheepEntityRuntime.setTier(sheep, upgradedTier)
        val rainbowTier = if (upgradedTier == SheepTier.RAINBOW) SheepEntityRuntime.getRainbowTier(sheep) else 0
        if (SheepMergeManager.shouldAnnounceTierUnlock(player, upgradedTier, rainbowTier)) {
            SheepMergeManager.announceTierUnlock(player, upgradedTier, rainbowTier)
            SheepMergeManager.markTierUnlockAnnounced(player, upgradedTier, rainbowTier)
        }
        SheepMergeManager.entitySpawnParticle(
            sheep.world, Particle.VILLAGER_HAPPY, sheep.location.add(0.0, 0.7, 0.0),
            12, 0.25, 0.2, 0.25, 0.02
        )
        SheepMergeManager.showOverlay(
            player,
            SheepMergeManager.accent("Tier Booster triggered: ${currentTier.displayName}") +
                SheepMergeManager.color(" &7-> ") + upgradedTier.displayName
        )
        SheepMergeManager.entityPlayTierBoostProcSound(player)
        return true
    }
}