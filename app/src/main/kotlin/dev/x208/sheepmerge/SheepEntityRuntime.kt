package dev.x208.sheepmerge

import org.bukkit.ChatColor
import org.bukkit.DyeColor
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.entity.Sheep
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.util.UUID

object SheepEntityRuntime {
    private const val RAINBOW_ANIMATION_STEP_MS = 220L
    private const val FARM_CENTER_X = 0.5
    private const val FARM_CENTER_Z = 0.5
    private const val FARM_BASE_Y = 100
    private const val FARM_MIN_XZ = -5
    private const val FARM_MAX_XZ = 6
    private const val SHEEP_RESCUE_TIMEOUT_MS = 10_000L
    private const val SHEEP_RESCUE_PATH_DURATION_MS = 1_350L
    private const val SHEEP_RESCUE_CORRECTION_INTERVAL_MS = 80L
    private const val SHEEP_RESCUE_POSITION_CORRECTION_DISTANCE = 0.42
    private const val SHEEP_RESCUE_HORIZONTAL_VELOCITY = 0.62
    private const val SHEEP_RESCUE_UPWARD_VELOCITY = 0.75
    private const val SHEEP_RESCUE_DOWNWARD_VELOCITY = -0.85
    private const val SHEEP_RESCUE_ARCH_HEIGHT_BASE = 1.20
    private const val SHEEP_RESCUE_ARCH_HEIGHT_PER_BLOCK = 0.14
    private const val SHEEP_RESCUE_ARCH_HEIGHT_MAX = 3.20
    private const val SHEEP_RESCUE_EDGE_MARGIN = 0.60
    private const val SHEEP_FALL_TRIGGER_EDGE_MARGIN = 0.25
    private const val SHEEP_FALL_TRIGGER_Y = FARM_BASE_Y + 1.05
    private const val PLAYER_FALL_RECOVERY_MARGIN_ABOVE_VOID = 2.0
    private val rainbowAnimationColors = arrayOf(
        DyeColor.RED,
        DyeColor.ORANGE,
        DyeColor.YELLOW,
        DyeColor.LIME,
        DyeColor.LIGHT_BLUE,
        DyeColor.BLUE,
        DyeColor.PURPLE,
        DyeColor.MAGENTA,
        DyeColor.PINK
    )

    private lateinit var tierKey: NamespacedKey
    private lateinit var nextEatKey: NamespacedKey
    private lateinit var rainbowTierKey: NamespacedKey
    private lateinit var legacyRainbowMergedCountKey: NamespacedKey

    @JvmStatic
    fun initialize(plugin: SheepMergePlugin) {
        tierKey = NamespacedKey(plugin, "sheep-tier")
        nextEatKey = NamespacedKey(plugin, "sheep-next-eat")
        rainbowTierKey = NamespacedKey(plugin, "rainbow-tier")
        legacyRainbowMergedCountKey = NamespacedKey(plugin, "rainbow-merged-count")
    }

    @JvmStatic
    fun getTierKey(): NamespacedKey = tierKey

    @JvmStatic
    fun getTier(sheep: Sheep?): SheepTier {
        if (sheep == null) return SheepTier.WHITE
        val level = sheep.persistentDataContainer.get(tierKey, PersistentDataType.INTEGER) ?: 0
        return SheepTier.byLevel(level)
    }

    @JvmStatic
    fun setTier(sheep: Sheep?, tier: SheepTier?) {
        if (sheep == null || tier == null) return
        sheep.removeWhenFarAway = false
        sheep.isPersistent = true
        sheep.isSilent = true
        sheep.color = tier.color ?: DyeColor.WHITE
        sheep.persistentDataContainer.set(tierKey, PersistentDataType.INTEGER, tier.level)
        if (tier == SheepTier.RAINBOW) {
            setRainbowTier(sheep, getRainbowTier(sheep).coerceAtLeast(1))
        } else {
            sheep.persistentDataContainer.remove(rainbowTierKey)
            sheep.persistentDataContainer.remove(legacyRainbowMergedCountKey)
        }
        if (sheep.isSheared) {
            setNextEatTimestamp(
                sheep,
                System.currentTimeMillis() + SheepMergeManager.getEatCooldownSeconds(sheep, tier) * 1000L
            )
        } else {
            setNextEatTimestamp(sheep, 0L)
        }
        applyRainbowColorAnimation(sheep, tier)
        updateName(sheep)
    }

    @JvmStatic
    fun getRainbowTier(sheep: Sheep?): Int {
        if (sheep == null || getTier(sheep) != SheepTier.RAINBOW) return 1
        val value = sheep.persistentDataContainer.get(rainbowTierKey, PersistentDataType.INTEGER)
            ?: sheep.persistentDataContainer.get(legacyRainbowMergedCountKey, PersistentDataType.INTEGER)
        return value?.coerceAtLeast(1) ?: 1
    }

    @JvmStatic
    fun setRainbowTier(sheep: Sheep?, tier: Int) {
        if (sheep == null) return
        sheep.persistentDataContainer.set(rainbowTierKey, PersistentDataType.INTEGER, tier.coerceAtLeast(1))
        sheep.persistentDataContainer.remove(legacyRainbowMergedCountKey)
        updateName(sheep)
    }

    @JvmStatic
    fun getNextEatTimestamp(sheep: Sheep?): Long {
        if (sheep == null) return 0L
        return sheep.persistentDataContainer.get(nextEatKey, PersistentDataType.LONG) ?: 0L
    }

    @JvmStatic
    fun setNextEatTimestamp(sheep: Sheep?, timestamp: Long) {
        if (sheep == null) return
        sheep.persistentDataContainer.set(nextEatKey, PersistentDataType.LONG, timestamp)
    }

    @JvmStatic
    fun updateName(sheep: Sheep?) {
        if (sheep == null) return
        val tier = getTier(sheep)
        var name = getTierDisplayNameWithColor(tier)
        if (tier == SheepTier.RAINBOW) {
            name += ChatColor.WHITE.toString() + " T" + SheepMergeManager.formatPoints(getRainbowTier(sheep).toLong())
        }
        if (sheep.isSheared) {
            val remainingSeconds = ((getNextEatTimestamp(sheep) - System.currentTimeMillis() + 999L) / 1000L)
                .coerceAtLeast(0L)
            name += ChatColor.YELLOW.toString() + " [" + remainingSeconds + "s]"
        }
        sheep.customName = name
        sheep.isCustomNameVisible = true
    }

    @JvmStatic
    fun getRemainingWoolRegenMs(sheep: Sheep?): Long {
        if (sheep == null || !sheep.isValid || !sheep.isSheared) return 0L
        return (getNextEatTimestamp(sheep) - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    @JvmStatic
    fun getCombinedRemainingWoolRegenMs(first: Sheep?, second: Sheep?): Long {
        return (getRemainingWoolRegenMs(first) + getRemainingWoolRegenMs(second)) / 2L
    }

    @JvmStatic
    fun initializeMergedSheep(sheep: Sheep?, tier: SheepTier?, remainingWoolRegenMs: Long) {
        if (sheep == null || tier == null) return
        sheep.setAI(true)
        sheep.setGravity(true)
        if (remainingWoolRegenMs > 0L) {
            sheep.isSheared = true
            setNextEatTimestamp(sheep, System.currentTimeMillis() + remainingWoolRegenMs)
        } else {
            sheep.isSheared = false
            setNextEatTimestamp(sheep, 0L)
        }
        updateName(sheep)
    }

    @JvmStatic
    fun mergePair(player: Player?, first: Sheep?, second: Sheep?, recordTutorial: Boolean): Boolean {
        if (player == null || first == null || second == null || !first.isValid || !second.isValid) return false
        val tier = getTier(first)
        if (getTier(second) != tier) return false

        val sourceRainbowTier = if (tier == SheepTier.RAINBOW) getRainbowTier(first) else 0
        val otherRainbowTier = if (tier == SheepTier.RAINBOW) getRainbowTier(second) else 0
        if (tier == SheepTier.RAINBOW && sourceRainbowTier != otherRainbowTier) return false

        val world = first.world
        if (world != second.world) return false

        val mergedTier = if (tier.hasNext()) tier.next() else SheepTier.RAINBOW
        val woolReadyCount = (if (!first.isSheared) 1 else 0) + (if (!second.isSheared) 1 else 0)
        val combinedWoolRegenMs = getCombinedRemainingWoolRegenMs(first, second)
        val spawnLocation = second.location.clone()

        first.remove()
        second.remove()

        val mergedSheep = world.spawn(spawnLocation, Sheep::class.java)
        setTier(mergedSheep, mergedTier)
        if (mergedTier == SheepTier.RAINBOW && !tier.hasNext()) {
            setRainbowTier(mergedSheep, sourceRainbowTier + 1)
        }
        initializeMergedSheep(mergedSheep, mergedTier, combinedWoolRegenMs)
        mergedSheep.velocity = Vector(0.0, 0.18, 0.0)
        SheepMergeManager.entitySpawnParticle(
            world,
            Particle.VILLAGER_HAPPY,
            spawnLocation.clone().add(0.0, 0.5, 0.0),
            10,
            0.25,
            0.25,
            0.25,
            0.02
        )

        val mergedRainbowTier = if (mergedTier == SheepTier.RAINBOW) getRainbowTier(mergedSheep) else 0
        if (SheepMergeManager.shouldAnnounceTierUnlock(player, mergedTier, mergedRainbowTier)) {
            SheepMergeManager.announceTierUnlock(player, mergedTier, mergedRainbowTier)
            SheepMergeManager.markTierUnlockAnnounced(player, mergedTier, mergedRainbowTier)
        }
        SheepMergeManager.recordSheepMerge(player, tier, woolReadyCount)
        SheepMergeManager.recordQuestMerge(player)
        if (recordTutorial) {
            SheepMergeManager.recordTutorialMerge(player)
        }
        return true
    }

    @JvmStatic
    fun applyRainbowColorAnimation(sheep: Sheep?, tier: SheepTier?) {
        if (sheep == null || tier != SheepTier.RAINBOW || rainbowAnimationColors.isEmpty()) return
        val index = ((System.currentTimeMillis() / RAINBOW_ANIMATION_STEP_MS) % rainbowAnimationColors.size).toInt()
        sheep.color = rainbowAnimationColors[index]
    }

    @JvmStatic
    fun processEatTimer(sheep: Sheep?) {
        if (sheep == null || !sheep.isValid || sheep.world == null ||
            !SheepMergeManager.isSheepFarmWorld(sheep.world)
        ) return

        sheep.isSilent = true
        applyRescueMotionIfNeeded(sheep)
        val tier = getTier(sheep)
        if (tier == SheepTier.RAINBOW) {
            applyRainbowColorAnimation(sheep, tier)
        } else if (tier.color != null && sheep.color != tier.color) {
            sheep.color = tier.color!!
        }

        val now = System.currentTimeMillis()
        val nextEat = getNextEatTimestamp(sheep)
        if (!sheep.isSheared) {
            if (nextEat > now) {
                sheep.isSheared = true
            } else if (nextEat > 0L) {
                setNextEatTimestamp(sheep, 0L)
            }
            updateName(sheep)
            return
        }
        if (now >= nextEat && nextEat > 0L) {
            promptToEatGrass(sheep)
            return
        }
        sheep.isSheared = true
        sheep.setGravity(true)
        sheep.setAI(true)
        updateName(sheep)
    }

    @JvmStatic
    fun recoverPlayerIfFallen(player: Player?) {
        if (player == null || !player.isOnline || !SheepMergeManager.isSheepFarmWorld(player.world)) return
        val location = player.location
        if (location.y > player.world.minHeight + PLAYER_FALL_RECOVERY_MARGIN_ABOVE_VOID) return
        val target = Location(player.world, FARM_CENTER_X, FARM_BASE_Y + 1.0, FARM_CENTER_Z, location.yaw, location.pitch)
        player.teleport(target)
        player.velocity = Vector(0.0, 0.0, 0.0)
        player.fallDistance = 0.0f
    }

    private fun promptToEatGrass(sheep: Sheep) {
        setNextEatTimestamp(sheep, 0L)
        sheep.setAI(true)
        sheep.setGravity(true)
        sheep.velocity = Vector(0.0, 0.0, 0.0)
        val mouth = sheep.location.add(0.0, 0.35, 0.0)
        SheepMergeManager.entitySpawnParticle(sheep.world, Particle.CLOUD, mouth, 10, 0.18, 0.08, 0.18, 0.01)
        SheepMergeManager.entityPlaySheepSound(sheep.world, mouth, Sound.ENTITY_SHEEP_AMBIENT, 0.75f, 1.05f)
        sheep.isSheared = false
        updateName(sheep)
    }

    private fun applyRescueMotionIfNeeded(sheep: Sheep): Boolean {
        if (!sheep.isValid) return false
        val sheepId = sheep.uniqueId
        val location = sheep.location
        val rescueInProgress = SheepEntityRuntimeState.isRescueInProgress(sheepId)
        val shouldRescue = if (rescueInProgress) {
            !isSafelyOnPlatform(sheep, location)
        } else {
            isOffPlatform(location) || isFallingOffPlatform(sheep, location)
        }
        if (!shouldRescue) {
            SheepEntityRuntimeState.clearRescue(sheepId)
            sheep.isCollidable = true
            return false
        }

        sheep.isCollidable = false
        sheep.setAI(true)
        sheep.setGravity(true)
        val now = System.currentTimeMillis()
        val started = SheepEntityRuntimeState.getOrStartRescue(sheepId, now)
        val origin = SheepEntityRuntimeState.getOrSetRescueOrigin(sheepId, location)
        SheepEntityRuntimeState.ensureRescueCorrectionAt(sheepId, now)
        if (now - started >= SHEEP_RESCUE_TIMEOUT_MS) {
            teleportToFarmCenter(sheep)
            SheepEntityRuntimeState.clearRescue(sheepId)
            sheep.isCollidable = true
            return false
        }

        val desired = getRescuePathTargetLocation(sheep, origin, started, now)
        sheep.velocity = getRescueSteeringVelocity(location, desired)
        val nextCorrectionAt = SheepEntityRuntimeState.getRescueCorrectionAt(sheepId, now)
        if (now >= nextCorrectionAt) {
            if (location.distanceSquared(desired) >= SHEEP_RESCUE_POSITION_CORRECTION_DISTANCE *
                SHEEP_RESCUE_POSITION_CORRECTION_DISTANCE
            ) sheep.teleport(desired)
            SheepEntityRuntimeState.setRescueCorrectionAt(sheepId, now + SHEEP_RESCUE_CORRECTION_INTERVAL_MS)
        }
        sheep.fallDistance = 0.0f
        return true
    }

    private fun getRescuePathTargetLocation(sheep: Sheep, origin: Location?, started: Long, now: Long): Location {
        val current = sheep.location
        val center = Location(sheep.world, FARM_CENTER_X, FARM_BASE_Y + 1.0, FARM_CENTER_Z, current.yaw, current.pitch)
        if (origin == null) return center
        val progress = ((now - started) / SHEEP_RESCUE_PATH_DURATION_MS.toDouble()).coerceIn(0.0, 1.0)
        val dx = center.x - origin.x
        val dz = center.z - origin.z
        val archHeight = minOf(
            SHEEP_RESCUE_ARCH_HEIGHT_MAX,
            SHEEP_RESCUE_ARCH_HEIGHT_BASE + kotlin.math.sqrt(dx * dx + dz * dz) * SHEEP_RESCUE_ARCH_HEIGHT_PER_BLOCK
        )
        val x = lerp(origin.x, center.x, progress)
        val z = lerp(origin.z, center.z, progress)
        val y = lerp(origin.y, center.y, progress) + kotlin.math.sin(Math.PI * progress) * archHeight
        return Location(sheep.world, x, y, z, center.yaw, center.pitch)
    }

    private fun getRescueSteeringVelocity(current: Location, target: Location): Vector {
        val horizon = SHEEP_RESCUE_CORRECTION_INTERVAL_MS / 1000.0
        val toTarget = target.toVector().subtract(current.toVector())
        val horizontal = Vector(toTarget.x, 0.0, toTarget.z)
        val horizontalLength = horizontal.length()
        if (horizontalLength > 0.0001) {
            horizontal.normalize().multiply(minOf(SHEEP_RESCUE_HORIZONTAL_VELOCITY, horizontalLength / maxOf(0.001, horizon)))
        } else {
            horizontal.zero()
        }
        val yVelocity = (toTarget.y / maxOf(0.001, horizon))
            .coerceIn(SHEEP_RESCUE_DOWNWARD_VELOCITY, SHEEP_RESCUE_UPWARD_VELOCITY)
        return Vector(horizontal.x, yVelocity, horizontal.z)
    }

    private fun teleportToFarmCenter(sheep: Sheep) {
        if (!sheep.isValid || sheep.world == null) return
        val current = sheep.location
        sheep.teleport(Location(sheep.world, FARM_CENTER_X, FARM_BASE_Y + 1.0, FARM_CENTER_Z, current.yaw, current.pitch))
        sheep.velocity = Vector(0.0, 0.0, 0.0)
        sheep.fallDistance = 0.0f
    }

    private fun isSafelyOnPlatform(sheep: Sheep, location: Location): Boolean {
        return sheep.isOnGround && location.y >= FARM_BASE_Y - 0.05 && !isOutsideFarmHorizontalBounds(location, -0.20)
    }

    private fun isFallingOffPlatform(sheep: Sheep, location: Location): Boolean {
        return location.y <= SHEEP_FALL_TRIGGER_Y &&
            isOutsideFarmHorizontalBounds(location, -SHEEP_FALL_TRIGGER_EDGE_MARGIN) && sheep.velocity.y < -0.02
    }

    private fun isOffPlatform(location: Location): Boolean {
        return location.y < FARM_BASE_Y - 0.05 || isOutsideFarmHorizontalBounds(location, SHEEP_RESCUE_EDGE_MARGIN)
    }

    private fun isOutsideFarmHorizontalBounds(location: Location, edgeMargin: Double): Boolean {
        val minBound = FARM_MIN_XZ - 0.5 - edgeMargin
        val maxBound = FARM_MAX_XZ + 0.5 + edgeMargin
        return location.x < minBound || location.x > maxBound || location.z < minBound || location.z > maxBound
    }

    private fun lerp(start: Double, end: Double, progress: Double): Double = start + (end - start) * progress

    @JvmStatic
    fun getSheepCount(world: World?): Int {
        if (world == null) return 0
        return SheepEntityRuntimeState.getLiveSheepCount(world.uid, countLiveSheep(world))
    }

    @JvmStatic
    fun isWorldAtLimit(world: World?): Boolean {
        if (world == null) return false
        refreshLiveSheepCount(world)
        return getSheepCount(world) >= SheepMergeManager.getOwnerLimit(world)
    }

    @JvmStatic
    fun refreshLiveSheepCounts(worlds: Iterable<World>?) {
        if (worlds == null) return
        val knownFarmWorlds = HashSet<UUID>()
        for (world in worlds) {
            if (!SheepMergeManager.isSheepFarmWorld(world)) continue
            knownFarmWorlds.add(world.uid)
            refreshLiveSheepCount(world)
        }
        SheepEntityRuntimeState.retainLiveSheepCounts(knownFarmWorlds)
    }

    @JvmStatic
    fun refreshLiveSheepCount(world: World?) {
        if (world == null || !SheepMergeManager.isSheepFarmWorld(world)) return
        SheepEntityRuntimeState.setLiveSheepCount(world.uid, countLiveSheep(world))
    }

    @JvmStatic
    fun storePickedUpSheep(player: Player?, sheep: Sheep?) {
        if (player == null || sheep == null) return
        sheep.setAI(false)
        sheep.setGravity(false)
        sheep.isInvulnerable = true
        sheep.velocity = Vector(0.0, 0.0, 0.0)
        SheepEntityRuntimeState.putCarriedSheep(player.uniqueId, sheep)
        updateCarriedSheepPosition(player)
    }

    @JvmStatic
    fun hasPickedUpSheep(player: Player?): Boolean {
        return player != null && SheepEntityRuntimeState.hasCarriedSheep(player.uniqueId)
    }

    @JvmStatic
    fun getPickedUpSheep(player: Player?): Sheep? {
        if (player == null) return null
        val sheep = SheepEntityRuntimeState.getCarriedSheep(player.uniqueId)
        if (sheep != null && !sheep.isValid) {
            SheepEntityRuntimeState.removeCarriedSheep(player.uniqueId)
            return null
        }
        return sheep
    }

    @JvmStatic
    fun dropPickedUpSheep(player: Player?): Boolean {
        if (player == null) return false
        val sheep = getPickedUpSheep(player) ?: return false
        sheep.setGravity(true)
        sheep.setAI(true)
        sheep.isInvulnerable = false
        val forward = player.location.direction.normalize()
        var dropLocation = player.location.clone().add(forward.clone().multiply(1.6)).add(0.0, 0.2, 0.0)
        if (!isDropSpacePassable(dropLocation)) {
            dropLocation = player.location.clone().add(0.0, 0.2, 0.0)
        }
        if (!isDropSpacePassable(dropLocation)) {
            dropLocation = player.location.clone().add(0.0, 1.0, 0.0)
        }
        sheep.teleport(dropLocation)
        sheep.velocity = forward.multiply(0.2).setY(0.15)
        SheepEntityRuntimeState.removeCarriedSheep(player.uniqueId)
        return true
    }

    @JvmStatic
    fun updateCarriedSheepPosition(player: Player?) {
        if (player == null) return
        val sheep = getPickedUpSheep(player)
        if (sheep?.world == null || sheep.world != player.world) return
        val carryLocation = player.location.clone().add(0.0, 1.8, 0.0)
        carryLocation.yaw = player.location.yaw
        carryLocation.pitch = 0.0f
        sheep.teleport(carryLocation)
        sheep.velocity = Vector(0.0, 0.0, 0.0)
        sheep.fallDistance = 0.0f
    }

    @JvmStatic
    fun clearPickedUpSheep(player: Player?) {
        if (player == null) return
        val sheep = SheepEntityRuntimeState.removeCarriedSheep(player.uniqueId)
        if (sheep != null && sheep.isValid) {
            sheep.setGravity(true)
            sheep.setAI(true)
            sheep.isInvulnerable = false
        }
    }

    @JvmStatic
    fun rollSpawnTier(world: World?): SheepTier {
        val cap = SheepMergeManager.getUnlockedTierCap(world)
        val baseTierLevel = SheepMergeManager.getBaseSpawnTierLevel(world)
        var chosen = minOf(baseTierLevel, cap)
        var chance = SheepMergeManager.getHigherTierChancePercent(world)
        val maxChanceTier = minOf(cap, SheepTier.RAINBOW.level - 1)
        while (chosen < maxChanceTier && SheepMergeManager.entityRandomNextInt(100) < chance) {
            chosen++
            chance = maxOf(5, chance / 2)
        }
        return SheepTier.byLevel(chosen)
    }

    @JvmStatic
    fun upgradeBelowMinimumSpawnTier(world: World?) {
        if (world == null || !SheepMergeManager.isSheepFarmWorld(world)) return
        val minimumTierLevel = SheepMergeManager.getBaseSpawnTierLevel(world)
        val minimumTier = SheepTier.byLevel(minimumTierLevel)
        for (sheep in world.getEntitiesByClass(Sheep::class.java)) {
            if (!sheep.isValid || sheep.isDead || getTier(sheep).level >= minimumTierLevel) continue
            setTier(sheep, minimumTier)
        }
    }

    @JvmStatic
    fun spawnFromEgg(player: Player?, spawnLocation: Location?): Boolean {
        if (player == null || spawnLocation?.world == null) return false
        if (!SheepMergeManager.isSheepFarmWorld(player.world) || !SheepMergeManager.isFarmOwner(player, player.world)) {
            return false
        }
        if (isWorldAtLimit(player.world) || !SheepMergeManager.entityTryConsumeEgg(player)) return false
        val sheep = player.world.spawn(spawnLocation, Sheep::class.java)
        setTier(sheep, rollSpawnTier(player.world))
        SheepMergeManager.entityAfterOwnedSheepSpawn(player)
        return true
    }

    private fun countLiveSheep(world: World): Int {
        return world.getEntitiesByClass(Sheep::class.java).count { it.isValid && !it.isDead }
    }

    private fun isDropSpacePassable(location: Location?): Boolean {
        if (location?.world == null) return false
        return location.block.isPassable && location.clone().add(0.0, 1.0, 0.0).block.isPassable
    }

    private fun getTierDisplayNameWithColor(tier: SheepTier?): String {
        if (tier == null) return ChatColor.WHITE.toString() + "White Sheep"
        val color = when (tier) {
            SheepTier.WHITE, SheepTier.LIGHT_GRAY -> ChatColor.WHITE
            SheepTier.ORANGE, SheepTier.BROWN -> ChatColor.GOLD
            SheepTier.MAGENTA, SheepTier.PINK, SheepTier.RAINBOW -> ChatColor.LIGHT_PURPLE
            SheepTier.LIGHT_BLUE, SheepTier.CYAN -> ChatColor.AQUA
            SheepTier.YELLOW -> ChatColor.YELLOW
            SheepTier.LIME, SheepTier.GREEN -> ChatColor.GREEN
            SheepTier.GRAY -> ChatColor.DARK_GRAY
            SheepTier.PURPLE -> ChatColor.DARK_PURPLE
            SheepTier.BLUE -> ChatColor.BLUE
            SheepTier.RED -> ChatColor.RED
            SheepTier.BLACK -> ChatColor.BLACK
        }
        return color.toString() + tier.displayName
    }
}