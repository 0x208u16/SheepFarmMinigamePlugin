package dev.x208.sheepmerge.commands;

public final class SetPointsCommandModule extends BaseRootCommandModule {

    public SetPointsCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("setpoints", executor, tabCompleter);
    }
}
