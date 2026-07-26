package dev.x208.sheepmerge

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object SheepInventoryRuntime {
    private const val QUICK_ACCESS_FIRST_SLOT = 0
    private const val QUICK_ACCESS_LAST_SLOT = 5
    private const val SHEARS_ITEM_SLOT = 6
    private const val EGG_ITEM_SLOT = 7
    private const val UPGRADE_COMMAND_SLOT = 8

    @JvmStatic
    fun enforceFarmLoadout(player: Player?) {
        if (player == null || !SheepMergeManager.isSheepFarmWorld(player.world)) {
            return
        }
        val shouldClearNonLoadoutItems = !player.isOp && !SheepMergeManager.isFarmBuildWorld(player.world)
        val quickAccessItems = SheepMergeManager.buildQuickAccessHotbarItemsForRuntime(player)
        val inventory = player.inventory
        val storageContents = inventory.storageContents
        val offHand = inventory.itemInOffHand
        val shearsInOffHand = isSheepMergeShearsItem(offHand)
        var storageChanged = false

        for (slot in storageContents.indices) {
            val itemStack = storageContents[slot]

            if (slot in QUICK_ACCESS_FIRST_SLOT..QUICK_ACCESS_LAST_SLOT) {
                val quickIndex = slot - QUICK_ACCESS_FIRST_SLOT
                val desired = quickAccessItems.getOrNull(quickIndex)
                if (desired == null) {
                    if (itemStack != null &&
                        (shouldClearNonLoadoutItems || SheepMergeManager.isQuickAccessCommandItem(itemStack))
                    ) {
                        storageContents[slot] = null
                        storageChanged = true
                    }
                    continue
                }
                if (itemStack == null || !itemStack.isSimilar(desired) || itemStack.amount != desired.amount) {
                    storageContents[slot] = desired
                    storageChanged = true
                }
                continue
            }

            if (slot == UPGRADE_COMMAND_SLOT) {
                if (itemStack == null || !isSheepMergeUpgradeCommandItem(itemStack) || itemStack.amount != 1) {
                    storageContents[slot] = getSheepMergeUpgradeCommandItem()
                    storageChanged = true
                }
                continue
            }

            if (slot == EGG_ITEM_SLOT) {
                if (itemStack == null || !isSheepMergeEggItem(itemStack) || itemStack.amount != 1) {
                    storageContents[slot] = getSheepMergeEggItem()
                    storageChanged = true
                }
                continue
            }

            if (slot == SHEARS_ITEM_SLOT) {
                if (shearsInOffHand) {
                    if (isSheepMergeShearsItem(itemStack)) {
                        storageContents[slot] = null
                        storageChanged = true
                    }
                } else if (itemStack == null || !isSheepMergeShearsItem(itemStack) || itemStack.amount != 1) {
                    storageContents[slot] = getSheepMergeShears()
                    storageChanged = true
                }
                continue
            }

            if (itemStack == null || !shouldClearNonLoadoutItems) {
                continue
            }
            storageContents[slot] = null
            storageChanged = true
        }

        if (storageChanged) {
            inventory.storageContents = storageContents
        }

        if (shouldClearNonLoadoutItems) {
            val armorContents = inventory.armorContents
            var armorChanged = false
            for (index in armorContents.indices) {
                if (armorContents[index] != null) {
                    armorContents[index] = null
                    armorChanged = true
                }
            }
            if (armorChanged) {
                inventory.armorContents = armorContents
            }
        }

        if (shearsInOffHand && (!isSheepMergeShearsItem(offHand) || offHand.amount != 1)) {
            inventory.setItemInOffHand(getSheepMergeShears())
        }
    }

    @JvmStatic
    fun applyFarmSaturation(player: Player?) {
        if (player == null || !SheepMergeManager.isSheepFarmWorld(player.world)) {
            return
        }
        player.foodLevel = 20
        player.saturation = 20.0f
        player.exhaustion = 0.0f
    }

    @JvmStatic
    fun getSheepMergeUpgradeCommandItem(): ItemStack {
        val item = ItemStack(Material.NETHER_STAR, 1)
        val meta = item.itemMeta
        if (meta != null) {
            meta.setDisplayName(SheepMergeManager.color("&bSheep Merge Menu"))
            meta.lore = listOf(
                SheepMergeManager.hint("Right-click to open menu"),
                SheepMergeManager.hint("Hotbar slot 9")
            )
            item.itemMeta = meta
        }
        return item
    }

    @JvmStatic
    fun getSheepMergeEggItem(): ItemStack {
        val item = ItemStack(Material.SHEEP_SPAWN_EGG, 1)
        val meta = item.itemMeta
        if (meta != null) {
            meta.setDisplayName(SheepMergeManager.color("&eSheep Spawn Egg"))
            meta.lore = listOf(
                SheepMergeManager.hint("Right-click a block to spawn a sheep"),
                SheepMergeManager.hint("Egg count is shown in your XP level"),
                SheepMergeManager.hint("Hotbar slot 8")
            )
            item.itemMeta = meta
        }
        return item
    }

    @JvmStatic
    fun isSheepMergeUpgradeCommandItem(itemStack: ItemStack?): Boolean =
        itemStack?.type == Material.NETHER_STAR

    @JvmStatic
    fun isSheepMergeEggItem(itemStack: ItemStack?): Boolean =
        itemStack?.type == Material.SHEEP_SPAWN_EGG

    @JvmStatic
    fun isForcedFarmLoadoutItem(itemStack: ItemStack?): Boolean =
        itemStack != null &&
            (itemStack.type == Material.SHEARS ||
                isSheepMergeUpgradeCommandItem(itemStack) ||
                isSheepMergeEggItem(itemStack) ||
                SheepMergeManager.isQuickAccessCommandItem(itemStack))

    @JvmStatic
    fun savePlayerInventory(player: Player?) {
        if (player == null || SheepRuntimeUiState.savedInventoriesInternal().containsKey(player.uniqueId)) {
            return
        }
        val inventory = player.inventory
        val contents = InventoryDataUtils.cloneItemStackArray(inventory.contents)
        val armor = InventoryDataUtils.cloneItemStackArray(inventory.armorContents)
        val offhand = inventory.itemInOffHand.clone()
        SheepRuntimeUiState.savedInventoriesInternal()[player.uniqueId] =
            InventoryDataUtils.Snapshot(contents, armor, offhand)
        SheepMergeManager.saveData()
    }

    @JvmStatic
    fun restorePlayerInventory(player: Player?) {
        if (player == null) {
            return
        }
        val snapshot = SheepRuntimeUiState.savedInventoriesInternal().remove(player.uniqueId) ?: return
        val inventory = player.inventory
        inventory.clear()
        inventory.contents = InventoryDataUtils.cloneItemStackArray(snapshot.contents()) ?: emptyArray()
        inventory.armorContents = InventoryDataUtils.cloneItemStackArray(snapshot.armor()) ?: emptyArray()
        inventory.setItemInOffHand(snapshot.offhand()?.clone())
        SheepMergeManager.saveData()
    }

    @JvmStatic
    fun hasSavedInventory(player: Player?): Boolean =
        player != null && SheepRuntimeUiState.savedInventoriesInternal().containsKey(player.uniqueId)

    @JvmStatic
    fun restoreSavedStateOutsideFarm(player: Player?) {
        if (player == null || SheepMergeManager.isSheepFarmWorld(player.world)) {
            return
        }
        maybeRestorePlayerInventoryOutsideFarm(player)
        SheepScoreboardRuntime.maybeRestorePlayerScoreboardOutsideFarm(player)
        player.setPlayerListName(null)
        SheepMergeManager.clearEggTimer(player)
    }

    private fun maybeRestorePlayerInventoryOutsideFarm(player: Player) {
        val playerId = player.uniqueId
        if (!SheepRuntimeUiState.savedInventoriesInternal().containsKey(playerId)) {
            return
        }
        if (hasAnyForcedFarmLoadoutItem(player) || isInventoryCompletelyEmpty(player)) {
            restorePlayerInventory(player)
            return
        }
        SheepRuntimeUiState.savedInventoriesInternal().remove(playerId)
        SheepMergeManager.saveData()
    }

    private fun hasAnyForcedFarmLoadoutItem(player: Player): Boolean =
        player.inventory.contents.any(::isForcedFarmLoadoutItem)

    private fun isInventoryCompletelyEmpty(player: Player): Boolean {
        val inventory = player.inventory
        return inventory.contents.none(::isPresentItem) &&
            inventory.armorContents.none(::isPresentItem) &&
            !isPresentItem(inventory.itemInOffHand)
    }

    private fun isPresentItem(itemStack: ItemStack?): Boolean =
        itemStack != null && itemStack.type != Material.AIR

    @JvmStatic
    fun clearForcedFarmLoadoutWithoutSnapshot(player: Player?) {
        if (player == null) {
            return
        }
        val inventory = player.inventory
        val storage = inventory.storageContents
        var changed = false

        if (UPGRADE_COMMAND_SLOT in storage.indices &&
            isSheepMergeUpgradeCommandItem(storage[UPGRADE_COMMAND_SLOT])
        ) {
            storage[UPGRADE_COMMAND_SLOT] = null
            changed = true
        }
        if (EGG_ITEM_SLOT in storage.indices && isSheepMergeEggItem(storage[EGG_ITEM_SLOT])) {
            storage[EGG_ITEM_SLOT] = null
            changed = true
        }
        if (SHEARS_ITEM_SLOT in storage.indices && isSheepMergeShearsItem(storage[SHEARS_ITEM_SLOT])) {
            storage[SHEARS_ITEM_SLOT] = null
            changed = true
        }

        val quickEnd = minOf(storage.lastIndex, QUICK_ACCESS_LAST_SLOT)
        for (slot in QUICK_ACCESS_FIRST_SLOT..quickEnd) {
            if (SheepMergeManager.isQuickAccessCommandItem(storage[slot])) {
                storage[slot] = null
                changed = true
            }
        }
        if (changed) {
            inventory.storageContents = storage
        }
        if (isSheepMergeShearsItem(inventory.itemInOffHand)) {
            inventory.setItemInOffHand(null)
        }
    }

    @JvmStatic
    fun isSheepMergeShearsItem(itemStack: ItemStack?): Boolean {
        if (itemStack?.type != Material.SHEARS) {
            return false
        }
        val meta = itemStack.itemMeta ?: return false
        return meta.isUnbreakable && meta.displayName == "Sheep Merge Shears"
    }

    @JvmStatic
    fun isManagedShearsHotbarSlot(slot: Int): Boolean = slot == SHEARS_ITEM_SLOT

    @JvmStatic
    fun getSheepMergeShears(): ItemStack {
        val shears = ItemStack(Material.SHEARS, 1)
        val meta = shears.itemMeta
        if (meta != null) {
            meta.isUnbreakable = true
            meta.setDisplayName("Sheep Merge Shears")
            shears.itemMeta = meta
        }
        return shears
    }
}