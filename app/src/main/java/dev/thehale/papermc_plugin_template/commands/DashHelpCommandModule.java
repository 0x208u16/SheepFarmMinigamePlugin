package dev.thehale.papermc_plugin_template.commands;

public final class DashHelpCommandModule extends BaseRootCommandModule {

    public DashHelpCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("-help", executor, tabCompleter);
    }
}
