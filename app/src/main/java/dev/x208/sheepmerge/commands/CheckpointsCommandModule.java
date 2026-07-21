package dev.x208.sheepmerge.commands;

public final class CheckpointsCommandModule extends BaseRootCommandModule {

    public CheckpointsCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("checkpoints", executor, tabCompleter);
    }
}
