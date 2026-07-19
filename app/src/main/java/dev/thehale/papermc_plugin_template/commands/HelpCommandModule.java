package dev.thehale.papermc_plugin_template.commands;

public final class HelpCommandModule extends BaseRootCommandModule {

    public HelpCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("help", executor, tabCompleter);
    }
}
