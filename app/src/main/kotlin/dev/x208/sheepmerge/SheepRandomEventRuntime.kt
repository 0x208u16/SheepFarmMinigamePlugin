package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Sheep
import org.bukkit.util.Vector
import java.util.Random

internal object SheepRandomEventRuntime {
    private const val RANDOM_EVENT_ROLL_INTERVAL_MS = 60_000L
    private const val RANDOM_EVENT_TRIGGER_CHANCE_DENOMINATOR = 10
    private const val SHEEP_RAIN_EVENT_DURATION_MS = 60_000L
    private const val SHEEP_RAIN_MIN_INTERVAL_MS = 1_000L
    private const val SHEEP_RAIN_MAX_INTERVAL_MS = 3_000L
    private const val SHEEP_RAIN_SPAWN_HEIGHT = 12
    private const val SHEEP_RAIN_HORIZONTAL_PADDING = 1.5
    private const val COMBO_FRENZY_EVENT_DURATION_MS = 60_000L
    private const val COMBO_FRENZY_MULTIPLIER = 10.0

    private val random = Random()
    private var nextRandomEventRollAtMs = 0L
    private var sheepRainEventEndsAtMs = 0L
    private var nextSheepRainSpawnAtMs = 0L
    private var comboFrenzyEventEndsAtMs = 0L
    private var sheepRainBossBar: BossBar? = null
    private var lastGameplayTipIndex = -1

    private val gameplayTips = listOf(
        "&7Use &e/sheepmerge &7to jump to your farm. Use it again while visiting to return home.",
        "&7Your menu item is the &bNether Star &7in hotbar slot 9. Right-click it to open Sheep Merge Menu.",
        "&7Eggs are shown as your XP level. The XP bar shows time until the next egg.",
        "&7Spawn eggs are in hotbar slot 8. No eggs? Wait for the timer or raise egg speed.",
        "&7Merge faster: sneak-right-click a sheep to carry it, then right-click a same-tier sheep.",
        "&7Shearing and merging together are your main point income. Keep both loops active.",
        "&7Rainbow sheep can merge with matching rainbow tier to push rainbow tiers higher forever.",
        "&7Shear Shop boosts shear value and adds procs like Wool Keeper and Tier Booster.",
        "&7Quest objectives reset over time. Finish them to earn quest points for active abilities.",
        "&7Quest Upgrades increase ability duration and lower ability costs.",
        "&7Merge Assist auto-merges carried sheep while charges remain.",
        "&7Combo score multiplies your gains. Keep merging to avoid decay and maintain high value.",
        "&7Combo Upgrades improve decay, gain, and max combo cap.",
        "&7Prestige resets normal progress and grants prestige points for permanent account upgrades.",
        "&7Prestige upgrades can raise egg cap, base spawn tier, and several maximum upgrade caps.",
        "&7Prestige refund lets you respec prestige upgrades after cooldown.",
        "&7Automation points are earned over playtime. Spend them in Automation Upgrades.",
        "&7Automation tracks start disabled. Buy and toggle each track on when you are ready.",
        "&7Auto Spawn now drops sheep from the sky and still spends eggs.",
        "&7Farm worlds now load from a cached structure copy instead of rebuilding every block.",
        "&7Auto Prestige can run automatically once unlocked and toggled on.",
        "&7Use &e/sheepmerge visit <player> &7to visit open farms and &e/sheepmerge visit -toggle &7to manage access.",
        "&7Use &e/sheepmerge status &7to quickly check your Coins, quests, combo, and prestige progress.",
        "&7Admins: &e/sheepmerge backup list/load/delete/recover &7manage compressed backups safely.",
    )

    @JvmStatic
    fun tickRandomFarmEvents() {
        val plugin = SheepMergePlugin.instance ?: return
        val now = System.currentTimeMillis()
        if (sheepRainEventEndsAtMs > now) {
            tickSheepRainEvent(plugin, now)
        } else if (sheepRainEventEndsAtMs > 0L) {
            endSheepRainEvent(plugin)
        }

        if (comboFrenzyEventEndsAtMs <= now && comboFrenzyEventEndsAtMs > 0L) {
            endComboFrenzyEvent(plugin)
        }

        if (nextRandomEventRollAtMs <= 0L) {
            nextRandomEventRollAtMs = now + RANDOM_EVENT_ROLL_INTERVAL_MS
            return
        }
        if (now < nextRandomEventRollAtMs) return

        nextRandomEventRollAtMs = now + RANDOM_EVENT_ROLL_INTERVAL_MS
        if (random.nextInt(RANDOM_EVENT_TRIGGER_CHANCE_DENOMINATOR) == 0) startSheepRainEvent(plugin, now)
        if (random.nextInt(RANDOM_EVENT_TRIGGER_CHANCE_DENOMINATOR) == 0) startComboFrenzyEvent(plugin, now)
    }

    @JvmStatic
    fun triggerSheepStormEvent(): Boolean {
        val plugin = SheepMergePlugin.instance ?: return false
        val now = System.currentTimeMillis()
        if (sheepRainEventEndsAtMs > now) return false
        nextRandomEventRollAtMs = now + RANDOM_EVENT_ROLL_INTERVAL_MS
        startSheepRainEvent(plugin, now)
        return true
    }

    @JvmStatic
    fun isSheepStormActive(): Boolean = sheepRainEventEndsAtMs > System.currentTimeMillis()

    @JvmStatic
    fun triggerComboFrenzyEvent(): Boolean {
        val plugin = SheepMergePlugin.instance ?: return false
        val now = System.currentTimeMillis()
        if (comboFrenzyEventEndsAtMs > now) return false
        nextRandomEventRollAtMs = now + RANDOM_EVENT_ROLL_INTERVAL_MS
        startComboFrenzyEvent(plugin, now)
        return true
    }

    @JvmStatic
    fun broadcastRandomGameplayTip() {
        val plugin = SheepMergePlugin.instance ?: return
        if (plugin.server.onlinePlayers.isEmpty()) return

        val message = SheepMergeManager.color("&8[&6SheepMerge Tip&8] &f${getNextGameplayTip()}")
        plugin.server.onlinePlayers.forEach { player ->
            if (SheepMergeManager.isSheepFarmWorld(player.world)) player.sendMessage(message)
        }
    }

    @JvmStatic
    fun getComboGainMultiplier(now: Long): Double =
        if (comboFrenzyEventEndsAtMs > now) COMBO_FRENZY_MULTIPLIER else 1.0

    @JvmStatic
    fun isComboFrenzyActive(now: Long): Boolean = comboFrenzyEventEndsAtMs > now

    @JvmStatic
    fun getComboFrenzyRemainingMs(now: Long): Long = (comboFrenzyEventEndsAtMs - now).coerceAtLeast(0L)

    @JvmStatic
    fun getComboFrenzyProgress(now: Long): Double =
        (getComboFrenzyRemainingMs(now).toDouble() / COMBO_FRENZY_EVENT_DURATION_MS).coerceIn(0.0, 1.0)

    private fun startComboFrenzyEvent(plugin: SheepMergePlugin, now: Long) {
        comboFrenzyEventEndsAtMs = now + COMBO_FRENZY_EVENT_DURATION_MS
        plugin.server.onlinePlayers.forEach { player ->
            if (!SheepMergeManager.isSheepFarmWorld(player.world)) return@forEach
            player.sendMessage(SheepMergeManager.action("Random Event: Combo Frenzy started (10x combo gain)."))
            SheepMergeManager.randomEventPlaySound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.6f)
        }
    }

    private fun endComboFrenzyEvent(plugin: SheepMergePlugin) {
        comboFrenzyEventEndsAtMs = 0L
        plugin.server.onlinePlayers.forEach { player ->
            if (!SheepMergeManager.isSheepFarmWorld(player.world)) return@forEach
            player.sendMessage(SheepMergeManager.hint("Random Event: Combo Frenzy ended."))
            SheepMergeManager.randomEventPlaySound(player, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.25f)
        }
    }

    private fun getNextGameplayTip(): String {
        if (gameplayTips.isEmpty()) return "&7Keep merging sheep and upgrading your farm."
        if (gameplayTips.size == 1) return gameplayTips[0]

        var nextIndex = random.nextInt(gameplayTips.size)
        while (nextIndex == lastGameplayTipIndex) nextIndex = random.nextInt(gameplayTips.size)
        lastGameplayTipIndex = nextIndex
        return gameplayTips[nextIndex]
    }

    private fun startSheepRainEvent(plugin: SheepMergePlugin, now: Long) {
        sheepRainEventEndsAtMs = now + SHEEP_RAIN_EVENT_DURATION_MS
        nextSheepRainSpawnAtMs = now

        val bossBar = sheepRainBossBar
            ?: Bukkit.createBossBar("Sheep Storm", BarColor.WHITE, BarStyle.SEGMENTED_10).also {
                sheepRainBossBar = it
            }
        bossBar.isVisible = true

        plugin.server.onlinePlayers.forEach { player ->
            if (!SheepMergeManager.isSheepFarmWorld(player.world)) {
                bossBar.removePlayer(player)
                return@forEach
            }
            bossBar.addPlayer(player)
            player.sendMessage(SheepMergeManager.action("Random Event: Sheep Storm started."))
            SheepMergeManager.randomEventPlaySound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.6f)
        }
        plugin.server.worlds.forEach { world ->
            if (!SheepMergeManager.isSheepFarmWorld(world)) return@forEach
            world.setStorm(true)
            world.isThundering = true
            world.weatherDuration = (SHEEP_RAIN_EVENT_DURATION_MS / 50L).toInt() + 40
            world.thunderDuration = (SHEEP_RAIN_EVENT_DURATION_MS / 50L).toInt() + 40
        }
        updateSheepRainBossBar(plugin, now)
    }

    private fun endSheepRainEvent(plugin: SheepMergePlugin) {
        sheepRainEventEndsAtMs = 0L
        nextSheepRainSpawnAtMs = 0L
        sheepRainBossBar?.let { bossBar ->
            bossBar.removeAll()
            bossBar.isVisible = false
        }
        plugin.server.worlds.forEach { world ->
            if (!SheepMergeManager.isSheepFarmWorld(world)) return@forEach
            world.isThundering = false
            world.setStorm(false)
            world.weatherDuration = 0
            world.thunderDuration = 0
        }
        plugin.server.onlinePlayers.forEach { player ->
            if (!SheepMergeManager.isSheepFarmWorld(player.world)) return@forEach
            player.sendMessage(SheepMergeManager.hint("Random Event: Sheep Storm ended."))
            SheepMergeManager.randomEventPlaySound(player, Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.2f)
        }
    }

    private fun tickSheepRainEvent(plugin: SheepMergePlugin, now: Long) {
        updateSheepRainBossBar(plugin, now)
        if (now < nextSheepRainSpawnAtMs) return

        plugin.server.worlds.forEach { world ->
            if (SheepMergeManager.isSheepFarmWorld(world) && !SheepMergeManager.isWorldAtLimit(world)) {
                spawnRainSheep(world)
            }
        }
        nextSheepRainSpawnAtMs = now + getRandomSheepRainIntervalMs()
    }

    private fun updateSheepRainBossBar(plugin: SheepMergePlugin, now: Long) {
        val bossBar = sheepRainBossBar ?: return
        val remaining = (sheepRainEventEndsAtMs - now).coerceAtLeast(0L)
        bossBar.progress = (remaining.toDouble() / SHEEP_RAIN_EVENT_DURATION_MS).coerceIn(0.0, 1.0)
        bossBar.setTitle(
            SheepMergeManager.color("&fSheep Storm &7- &e${SheepFormatting.formatDuration(remaining)} left")
        )
        plugin.server.onlinePlayers.forEach { player ->
            if (SheepMergeManager.isSheepFarmWorld(player.world)) bossBar.addPlayer(player)
            else bossBar.removePlayer(player)
        }
    }

    private fun getRandomSheepRainIntervalMs(): Long {
        val range = SHEEP_RAIN_MAX_INTERVAL_MS - SHEEP_RAIN_MIN_INTERVAL_MS
        if (range <= 0L) return SHEEP_RAIN_MIN_INTERVAL_MS
        return SHEEP_RAIN_MIN_INTERVAL_MS + random.nextInt(range.toInt() + 1)
    }

    @JvmStatic
    fun createSkySheepSpawnLocation(world: World): Location {
        val min = SheepMergeManager.randomEventFarmMinXz() + SHEEP_RAIN_HORIZONTAL_PADDING
        val max = SheepMergeManager.randomEventFarmMaxXz() - SHEEP_RAIN_HORIZONTAL_PADDING
        val x = min + random.nextDouble() * (max - min).coerceAtLeast(0.01)
        val z = min + random.nextDouble() * (max - min).coerceAtLeast(0.01)
        val y = (SheepMergeManager.randomEventFarmBaseY() + SHEEP_RAIN_SPAWN_HEIGHT).toDouble()
        return Location(world, x, y, z)
    }

    private fun spawnRainSheep(world: World) {
        val spawnLocation = createSkySheepSpawnLocation(world)
        val sheep = world.spawn(spawnLocation, Sheep::class.java)
        SheepMergeManager.setSheepTier(sheep, SheepMergeManager.rollSpawnTier(world))
        sheep.velocity = Vector(0.0, -0.1, 0.0)
        SheepMergeManager.entitySpawnParticle(
            world,
            Particle.CLOUD,
            spawnLocation.clone().add(0.0, 0.4, 0.0),
            14,
            0.2,
            0.4,
            0.2,
            0.02,
        )
        SheepMergeManager.entityPlaySheepSound(world, spawnLocation, Sound.ENTITY_SHEEP_AMBIENT, 0.7f, 1.5f)
    }
}