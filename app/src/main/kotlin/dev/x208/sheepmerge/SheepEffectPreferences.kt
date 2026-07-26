package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import java.util.UUID

object SheepEffectPreferences {

    private const val SOUND_EFFECTS_KEY = "soundEffectsEnabled"
    private const val SHEEP_SOUNDS_KEY = "sheepSoundsEnabled"
    private const val PARTICLE_EFFECTS_KEY = "particleEffectsEnabled"

    private val soundEffectsEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val sheepSoundsEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()
    private val particleEffectsEnabledByPlayer: MutableMap<UUID, Boolean> = HashMap()

    @JvmStatic
    fun areSoundEffectsEnabled(player: Player?): Boolean {
        return player != null && soundEffectsEnabledByPlayer.getOrDefault(player.uniqueId, true)
    }

    @JvmStatic
    fun toggleSoundEffects(player: Player?): Boolean {
        return toggle(player, soundEffectsEnabledByPlayer)
    }

    @JvmStatic
    fun areSheepSoundsEnabled(player: Player?): Boolean {
        return player != null && sheepSoundsEnabledByPlayer.getOrDefault(player.uniqueId, true)
    }

    @JvmStatic
    fun toggleSheepSounds(player: Player?): Boolean {
        return toggle(player, sheepSoundsEnabledByPlayer)
    }

    @JvmStatic
    fun areParticleEffectsEnabled(player: Player?): Boolean {
        return player != null && particleEffectsEnabledByPlayer.getOrDefault(player.uniqueId, true)
    }

    @JvmStatic
    fun toggleParticleEffects(player: Player?): Boolean {
        return toggle(player, particleEffectsEnabledByPlayer)
    }

    @JvmStatic
    fun resetPlayer(playerId: UUID?) {
        if (playerId == null) {
            return
        }
        soundEffectsEnabledByPlayer.remove(playerId)
        sheepSoundsEnabledByPlayer.remove(playerId)
        particleEffectsEnabledByPlayer.remove(playerId)
    }

    @JvmStatic
    fun clear() {
        soundEffectsEnabledByPlayer.clear()
        sheepSoundsEnabledByPlayer.clear()
        particleEffectsEnabledByPlayer.clear()
    }

    @JvmStatic
    fun clearPersistedKeys(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        dataConfig.set(SOUND_EFFECTS_KEY, null)
        dataConfig.set(SHEEP_SOUNDS_KEY, null)
        dataConfig.set(PARTICLE_EFFECTS_KEY, null)
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        savePreferences(dataConfig, SOUND_EFFECTS_KEY, soundEffectsEnabledByPlayer)
        savePreferences(dataConfig, SHEEP_SOUNDS_KEY, sheepSoundsEnabledByPlayer)
        savePreferences(dataConfig, PARTICLE_EFFECTS_KEY, particleEffectsEnabledByPlayer)
    }

    @JvmStatic
    fun loadFrom(dataConfig: FileConfiguration?) {
        if (dataConfig == null) {
            return
        }
        loadDisabledPreferences(dataConfig, SOUND_EFFECTS_KEY, soundEffectsEnabledByPlayer)
        loadDisabledPreferences(dataConfig, SHEEP_SOUNDS_KEY, sheepSoundsEnabledByPlayer)
        loadDisabledPreferences(dataConfig, PARTICLE_EFFECTS_KEY, particleEffectsEnabledByPlayer)
    }

    private fun toggle(player: Player?, preferences: MutableMap<UUID, Boolean>): Boolean {
        if (player == null) {
            return false
        }
        val playerId = player.uniqueId
        val enabled = !preferences.getOrDefault(playerId, true)
        preferences[playerId] = enabled
        return enabled
    }

    private fun savePreferences(
        dataConfig: FileConfiguration,
        key: String,
        preferences: Map<UUID, Boolean>
    ) {
        for ((playerId, enabled) in preferences) {
            dataConfig.set("$key.$playerId", enabled)
        }
    }

    private fun loadDisabledPreferences(
        dataConfig: FileConfiguration,
        key: String,
        preferences: MutableMap<UUID, Boolean>
    ) {
        dataConfig.getConfigurationSection(key)?.getKeys(false)?.forEach { playerIdKey ->
            try {
                val playerId = UUID.fromString(playerIdKey)
                if (!dataConfig.getBoolean("$key.$playerIdKey", true)) {
                    preferences[playerId] = false
                }
            } catch (_: IllegalArgumentException) {
                // Ignore invalid UUIDs.
            }
        }
    }
}