package dev.x208.sheepmerge.commands;

public final class BackupCommandModule extends BaseRootCommandModule {

    public BackupCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("backup", executor, tabCompleter);
    }
}
