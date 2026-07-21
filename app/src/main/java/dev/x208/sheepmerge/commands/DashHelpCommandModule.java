package dev.x208.sheepmerge.commands;

public final class DashHelpCommandModule extends BaseRootCommandModule {

    public DashHelpCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("-help", executor, tabCompleter);
    }
}
