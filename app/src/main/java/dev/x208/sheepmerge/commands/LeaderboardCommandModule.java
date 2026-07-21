package dev.x208.sheepmerge.commands;

public final class LeaderboardCommandModule extends BaseRootCommandModule {

    public LeaderboardCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("leaderboard", executor, tabCompleter);
    }
}
