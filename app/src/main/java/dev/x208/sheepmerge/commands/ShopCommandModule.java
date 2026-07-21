package dev.x208.sheepmerge.commands;

public final class ShopCommandModule extends BaseRootCommandModule {

    public ShopCommandModule(RootCommandExecutor executor, RootTabCompleter tabCompleter) {
        super("shop", executor, tabCompleter);
    }
}
