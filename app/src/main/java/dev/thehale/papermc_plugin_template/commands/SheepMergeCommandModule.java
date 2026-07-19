package dev.thehale.papermc_plugin_template.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface SheepMergeCommandModule {
    String root();

    boolean execute(Player player, String[] args);

    List<String> tabComplete(CommandSender sender, String[] args);
}
