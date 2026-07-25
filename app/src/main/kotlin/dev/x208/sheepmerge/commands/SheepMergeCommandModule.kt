package dev.x208.sheepmerge.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

interface SheepMergeCommandModule {
    fun root(): String

    fun execute(player: Player, args: Array<String>): Boolean

    fun tabComplete(sender: CommandSender, args: Array<String>): List<String>
}
