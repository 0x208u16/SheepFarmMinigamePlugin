package dev.x208.sheepmerge.commands;

public final class WorldCommandModule extends BaseRootCommandModule {

    public WorldCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("world", executor, tabCompleter);
    }
}
