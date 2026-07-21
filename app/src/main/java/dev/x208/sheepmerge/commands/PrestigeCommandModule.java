package dev.x208.sheepmerge.commands;

public final class PrestigeCommandModule extends BaseRootCommandModule {

    public PrestigeCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("prestige", executor, tabCompleter);
    }
}
