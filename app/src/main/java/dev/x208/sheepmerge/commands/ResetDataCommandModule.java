package dev.x208.sheepmerge.commands;

public final class ResetDataCommandModule extends BaseRootCommandModule {

    public ResetDataCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("resetdata", executor, tabCompleter);
    }
}
