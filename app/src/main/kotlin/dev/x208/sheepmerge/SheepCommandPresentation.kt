package dev.x208.sheepmerge

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.math.BigInteger

internal object SheepCommandPresentation {
    @JvmStatic
    fun sendDetailedStats(sender: Player, target: Player, title: String) {
        sender.sendMessage(
            adminHeader(title) +
                " " + label("Player") + ": " + value(target.name) +
                ChatColor.DARK_GRAY + "  " + ChatColor.GRAY + "Hover each stat for details."
        )

        sender.spigot().sendMessage(
            *composeStatLine(
                statChip(
                    "Coins",
                    SheepMergeManager.formatPoints(SheepMergeManager.getPlayerPoints(target)),
                    "Coins",
                    listOf(
                        "Current coin balance",
                        "Value: " + SheepMergeManager.formatPoints(SheepMergeManager.getPlayerPoints(target))
                    )
                ),
                statChip(
                    "Quest",
                    SheepMergeManager.formatPoints(SheepMergeManager.getQuestPoints(target).toLong()),
                    "Quest Points",
                    listOf(
                        "Currency for ability activations",
                        "Current: " + SheepMergeManager.formatPoints(SheepMergeManager.getQuestPoints(target).toLong()),
                        "Quest duration lv: " + SheepMergeManager.getQuestUpgradeDurationLevel(target),
                        "Quest power lv: " + SheepMergeManager.getQuestUpgradePowerLevel(target)
                    )
                ),
                statChip(
                    "Auto",
                    SheepMergeManager.formatPoints(SheepMergeManager.getAutomationPoints(target).toLong()),
                    "Automation Points",
                    listOf(
                        "Currency for automation upgrades",
                        "Current: " +
                            SheepMergeManager.formatPoints(SheepMergeManager.getAutomationPoints(target).toLong()),
                        "Auto Buy lv: " + SheepMergeManager.getAutomationAutoBuyUpgradeLevel(target),
                        "Auto Ability lv: " + SheepMergeManager.getAutomationAutoAbilityUpgradeLevel(target),
                        "Auto Spawn lv: " + SheepMergeManager.getAutomationAutoSpawnUpgradeLevel(target),
                        "Auto Prestige lv: " + SheepMergeManager.getAutomationAutoPrestigeUpgradeLevel(target)
                    )
                ),
                statChip(
                    "Sac",
                    SheepMergeManager.formatPoints(SheepMergeManager.getSacrificePoints(target)),
                    "Sacrifice Points",
                    listOf(
                        "Currency from sacrificing sheep",
                        "Current: " + SheepMergeManager.formatPoints(SheepMergeManager.getSacrificePoints(target)),
                        "Unlocks bought: " + SheepMergeManager.getSacrificeUnlocksBought(target) + " / " +
                            SheepMergeManager.SACRIFICE_UNLOCK_MAX
                    )
                )
            )
        )

        sender.spigot().sendMessage(
            *composeStatLine(
                statChip(
                    "Prestige",
                    SheepMergeManager.getPrestigeLevel(target).toString(),
                    "Prestige",
                    listOf(
                        "Total prestige level",
                        "Level: " + SheepMergeManager.getPrestigeLevel(target),
                        "Prestige points: " +
                            SheepMergeManager.formatPoints(SheepMergeManager.getPrestigePoints(target).toLong()),
                        "Double Coins lv: " + SheepMergeManager.getPrestigeDoublePointsChanceLevel(target),
                        "Higher Max lv: " + SheepMergeManager.getPrestigeHigherMaxLevel(target),
                        "Start Eggs lv: " + SheepMergeManager.getPrestigeStartEggsLevel(target),
                        "Egg Cap lv: " + SheepMergeManager.getPrestigeEggCapLevel(target),
                        "Base Tier lv: " + SheepMergeManager.getBaseSpawnTierLevel(target),
                        "Quest Reward lv: " + SheepMergeManager.getPrestigeQuestRewardLevel(target)
                    )
                ),
                statChip(
                    "P.Pts",
                    SheepMergeManager.formatPoints(SheepMergeManager.getPrestigePoints(target).toLong()),
                    "Prestige Points",
                    listOf(
                        "Unspent prestige currency",
                        "Current: " +
                            SheepMergeManager.formatPoints(SheepMergeManager.getPrestigePoints(target).toLong()),
                        "Double Coins chance: " + SheepMergeManager.getDoublePointsChancePercent(target) + "%"
                    )
                ),
                statChip(
                    "Rebirth",
                    SheepMergeManager.getRebirthLevel(target).toString(),
                    "Rebirth",
                    listOf(
                        "Long-term reset progression",
                        "Level: " + SheepMergeManager.getRebirthLevel(target),
                        "Rebirth points: " +
                            SheepMergeManager.formatPoints(SheepMergeManager.getRebirthPoints(target).toLong()),
                        "Unspent: " + SheepMergeManager.formatPoints(
                            SheepMergeManager.getUnspentRebirthPointsDisplay(target).toLong()
                        ),
                        "Next cost: " + SheepMergeManager.getRebirthNextCostInPrestigeLevels(target) +
                            " prestige levels",
                        "Affordable now: " + SheepMergeManager.getAffordableRebirthLevelsDisplay(target)
                    )
                ),
                statChip(
                    "A.Pts",
                    SheepMergeManager.getAchievementPoints(target).toString(),
                    "Achievement Points",
                    listOf(
                        "Permanent milestone currency",
                        "Current: " + SheepMergeManager.getAchievementPoints(target),
                        "Used for milestone multipliers"
                    )
                )
            )
        )

        sender.spigot().sendMessage(
            *composeStatLine(
                statChip(
                    "Limit",
                    SheepMergeManager.getPlayerLimit(target).toString(),
                    "Sheep Limit",
                    listOf(
                        "Current farm capacity",
                        "Limit: " + SheepMergeManager.getPlayerLimit(target),
                        "Upgrade lv: " + SheepMergeManager.getLimitUpgradeLevel(target)
                    )
                ),
                statChip(
                    "Egg",
                    SheepMergeManager.getEggIntervalSeconds(target).toString() + "s",
                    "Egg Interval",
                    listOf(
                        "Auto egg spawn interval",
                        "Interval: " + SheepMergeManager.getEggIntervalSeconds(target) + " seconds",
                        "Egg Speed lv: " + SheepMergeManager.getEggSpeedLevel(target),
                        "Egg Cap: " + SheepMergeManager.getEggCap(target)
                    )
                ),
                statChip(
                    "Wool",
                    "Lv " + SheepMergeManager.getWoolRegenLevel(target),
                    "Wool Regen",
                    listOf(
                        "Passive wool recovery upgrade",
                        "Level: " + SheepMergeManager.getWoolRegenLevel(target),
                        "Current max level: " + SheepMergeManager.getWoolRegenMaxLevel(target)
                    )
                ),
                statChip(
                    "Tier",
                    SheepMergeManager.getHigherTierChancePercent(target).toString() + "%",
                    "Higher-Tier Chance",
                    listOf(
                        "Chance to spawn higher-tier sheep",
                        "Chance: " + SheepMergeManager.getHigherTierChancePercent(target) + "%",
                        "Upgrade lv: " + SheepMergeManager.getHigherTierChanceLevel(target)
                    )
                )
            )
        )

        sender.spigot().sendMessage(
            *composeStatLine(
                statChip(
                    "Shears",
                    "Lv " + SheepMergeManager.getShearShopLevel(target),
                    "Shear Shop",
                    listOf(
                        "Main shearing value upgrade",
                        "Level: " + SheepMergeManager.getShearShopLevel(target),
                        "Wool Keeper lv: " + SheepMergeManager.getShearWoolSaveLevel(target),
                        "Wool Keeper chance: " + SheepMergeManager.getShearWoolSaveChancePercent(target) + "%",
                        "Tier Booster lv: " + SheepMergeManager.getShearTierBoostLevel(target),
                        "Tier Booster chance: " + SheepMergeManager.getShearTierBoostChancePercent(target) + "%"
                    )
                ),
                statChip(
                    "Combo",
                    "D" + SheepMergeManager.getComboDecayUpgradeLevel(target) +
                        " G" + SheepMergeManager.getComboGainUpgradeLevel(target) +
                        " M" + SheepMergeManager.getComboMaxUpgradeLevel(target),
                    "Combo Upgrades",
                    listOf(
                        "Decay / Gain / Max upgrade levels",
                        "Decay lv: " + SheepMergeManager.getComboDecayUpgradeLevel(target),
                        "Gain lv: " + SheepMergeManager.getComboGainUpgradeLevel(target),
                        "Max lv: " + SheepMergeManager.getComboMaxUpgradeLevel(target)
                    )
                ),
                statChip(
                    "QuestUp",
                    "D" + SheepMergeManager.getQuestUpgradeDurationLevel(target) +
                        " P" + SheepMergeManager.getQuestUpgradePowerLevel(target),
                    "Quest Upgrades",
                    listOf(
                        "Quest duration and power upgrades",
                        "Duration lv: " + SheepMergeManager.getQuestUpgradeDurationLevel(target),
                        "Power lv: " + SheepMergeManager.getQuestUpgradePowerLevel(target)
                    )
                ),
                statChip(
                    "Visit",
                    if (SheepMergeManager.isFarmVisitable(target.uniqueId)) "Open" else "Closed",
                    "Farm Visit Access",
                    listOf(
                        "Whether other players can visit this farm",
                        "Current: " + if (SheepMergeManager.isFarmVisitable(target.uniqueId)) "Open" else "Closed"
                    )
                )
            )
        )

        sender.spigot().sendMessage(
            *composeStatLine(
                statChip(
                    "AutoBuy",
                    onOffShort(SheepMergeManager.isAutomationAutoBuyEnabled(target)),
                    "Automation Toggle: Auto Buy",
                    listOf("Status: " + onOffLong(SheepMergeManager.isAutomationAutoBuyEnabled(target)))
                ),
                statChip(
                    "Ability",
                    onOffShort(SheepMergeManager.isAutomationAutoAbilityEnabled(target)),
                    "Automation Toggle: Auto Ability",
                    listOf("Status: " + onOffLong(SheepMergeManager.isAutomationAutoAbilityEnabled(target)))
                ),
                statChip(
                    "Merge",
                    onOffShort(SheepMergeManager.isAutomationSlowAutoMergeEnabled(target)),
                    "Automation Toggle: Slow Merge",
                    listOf("Status: " + onOffLong(SheepMergeManager.isAutomationSlowAutoMergeEnabled(target)))
                ),
                statChip(
                    "Shear",
                    onOffShort(SheepMergeManager.isAutomationSlowAutoShearEnabled(target)),
                    "Automation Toggle: Slow Shear",
                    listOf("Status: " + onOffLong(SheepMergeManager.isAutomationSlowAutoShearEnabled(target)))
                ),
                statChip(
                    "Spawn",
                    onOffShort(SheepMergeManager.isAutomationAutoSpawnEnabled(target)),
                    "Automation Toggle: Auto Spawn",
                    listOf("Status: " + onOffLong(SheepMergeManager.isAutomationAutoSpawnEnabled(target)))
                ),
                statChip(
                    "Prestg",
                    onOffShort(SheepMergeManager.isAutomationAutoPrestigeEnabled(target)),
                    "Automation Toggle: Auto Prestige",
                    listOf("Status: " + onOffLong(SheepMergeManager.isAutomationAutoPrestigeEnabled(target)))
                )
            )
        )
    }

    @JvmStatic
    fun composeStatLine(vararg chips: TextComponent): Array<BaseComponent> {
        val builder = ComponentBuilder()
        chips.forEachIndexed { index, chip ->
            if (index > 0) {
                builder.append("  ").color(net.md_5.bungee.api.ChatColor.DARK_GRAY)
            }
            builder.append(chip)
        }
        return builder.create()
    }

    @JvmStatic
    fun statChip(shortLabel: String, value: String, hoverTitle: String, hoverLines: List<String>): TextComponent {
        val chip = TextComponent("$shortLabel: $value")
        chip.color = net.md_5.bungee.api.ChatColor.AQUA
        chip.isBold = true

        val hover = ComponentBuilder(hoverTitle)
            .color(net.md_5.bungee.api.ChatColor.GOLD)
            .bold(true)
        hoverLines.forEach { line ->
            hover.append("\n$line").color(net.md_5.bungee.api.ChatColor.GRAY).bold(false)
        }
        chip.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, hover.create())
        return chip
    }

    @JvmStatic
    fun onOffShort(enabled: Boolean): String = if (enabled) "ON" else "OFF"

    @JvmStatic
    fun onOffLong(enabled: Boolean): String = if (enabled) "Enabled" else "Disabled"

    @JvmStatic
    fun statUpdateMessage(
        importance: String,
        target: Player,
        statLabel: String,
        fromValue: Long,
        toValue: Long
    ): String {
        val delta = toValue - fromValue
        val deltaColor = if (delta >= 0) ChatColor.GREEN else ChatColor.RED
        val formattedFrom = if (shouldFormatPointStat(statLabel)) {
            SheepMergeManager.formatPoints(fromValue)
        } else {
            fromValue.toString()
        }
        val formattedTo = if (shouldFormatPointStat(statLabel)) {
            SheepMergeManager.formatPoints(toValue)
        } else {
            toValue.toString()
        }
        val signedDelta = (if (delta >= 0) "+" else "") + if (shouldFormatPointStat(statLabel)) {
            SheepMergeManager.formatPoints(java.lang.Math.abs(delta))
        } else {
            java.lang.Math.abs(delta).toString()
        }
        return adminHeader(importance) +
            " " + label("Player") + ": " + value(target.name) +
            ChatColor.DARK_GRAY + " | " +
            label("Stat") + ": " + value(statLabel) +
            ChatColor.DARK_GRAY + " | " +
            label("From") + ": " + value(formattedFrom) +
            ChatColor.DARK_GRAY + " -> " +
            label("To") + ": " + value(formattedTo) +
            ChatColor.DARK_GRAY + " | " +
            label("Change") + ": " + deltaColor + signedDelta
    }

    @JvmStatic
    fun bigIntegerStatUpdateMessage(
        importance: String,
        target: Player,
        statLabel: String,
        fromValue: BigInteger?,
        toValue: BigInteger?
    ): String {
        val safeFrom = fromValue ?: BigInteger.ZERO
        val safeTo = toValue ?: BigInteger.ZERO
        val delta = safeTo.subtract(safeFrom)
        val deltaColor = if (delta.signum() >= 0) ChatColor.GREEN else ChatColor.RED
        val signedDelta = (if (delta.signum() >= 0) "+" else "") + SheepMergeManager.formatPoints(delta.abs())
        return adminHeader(importance) +
            " " + label("Player") + ": " + value(target.name) +
            ChatColor.DARK_GRAY + " | " +
            label("Stat") + ": " + value(statLabel) +
            ChatColor.DARK_GRAY + " | " +
            label("From") + ": " + value(SheepMergeManager.formatPoints(safeFrom)) +
            ChatColor.DARK_GRAY + " -> " +
            label("To") + ": " + value(SheepMergeManager.formatPoints(safeTo)) +
            ChatColor.DARK_GRAY + " | " +
            label("Change") + ": " + deltaColor + signedDelta
    }

    @JvmStatic
    fun shouldFormatPointStat(statLabel: String): Boolean {
        return statLabel == "Coins" ||
            statLabel == "Quest Points" ||
            statLabel == "Prestige Points" ||
            statLabel == "Rebirth Points"
    }

    @JvmStatic
    fun adminHeader(text: String): String = ChatColor.DARK_AQUA.toString() + "[SheepMerge] " + ChatColor.GOLD + text

    @JvmStatic
    fun label(text: String): String = ChatColor.YELLOW.toString() + text

    @JvmStatic
    fun value(text: String): String = ChatColor.AQUA.toString() + text

    @JvmStatic
    fun error(text: String): String = ChatColor.RED.toString() + text
}