package dev.thehale.papermc_plugin_template.commands;

public final class StatusCommandModule extends BaseRootCommandModule {

    public StatusCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("status", executor, tabCompleter);
    }
}
