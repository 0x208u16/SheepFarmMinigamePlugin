package dev._0x208u16.sheepmerge.commands;

public final class CheckpointsCommandModule extends BaseRootCommandModule {

    public CheckpointsCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("checkpoints", executor, tabCompleter);
    }
}
