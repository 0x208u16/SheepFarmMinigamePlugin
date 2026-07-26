package dev.x208.sheepmerge

import org.bukkit.ChatColor
import java.time.Instant
import java.time.format.DateTimeFormatter

object SheepLiveUpdateState {

    private var liveUpdateEnabled = true
    private var dataSchemaVersion = 0
    private var stagedLiveUpdateVersion: String? = ""
    private var lastLiveUpdateStatus: String? = "Not checked yet."
    private var lastLiveUpdateCheckAt = 0L

    @JvmStatic
    fun isLiveUpdateEnabled(): Boolean = liveUpdateEnabled

    @JvmStatic
    fun setLiveUpdateEnabled(enabled: Boolean, saveData: Runnable) {
        liveUpdateEnabled = enabled
        saveData.run()
    }

    @JvmStatic
    fun getDataSchemaVersion(): Int = dataSchemaVersion

    @JvmStatic
    @Synchronized
    fun applyDataSchemaVersion(
        targetVersion: Int,
        currentDataSchemaVersion: Int,
        reason: String?,
        reconcileAchievementAutomationPointGrants: Runnable,
        saveData: Runnable
    ): Boolean {
        val clampedTarget = targetVersion.coerceAtLeast(0)
        if (clampedTarget > currentDataSchemaVersion) {
            return false
        }
        var changed = false
        while (dataSchemaVersion < clampedTarget) {
            val nextVersion = dataSchemaVersion + 1
            if (!applyDataSchemaMigrationStep(nextVersion, reason, reconcileAchievementAutomationPointGrants)) {
                return false
            }
            dataSchemaVersion = nextVersion
            changed = true
        }
        if (changed) {
            saveData.run()
        }
        return true
    }

    private fun applyDataSchemaMigrationStep(
        targetVersion: Int,
        reason: String?,
        reconcileAchievementAutomationPointGrants: Runnable
    ): Boolean {
        return when (targetVersion) {
            1 -> {
                if (lastLiveUpdateStatus.isNullOrBlank()) {
                    lastLiveUpdateStatus = "Schema v1 initialized via ${reason ?: "migration"}."
                }
                if (stagedLiveUpdateVersion == null) {
                    stagedLiveUpdateVersion = ""
                }
                true
            }
            2 -> {
                reconcileAchievementAutomationPointGrants.run()
                if (lastLiveUpdateStatus.isNullOrBlank()) {
                    lastLiveUpdateStatus = "Schema v2 initialized via ${reason ?: "migration"}."
                }
                true
            }
            else -> false
        }
    }

    @JvmStatic
    fun recordLiveUpdateCheck(status: String?, saveData: Runnable) {
        lastLiveUpdateCheckAt = System.currentTimeMillis()
        lastLiveUpdateStatus = normalizeLiveUpdateStatus(if (status.isNullOrBlank()) "Checked." else status)
        saveData.run()
    }

    @JvmStatic
    fun recordStagedLiveUpdate(version: String?, status: String?, saveData: Runnable) {
        stagedLiveUpdateVersion = version ?: ""
        recordLiveUpdateCheck(status, saveData)
    }

    @JvmStatic
    fun getStagedLiveUpdateVersion(): String = stagedLiveUpdateVersion ?: ""

    @JvmStatic
    fun clearStagedLiveUpdate(status: String?, saveData: Runnable) {
        stagedLiveUpdateVersion = ""
        recordLiveUpdateCheck(
            if (status.isNullOrBlank()) "Cleared staged live update state." else status,
            saveData
        )
    }

    @JvmStatic
    fun recordLiveUpdateApply(status: String?, currentDataSchemaVersion: Int, saveData: Runnable) {
        lastLiveUpdateCheckAt = System.currentTimeMillis()
        lastLiveUpdateStatus = normalizeLiveUpdateStatus(
            if (status.isNullOrBlank()) "Applied staged live update." else status
        )
        if (dataSchemaVersion >= currentDataSchemaVersion) {
            stagedLiveUpdateVersion = ""
        }
        saveData.run()
    }

    @JvmStatic
    fun getLiveUpdateStatusLines(currentDataSchemaVersion: Int, currentPluginVersion: String): List<String> {
        val checkedAt = if (lastLiveUpdateCheckAt <= 0L) {
            "never"
        } else {
            DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(lastLiveUpdateCheckAt))
        }
        return listOf(
            ChatColor.GRAY.toString() + "Current Version: " + ChatColor.AQUA + currentPluginVersion,
            ChatColor.GRAY.toString() + "Enabled: " +
                if (liveUpdateEnabled) ChatColor.GREEN.toString() + "YES" else ChatColor.RED.toString() + "NO",
            ChatColor.GRAY.toString() + "Schema: " + ChatColor.AQUA + dataSchemaVersion + ChatColor.DARK_GRAY + " / " +
                ChatColor.AQUA + currentDataSchemaVersion,
            ChatColor.GRAY.toString() + "Staged Release: " + ChatColor.YELLOW +
                stagedLiveUpdateVersion?.takeUnless { it.isBlank() } ?: "none",
            ChatColor.GRAY.toString() + "Last Check: " + ChatColor.AQUA + checkedAt,
            ChatColor.GRAY.toString() + "Status: " + ChatColor.WHITE +
                (if (lastLiveUpdateStatus.isNullOrBlank()) {
                    "Not checked yet."
                } else {
                    normalizeLiveUpdateStatus(lastLiveUpdateStatus)
                })
        )
    }

    @JvmStatic
    fun reset() {
        liveUpdateEnabled = true
        dataSchemaVersion = 0
        stagedLiveUpdateVersion = ""
        lastLiveUpdateStatus = "Not checked yet."
        lastLiveUpdateCheckAt = 0L
    }

    @JvmStatic
    fun loadPersistedState(
        enabled: Boolean,
        stagedVersion: String?,
        status: String?,
        lastCheckAt: Long,
        schemaVersion: Int
    ) {
        liveUpdateEnabled = enabled
        stagedLiveUpdateVersion = stagedVersion
        lastLiveUpdateStatus = normalizeLiveUpdateStatus(status)
        lastLiveUpdateCheckAt = lastCheckAt.coerceAtLeast(0L)
        dataSchemaVersion = schemaVersion.coerceAtLeast(0)
    }

    @JvmStatic
    fun getLastLiveUpdateStatus(): String = lastLiveUpdateStatus ?: "Not checked yet."

    @JvmStatic
    fun getLastLiveUpdateCheckAt(): Long = lastLiveUpdateCheckAt

    private fun normalizeLiveUpdateStatus(status: String?): String? {
        if (status.isNullOrBlank()) {
            return status
        }
        return status.replace("x208/SheepMerge", "0x208u16/SheepFarmMinigamePlugin")
    }
}