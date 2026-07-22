package dev.x208.sheepmerge;

import java.util.List;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class MenuItemFactory {

    private MenuItemFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    static ItemStack create(Material material, String name, List<String> lore) {
        return create(material, name, lore, false);
    }

    static ItemStack create(Material material, String name, List<String> lore, boolean forceGlint) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(formatName(name));
            List<String> formattedLore = formatLore(lore);
            meta.setLore(formattedLore);
            if (forceGlint || shouldGlint(formattedLore)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    static ItemStack createEnchanted(Material material, String name, List<String> lore) {
        return create(material, name, lore, true);
    }

    static ItemStack createShieldWithWhiteBanner(String name, List<String> lore) {
        ItemStack item = create(Material.SHIELD, name, lore, true);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            try {
                meta.getClass().getMethod("setBaseColor", DyeColor.class).invoke(meta, DyeColor.WHITE);
            } catch (ReflectiveOperationException ignored) {
                // The API version in use may not expose shield banner colors.
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static boolean shouldGlint(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return false;
        }
        for (String line : lore) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String stripped = ChatColor.stripColor(line);
            if (stripped == null || stripped.isBlank()) {
                continue;
            }
            String upper = stripped.toUpperCase(Locale.ROOT);
            if (upper.contains("MAXED")) {
                return true;
            }
        }
        return false;
    }

    private static String formatName(String name) {
        if (name == null || name.isBlank()) {
            return SheepMergeManager.color("&fMenu Item");
        }
        if (name.startsWith("&")) {
            return SheepMergeManager.color(name);
        }
        return SheepMergeManager.color("&e" + name);
    }

    private static List<String> formatLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            return List.of(SheepMergeManager.color("&7No details."));
        }
        return lore.stream()
                .map(MenuItemFactory::normalizeLoreLine)
                .map(MenuItemFactory::colorLoreLine)
                .toList();
    }

    private static String normalizeLoreLine(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (trimmed.equalsIgnoreCase("Click to purchase")) {
            return "Click: Buy";
        }
        if (trimmed.equalsIgnoreCase("Click to open")) {
            return "Click: Open";
        }
        if (trimmed.equalsIgnoreCase("Click to upgrade")) {
            return "Click: Upgrade";
        }
        if (trimmed.equalsIgnoreCase("Click to unlock")) {
            return "Click: Unlock";
        }
        if (trimmed.equalsIgnoreCase("Click to activate")) {
            return "Click: Activate";
        }
        if (trimmed.equalsIgnoreCase("Click to toggle enable/disable")) {
            return "Click: Toggle";
        }
        if (trimmed.equalsIgnoreCase("Click to toggle")) {
            return "Click: Toggle";
        }
        if (trimmed.equalsIgnoreCase("Click to go back")) {
            return "Click: Back";
        }
        if (trimmed.equalsIgnoreCase("Click to refund")) {
            return "Click: Refund";
        }
        if (trimmed.equalsIgnoreCase("Click to prestige multiple")) {
            return "Click: Prestige";
        }
        return trimmed;
    }

    private static String colorLoreLine(String line) {
        if (line == null || line.isBlank()) {
            return SheepMergeManager.color("&7-");
        }
        if (line.startsWith("&")) {
            return SheepMergeManager.color(line);
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.startsWith("click:")) {
            return SheepMergeManager.color("&a" + line);
        }
        if (lower.startsWith("cost:") || lower.startsWith("next unlock cost:")) {
            return SheepMergeManager.color("&6" + line);
        }
        if (lower.startsWith("level:") || lower.startsWith("status:")
                || lower.startsWith("current:") || lower.startsWith("next:")
                || lower.startsWith("chance:") || lower.startsWith("duration:")
                || lower.startsWith("uses per activation:") || lower.startsWith("unlocks bought:")
                || lower.startsWith("in stock:")) {
            return SheepMergeManager.color("&b" + line);
        }
        if (lower.startsWith("maxed") || lower.contains("maxed")
                || lower.contains("unlocked") || lower.startsWith("done ")) {
            return SheepMergeManager.color("&a" + line);
        }
        if (lower.contains("locked") || lower.startsWith("todo ")) {
            return SheepMergeManager.color("&c" + line);
        }
        return SheepMergeManager.color("&7" + line);
    }
}
