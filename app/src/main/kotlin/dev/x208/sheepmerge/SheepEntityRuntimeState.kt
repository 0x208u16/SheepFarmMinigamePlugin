package dev.x208.sheepmerge

import org.bukkit.Location
import org.bukkit.entity.Sheep
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

object SheepEntityRuntimeState {
    private val carriedSheepByPlayer = HashMap<UUID, Sheep>()
    private val sheepRescueStartByEntity = HashMap<UUID, Long>()
    private val sheepRescueOriginByEntity = HashMap<UUID, Location>()
    private val sheepRescueNextCorrectionAtByEntity = HashMap<UUID, Long>()
    private val activeShearAllTaskByPlayer = HashMap<UUID, BukkitTask>()
    private val liveSheepCountByWorld = HashMap<UUID, Int>()

    @JvmStatic
    fun putCarriedSheep(playerId: UUID, sheep: Sheep) {
        carriedSheepByPlayer[playerId] = sheep
    }

    @JvmStatic
    fun hasCarriedSheep(playerId: UUID): Boolean = carriedSheepByPlayer.containsKey(playerId)

    @JvmStatic
    fun getCarriedSheep(playerId: UUID): Sheep? = carriedSheepByPlayer[playerId]

    @JvmStatic
    fun removeCarriedSheep(playerId: UUID): Sheep? = carriedSheepByPlayer.remove(playerId)

    @JvmStatic
    fun clearCarriedSheep() {
        carriedSheepByPlayer.clear()
    }

    @JvmStatic
    fun isRescueInProgress(sheepId: UUID): Boolean = sheepRescueStartByEntity.containsKey(sheepId)

    @JvmStatic
    fun getOrStartRescue(sheepId: UUID, startedAt: Long): Long =
        sheepRescueStartByEntity.getOrPut(sheepId) { startedAt }

    @JvmStatic
    fun getOrSetRescueOrigin(sheepId: UUID, origin: Location): Location =
        sheepRescueOriginByEntity.getOrPut(sheepId) { origin.clone() }

    @JvmStatic
    fun ensureRescueCorrectionAt(sheepId: UUID, correctionAt: Long) {
        sheepRescueNextCorrectionAtByEntity.putIfAbsent(sheepId, correctionAt)
    }

    @JvmStatic
    fun getRescueCorrectionAt(sheepId: UUID, defaultValue: Long): Long =
        sheepRescueNextCorrectionAtByEntity.getOrDefault(sheepId, defaultValue)

    @JvmStatic
    fun setRescueCorrectionAt(sheepId: UUID, correctionAt: Long) {
        sheepRescueNextCorrectionAtByEntity[sheepId] = correctionAt
    }

    @JvmStatic
    fun clearRescue(sheepId: UUID) {
        sheepRescueStartByEntity.remove(sheepId)
        sheepRescueOriginByEntity.remove(sheepId)
        sheepRescueNextCorrectionAtByEntity.remove(sheepId)
    }

    @JvmStatic
    fun getShearAllTask(playerId: UUID): BukkitTask? = activeShearAllTaskByPlayer[playerId]

    @JvmStatic
    fun putShearAllTask(playerId: UUID, task: BukkitTask) {
        activeShearAllTaskByPlayer[playerId] = task
    }

    @JvmStatic
    fun removeShearAllTask(playerId: UUID): BukkitTask? = activeShearAllTaskByPlayer.remove(playerId)

    @JvmStatic
    fun shearAllTaskPlayerIds(): Set<UUID> = HashSet(activeShearAllTaskByPlayer.keys)

    @JvmStatic
    fun getLiveSheepCount(worldId: UUID, defaultValue: Int): Int =
        liveSheepCountByWorld.getOrDefault(worldId, defaultValue)

    @JvmStatic
    fun setLiveSheepCount(worldId: UUID, count: Int) {
        liveSheepCountByWorld[worldId] = count
    }

    @JvmStatic
    fun retainLiveSheepCounts(worldIds: Set<UUID>) {
        liveSheepCountByWorld.keys.removeIf { worldId -> !worldIds.contains(worldId) }
    }

    @JvmStatic
    fun clearLiveSheepCounts() {
        liveSheepCountByWorld.clear()
    }
}