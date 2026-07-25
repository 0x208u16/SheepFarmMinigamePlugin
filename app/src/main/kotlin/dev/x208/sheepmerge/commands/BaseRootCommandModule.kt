package dev.x208.sheepmerge.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

open class BaseRootCommandModule(
    private val root: String,
    private val executor: RootCommandExecutor,
    private val tabCompleter: RootTabCompleter
) : SheepMergeCommandModule {

    fun interface RootCommandExecutor {
        fun execute(player: Player, args: Array<String>): Boolean
    }

    fun interface RootTabCompleter {
        fun complete(sender: CommandSender, args: Array<String>): List<String>
    }

    override fun root(): String {
        return root
    }

    override fun execute(player: Player, args: Array<String>): Boolean {
        return executor.execute(player, args)
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        return tabCompleter.complete(sender, args)
    }
}

typealias RootCommandExecutor = BaseRootCommandModule.RootCommandExecutor
typealias RootTabCompleter = BaseRootCommandModule.RootTabCompleter
