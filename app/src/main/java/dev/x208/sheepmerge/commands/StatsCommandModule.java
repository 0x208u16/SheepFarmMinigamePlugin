package dev.x208.sheepmerge.commands;

public final class StatsCommandModule extends BaseRootCommandModule {

    public StatsCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("stats", executor, tabCompleter);
    }
}
