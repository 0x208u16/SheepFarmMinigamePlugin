package dev._0x208u16.sheepmerge.commands;

public final class KickCommandModule extends BaseRootCommandModule {

    public KickCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("kick", executor, tabCompleter);
    }
}
