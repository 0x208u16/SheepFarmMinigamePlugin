package dev.x208.sheepmerge.commands;

public final class SetQuestPointsCommandModule extends BaseRootCommandModule {

    public SetQuestPointsCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("setquestpoints", executor, tabCompleter);
    }
}
