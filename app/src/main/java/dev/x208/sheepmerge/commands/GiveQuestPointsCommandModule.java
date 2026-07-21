package dev.x208.sheepmerge.commands;

public final class GiveQuestPointsCommandModule extends BaseRootCommandModule {

    public GiveQuestPointsCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("givequestpoints", executor, tabCompleter);
    }
}
