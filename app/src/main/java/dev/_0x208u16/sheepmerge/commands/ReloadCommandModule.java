package dev._0x208u16.sheepmerge.commands;

public final class ReloadCommandModule extends BaseRootCommandModule {

    public ReloadCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("reload", executor, tabCompleter);
    }
}
