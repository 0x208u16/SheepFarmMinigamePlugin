package dev.thehale.papermc_plugin_template.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BaseRootCommandModule implements SheepMergeCommandModule {

    @FunctionalInterface
    public interface RootCommandExecutor {
        boolean execute(Player player, String[] args);
    }

    @FunctionalInterface
    public interface RootTabCompleter {
        List<String> complete(CommandSender sender, String[] args);
    }

    private final String root;
    private final RootCommandExecutor executor;
    private final RootTabCompleter tabCompleter;

    protected BaseRootCommandModule(String root, RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        this.root = root;
        this.executor = executor;
        this.tabCompleter = tabCompleter;
    }

    @Override
    public String root() {
        return root;
    }

    @Override
    public boolean execute(Player player, String[] args) {
        return executor.execute(player, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return tabCompleter.complete(sender, args);
    }
}
