package dev.x208.sheepmerge.commands;

public final class TopCommandModule extends BaseRootCommandModule {

    public TopCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("top", executor, tabCompleter);
    }
}
