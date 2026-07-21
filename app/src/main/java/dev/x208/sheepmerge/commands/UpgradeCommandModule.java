package dev.x208.sheepmerge.commands;

public final class UpgradeCommandModule extends BaseRootCommandModule {

    public UpgradeCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("upgrade", executor, tabCompleter);
    }
}
