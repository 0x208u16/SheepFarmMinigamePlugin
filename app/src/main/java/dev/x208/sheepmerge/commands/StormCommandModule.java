package dev.x208.sheepmerge.commands;

public final class StormCommandModule extends BaseRootCommandModule {

    public StormCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("storm", executor, tabCompleter);
    }
}
