package dev.x208.sheepmerge

import org.bukkit.ChatColor
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.Banner
import org.bukkit.block.banner.Pattern
import org.bukkit.block.banner.PatternType
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.ItemMeta
import java.util.Locale

internal object MenuItemFactory {

    @JvmStatic
    fun create(material: Material, name: String, lore: List<String>): ItemStack {
        return create(material, name, lore, false)
    }

    @JvmStatic
    fun create(material: Material, name: String, lore: List<String>, forceGlint: Boolean): ItemStack {
        val item = ItemStack(material, 1)
        val meta = item.itemMeta
        if (meta != null) {
            meta.setDisplayName(formatName(name))
            val formattedLore = formatLore(lore)
            meta.lore = formattedLore
            if (forceGlint || shouldGlint(formattedLore)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true)
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
            item.itemMeta = meta
        }
        return item
    }

    @JvmStatic
    fun createEnchanted(material: Material, name: String, lore: List<String>): ItemStack {
        return create(material, name, lore, true)
    }

    @JvmStatic
    fun createShieldWithWhiteBanner(name: String, lore: List<String>): ItemStack {
        return createShieldWithWhiteBanner(name, lore, false)
    }

    @JvmStatic
    fun createShieldWithWhiteBanner(name: String, lore: List<String>, forceGlint: Boolean): ItemStack {
        val item = create(Material.SHIELD, name, lore, forceGlint)
        val meta = item.itemMeta
        if (meta is BlockStateMeta && meta.blockState is Banner) {
            val banner = meta.blockState as Banner
            banner.baseColor = DyeColor.WHITE
            if (banner.patterns.isEmpty()) {
                banner.addPattern(Pattern(DyeColor.WHITE, PatternType.BASE))
            }
            meta.blockState = banner
            item.itemMeta = meta
            return item
        }

        if (meta != null) {
            try {
                meta.javaClass.getMethod("setBaseColor", DyeColor::class.java).invoke(meta, DyeColor.WHITE)
            } catch (_: ReflectiveOperationException) {
                // The API version in use may not expose shield banner colors.
            }
            item.itemMeta = meta
        }
        return item
    }

    private fun shouldGlint(lore: List<String>?): Boolean {
        if (lore.isNullOrEmpty()) {
            return false
        }
        return lore.any { line ->
            if (line.isBlank()) {
                return@any false
            }
            val stripped = ChatColor.stripColor(line)
            if (stripped.isNullOrBlank()) {
                return@any false
            }
            stripped.uppercase(Locale.ROOT).contains("MAXED")
        }
    }

    private fun formatName(name: String?): String {
        if (name.isNullOrBlank()) {
            return SheepMergeManager.color("&fMenu Item")
        }
        if (name.startsWith("&")) {
            return SheepMergeManager.color(name)
        }
        return SheepMergeManager.color("&e$name")
    }

    private fun formatLore(lore: List<String>?): List<String> {
        if (lore.isNullOrEmpty()) {
            return listOf(SheepMergeManager.color("&7No details."))
        }
        return lore
            .map { normalizeLoreLine(it) }
            .map { colorLoreLine(it) }
    }

    private fun normalizeLoreLine(line: String?): String {
        if (line == null) {
            return ""
        }
        val trimmed = line.trim()
        return when {
            trimmed.equals("Click to purchase", ignoreCase = true) -> "Click: Buy"
            trimmed.equals("Click to open", ignoreCase = true) -> "Click: Open"
            trimmed.equals("Click to upgrade", ignoreCase = true) -> "Click: Upgrade"
            trimmed.equals("Click to unlock", ignoreCase = true) -> "Click: Unlock"
            trimmed.equals("Click to activate", ignoreCase = true) -> "Click: Activate"
            trimmed.equals("Click to toggle enable/disable", ignoreCase = true) -> "Click: Toggle"
            trimmed.equals("Click to toggle", ignoreCase = true) -> "Click: Toggle"
            trimmed.equals("Click to go back", ignoreCase = true) -> "Click: Back"
            trimmed.equals("Click to refund", ignoreCase = true) -> "Click: Refund"
            trimmed.equals("Click to prestige multiple", ignoreCase = true) -> "Click: Prestige"
            else -> trimmed
        }
    }

    private fun colorLoreLine(line: String?): String {
        if (line.isNullOrBlank()) {
            return SheepMergeManager.color("&7-")
        }
        if (line.startsWith("&")) {
            return SheepMergeManager.color(line)
        }
        val lower = line.lowercase(Locale.ROOT)
        return when {
            lower.startsWith("click:") -> SheepMergeManager.color("&a$line")
            lower.startsWith("cost:") || lower.startsWith("next unlock cost:") -> SheepMergeManager.color("&6$line")
            lower.startsWith("level:") || lower.startsWith("status:") ||
                lower.startsWith("current:") || lower.startsWith("next:") ||
                lower.startsWith("chance:") || lower.startsWith("duration:") ||
                lower.startsWith("uses per activation:") || lower.startsWith("unlocks bought:") ||
                lower.startsWith("in stock:") -> SheepMergeManager.color("&b$line")
            lower.startsWith("maxed") || lower.contains("maxed") ||
                lower.contains("unlocked") || lower.startsWith("done ") -> SheepMergeManager.color("&a$line")
            lower.contains("locked") || lower.startsWith("todo ") -> SheepMergeManager.color("&c$line")
            else -> SheepMergeManager.color("&7$line")
        }
    }
}
