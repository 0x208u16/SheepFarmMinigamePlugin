package dev.thehale.papermc_plugin_template.commands;

public final class WorldCommandModule extends BaseRootCommandModule {

    public WorldCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("world", executor, tabCompleter);
    }
}
