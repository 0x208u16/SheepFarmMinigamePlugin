package dev.x208.sheepmerge

import org.bukkit.configuration.file.FileConfiguration
import java.util.UUID

object SheepUpgradeState {
    private const val HIGHEST_ANNOUNCED_TIER_KEY = "highestAnnouncedTier"
    private const val HIGHEST_ANNOUNCED_RAINBOW_TIER_KEY = "highestAnnouncedRainbowTier"
    private const val SHEAR_SHOP_KEY = "shearShop"
    private const val SHEAR_WOOL_SAVE_KEY = "shearWoolSave"
    private const val SHEAR_TIER_BOOST_KEY = "shearTierBoost"

    private val highestAnnouncedTierByPlayer: MutableMap<UUID, Int> = HashMap()
    private val highestAnnouncedRainbowTierByPlayer: MutableMap<UUID, Int> = HashMap()
    private val shearShopLevelByPlayer: MutableMap<UUID, Int> = HashMap()
    private val shearWoolSaveLevelByPlayer: MutableMap<UUID, Int> = HashMap()
    private val shearTierBoostLevelByPlayer: MutableMap<UUID, Int> = HashMap()

    @JvmStatic fun getHighestAnnouncedTier(playerId: UUID?, defaultTier: Int): Int =
        playerId?.let { highestAnnouncedTierByPlayer[it] } ?: defaultTier
    @JvmStatic fun setHighestAnnouncedTier(playerId: UUID, tier: Int) { highestAnnouncedTierByPlayer[playerId] = tier }
    @JvmStatic fun getHighestAnnouncedRainbowTier(playerId: UUID?): Int =
        playerId?.let { highestAnnouncedRainbowTierByPlayer[it] } ?: 0
    @JvmStatic fun setHighestAnnouncedRainbowTier(playerId: UUID, tier: Int) {
        highestAnnouncedRainbowTierByPlayer[playerId] = tier
    }
    @JvmStatic fun getShearShopLevel(playerId: UUID?): Int = playerId?.let { shearShopLevelByPlayer[it] } ?: 0
    @JvmStatic fun setShearShopLevel(playerId: UUID, level: Int) { shearShopLevelByPlayer[playerId] = level }
    @JvmStatic fun getShearWoolSaveLevel(playerId: UUID?): Int = playerId?.let { shearWoolSaveLevelByPlayer[it] } ?: 0
    @JvmStatic fun setShearWoolSaveLevel(playerId: UUID, level: Int) { shearWoolSaveLevelByPlayer[playerId] = level }
    @JvmStatic fun getShearTierBoostLevel(playerId: UUID?): Int = playerId?.let { shearTierBoostLevelByPlayer[it] } ?: 0
    @JvmStatic fun setShearTierBoostLevel(playerId: UUID, level: Int) { shearTierBoostLevelByPlayer[playerId] = level }

    @JvmStatic
    fun resetShearUpgrades(playerId: UUID?) {
        if (playerId == null) return
        shearShopLevelByPlayer.remove(playerId)
        shearWoolSaveLevelByPlayer.remove(playerId)
        shearTierBoostLevelByPlayer.remove(playerId)
    }

    @JvmStatic
    fun resetAdminPlayer(playerId: UUID?) {
        if (playerId == null) return
        highestAnnouncedTierByPlayer.remove(playerId)
        highestAnnouncedRainbowTierByPlayer.remove(playerId)
        resetShearUpgrades(playerId)
    }

    @JvmStatic
    fun clear() {
        highestAnnouncedTierByPlayer.clear()
        highestAnnouncedRainbowTierByPlayer.clear()
        shearShopLevelByPlayer.clear()
        shearWoolSaveLevelByPlayer.clear()
        shearTierBoostLevelByPlayer.clear()
    }

    @JvmStatic
    fun clearPersistedKeys(dataConfig: FileConfiguration) {
        dataConfig.set(HIGHEST_ANNOUNCED_TIER_KEY, null)
        dataConfig.set(HIGHEST_ANNOUNCED_RAINBOW_TIER_KEY, null)
        dataConfig.set(SHEAR_SHOP_KEY, null)
        dataConfig.set(SHEAR_WOOL_SAVE_KEY, null)
        dataConfig.set(SHEAR_TIER_BOOST_KEY, null)
    }

    @JvmStatic
    fun saveTo(dataConfig: FileConfiguration) {
        saveIntMap(dataConfig, HIGHEST_ANNOUNCED_TIER_KEY, highestAnnouncedTierByPlayer)
        saveIntMap(dataConfig, HIGHEST_ANNOUNCED_RAINBOW_TIER_KEY, highestAnnouncedRainbowTierByPlayer)
        saveIntMap(dataConfig, SHEAR_SHOP_KEY, shearShopLevelByPlayer)
        saveIntMap(dataConfig, SHEAR_WOOL_SAVE_KEY, shearWoolSaveLevelByPlayer)
        saveIntMap(dataConfig, SHEAR_TIER_BOOST_KEY, shearTierBoostLevelByPlayer)
    }

    @JvmStatic
    fun loadFrom(dataConfig: FileConfiguration, minimumTier: Int, maximumTier: Int) {
        loadIntMap(dataConfig, HIGHEST_ANNOUNCED_TIER_KEY, highestAnnouncedTierByPlayer) {
            it.coerceIn(minimumTier, maximumTier)
        }
        loadIntMap(dataConfig, HIGHEST_ANNOUNCED_RAINBOW_TIER_KEY, highestAnnouncedRainbowTierByPlayer) {
            it.coerceAtLeast(0)
        }
        loadIntMap(dataConfig, SHEAR_SHOP_KEY, shearShopLevelByPlayer)
        loadIntMap(dataConfig, SHEAR_WOOL_SAVE_KEY, shearWoolSaveLevelByPlayer)
        loadIntMap(dataConfig, SHEAR_TIER_BOOST_KEY, shearTierBoostLevelByPlayer)
    }

    private fun saveIntMap(dataConfig: FileConfiguration, key: String, values: Map<UUID, Int>) {
        for ((playerId, value) in values) dataConfig.set("$key.$playerId", value)
    }

    private fun loadIntMap(
        dataConfig: FileConfiguration,
        key: String,
        destination: MutableMap<UUID, Int>,
        transform: (Int) -> Int = { it }
    ) {
        val section = dataConfig.getConfigurationSection(key) ?: return
        for (playerKey in section.getKeys(false)) {
            val playerId = runCatching { UUID.fromString(playerKey) }.getOrNull() ?: continue
            destination[playerId] = transform(dataConfig.getInt("$key.$playerKey", 0))
        }
    }
}