// Copyright (c) 2023 Joseph Hale
// 
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package dev.thehale.papermc_plugin_template;

import java.io.File;
import java.util.logging.Logger;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import dev.thehale.papermc_plugin_template.bstats.Metrics;

public class SheepMergePlugin extends JavaPlugin {

    public static SheepMergePlugin instance;
    public static Logger log;
    public final static String NAME = "PapermcPluginTemplate";
    public final static int BSTATS_PLUGIN_ID = 20765; // Optional: Replace with your own bStats plugin ID

    /**
     * Default constructor.
     * 
     * <p>
     * Used solely by MockBukkit during unit tests.
     */
    public SheepMergePlugin() {
        super();
    }

    /**
     * Parameterized constructor.
     * 
     * <p>
     * Used solely by MockBukkit during unit tests.
     */
    protected SheepMergePlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder,
            File file) {
        super(loader, description, dataFolder, file);
    }

    @Override
    public void onEnable() {
        instance = this;
        log = getLogger();
        setup();
        log.info("Ready!");
    }

    private void setup() {
        getServer().getPluginManager().registerEvents(new PapermcPluginTemplateListener(), this);
        getServer().getPluginManager().registerEvents(new SheepFarmWorldProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new SheepFarmWorldCleanupListener(), this);
        getCommand("ping").setExecutor(new PingCommand());
        getCommand("sheepmerge").setExecutor(new SheepFarmWorldCommand());
        new Metrics(this, BSTATS_PLUGIN_ID); // Enable bStats metrics
    }

    @Override
    public void onDisable() {
        log.info("Thanks for using " + NAME + "!");
    }
}