package dev.thehale.papermc_plugin_template.commands;

public final class KickCommandModule extends BaseRootCommandModule {

    public KickCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("kick", executor, tabCompleter);
    }
}
