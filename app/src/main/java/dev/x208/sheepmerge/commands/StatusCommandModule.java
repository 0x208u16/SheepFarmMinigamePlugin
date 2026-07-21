package dev.x208.sheepmerge.commands;

public final class StatusCommandModule extends BaseRootCommandModule {

    public StatusCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("status", executor, tabCompleter);
    }
}
