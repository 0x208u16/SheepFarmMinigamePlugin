package dev.x208.sheepmerge.commands;

public final class GivePointsCommandModule extends BaseRootCommandModule {

    public GivePointsCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("givepoints", executor, tabCompleter);
    }
}
