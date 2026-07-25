package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.regex.Pattern

object LiveUpdateCoordinator {

    private const val GITHUB_API_BASE = "https://api.github.com"
    private const val GITHUB_ACCEPT = "application/vnd.github+json"
    private const val GITHUB_API_VERSION = "2022-11-28"
    private const val GITHUB_USER_AGENT = "SheepMerge-LiveUpdate"

    private val tagPattern = Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
    private val assetPattern = Pattern.compile(
        "\\{[^{}]*\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^{}]*\\\"browser_download_url\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[^{}]*}",
        Pattern.DOTALL
    )

    private data class ReleaseAsset(val name: String, val downloadUrl: String)
    private data class ReleaseFetchResult(
        val release: Pair<String, List<ReleaseAsset>>? = null,
        val errorMessage: String? = null
    )

    data class LiveUpdateManifest(
        val tagName: String,
        val dataSchemaVersion: Int,
        val requiresBinarySwap: Boolean,
        val liveSafeMigration: Boolean,
        val binaryAssetName: String?,
        val summary: String,
        val reloadConfiguration: Boolean
    )

    @JvmStatic
    fun scheduleAutomaticChecks(plugin: SheepMergePlugin?) {
        if (plugin == null) {
            return
        }
        val configuration = SheepMergeConfiguration.get() ?: return
        val interval = configuration.liveUpdateCheckIntervalTicks
        plugin.server.scheduler.runTaskTimerAsynchronously(
            plugin,
            Runnable {
                if (!SheepMergeManager.isLiveUpdateEnabled()) {
                    return@Runnable
                }
                checkForUpdatesNow(null)
            },
            interval,
            interval
        )
    }

    @JvmStatic
    fun checkForUpdatesNow(sender: CommandSender?) {
        val plugin = SheepMergePlugin.instance
        val configuration = SheepMergeConfiguration.get()
        if (plugin == null || configuration == null) {
            sender?.sendMessage(ChatColor.RED.toString() + "Live update system is not ready.")
            return
        }
        if (!SheepMergeManager.isLiveUpdateEnabled()) {
            sender?.sendMessage(ChatColor.YELLOW.toString() + "Live updates are disabled.")
            return
        }

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val resultMessage = try {
                val fetchResult = fetchLatestRelease(configuration)
                val latest = fetchResult.release
                if (latest == null) {
                    val noInfoMessage = fetchResult.errorMessage ?: "No release information available."
                    SheepMergeManager.recordLiveUpdateCheck(noInfoMessage)
                    noInfoMessage
                } else {
                    stageLatestRelease(plugin, configuration, latest)
                }
            } catch (exception: Exception) {
                val message = "Live update check failed: " + (exception.message ?: exception.javaClass.simpleName)
                SheepMergeManager.recordLiveUpdateCheck(message)
                message
            }
            if (sender != null) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage(ChatColor.DARK_AQUA.toString() + "[SheepMerge] " + ChatColor.GRAY + resultMessage)
                })
            }
        })
    }

    @JvmStatic
    fun applyStagedUpdateNow(sender: CommandSender?) {
        val plugin = SheepMergePlugin.instance
        if (plugin == null) {
            sender?.sendMessage(ChatColor.RED.toString() + "Plugin instance unavailable.")
            return
        }
        val manifestFile = File(File(plugin.dataFolder, "live-update"), "staged-manifest.yml")
        if (!manifestFile.exists()) {
            sender?.sendMessage(ChatColor.YELLOW.toString() + "No staged live update manifest found.")
            return
        }

        val manifest = loadManifest(manifestFile.readText())
        if (manifest == null) {
            sender?.sendMessage(ChatColor.RED.toString() + "Staged live update manifest is invalid.")
            return
        }

        if (manifest.requiresBinarySwap) {
            sender?.sendMessage(ChatColor.YELLOW.toString() + "Update " + manifest.tagName
                + " is staged, but the plugin binary requires a server handoff/restart to activate.")
            return
        }

        if (!manifest.liveSafeMigration) {
            sender?.sendMessage(ChatColor.YELLOW.toString() + "Update " + manifest.tagName
                + " is not marked as live-safe and cannot be applied in-place.")
            return
        }

        val applied = SheepMergeManager.applyLiveDataSchemaVersion(manifest.dataSchemaVersion, manifest.summary)
        if (!applied) {
            sender?.sendMessage(ChatColor.RED.toString() + "Unable to apply staged live migration to schema v" + manifest.dataSchemaVersion + ".")
            return
        }

        if (manifest.reloadConfiguration) {
            plugin.reloadConfig()
            SheepMergeConfiguration.initialize(plugin)
            SheepMergeManager.applyConfiguration(SheepMergeConfiguration.get())
        }

        SheepMergeManager.recordLiveUpdateApply("Applied live update manifest " + manifest.tagName + ".")
        sender?.sendMessage(ChatColor.DARK_AQUA.toString() + "[SheepMerge] " + ChatColor.GREEN
            + "Applied live update manifest " + manifest.tagName + ".")
    }

    private fun fetchLatestRelease(configuration: SheepMergeConfiguration): ReleaseFetchResult {
        val owner = configuration.liveUpdateGitHubOwner.trim()
        val repo = configuration.liveUpdateGitHubRepo.trim()
        if (owner.isBlank() || repo.isBlank()) {
            return ReleaseFetchResult(errorMessage = "Live update GitHub owner/repo is not configured.")
        }

        val timeout = Duration.ofMillis(configuration.liveUpdateApiTimeoutMs)
        val client = HttpClient.newBuilder().connectTimeout(timeout).build()
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("$GITHUB_API_BASE/repos/$owner/$repo/releases/latest"))
            .timeout(timeout)
            .GET()
        applyGitHubHeaders(requestBuilder, configuration)
        val request = requestBuilder.build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            val status = response.statusCode()
            val rateRemaining = response.headers().firstValue("x-ratelimit-remaining").orElse("")
            val statusMessage = when (status) {
                401, 403 -> if (rateRemaining == "0") {
                    "GitHub API rate limit reached while checking releases (HTTP $status)."
                } else {
                    "GitHub API denied release lookup (HTTP $status)."
                }
                404 -> "No published GitHub release found for $owner/$repo yet."
                else -> "GitHub API returned HTTP $status while checking latest release."
            }
            return ReleaseFetchResult(errorMessage = statusMessage)
        }

        val body = response.body()
        val tagMatcher = tagPattern.matcher(body)
        if (!tagMatcher.find()) {
            return ReleaseFetchResult(errorMessage = "GitHub API response did not include a release tag.")
        }

        val tagName = unescape(tagMatcher.group(1))
        val assets = mutableListOf<ReleaseAsset>()
        val assetMatcher = assetPattern.matcher(body)
        while (assetMatcher.find()) {
            val name = unescape(assetMatcher.group(1))
            val url = unescape(assetMatcher.group(2))
            assets.add(ReleaseAsset(name, url))
        }
        return ReleaseFetchResult(release = tagName to assets)
    }

    private fun stageLatestRelease(
        plugin: SheepMergePlugin,
        configuration: SheepMergeConfiguration,
        latest: Pair<String, List<ReleaseAsset>>
    ): String {
        val tagName = latest.first
        val currentVersion = plugin.description.version ?: ""
        val assets = latest.second
        if (normalizeReleaseVersion(tagName).equals(normalizeReleaseVersion(currentVersion), ignoreCase = true)) {
            val message = "Already on latest release $tagName."
            SheepMergeManager.recordLiveUpdateCheck(message)
            return message
        }

        val manifestAsset = assets.firstOrNull { it.name.equals(configuration.liveUpdateManifestAssetName, ignoreCase = true) }
        val manifest = manifestAsset?.let { downloadManifest(configuration, it.downloadUrl, tagName) }
            ?: LiveUpdateManifest(tagName, SheepMergeManager.getCurrentDataSchemaVersion(), true, false, null, "Release $tagName", false)

        val selectedJar = selectJarAsset(assets, manifest)
        val updateDir = File(plugin.dataFolder, "live-update")
        if (!updateDir.exists()) {
            updateDir.mkdirs()
        }
        File(updateDir, "staged-manifest.yml").writeText(buildManifestYaml(manifest))

        if (selectedJar != null) {
            stageBinaryJar(plugin, configuration, selectedJar)
        }

        val message = if (manifest.requiresBinarySwap) {
            "Staged update $tagName for next server handoff/restart."
        } else {
            "Staged live-safe update $tagName. Use /sheepmerge liveupdate apply to apply migrations."
        }
        SheepMergeManager.recordStagedLiveUpdate(tagName, message)
        return message
    }

    private fun downloadManifest(configuration: SheepMergeConfiguration, url: String, fallbackTag: String): LiveUpdateManifest? {
        val timeout = Duration.ofMillis(configuration.liveUpdateApiTimeoutMs)
        val client = HttpClient.newBuilder().connectTimeout(timeout).build()
        val requestBuilder = HttpRequest.newBuilder().uri(URI.create(url)).timeout(timeout).GET()
        applyGitHubHeaders(requestBuilder, configuration)
        val request = requestBuilder.build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            return null
        }
        val loaded = loadManifest(response.body()) ?: return null
        return loaded.copy(tagName = loaded.tagName.ifBlank { fallbackTag })
    }

    private fun loadManifest(raw: String): LiveUpdateManifest? {
        return try {
            val yaml = YamlConfiguration()
            yaml.loadFromString(raw)
            LiveUpdateManifest(
                yaml.getString("tagName", "") ?: "",
                yaml.getInt("dataSchemaVersion", SheepMergeManager.getCurrentDataSchemaVersion()),
                yaml.getBoolean("requiresBinarySwap", true),
                yaml.getBoolean("liveSafeMigration", false),
                yaml.getString("binaryAssetName"),
                yaml.getString("summary", "") ?: "",
                yaml.getBoolean("reloadConfiguration", false)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun buildManifestYaml(manifest: LiveUpdateManifest): String {
        val yaml = YamlConfiguration()
        yaml.set("tagName", manifest.tagName)
        yaml.set("dataSchemaVersion", manifest.dataSchemaVersion)
        yaml.set("requiresBinarySwap", manifest.requiresBinarySwap)
        yaml.set("liveSafeMigration", manifest.liveSafeMigration)
        yaml.set("binaryAssetName", manifest.binaryAssetName)
        yaml.set("summary", manifest.summary)
        yaml.set("reloadConfiguration", manifest.reloadConfiguration)
        return yaml.saveToString()
    }

    private fun selectJarAsset(assets: List<ReleaseAsset>, manifest: LiveUpdateManifest): ReleaseAsset? {
        val manifestAssetName = manifest.binaryAssetName
        if (!manifestAssetName.isNullOrBlank()) {
            return assets.firstOrNull { it.name.equals(manifestAssetName, ignoreCase = true) }
        }
        return assets.firstOrNull { it.name.startsWith("SheepMerge", ignoreCase = true) && it.name.endsWith(".jar", ignoreCase = true) }
    }

    private fun stageBinaryJar(plugin: SheepMergePlugin, configuration: SheepMergeConfiguration, asset: ReleaseAsset) {
        val timeout = Duration.ofMillis(configuration.liveUpdateApiTimeoutMs)
        val client = HttpClient.newBuilder().connectTimeout(timeout).build()
        val requestBuilder = HttpRequest.newBuilder().uri(URI.create(asset.downloadUrl)).timeout(timeout).GET()
        applyGitHubHeaders(requestBuilder, configuration)
        val request = requestBuilder.build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) {
            return
        }
        response.body().use { input ->
            val updateFolder = plugin.server.updateFolderFile
            if (!updateFolder.exists()) {
                updateFolder.mkdirs()
            }
            val target = File(updateFolder, asset.name)
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun unescape(value: String): String {
        return value.replace("\\/", "/")
    }

    private fun normalizeReleaseVersion(value: String?): String {
        if (value.isNullOrBlank()) {
            return ""
        }
        return value.trim().removePrefix("refs/tags/").removePrefix("v")
    }

    private fun applyGitHubHeaders(builder: HttpRequest.Builder, configuration: SheepMergeConfiguration) {
        builder
            .header("Accept", GITHUB_ACCEPT)
            .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
            .header("User-Agent", GITHUB_USER_AGENT)

        val token = configuration.liveUpdateGitHubToken.trim()
        if (token.isNotBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
    }
}
