package dev.x208.sheepmerge.commands;

public final class SummonCommandModule extends BaseRootCommandModule {

    public SummonCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("summon", executor, tabCompleter);
    }
}
