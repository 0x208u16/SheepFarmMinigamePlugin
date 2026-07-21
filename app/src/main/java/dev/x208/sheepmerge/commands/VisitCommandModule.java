package dev.x208.sheepmerge.commands;

public final class VisitCommandModule extends BaseRootCommandModule {

    public VisitCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("visit", executor, tabCompleter);
    }
}
