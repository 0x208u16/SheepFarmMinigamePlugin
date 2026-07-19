package dev.thehale.papermc_plugin_template.commands;

public final class CheckpointsCommandModule extends BaseRootCommandModule {

    public CheckpointsCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("checkpoints", executor, tabCompleter);
    }
}
