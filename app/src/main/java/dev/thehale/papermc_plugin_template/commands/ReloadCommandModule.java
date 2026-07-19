package dev.thehale.papermc_plugin_template.commands;

public final class ReloadCommandModule extends BaseRootCommandModule {

    public ReloadCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("reload", executor, tabCompleter);
    }
}
