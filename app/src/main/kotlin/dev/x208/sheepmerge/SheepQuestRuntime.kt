package dev.x208.sheepmerge

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

object SheepQuestRuntime {
    private const val BASE_QUEST_RESET_MS = 15L * 60L * 1000L
    private const val MIN_QUEST_RESET_MS = 5L * 60L * 1000L
    private const val QUEST_RESET_REDUCTION_PER_PRESTIGE_MS = 60L * 1000L
    private const val ABILITY_AURA_SOUND_INTERVAL_MS = 15_000L
    private const val LUCKY_BURST_SPAWN_CHANCE_BONUS_PERCENT = 50

    private var shearsTarget = 20
    private var spawnsTarget = 12
    private var mergesTarget = 8
    private var shearsReward = 8
    private var spawnsReward = 5
    private var mergesReward = 7
    private var luckyBurstBaseCost = 8
    private var woolRushBaseCost = 10
    private var jackpotShearsBaseCost = 15
    private var autoMergeBaseCost = 18
    private var autoShearBaseCost = 12
    private var luckyBurstBaseDurationMs = 3L * 60L * 1000L
    private var woolRushBaseDurationMs = 4L * 60L * 1000L
    private var jackpotShearsBaseDurationMs = 2L * 60L * 1000L
    private var autoMergeBaseDurationMs = 90L * 1000L
    private var autoShearBaseDurationMs = 90L * 1000L
    private var upgradeDurationBaseCost = 12
    private var upgradePowerBaseCost = 15

    @JvmStatic
    fun configure(
        shearsTarget: Int,
        spawnsTarget: Int,
        mergesTarget: Int,
        shearsReward: Int,
        spawnsReward: Int,
        mergesReward: Int,
        luckyBurstBaseCost: Int,
        woolRushBaseCost: Int,
        jackpotShearsBaseCost: Int,
        autoMergeBaseCost: Int,
        autoShearBaseCost: Int,
        luckyBurstBaseDurationMs: Long,
        woolRushBaseDurationMs: Long,
        jackpotShearsBaseDurationMs: Long,
        autoMergeBaseDurationMs: Long,
        autoShearBaseDurationMs: Long,
        upgradeDurationBaseCost: Int,
        upgradePowerBaseCost: Int,
    ) {
        this.shearsTarget = shearsTarget
        this.spawnsTarget = spawnsTarget
        this.mergesTarget = mergesTarget
        this.shearsReward = shearsReward
        this.spawnsReward = spawnsReward
        this.mergesReward = mergesReward
        this.luckyBurstBaseCost = luckyBurstBaseCost
        this.woolRushBaseCost = woolRushBaseCost
        this.jackpotShearsBaseCost = jackpotShearsBaseCost
        this.autoMergeBaseCost = autoMergeBaseCost
        this.autoShearBaseCost = autoShearBaseCost
        this.luckyBurstBaseDurationMs = luckyBurstBaseDurationMs
        this.woolRushBaseDurationMs = woolRushBaseDurationMs
        this.jackpotShearsBaseDurationMs = jackpotShearsBaseDurationMs
        this.autoMergeBaseDurationMs = autoMergeBaseDurationMs
        this.autoShearBaseDurationMs = autoShearBaseDurationMs
        this.upgradeDurationBaseCost = upgradeDurationBaseCost
        this.upgradePowerBaseCost = upgradePowerBaseCost
    }

    @JvmStatic
    fun getPoints(player: Player?): Int =
        player?.let { SheepQuestState.questPoints().getOrDefault(it.uniqueId, 10) } ?: 0

    @JvmStatic
    fun getTarget(player: Player?, baseTarget: Int): Int {
        val effectiveBase = max(1, baseTarget)
        return if (SheepMergeManager.questHasQuestMaster(player)) effectiveBase * 2 else effectiveBase
    }

    @JvmStatic
    fun getReward(player: Player?, baseReward: Int): Int {
        val effectiveBase = max(1, baseReward)
        return if (SheepMergeManager.questHasQuestMaster(player)) effectiveBase * 2 else effectiveBase
    }

    @JvmStatic fun getShearsTarget(player: Player?) = getTarget(player, shearsTarget)
    @JvmStatic fun getSpawnsTarget(player: Player?) = getTarget(player, spawnsTarget)
    @JvmStatic fun getMergesTarget(player: Player?) = getTarget(player, mergesTarget)
    @JvmStatic fun getShearsReward(player: Player?) = getReward(player, shearsReward)
    @JvmStatic fun getSpawnsReward(player: Player?) = getReward(player, spawnsReward)
    @JvmStatic fun getMergesReward(player: Player?) = getReward(player, mergesReward)

    @JvmStatic
    fun addPoints(player: Player?, amount: Int) {
        if (player == null || amount <= 0) return
        val boosted = max(1, amount) * SheepMergeManager.questPointsGainMultiplier(player)
        SheepQuestState.questPoints()[player.uniqueId] =
            SheepMergeManager.questAddSaturated(getPoints(player), boosted)
        SheepMergeManager.questSaveData()
    }

    @JvmStatic
    fun trySpendPoints(player: Player?, amount: Int): Boolean {
        if (player == null || amount <= 0) return false
        val current = getPoints(player)
        if (current < amount) return false
        SheepQuestState.questPoints()[player.uniqueId] = current - amount
        SheepMergeManager.questSaveData()
        return true
    }

    @JvmStatic
    fun getResetIntervalMs(player: Player?): Long {
        if (SheepMergeManager.questHasQuestMaster(player)) return 150_000L
        val interval = BASE_QUEST_RESET_MS -
            SheepMergeManager.questPrestigeLevel(player) * QUEST_RESET_REDUCTION_PER_PRESTIGE_MS
        return max(MIN_QUEST_RESET_MS, interval)
    }

    @JvmStatic
    fun getResetRemainingMs(player: Player?): Long {
        if (player == null) return 0L
        val nextReset = SheepQuestState.nextQuestResetTimestamps().getOrDefault(player.uniqueId, 0L)
        if (nextReset <= 0L) return getResetIntervalMs(player)
        return max(0L, nextReset - System.currentTimeMillis())
    }

    @JvmStatic
    fun tick(player: Player?) {
        if (player == null || !SheepMergeManager.questIsSheepFarmWorld(player.world)) return
        val playerId = player.uniqueId
        val now = System.currentTimeMillis()
        val interval = getResetIntervalMs(player)
        var nextReset = SheepQuestState.nextQuestResetTimestamps().getOrDefault(playerId, 0L)
        if (nextReset > now + interval) {
            nextReset = now + interval
            SheepQuestState.nextQuestResetTimestamps()[playerId] = nextReset
        }
        if (nextReset <= 0L) {
            SheepQuestState.nextQuestResetTimestamps()[playerId] = now + interval
            return
        }
        if (now < nextReset) return

        SheepQuestState.questShears()[playerId] = 0
        SheepQuestState.questSpawns()[playerId] = 0
        SheepQuestState.questMerges()[playerId] = 0
        SheepQuestState.questShearsComplete()[playerId] = false
        SheepQuestState.questSpawnsComplete()[playerId] = false
        SheepQuestState.questMergesComplete()[playerId] = false
        SheepQuestState.nextQuestResetTimestamps()[playerId] = now + interval
        player.sendTitle(
            SheepMergeManager.questColor("&eNew quests"),
            SheepMergeManager.questColor("&7Quest board refreshed"),
            10,
            40,
            10,
        )
    }

    @JvmStatic
    fun recordShear(player: Player?) = updateProgress(
        player,
        SheepQuestState.questShears(),
        SheepQuestState.questShearsComplete(),
        getShearsTarget(player),
        getShearsReward(player),
        "Shearing quest complete",
    )

    @JvmStatic
    fun recordSpawn(player: Player?) = updateProgress(
        player,
        SheepQuestState.questSpawns(),
        SheepQuestState.questSpawnsComplete(),
        getSpawnsTarget(player),
        getSpawnsReward(player),
        "Spawning quest complete",
    )

    @JvmStatic
    fun recordMerge(player: Player?) = updateProgress(
        player,
        SheepQuestState.questMerges(),
        SheepQuestState.questMergesComplete(),
        getMergesTarget(player),
        getMergesReward(player),
        "Merging quest complete",
    )

    private fun updateProgress(
        player: Player?,
        progress: MutableMap<UUID, Int>,
        completed: MutableMap<UUID, Boolean>,
        target: Int,
        reward: Int,
        completionText: String,
    ) {
        if (player == null || !SheepMergeManager.questIsSheepFarmWorld(player.world)) return
        val playerId = player.uniqueId
        if (completed.getOrDefault(playerId, false)) return
        val value = progress.getOrDefault(playerId, 0) + 1
        progress[playerId] = value
        if (value < target) return

        completed[playerId] = true
        val boostedReward = max(1, (reward * SheepMergeManager.questPrestigeRewardMultiplier(player)).roundToInt())
        addPoints(player, boostedReward)
        player.sendMessage(
            SheepMergeManager.questAction(
                "$completionText: +${SheepMergeManager.formatPoints(boostedReward.toLong())} quest points",
            ),
        )
        SheepMergeManager.questPlaySound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f)
        SheepMergeManager.questSpawnParticle(
            player,
            Particle.VILLAGER_HAPPY,
            player.location.add(0.0, 1.0, 0.0),
            14,
            0.35,
            0.4,
            0.35,
            0.02,
        )
        if (areAllCompleted(playerId)) {
            SheepLifetimeProgressState.incrementCompletedQuestCycles(playerId)
            player.sendTitle(
                SheepMergeManager.questColor("&aAll Quests Complete"),
                SheepMergeManager.questColor("&7Nice cycle. New quests on reset."),
                10,
                45,
                10,
            )
            SheepMergeManager.questPlaySound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f)
            player.sendMessage(SheepMergeManager.questAction("All current quests are completed."))
        }
    }

    private fun areAllCompleted(playerId: UUID): Boolean =
        SheepQuestState.questShearsComplete().getOrDefault(playerId, false) &&
            SheepQuestState.questSpawnsComplete().getOrDefault(playerId, false) &&
            SheepQuestState.questMergesComplete().getOrDefault(playerId, false)

    @JvmStatic
    fun getUpgradeDurationLevel(player: Player?): Int =
        player?.let { SheepQuestState.questUpgradeDurations().getOrDefault(it.uniqueId, 0) } ?: 0

    @JvmStatic
    fun getUpgradePowerLevel(player: Player?): Int =
        player?.let { SheepQuestState.questUpgradePowers().getOrDefault(it.uniqueId, 0) } ?: 0

    @JvmStatic
    fun getUpgradeDurationCost(player: Player?): Int =
        SheepMergeManager.questDoubledUpgradeCost(upgradeDurationBaseCost, getUpgradeDurationLevel(player))

    @JvmStatic
    fun getUpgradePowerCost(player: Player?): Int =
        SheepMergeManager.questDoubledUpgradeCost(upgradePowerBaseCost, getUpgradePowerLevel(player))

    @JvmStatic
    fun getAbilityDurationMs(player: Player?, baseDurationMs: Long): Long =
        baseDurationMs + getUpgradeDurationLevel(player) * 30_000L

    @JvmStatic
    fun getAbilityUseCount(player: Player?, baseDurationMs: Long): Int =
        max(1, ceil(getAbilityDurationMs(player, baseDurationMs) / 1000.0).toInt())

    @JvmStatic fun getLuckyBurstUseCount(player: Player?) = getAbilityUseCount(player, luckyBurstBaseDurationMs)
    @JvmStatic fun getAutoMergeUseCount(player: Player?) = getAbilityUseCount(player, autoMergeBaseDurationMs)
    @JvmStatic fun getAutoShearUseCount(player: Player?) = getAbilityUseCount(player, autoShearBaseDurationMs)
    @JvmStatic fun getWoolRushDurationMs(player: Player?) = getAbilityDurationMs(player, woolRushBaseDurationMs)
    @JvmStatic fun getJackpotDurationMs(player: Player?) = getAbilityDurationMs(player, jackpotShearsBaseDurationMs)
    @JvmStatic fun getLuckyBurstCost(player: Player?) = max(3, luckyBurstBaseCost - getUpgradePowerLevel(player))
    @JvmStatic fun getWoolRushCost(player: Player?) = max(4, woolRushBaseCost - getUpgradePowerLevel(player))
    @JvmStatic fun getJackpotCost(player: Player?) = max(6, jackpotShearsBaseCost - getUpgradePowerLevel(player))
    @JvmStatic fun getAutoMergeCost(player: Player?) = max(8, autoMergeBaseCost - getUpgradePowerLevel(player))
    @JvmStatic fun getAutoShearCost(player: Player?) = max(5, autoShearBaseCost - getUpgradePowerLevel(player))
    @JvmStatic fun getLuckyBurstBonusPercent() = LUCKY_BURST_SPAWN_CHANCE_BONUS_PERCENT

    @JvmStatic
    fun isAbilityActive(activeUntil: Map<UUID, Long>?, playerId: UUID?): Boolean =
        playerId != null && activeUntil?.getOrDefault(playerId, 0L)?.let { it > System.currentTimeMillis() } == true

    @JvmStatic
    fun isCountAbilityActive(
        remainingUses: Map<UUID, Int>?,
        enabled: Map<UUID, Boolean>?,
        playerId: UUID?,
    ): Boolean = playerId != null &&
        remainingUses?.getOrDefault(playerId, 0)?.let { it > 0 } == true &&
        enabled?.getOrDefault(playerId, true) == true

    @JvmStatic
    fun getCountAbilityRemainingUses(remainingUses: Map<UUID, Int>?, playerId: UUID?): Int =
        if (playerId == null) 0 else max(0, remainingUses?.getOrDefault(playerId, 0) ?: 0)

    @JvmStatic
    fun toggleCountAbilityEnabled(
        player: Player?,
        remainingUses: MutableMap<UUID, Int>?,
        enabled: MutableMap<UUID, Boolean>?,
    ): Boolean {
        if (player == null || remainingUses == null || enabled == null) return false
        val playerId = player.uniqueId
        if (remainingUses.getOrDefault(playerId, 0) <= 0) return false
        enabled[playerId] = !enabled.getOrDefault(playerId, true)
        SheepMergeManager.questSaveData()
        return true
    }

    @JvmStatic
    fun consumeCountAbilityUse(remainingUses: MutableMap<UUID, Int>?, playerId: UUID?) {
        consumeCountAbilityUses(remainingUses, playerId, 1)
    }

    @JvmStatic
    fun consumeCountAbilityUses(remainingUses: MutableMap<UUID, Int>?, playerId: UUID?, useCount: Int) {
        if (remainingUses == null || playerId == null) return
        val usesToConsume = max(0, useCount)
        if (usesToConsume <= 0) return
        val current = max(0, remainingUses.getOrDefault(playerId, 0))
        if (current <= usesToConsume) remainingUses.remove(playerId)
        else remainingUses[playerId] = current - usesToConsume
    }

    @JvmStatic
    fun activateAbility(
        player: Player?,
        activeUntil: MutableMap<UUID, Long>,
        questPointCost: Int,
        durationMs: Long,
        sound: Sound,
        particle: Particle,
    ): Boolean {
        if (player == null || !trySpendPoints(player, questPointCost)) return false
        activeUntil[player.uniqueId] = System.currentTimeMillis() + durationMs
        SheepMergeManager.questPlayActivationEffect(player, sound, particle)
        return true
    }

    @JvmStatic
    fun extendAbility(
        player: Player?,
        activeUntil: MutableMap<UUID, Long>?,
        questPointCost: Int,
        durationMs: Long,
        sound: Sound,
        particle: Particle,
    ): Boolean {
        if (player == null || activeUntil == null || !trySpendPoints(player, questPointCost)) return false
        val playerId = player.uniqueId
        val now = System.currentTimeMillis()
        val currentRemaining = max(0L, activeUntil.getOrDefault(playerId, 0L) - now)
        activeUntil[playerId] = now + currentRemaining + durationMs
        SheepMergeManager.questPlayActivationEffect(player, sound, particle)
        SheepMergeManager.questSaveData()
        return true
    }

    @JvmStatic
    fun activateCountAbility(
        player: Player?,
        remainingUses: MutableMap<UUID, Int>?,
        enabled: MutableMap<UUID, Boolean>?,
        questPointCost: Int,
        useCount: Int,
        sound: Sound,
        particle: Particle,
    ): Boolean {
        if (player == null || remainingUses == null || enabled == null || !trySpendPoints(player, questPointCost)) {
            return false
        }
        val playerId = player.uniqueId
        remainingUses[playerId] = SheepMergeManager.questAddSaturated(
            remainingUses.getOrDefault(playerId, 0),
            useCount,
        )
        enabled[playerId] = true
        SheepMergeManager.questPlayActivationEffect(player, sound, particle)
        SheepMergeManager.questSaveData()
        return true
    }

    @JvmStatic
    fun upgradeDuration(player: Player?): Boolean {
        if (player == null || !trySpendPoints(player, getUpgradeDurationCost(player))) return false
        SheepQuestState.questUpgradeDurations()[player.uniqueId] = getUpgradeDurationLevel(player) + 1
        SheepMergeManager.questSaveData()
        return true
    }

    @JvmStatic
    fun upgradePower(player: Player?): Boolean {
        if (player == null || !trySpendPoints(player, getUpgradePowerCost(player))) return false
        SheepQuestState.questUpgradePowers()[player.uniqueId] = getUpgradePowerLevel(player) + 1
        SheepMergeManager.questSaveData()
        return true
    }

    @JvmStatic fun isLuckyBurstActive(playerId: UUID?) = isCountAbilityActive(
        SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(), playerId,
    )
    @JvmStatic fun isWoolRushActive(playerId: UUID?) = isAbilityActive(SheepQuestState.activeWoolRushUntil(), playerId)
    @JvmStatic fun isJackpotActive(playerId: UUID?) = isAbilityActive(SheepQuestState.activeJackpotShearsUntil(), playerId)
    @JvmStatic fun isAutoMergeActive(playerId: UUID?) = isCountAbilityActive(
        SheepQuestState.activeAutoMergeUses(), SheepQuestState.autoMergeEnabled(), playerId,
    )
    @JvmStatic fun isAutoShearActive(playerId: UUID?) = isCountAbilityActive(
        SheepQuestState.activeAutoShearUses(), SheepQuestState.autoShearEnabled(), playerId,
    )

    @JvmStatic fun toggleLuckyBurst(player: Player?) = toggleCountAbilityEnabled(
        player, SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
    )
    @JvmStatic fun toggleAutoMerge(player: Player?) = toggleCountAbilityEnabled(
        player, SheepQuestState.activeAutoMergeUses(), SheepQuestState.autoMergeEnabled(),
    )
    @JvmStatic fun toggleAutoShear(player: Player?) = toggleCountAbilityEnabled(
        player, SheepQuestState.activeAutoShearUses(), SheepQuestState.autoShearEnabled(),
    )

    @JvmStatic fun activateLuckyBurst(player: Player?) = activateCountAbility(
        player, SheepQuestState.activeLuckyBurstUses(), SheepQuestState.luckyBurstEnabled(),
        getLuckyBurstCost(player), getLuckyBurstUseCount(player), Sound.BLOCK_BEACON_POWER_SELECT, Particle.END_ROD,
    )
    @JvmStatic fun applyWoolRush(player: Player?, extend: Boolean) = if (extend) extendAbility(
        player, SheepQuestState.activeWoolRushUntil(), getWoolRushCost(player), getWoolRushDurationMs(player),
        Sound.ENTITY_ENDER_DRAGON_FLAP, Particle.CLOUD,
    ) else activateAbility(
        player, SheepQuestState.activeWoolRushUntil(), getWoolRushCost(player), getWoolRushDurationMs(player),
        Sound.ENTITY_ENDER_DRAGON_FLAP, Particle.CLOUD,
    )
    @JvmStatic fun applyJackpot(player: Player?, extend: Boolean) = if (extend) extendAbility(
        player, SheepQuestState.activeJackpotShearsUntil(), getJackpotCost(player), getJackpotDurationMs(player),
        Sound.ENTITY_PLAYER_LEVELUP, Particle.CRIT,
    ) else activateAbility(
        player, SheepQuestState.activeJackpotShearsUntil(), getJackpotCost(player), getJackpotDurationMs(player),
        Sound.ENTITY_PLAYER_LEVELUP, Particle.CRIT,
    )
    @JvmStatic fun activateAutoMerge(player: Player?) = activateCountAbility(
        player, SheepQuestState.activeAutoMergeUses(), SheepQuestState.autoMergeEnabled(),
        getAutoMergeCost(player), getAutoMergeUseCount(player), Sound.BLOCK_PISTON_EXTEND, Particle.ENCHANTMENT_TABLE,
    )
    @JvmStatic fun activateAutoShear(player: Player?) = activateCountAbility(
        player, SheepQuestState.activeAutoShearUses(), SheepQuestState.autoShearEnabled(),
        getAutoShearCost(player), getAutoShearUseCount(player), Sound.ENTITY_SHEEP_SHEAR, Particle.WAX_OFF,
    )

    @JvmStatic
    fun tryAutoActivateAbility(player: Player?, buyAllMissing: Boolean): Boolean {
        if (player == null) return false
        val playerId = player.uniqueId
        var changed = false
        if (getCountAbilityRemainingUses(SheepQuestState.activeAutoShearUses(), playerId) <= 0 &&
            activateAutoShear(player)
        ) {
            SheepQuestState.nextAutoShearAt()[playerId] = 0L
            if (!buyAllMissing) return true
            changed = true
        }
        if (getCountAbilityRemainingUses(SheepQuestState.activeAutoMergeUses(), playerId) <= 0 &&
            activateAutoMerge(player)
        ) {
            SheepQuestState.nextAutoMergeAt()[playerId] = 0L
            if (!buyAllMissing) return true
            changed = true
        }
        if (!isJackpotActive(playerId) && applyJackpot(player, false)) {
            if (!buyAllMissing) return true
            changed = true
        }
        if (!isWoolRushActive(playerId) && applyWoolRush(player, false)) {
            if (!buyAllMissing) return true
            changed = true
        }
        if (getCountAbilityRemainingUses(SheepQuestState.activeLuckyBurstUses(), playerId) <= 0 &&
            activateLuckyBurst(player)
        ) {
            if (!buyAllMissing) return true
            changed = true
        }
        return changed
    }

    @JvmStatic
    fun tickAbilities(player: Player?, now: Long) {
        if (player == null) return
        val playerId = player.uniqueId
        tickAbilityVisual(player, playerId, now, SheepQuestState.activeWoolRushUntil(), Particle.CLOUD, "Wool Rush ended")
        tickAbilityVisual(player, playerId, now, SheepQuestState.activeJackpotShearsUntil(), Particle.CRIT, "Jackpot Shears ended")
        emitAbilityAura(player, playerId, now)
        if (!isAutoMergeActive(playerId)) SheepQuestState.nextAutoMergeAt().remove(playerId)
        tickAutoShear(player, playerId, now)
    }

    private fun tickAbilityVisual(
        player: Player,
        playerId: UUID,
        now: Long,
        activeUntil: MutableMap<UUID, Long>,
        particle: Particle,
        endedText: String,
    ) {
        val until = activeUntil.getOrDefault(playerId, 0L)
        if (until <= 0L) return
        if (now >= until) {
            activeUntil.remove(playerId)
            player.sendMessage(SheepMergeManager.questHint(endedText))
            SheepMergeManager.questPlaySound(player, Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.6f)
            return
        }
        SheepMergeManager.questSpawnParticle(player, particle, player.location.add(0.0, 2.0, 0.0), 5, 0.25, 0.35, 0.25, 0.01)
    }

    private fun emitAbilityAura(player: Player, playerId: UUID, now: Long) {
        var hasActiveAbility = false
        if (isLuckyBurstActive(playerId)) {
            hasActiveAbility = true
            SheepMergeManager.questSpawnParticle(player, Particle.TOTEM, player.location.add(0.0, 2.1, 0.0), 2, 0.18, 0.28, 0.18, 0.0)
        }
        if (isWoolRushActive(playerId)) {
            hasActiveAbility = true
            SheepMergeManager.questSpawnParticle(player, Particle.SPORE_BLOSSOM_AIR, player.location.add(0.0, 1.9, 0.0), 4, 0.22, 0.26, 0.22, 0.01)
        }
        if (isJackpotActive(playerId)) {
            hasActiveAbility = true
            SheepMergeManager.questSpawnParticle(player, Particle.FIREWORKS_SPARK, player.location.add(0.0, 2.25, 0.0), 3, 0.25, 0.35, 0.25, 0.01)
        }
        if (isAutoMergeActive(playerId)) {
            hasActiveAbility = true
            SheepMergeManager.questSpawnParticle(player, Particle.WAX_ON, player.location.add(0.0, 2.0, 0.0), 5, 0.22, 0.28, 0.22, 0.02)
        }
        if (isAutoShearActive(playerId)) {
            hasActiveAbility = true
            SheepMergeManager.questSpawnParticle(player, Particle.WAX_OFF, player.location.add(0.0, 2.0, 0.0), 5, 0.22, 0.28, 0.22, 0.02)
        }
        if (!hasActiveAbility) return
        val lastSoundAt = SheepQuestState.lastAbilityAuraSoundTimestamps().getOrDefault(playerId, 0L)
        if (now - lastSoundAt < ABILITY_AURA_SOUND_INTERVAL_MS) return
        SheepQuestState.lastAbilityAuraSoundTimestamps()[playerId] = now
        SheepMergeManager.questPlayRandomAuraSound(player)
    }

    private fun tickAutoShear(player: Player, playerId: UUID, now: Long) {
        if (!isAutoShearActive(playerId)) {
            SheepQuestState.nextAutoShearAt().remove(playerId)
            return
        }
        val nextAt = SheepQuestState.nextAutoShearAt().getOrDefault(playerId, 0L)
        if (now < nextAt) return
        SheepQuestState.nextAutoShearAt()[playerId] = now + 100L
        tryAutoShearLookTarget(player)
    }

    @JvmStatic
    fun tryAutoShearLookTarget(player: Player?): Boolean {
        if (player == null || !SheepMergeManager.questIsSheepFarmWorld(player.world)) return false
        val result = player.world.rayTraceEntities(
            player.eyeLocation,
            player.location.direction,
            6.0,
            0.22,
        ) { it is Sheep }
        return SheepMergeManager.questShearSheep(player, result?.hitEntity as? Sheep)
    }

    @JvmStatic
    fun tryAutoMergeOnce(player: Player?): Boolean {
        if (player == null || !SheepMergeManager.questIsSheepFarmWorld(player.world) ||
            !SheepMergeManager.isFarmOwner(player, player.world)
        ) return false
        val firstByTier = HashMap<String, Sheep>()
        for (sheep in player.world.getEntitiesByClass(Sheep::class.java)) {
            if (!sheep.isValid || sheep.isDead || sheep.isInsideVehicle) continue
            val tier = SheepMergeManager.getSheepTier(sheep) ?: continue
            val key = if (tier == SheepTier.RAINBOW) "${tier.level}:${SheepMergeManager.getRainbowTier(sheep)}" else tier.level.toString()
            val first = firstByTier.putIfAbsent(key, sheep)
            if (first == null || !first.isValid || first.uniqueId == sheep.uniqueId) continue
            return SheepMergeManager.questMergeSheepPair(player, first, sheep, false)
        }
        return false
    }

    @JvmStatic
    fun tryAutoMergeOnPickup(player: Player?, pickedSheep: Sheep?): Boolean {
        if (player == null || pickedSheep == null || !pickedSheep.isValid || !isAutoMergeActive(player.uniqueId)) return false
        if (!SheepMergeManager.isFarmOwner(player, pickedSheep.world)) return false
        val tier = SheepMergeManager.getSheepTier(pickedSheep) ?: return false
        val matches = pickedSheep.world.getEntitiesByClass(Sheep::class.java).filter { candidate ->
            candidate.isValid && !candidate.isDead && SheepMergeManager.getSheepTier(candidate) == tier &&
                (tier != SheepTier.RAINBOW || SheepMergeManager.getRainbowTier(candidate) == SheepMergeManager.getRainbowTier(pickedSheep))
        }
        if (matches.size < 2) return false
        val partner = matches.firstOrNull { it.uniqueId != pickedSheep.uniqueId } ?: return false
        if (!SheepMergeManager.questMergeSheepPair(player, pickedSheep, partner, false)) return false
        consumeCountAbilityUse(SheepQuestState.activeAutoMergeUses(), player.uniqueId)
        return true
    }

    @JvmStatic
    fun tryAutoShearOnce(player: Player?): Boolean {
        if (player == null || !SheepMergeManager.questIsSheepFarmWorld(player.world) ||
            !SheepMergeManager.isFarmOwner(player, player.world)
        ) return false
        val carriedId = SheepMergeManager.questPickedUpSheep(player)?.uniqueId
        val now = System.currentTimeMillis()
        var bestCandidate: Sheep? = null
        var bestTierWeight = -1
        for (sheep in player.world.getEntitiesByClass(Sheep::class.java)) {
            if (!sheep.isValid || sheep.isDead || sheep.isSheared || !sheep.isAdult) continue
            if (SheepMergeManager.questNextEatTimestamp(sheep) > now) {
                sheep.isSheared = true
                SheepMergeManager.questUpdateSheepName(sheep)
                continue
            }
            if (carriedId == sheep.uniqueId) continue
            val tier = SheepMergeManager.getSheepTier(sheep) ?: continue
            val tierWeight = tier.level * 10_000 + if (tier == SheepTier.RAINBOW) SheepMergeManager.getRainbowTier(sheep) else 0
            if (bestCandidate == null || tierWeight > bestTierWeight) {
                bestCandidate = sheep
                bestTierWeight = tierWeight
            }
        }
        return SheepMergeManager.questShearSheep(player, bestCandidate)
    }

    @JvmStatic
    fun getAbilityMenuStatus(activeUntil: Map<UUID, Long>?, playerId: UUID?): String {
        val remaining = getAbilityRemainingMs(activeUntil, playerId)
        return if (remaining > 0L) "&aStatus: ON &7(${SheepMergeManager.questFormatDuration(remaining)} left)" else "&8Status: OFF"
    }

    @JvmStatic
    fun getAbilityScoreLine(label: String, activeUntil: Map<UUID, Long>?, playerId: UUID?): String {
        val remaining = getAbilityRemainingMs(activeUntil, playerId)
        return SheepMergeManager.questColor(
            (if (remaining > 0L) "&d" else "&8") + label + "&8: &f" +
                if (remaining > 0L) SheepMergeManager.questFormatDuration(remaining) else "inactive",
        )
    }

    @JvmStatic
    fun getCountAbilityMenuStatus(remainingUses: Map<UUID, Int>?, enabled: Map<UUID, Boolean>?, playerId: UUID?): String {
        val remaining = getCountAbilityRemainingUses(remainingUses, playerId)
        if (remaining <= 0) return "&8Status: DEACTIVATED"
        return (if (enabled?.getOrDefault(playerId, true) == true) "&aStatus: ON" else "&cStatus: OFF") +
            " (&b$remaining&7 uses left)"
    }

    @JvmStatic
    fun getCountAbilityToggleActionLine(remainingUses: Map<UUID, Int>?, enabled: Map<UUID, Boolean>?, playerId: UUID?): String {
        if (getCountAbilityRemainingUses(remainingUses, playerId) <= 0) return "&aClick: Activate"
        return if (enabled?.getOrDefault(playerId, true) == true) "&cClick: Toggle OFF" else "&aClick: Toggle ON"
    }

    @JvmStatic
    fun getCountAbilityScoreLine(label: String, remainingUses: Map<UUID, Int>?, enabled: Map<UUID, Boolean>?, playerId: UUID?): String {
        val remaining = getCountAbilityRemainingUses(remainingUses, playerId)
        if (remaining <= 0) return SheepMergeManager.questColor("&8$label&8: &finactive")
        val status = if (enabled?.getOrDefault(playerId, true) == true) "&aON" else "&cOFF"
        return SheepMergeManager.questColor("&d$label&8: &f$remaining uses $status")
    }

    private fun getAbilityRemainingMs(activeUntil: Map<UUID, Long>?, playerId: UUID?): Long =
        if (playerId == null) 0L else max(0L, (activeUntil?.getOrDefault(playerId, 0L) ?: 0L) - System.currentTimeMillis())
}