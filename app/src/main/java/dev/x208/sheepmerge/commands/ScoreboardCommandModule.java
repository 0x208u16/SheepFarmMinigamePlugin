package dev.x208.sheepmerge.commands;

public final class ScoreboardCommandModule extends BaseRootCommandModule {

    public ScoreboardCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("scoreboard", executor, tabCompleter);
    }
}
