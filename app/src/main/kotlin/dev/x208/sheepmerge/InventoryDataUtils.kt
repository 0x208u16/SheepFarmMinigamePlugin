package dev.x208.sheepmerge

import org.bukkit.inventory.ItemStack

internal object InventoryDataUtils {

    @JvmStatic
    fun serializeInventoryList(source: Array<ItemStack?>?): List<ItemStack?>? {
        if (source == null) {
            return null
        }
        return source.map { itemStack -> itemStack?.clone() }
    }

    @JvmStatic
    fun deserializeInventoryList(source: List<*>?): Array<ItemStack?>? {
        if (source == null) {
            return null
        }
        return Array(source.size) { index ->
            val value = source[index]
            if (value is ItemStack) value.clone() else null
        }
    }

    @JvmStatic
    fun cloneItemStackArray(source: Array<ItemStack?>?): Array<ItemStack?>? {
        if (source == null) {
            return null
        }
        return Array(source.size) { index -> source[index]?.clone() }
    }

    class Snapshot(
        private val contents: Array<ItemStack?>?,
        private val armor: Array<ItemStack?>?,
        private val offhand: ItemStack?
    ) {
        fun contents(): Array<ItemStack?>? {
            return contents
        }

        fun armor(): Array<ItemStack?>? {
            return armor
        }

        fun offhand(): ItemStack? {
            return offhand
        }
    }
}
