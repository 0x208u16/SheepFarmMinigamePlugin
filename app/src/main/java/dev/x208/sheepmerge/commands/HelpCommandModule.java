package dev.x208.sheepmerge.commands;

public final class HelpCommandModule extends BaseRootCommandModule {

    public HelpCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("help", executor, tabCompleter);
    }
}
