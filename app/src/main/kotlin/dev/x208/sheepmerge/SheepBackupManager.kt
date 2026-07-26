package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object SheepBackupManager {
    private const val BACKUP_AUTOMATIC_PERMANENT_INTERVAL_MS = 7L * 24L * 60L * 60L * 1000L
    private const val BACKUP_AUTOMATIC_BUFFER_INTERVAL_MS = 24L * 60L * 60L * 1000L
    private const val BACKUP_AUTOMATIC_BUFFER_MAX_FILES = 7
    private const val BACKUP_AUTOMATIC_ROLLING_INTERVAL_TICKS = 20L * 60L * 60L
    private const val BACKUP_DIR_NAME = "backups"
    private const val BACKUP_INDEX_FILE_NAME = "backup-index.yml"
    private const val BACKUP_ROLLING_FILE_NAME = "rolling-auto-latest.zip"
    private const val BACKUP_INDEX_LAST_PERMANENT_AT_KEY = "lastPermanentAt"
    private const val BACKUP_INDEX_LAST_BUFFER_AT_KEY = "lastBufferAt"
    private const val BACKUP_INDEX_MARKED_FOR_DELETION_KEY = "markedForDeletion"
    private const val BACKUP_BUFFER_FILE_PREFIX = "buffer-24h-"
    private const val BACKUP_SOFT_DELETE_GRACE_MS = 24L * 60L * 60L * 1000L
    private val BACKUP_TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC)
    private val REQUIRED_ARCHIVE_ENTRIES = setOf("scores.yml", "farm-layout.yml", "config.yml")

    @JvmStatic
    @Synchronized
    fun createBackup(permanent: Boolean, trigger: String?): File? {
        val plugin = SheepMergeManager.getBackupPlugin() ?: return null

        captureLiveSheepSnapshotsForLoadedWorlds()
        SheepMergeManager.saveData()
        captureFarmLayoutSnapshotForBackup()

        val backupDir = File(plugin.dataFolder, BACKUP_DIR_NAME)
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            return null
        }

        if (permanent) {
            val timestamp = BACKUP_TIMESTAMP_FORMATTER.format(Instant.now())
            val suffix = if (trigger.isNullOrBlank()) "manual" else sanitizeBackupToken(trigger)
            val destination = File(backupDir, "permanent-$timestamp-$suffix.zip")
            if (!writeBackupArchive(destination)) {
                return null
            }
            markLastPermanentBackupNow()
            return destination
        }

        val rolling = File(backupDir, BACKUP_ROLLING_FILE_NAME)
        if (!writeBackupArchive(rolling)) {
            return null
        }
        return rolling
    }

    @JvmStatic
    @Synchronized
    fun maybeCreateAutomaticBackup(trigger: String?): Boolean {
        if (SheepMergeManager.getBackupPlugin() == null) {
            return false
        }
        val now = System.currentTimeMillis()
        val duePermanent = now - getLastPermanentBackupAt() >= BACKUP_AUTOMATIC_PERMANENT_INTERVAL_MS
        if (duePermanent) {
            return createBackup(true, if (trigger == null) "auto-weekly" else "$trigger-weekly") != null
        }

        val dueBuffer = now - getLastBufferBackupAt() >= BACKUP_AUTOMATIC_BUFFER_INTERVAL_MS
        if (dueBuffer) {
            return createBufferBackup(if (trigger == null) "auto-24h" else "$trigger-24h") != null
        }

        return createBackup(false, if (trigger == null) "auto" else "$trigger-rolling") != null
    }

    @JvmStatic
    fun getAutomaticBackupIntervalTicks(): Long = BACKUP_AUTOMATIC_ROLLING_INTERVAL_TICKS

    @JvmStatic
    @Synchronized
    fun createManualBackup(): File? = createBackup(true, "manual")

    @Synchronized
    private fun createBufferBackup(trigger: String?): File? {
        val plugin = SheepMergeManager.getBackupPlugin() ?: return null

        captureLiveSheepSnapshotsForLoadedWorlds()
        SheepMergeManager.saveData()
        captureFarmLayoutSnapshotForBackup()

        val backupDir = File(plugin.dataFolder, BACKUP_DIR_NAME)
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            return null
        }

        val timestamp = BACKUP_TIMESTAMP_FORMATTER.format(Instant.now())
        val suffix = if (trigger.isNullOrBlank()) "auto-24h" else sanitizeBackupToken(trigger)
        val destination = File(backupDir, "$BACKUP_BUFFER_FILE_PREFIX$timestamp-$suffix.zip")
        if (!writeBackupArchive(destination)) {
            return null
        }

        markLastBufferBackupNow()
        pruneBufferBackups(backupDir)
        return destination
    }

    private fun captureLiveSheepSnapshotsForLoadedWorlds() {
        val plugin = SheepMergeManager.getBackupPlugin() ?: return
        for (world in plugin.server.worlds) {
            if (SheepMergeManager.isSheepFarmWorld(world)) {
                SheepMergeManager.saveSheepSnapshotForBackup(world)
            }
        }
    }

    private fun captureFarmLayoutSnapshotForBackup() {
        val plugin = SheepMergeManager.getBackupPlugin() ?: return
        plugin.server ?: return

        val buildWorld = Bukkit.getWorld("sheepfarm_build")
        if (SheepMergeManager.isFarmBuildWorld(buildWorld)) {
            if (!SheepMergeManager.saveSharedFarmLayoutFromWorld(buildWorld) && SheepMergeManager.hasSavedFarmLayout()) {
                SheepMergeManager.saveFarmLayoutForBackup()
            }
            return
        }

        if (SheepMergeManager.hasSavedFarmLayout()) {
            SheepMergeManager.saveFarmLayoutForBackup()
        }
    }

    private fun pruneBufferBackups(backupDir: File?) {
        if (backupDir == null || !backupDir.exists() || !backupDir.isDirectory) {
            return
        }

        val bufferFiles = backupDir.listFiles { file ->
            file != null && file.isFile && file.name.startsWith(BACKUP_BUFFER_FILE_PREFIX) && file.name.endsWith(".zip")
        } ?: return
        if (bufferFiles.size <= BACKUP_AUTOMATIC_BUFFER_MAX_FILES) {
            return
        }

        val sorted = bufferFiles.sortedByDescending(File::lastModified)
        for (index in BACKUP_AUTOMATIC_BUFFER_MAX_FILES until sorted.size) {
            sorted[index].delete()
        }
    }

    @JvmStatic
    @Synchronized
    fun listBackups(): List<String> {
        val plugin = SheepMergeManager.getBackupPlugin() ?: return emptyList()
        val backupDir = File(plugin.dataFolder, BACKUP_DIR_NAME)
        if (!backupDir.exists() || !backupDir.isDirectory) {
            return emptyList()
        }
        val files = backupDir.listFiles { file -> file != null && file.isFile && file.name.endsWith(".zip") }
            ?: return emptyList()
        if (files.isEmpty()) {
            return emptyList()
        }
        return files.map(File::getName).sortedDescending()
    }

    @JvmStatic
    @Synchronized
    fun markBackupForDeletion(backupName: String?): Boolean {
        if (SheepMergeManager.getBackupPlugin() == null || backupName.isNullOrBlank()) {
            return false
        }
        if (BACKUP_ROLLING_FILE_NAME == backupName) {
            return false
        }

        val source = resolveBackupArchiveFile(backupName, true) ?: return false
        if (!source.exists() || !source.isFile || !source.name.endsWith(".zip")) {
            return false
        }

        val marks = getMarkedBackupsMap()
        marks[backupName] = System.currentTimeMillis()
        saveMarkedBackupsMap(marks)
        return true
    }

    @JvmStatic
    @Synchronized
    fun recoverBackupMarkedForDeletion(backupName: String?): Boolean {
        if (SheepMergeManager.getBackupPlugin() == null || backupName.isNullOrBlank()) {
            return false
        }
        if (!isValidBackupArchiveName(backupName)) {
            return false
        }
        val marks = getMarkedBackupsMap()
        if (marks.remove(backupName) == null) {
            return false
        }
        saveMarkedBackupsMap(marks)
        return true
    }

    @JvmStatic
    @Synchronized
    fun isBackupMarkedForDeletion(backupName: String?): Boolean {
        if (backupName.isNullOrBlank()) {
            return false
        }
        return getMarkedBackupsMap().containsKey(backupName)
    }

    @JvmStatic
    @Synchronized
    fun purgeMarkedBackupsIfEligibleOnStartup(): Int {
        if (SheepMergeManager.getBackupPlugin() == null) {
            return 0
        }

        val marks = getMarkedBackupsMap()
        if (marks.isEmpty()) {
            return 0
        }

        val now = System.currentTimeMillis()
        var deleted = 0
        var changed = false
        val iterator = marks.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val markedAt = entry.value.coerceAtLeast(0L)
            val target = resolveBackupArchiveFile(entry.key, false)
            if (target == null) {
                iterator.remove()
                changed = true
                continue
            }
            if (now - markedAt < BACKUP_SOFT_DELETE_GRACE_MS) {
                continue
            }
            if (!target.exists() || target.delete()) {
                if (!target.exists()) {
                    deleted++
                    iterator.remove()
                    changed = true
                }
            }
        }

        if (changed) {
            saveMarkedBackupsMap(marks)
        }
        return deleted
    }

    @JvmStatic
    @Synchronized
    fun loadBackup(backupName: String?): File? {
        val plugin = SheepMergeManager.getBackupPlugin()
        if (plugin == null || backupName.isNullOrBlank()) {
            return null
        }

        val source = resolveBackupArchiveFile(backupName, true) ?: return null
        if (!source.exists() || !source.isFile || !source.name.endsWith(".zip")) {
            return null
        }
        if (!restoreBackupArchive(source)) {
            return null
        }

        plugin.reloadConfig()
        SheepMergeConfiguration.initialize(plugin)
        SheepMergeManager.applyConfiguration(SheepMergeConfiguration.get())

        SheepMergeManager.clearStateForBackupLoad()
        SheepMergeManager.loadDataForBackupLoad()
        SheepMergeManager.loadFarmLayoutForBackupLoad()

        for (world in plugin.server.worlds) {
            if (SheepMergeManager.isSheepFarmWorld(world)) {
                SheepMergeManager.rebuildFarmWorld(world)
                continue
            }
            if (SheepMergeManager.isFarmBuildWorld(world)) {
                SheepMergeManager.applyFarmLayout(world)
                world.save()
            }
        }

        SheepMergeManager.restoreTopPointsDisplayAfterRestart(null)
        return createBackup(true, "post-load") ?: source
    }

    private fun writeBackupArchive(destination: File?): Boolean {
        val plugin = SheepMergeManager.getBackupPlugin()
        if (plugin == null || destination == null) {
            return false
        }

        val dataFolder = plugin.dataFolder
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            return false
        }

        val sources = listOf(
            File(dataFolder, "scores.yml") to "scores.yml",
            File(dataFolder, "farm-layout.yml") to "farm-layout.yml",
            File(dataFolder, "config.yml") to "config.yml"
        )

        return try {
            Files.newOutputStream(destination.toPath()).use { fileOut ->
                ZipOutputStream(fileOut).use { zipOut ->
                    zipOut.setLevel(Deflater.BEST_COMPRESSION)
                    for ((source, entryName) in sources) {
                        addFileToZip(zipOut, source, entryName)
                    }
                }
            }
            true
        } catch (exception: IOException) {
            plugin.logger.warning("Unable to create backup archive: ${exception.message}")
            false
        }
    }

    private fun restoreBackupArchive(source: File?): Boolean {
        val plugin = SheepMergeManager.getBackupPlugin()
        if (plugin == null || source == null || !source.exists()) {
            return false
        }

        val dataFolder = plugin.dataFolder
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            return false
        }

        var tempDir: java.nio.file.Path? = null
        try {
            tempDir = Files.createTempDirectory(dataFolder.toPath(), "backup-restore-")
            val restoredEntries = mutableSetOf<String>()
            ZipInputStream(Files.newInputStream(source.toPath())).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name !in REQUIRED_ARCHIVE_ENTRIES || entry.isDirectory) {
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                        continue
                    }

                    val tempTarget = tempDir.resolve(name).normalize()
                    if (tempTarget.parent != tempDir) {
                        return false
                    }
                    Files.newOutputStream(tempTarget).use { out -> zipIn.copyTo(out) }
                    restoredEntries.add(name)
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            if (!restoredEntries.containsAll(REQUIRED_ARCHIVE_ENTRIES)) {
                return false
            }

            for (name in REQUIRED_ARCHIVE_ENTRIES) {
                Files.move(
                    tempDir.resolve(name),
                    dataFolder.toPath().resolve(name),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            }
            return true
        } catch (exception: IOException) {
            plugin.logger.warning("Unable to restore backup archive: ${exception.message}")
            return false
        } finally {
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir)
                } catch (_: IOException) {
                    // Best-effort cleanup only.
                }
            }
        }
    }

    private fun isValidBackupArchiveName(backupName: String?): Boolean =
        !backupName.isNullOrBlank() &&
            backupName.endsWith(".zip") &&
            !backupName.contains('/') &&
            !backupName.contains('\\') &&
            backupName == File(backupName).name

    private fun resolveBackupArchiveFile(backupName: String?, requireExisting: Boolean): File? {
        val plugin = SheepMergeManager.getBackupPlugin()
        if (plugin == null || !isValidBackupArchiveName(backupName)) {
            return null
        }

        val backupDir = File(plugin.dataFolder, BACKUP_DIR_NAME)
        val candidate = File(backupDir, backupName!!)
        return try {
            val canonicalDir = backupDir.canonicalFile
            val canonicalCandidate = candidate.canonicalFile
            if (canonicalCandidate.parentFile != canonicalDir) {
                null
            } else if (requireExisting && !canonicalCandidate.exists()) {
                null
            } else {
                canonicalCandidate
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun addFileToZip(zipOut: ZipOutputStream?, source: File?, entryName: String?) {
        if (zipOut == null || source == null || entryName == null || !source.exists() || !source.isFile) {
            return
        }
        val entry = ZipEntry(entryName)
        entry.time = source.lastModified()
        zipOut.putNextEntry(entry)
        Files.newInputStream(source.toPath()).use { input -> input.copyTo(zipOut) }
        zipOut.closeEntry()
    }

    private fun sanitizeBackupToken(token: String?): String {
        if (token.isNullOrBlank()) {
            return "auto"
        }
        val cleaned = token.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9-]+"), "-")
        return cleaned.ifBlank { "auto" }
    }

    private fun getMarkedBackupsMap(): MutableMap<String, Long> {
        val indexFile = getBackupIndexFile()
        if (indexFile == null || !indexFile.exists()) {
            return HashMap()
        }

        val indexConfig = YamlConfiguration.loadConfiguration(indexFile)
        if (!indexConfig.isConfigurationSection(BACKUP_INDEX_MARKED_FOR_DELETION_KEY)) {
            return HashMap()
        }

        val marks = HashMap<String, Long>()
        val section = indexConfig.getConfigurationSection(BACKUP_INDEX_MARKED_FOR_DELETION_KEY) ?: return marks
        for (encodedName in section.getKeys(false)) {
            val backupName = decodeBackupName(encodedName)
            if (backupName.isNullOrBlank()) {
                continue
            }
            val markedAt = indexConfig
                .getLong("$BACKUP_INDEX_MARKED_FOR_DELETION_KEY.$encodedName", 0L)
                .coerceAtLeast(0L)
            if (markedAt > 0L) {
                marks[backupName] = markedAt
            }
        }
        return marks
    }

    private fun saveMarkedBackupsMap(marks: Map<String, Long>?) {
        val indexFile = getBackupIndexFile() ?: return
        val indexConfig = YamlConfiguration.loadConfiguration(indexFile)
        indexConfig.set(BACKUP_INDEX_MARKED_FOR_DELETION_KEY, null)
        if (marks != null) {
            for ((backupName, markedAt) in marks) {
                val key = encodeBackupName(backupName) ?: continue
                indexConfig.set("$BACKUP_INDEX_MARKED_FOR_DELETION_KEY.$key", markedAt.coerceAtLeast(0L))
            }
        }
        try {
            indexConfig.save(indexFile)
        } catch (_: IOException) {
            // Best effort metadata write.
        }
    }

    private fun encodeBackupName(backupName: String?): String? {
        if (backupName.isNullOrBlank()) {
            return null
        }
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(backupName.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeBackupName(encodedBackupName: String?): String? {
        if (encodedBackupName.isNullOrBlank()) {
            return null
        }
        return try {
            String(Base64.getUrlDecoder().decode(encodedBackupName), StandardCharsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun getLastPermanentBackupAt(): Long = getBackupIndexTimestamp(BACKUP_INDEX_LAST_PERMANENT_AT_KEY)

    private fun getLastBufferBackupAt(): Long = getBackupIndexTimestamp(BACKUP_INDEX_LAST_BUFFER_AT_KEY)

    private fun getBackupIndexTimestamp(key: String): Long {
        val indexFile = getBackupIndexFile()
        if (indexFile == null || !indexFile.exists()) {
            return 0L
        }
        return YamlConfiguration.loadConfiguration(indexFile).getLong(key, 0L).coerceAtLeast(0L)
    }

    private fun markLastPermanentBackupNow() {
        markBackupTimestampNow(BACKUP_INDEX_LAST_PERMANENT_AT_KEY)
    }

    private fun markLastBufferBackupNow() {
        markBackupTimestampNow(BACKUP_INDEX_LAST_BUFFER_AT_KEY)
    }

    private fun markBackupTimestampNow(key: String) {
        val indexFile = getBackupIndexFile() ?: return
        val indexConfig = YamlConfiguration.loadConfiguration(indexFile)
        indexConfig.set(key, System.currentTimeMillis())
        try {
            indexConfig.save(indexFile)
        } catch (_: IOException) {
            // Best effort metadata write; backup file has already been created.
        }
    }

    private fun getBackupIndexFile(): File? {
        val plugin = SheepMergeManager.getBackupPlugin() ?: return null
        val backupDir = File(plugin.dataFolder, BACKUP_DIR_NAME)
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            return null
        }
        return File(backupDir, BACKUP_INDEX_FILE_NAME)
    }

    private fun deleteDirectory(directory: java.nio.file.Path) {
        Files.walk(directory).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
