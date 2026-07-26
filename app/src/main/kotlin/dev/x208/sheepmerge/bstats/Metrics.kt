package dev.x208.sheepmerge.bstats

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.HashSet
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.logging.Level
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection

// From https://github.com/Bastian/bStats-Metrics/blob/40c234fe8213fb90600261dc882832490ee2130b/bukkit/Metrics.java
class Metrics(plugin: JavaPlugin, serviceId: Int) {

    private val plugin: Plugin = plugin
    private val metricsBase: MetricsBase

    init {
        val bStatsFolder = File(plugin.dataFolder.parentFile, "bStats")
        val configFile = File(bStatsFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        if (!config.isSet("serverUuid")) {
            config.addDefault("enabled", true)
            config.addDefault("serverUuid", UUID.randomUUID().toString())
            config.addDefault("logFailedRequests", false)
            config.addDefault("logSentData", false)
            config.addDefault("logResponseStatusText", false)
            config.options()
                .header(
                    "bStats (https://bStats.org) collects some basic information for plugin authors, like how\n" +
                        "many people use their plugin and their total player count. It's recommended to keep bStats\n" +
                        "enabled, but if you're not comfortable with this, you can turn this setting off. There is no\n" +
                        "performance penalty associated with having metrics enabled, and data sent to bStats is fully\n" +
                        "anonymous."
                )
                .copyDefaults(true)
            try {
                config.save(configFile)
            } catch (_: IOException) {
            }
        }

        val enabled = config.getBoolean("enabled", true)
        val serverUUID = config.getString("serverUuid")
        val logErrors = config.getBoolean("logFailedRequests", false)
        val logSentData = config.getBoolean("logSentData", false)
        val logResponseStatusText = config.getBoolean("logResponseStatusText", false)

        metricsBase = MetricsBase(
            "bukkit",
            serverUUID,
            serviceId,
            enabled,
            this::appendPlatformData,
            this::appendServiceData,
            Consumer { submitDataTask -> Bukkit.getScheduler().runTask(plugin, submitDataTask) },
            Supplier { plugin.isEnabled },
            BiConsumer { message, error -> this.plugin.logger.log(Level.WARNING, message, error) },
            Consumer { message -> this.plugin.logger.log(Level.INFO, message) },
            logErrors,
            logSentData,
            logResponseStatusText
        )
    }

    fun addCustomChart(chart: CustomChart) {
        metricsBase.addCustomChart(chart)
    }

    private fun appendPlatformData(builder: JsonObjectBuilder) {
        builder.appendField("playerAmount", getPlayerAmount())
        builder.appendField("onlineMode", if (Bukkit.getOnlineMode()) 1 else 0)
        builder.appendField("bukkitVersion", Bukkit.getVersion())
        builder.appendField("bukkitName", Bukkit.getName())
        builder.appendField("javaVersion", System.getProperty("java.version"))
        builder.appendField("osName", System.getProperty("os.name"))
        builder.appendField("osArch", System.getProperty("os.arch"))
        builder.appendField("osVersion", System.getProperty("os.version"))
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors())
    }

    private fun appendServiceData(builder: JsonObjectBuilder) {
        builder.appendField("pluginVersion", plugin.description.version)
    }

    private fun getPlayerAmount(): Int {
        return try {
            val onlinePlayersMethod: Method = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers")
            if (onlinePlayersMethod.returnType == java.util.Collection::class.java) {
                @Suppress("UNCHECKED_CAST")
                (onlinePlayersMethod.invoke(Bukkit.getServer()) as Collection<*>).size
            } else {
                (onlinePlayersMethod.invoke(Bukkit.getServer()) as Array<Player>).size
            }
        } catch (_: Exception) {
            Bukkit.getOnlinePlayers().size
        }
    }

    class MetricsBase(
        private val platform: String,
        private val serverUuid: String?,
        private val serviceId: Int,
        private val enabled: Boolean,
        private val appendPlatformDataConsumer: Consumer<JsonObjectBuilder>,
        private val appendServiceDataConsumer: Consumer<JsonObjectBuilder>,
        private val submitTaskConsumer: Consumer<Runnable>?,
        private val checkServiceEnabledSupplier: Supplier<Boolean>,
        private val errorLogger: BiConsumer<String, Throwable>,
        private val infoLogger: Consumer<String>,
        private val logErrors: Boolean,
        private val logSentData: Boolean,
        private val logResponseStatusText: Boolean
    ) {

        private val customCharts: MutableSet<CustomChart> = HashSet()

        init {
            checkRelocation()
            if (enabled) {
                startSubmitting()
            }
        }

        fun addCustomChart(chart: CustomChart) {
            customCharts.add(chart)
        }

        private fun startSubmitting() {
            val submitTask = Runnable {
                if (!enabled || !checkServiceEnabledSupplier.get()) {
                    scheduler.shutdown()
                    return@Runnable
                }
                if (submitTaskConsumer != null) {
                    submitTaskConsumer.accept(Runnable { submitData() })
                } else {
                    submitData()
                }
            }

            val initialDelay = (1000 * 60 * (3 + Math.random() * 3)).toLong()
            val secondDelay = (1000 * 60 * (Math.random() * 30)).toLong()
            scheduler.schedule(submitTask, initialDelay, TimeUnit.MILLISECONDS)
            scheduler.scheduleAtFixedRate(submitTask, initialDelay + secondDelay, 1000L * 60L * 30L, TimeUnit.MILLISECONDS)
        }

        private fun submitData() {
            val baseJsonBuilder = JsonObjectBuilder()
            appendPlatformDataConsumer.accept(baseJsonBuilder)

            val serviceJsonBuilder = JsonObjectBuilder()
            appendServiceDataConsumer.accept(serviceJsonBuilder)

            val chartData = customCharts
                .mapNotNull { customChart -> customChart.getRequestJsonObject(errorLogger, logErrors) }
                .toTypedArray()

            serviceJsonBuilder.appendField("id", serviceId)
            serviceJsonBuilder.appendField("customCharts", chartData)
            baseJsonBuilder.appendField("service", serviceJsonBuilder.build())
            baseJsonBuilder.appendField("serverUUID", serverUuid)
            baseJsonBuilder.appendField("metricsVersion", METRICS_VERSION)
            val data = baseJsonBuilder.build()

            scheduler.execute {
                try {
                    sendData(data)
                } catch (e: Exception) {
                    if (logErrors) {
                        errorLogger.accept("Could not submit bStats metrics data", e)
                    }
                }
            }
        }

        @Throws(Exception::class)
        private fun sendData(data: JsonObjectBuilder.JsonObject) {
            if (logSentData) {
                infoLogger.accept("Sent bStats metrics data: $data")
            }
            val url = String.format(REPORT_URL, platform)
            val connection = URL(url).openConnection() as HttpsURLConnection
            val compressedData = compress(data.toString())

            connection.requestMethod = "POST"
            connection.addRequestProperty("Accept", "application/json")
            connection.addRequestProperty("Connection", "close")
            connection.addRequestProperty("Content-Encoding", "gzip")
            connection.addRequestProperty("Content-Length", compressedData.size.toString())
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Metrics-Service/1")
            connection.doOutput = true

            DataOutputStream(connection.outputStream).use { outputStream ->
                outputStream.write(compressedData)
            }

            val builder = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream)).use { bufferedReader ->
                var line = bufferedReader.readLine()
                while (line != null) {
                    builder.append(line)
                    line = bufferedReader.readLine()
                }
            }
            if (logResponseStatusText) {
                infoLogger.accept("Sent data to bStats and received response: $builder")
            }
        }

        private fun checkRelocation() {
            if (System.getProperty("bstats.relocatecheck") == null
                || System.getProperty("bstats.relocatecheck") != "false"
            ) {
                val defaultPackage = String(
                    byteArrayOf(
                        'o'.code.toByte(),
                        'r'.code.toByte(),
                        'g'.code.toByte(),
                        '.'.code.toByte(),
                        'b'.code.toByte(),
                        's'.code.toByte(),
                        't'.code.toByte(),
                        'a'.code.toByte(),
                        't'.code.toByte(),
                        's'.code.toByte()
                    )
                )
                val examplePackage = String(
                    byteArrayOf(
                        'y'.code.toByte(),
                        'o'.code.toByte(),
                        'u'.code.toByte(),
                        'r'.code.toByte(),
                        '.'.code.toByte(),
                        'p'.code.toByte(),
                        'a'.code.toByte(),
                        'c'.code.toByte(),
                        'k'.code.toByte(),
                        'a'.code.toByte(),
                        'g'.code.toByte(),
                        'e'.code.toByte()
                    )
                )
                if (MetricsBase::class.java.`package`.name.startsWith(defaultPackage)
                    || MetricsBase::class.java.`package`.name.startsWith(examplePackage)
                ) {
                    throw IllegalStateException("bStats Metrics class has not been relocated correctly!")
                }
            }
        }

        companion object {
            const val METRICS_VERSION: String = "2.2.1"

            private val scheduler: ScheduledExecutorService =
                Executors.newScheduledThreadPool(1) { task -> Thread(task, "bStats-Metrics") }

            private const val REPORT_URL = "https://bStats.org/api/v2/data/%s"

            @Throws(IOException::class)
            private fun compress(str: String): ByteArray {
                val outputStream = ByteArrayOutputStream()
                GZIPOutputStream(outputStream).use { gzip ->
                    gzip.write(str.toByteArray(StandardCharsets.UTF_8))
                }
                return outputStream.toByteArray()
            }
        }
    }

    class AdvancedBarChart(private val chartId: String, private val callable: Callable<Map<String, IntArray>>) :
        CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call()
            if (map == null || map.isEmpty()) {
                return null
            }

            var allSkipped = true
            for ((key, value) in map) {
                if (value.isEmpty()) {
                    continue
                }
                allSkipped = false
                valuesBuilder.appendField(key, value)
            }
            if (allSkipped) {
                return null
            }
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class SimpleBarChart(private val chartId: String, private val callable: Callable<Map<String, Int>>) :
        CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call()
            if (map == null || map.isEmpty()) {
                return null
            }
            for ((key, value) in map) {
                valuesBuilder.appendField(key, intArrayOf(value))
            }
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class MultiLineChart(private val chartId: String, private val callable: Callable<Map<String, Int>>) :
        CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call()
            if (map == null || map.isEmpty()) {
                return null
            }
            var allSkipped = true
            for ((key, value) in map) {
                if (value == 0) {
                    continue
                }
                allSkipped = false
                valuesBuilder.appendField(key, value)
            }
            if (allSkipped) {
                return null
            }
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class AdvancedPie(private val chartId: String, private val callable: Callable<Map<String, Int>>) :
        CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call()
            if (map == null || map.isEmpty()) {
                return null
            }
            var allSkipped = true
            for ((key, value) in map) {
                if (value == 0) {
                    continue
                }
                allSkipped = false
                valuesBuilder.appendField(key, value)
            }
            if (allSkipped) {
                return null
            }
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    abstract class CustomChart(private val chartId: String) {

        init {
            requireNotNull(chartId) { "chartId must not be null" }
        }

        fun getRequestJsonObject(
            errorLogger: BiConsumer<String, Throwable>,
            logErrors: Boolean
        ): JsonObjectBuilder.JsonObject? {
            val builder = JsonObjectBuilder()
            builder.appendField("chartId", chartId)
            return try {
                val data = getChartData()
                if (data == null) {
                    null
                } else {
                    builder.appendField("data", data)
                    builder.build()
                }
            } catch (t: Throwable) {
                if (logErrors) {
                    errorLogger.accept("Failed to get data for custom chart with id $chartId", t)
                }
                null
            }
        }

        @Throws(Exception::class)
        protected abstract fun getChartData(): JsonObjectBuilder.JsonObject?
    }

    class SingleLineChart(private val chartId: String, private val callable: Callable<Int>) : CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val value = callable.call()
            if (value == 0) {
                return null
            }
            return JsonObjectBuilder().appendField("value", value).build()
        }
    }

    class SimplePie(private val chartId: String, private val callable: Callable<String>) : CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val value = callable.call()
            if (value.isNullOrEmpty()) {
                return null
            }
            return JsonObjectBuilder().appendField("value", value).build()
        }
    }

    class DrilldownPie(
        private val chartId: String,
        private val callable: Callable<Map<String, Map<String, Int>>>
    ) : CustomChart(chartId) {
        @Throws(Exception::class)
        override fun getChartData(): JsonObjectBuilder.JsonObject? {
            val valuesBuilder = JsonObjectBuilder()
            val map = callable.call()
            if (map == null || map.isEmpty()) {
                return null
            }
            var reallyAllSkipped = true
            for ((outerKey, innerMap) in map) {
                val valueBuilder = JsonObjectBuilder()
                var allSkipped = true
                for ((innerKey, value) in innerMap) {
                    valueBuilder.appendField(innerKey, value)
                    allSkipped = false
                }
                if (!allSkipped) {
                    reallyAllSkipped = false
                    valuesBuilder.appendField(outerKey, valueBuilder.build())
                }
            }
            if (reallyAllSkipped) {
                return null
            }
            return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
        }
    }

    class JsonObjectBuilder {

        private var builder: StringBuilder? = StringBuilder().append("{")
        private var hasAtLeastOneField = false

        fun appendNull(key: String): JsonObjectBuilder {
            appendFieldUnescaped(key, "null")
            return this
        }

        fun appendField(key: String, value: String?): JsonObjectBuilder {
            if (value == null) {
                throw IllegalArgumentException("JSON value must not be null")
            }
            appendFieldUnescaped(key, "\"${escape(value)}\"")
            return this
        }

        fun appendField(key: String, value: Int): JsonObjectBuilder {
            appendFieldUnescaped(key, value.toString())
            return this
        }

        fun appendField(key: String, obj: JsonObject?): JsonObjectBuilder {
            if (obj == null) {
                throw IllegalArgumentException("JSON object must not be null")
            }
            appendFieldUnescaped(key, obj.toString())
            return this
        }

        fun appendField(key: String, values: Array<String>?): JsonObjectBuilder {
            if (values == null) {
                throw IllegalArgumentException("JSON values must not be null")
            }
            val escapedValues = values.joinToString(",") { value ->
                "\"${escape(value)}\""
            }
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        fun appendField(key: String, values: IntArray?): JsonObjectBuilder {
            if (values == null) {
                throw IllegalArgumentException("JSON values must not be null")
            }
            val escapedValues = values.joinToString(",")
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        fun appendField(key: String, values: Array<JsonObject>?): JsonObjectBuilder {
            if (values == null) {
                throw IllegalArgumentException("JSON values must not be null")
            }
            val escapedValues = values.joinToString(",") { value -> value.toString() }
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        private fun appendFieldUnescaped(key: String, escapedValue: String) {
            val currentBuilder = builder ?: throw IllegalStateException("JSON has already been built")
            if (key == null) {
                throw IllegalArgumentException("JSON key must not be null")
            }
            if (hasAtLeastOneField) {
                currentBuilder.append(",")
            }
            currentBuilder.append("\"").append(escape(key)).append("\":").append(escapedValue)
            hasAtLeastOneField = true
        }

        fun build(): JsonObject {
            val currentBuilder = builder ?: throw IllegalStateException("JSON has already been built")
            val obj = JsonObject(currentBuilder.append("}").toString())
            builder = null
            return obj
        }

        companion object {
            private fun escape(value: String): String {
                val escaped = StringBuilder()
                for (c in value) {
                    when {
                        c == '"' -> escaped.append("\\\"")
                        c == '\\' -> escaped.append("\\\\")
                        c <= '\u000F' -> escaped.append("\\u000").append(c.code.toString(16).lowercase(Locale.ROOT))
                        c <= '\u001F' -> escaped.append("\\u00").append(c.code.toString(16).lowercase(Locale.ROOT))
                        else -> escaped.append(c)
                    }
                }
                return escaped.toString()
            }
        }

        class JsonObject(private val value: String) {
            override fun toString(): String = value
        }
    }
}
