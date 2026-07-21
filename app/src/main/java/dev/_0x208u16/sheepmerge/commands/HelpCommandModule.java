package dev._0x208u16.sheepmerge.commands;

public final class HelpCommandModule extends BaseRootCommandModule {

    public HelpCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("help", executor, tabCompleter);
    }
}
