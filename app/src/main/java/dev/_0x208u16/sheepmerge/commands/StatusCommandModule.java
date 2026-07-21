package dev._0x208u16.sheepmerge.commands;

public final class StatusCommandModule extends BaseRootCommandModule {

    public StatusCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("status", executor, tabCompleter);
    }
}
