package dev._0x208u16.sheepmerge.commands;

public final class UpgradeCommandModule extends BaseRootCommandModule {

    public UpgradeCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("upgrade", executor, tabCompleter);
    }
}
