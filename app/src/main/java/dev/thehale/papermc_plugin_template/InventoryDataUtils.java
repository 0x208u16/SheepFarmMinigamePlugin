package dev.thehale.papermc_plugin_template;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;

final class InventoryDataUtils {

    private InventoryDataUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    static List<ItemStack> serializeInventoryList(ItemStack[] source) {
        if (source == null) {
            return null;
        }
        List<ItemStack> serialized = new ArrayList<>(source.length);
        for (ItemStack itemStack : source) {
            serialized.add(itemStack == null ? null : itemStack.clone());
        }
        return serialized;
    }

    static ItemStack[] deserializeInventoryList(List<?> source) {
        if (source == null) {
            return null;
        }
        ItemStack[] deserialized = new ItemStack[source.size()];
        for (int i = 0; i < source.size(); i++) {
            Object value = source.get(i);
            deserialized[i] = value instanceof ItemStack itemStack ? itemStack.clone() : null;
        }
        return deserialized;
    }

    static ItemStack[] cloneItemStackArray(ItemStack[] source) {
        if (source == null) {
            return null;
        }
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            clone[i] = source[i] == null ? null : source[i].clone();
        }
        return clone;
    }

    static final class Snapshot {
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final ItemStack offhand;

        Snapshot(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
            this.contents = contents;
            this.armor = armor;
            this.offhand = offhand;
        }

        ItemStack[] contents() {
            return contents;
        }

        ItemStack[] armor() {
            return armor;
        }

        ItemStack offhand() {
            return offhand;
        }
    }
}
