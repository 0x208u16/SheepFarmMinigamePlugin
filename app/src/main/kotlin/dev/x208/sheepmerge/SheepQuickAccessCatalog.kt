package dev.x208.sheepmerge

import org.bukkit.Material

internal data class SheepQuickAccessDefinition(
    val id: String,
    val material: Material,
    val name: String,
    val description: String
)

internal object SheepQuickAccessCatalog {

    private val definitions = listOf(
        SheepQuickAccessDefinition("menu_quest", Material.WRITABLE_BOOK, "Open Quest Abilities", "Open quest abilities menu"),
        SheepQuickAccessDefinition("menu_automation", Material.COMPARATOR, "Open Automation", "Open automation menu"),
        SheepQuickAccessDefinition("menu_socials", Material.PLAYER_HEAD, "Open Socials", "Open socials menu"),
        SheepQuickAccessDefinition("menu_scoreboard", Material.MAP, "Open Scoreboard Settings", "Open scoreboard settings"),
        SheepQuickAccessDefinition("upgrade_limit", Material.OAK_FENCE, "Buy Sheep Limit", "Buy sheep limit upgrade"),
        SheepQuickAccessDefinition("upgrade_egg_speed", Material.CLOCK, "Buy Egg Speed", "Buy faster egg spawn"),
        SheepQuickAccessDefinition("upgrade_wool", Material.WHITE_WOOL, "Buy Wool Regen", "Buy faster wool regen"),
        SheepQuickAccessDefinition("upgrade_tier_chance", Material.GOLDEN_APPLE, "Buy Tier Chance", "Buy higher tier spawn chance"),
        SheepQuickAccessDefinition("quest_lucky_burst", Material.ENDER_EYE, "Cast Lucky Burst", "Buy/toggle Lucky Burst"),
        SheepQuickAccessDefinition("quest_merge_assist", Material.ANVIL, "Cast Merge Assist", "Buy/toggle Merge Assist"),
        SheepQuickAccessDefinition("quest_shear_all", Material.FLINT, "Cast Shear All Sheep", "Buy/toggle Shear All Sheep"),
        SheepQuickAccessDefinition("automation_toggle_auto_buy", Material.HOPPER, "Toggle Auto Buy", "Toggle automation: Auto Buy"),
        SheepQuickAccessDefinition("automation_toggle_auto_ability", Material.REDSTONE_TORCH, "Toggle Auto Ability", "Toggle automation: Auto Ability"),
        SheepQuickAccessDefinition("automation_toggle_auto_merge", Material.PISTON, "Toggle Auto Merge", "Toggle automation: Auto Merge"),
        SheepQuickAccessDefinition("automation_toggle_auto_spawn", Material.FIREWORK_STAR, "Toggle Auto Spawn", "Toggle automation: Auto Spawn"),
        SheepQuickAccessDefinition("automation_toggle_auto_prestige", Material.BEACON, "Toggle Auto Prestige", "Toggle automation: Auto Prestige"),
        SheepQuickAccessDefinition("automation_enable_all", Material.LIME_DYE, "Enable All Automation", "Turn all automation toggles on"),
        SheepQuickAccessDefinition("automation_disable_all", Material.RED_DYE, "Disable All Automation", "Turn all automation toggles off")
    )

    private val definitionsById = definitions.associateBy(SheepQuickAccessDefinition::id)

    @JvmStatic
    fun all(): List<SheepQuickAccessDefinition> = definitions

    @JvmStatic
    fun find(id: String?): SheepQuickAccessDefinition? {
        if (id.isNullOrBlank()) {
            return null
        }
        return definitionsById[id]
    }
}