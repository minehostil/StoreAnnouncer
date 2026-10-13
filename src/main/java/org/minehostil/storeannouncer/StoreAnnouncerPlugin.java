package org.minehostil.storeannouncer;

import org.minehostil.storeannouncer.announcer.PurchaseAnnouncer;
import org.minehostil.storeannouncer.announcer.SkinHeadRenderer;
import org.minehostil.storeannouncer.command.StoreCommand;
import org.minehostil.storeannouncer.config.Settings;
import org.minehostil.storeannouncer.gg.GGWaveManager;
import org.minehostil.storeannouncer.listener.ChatListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class StoreAnnouncerPlugin extends JavaPlugin {

    private Settings settings;
    private GGWaveManager waveManager;
    private PurchaseAnnouncer announcer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.settings = new Settings(this);
        this.waveManager = new GGWaveManager(this, settings);
        this.announcer = new PurchaseAnnouncer(this, settings, waveManager, new SkinHeadRenderer(settings));

        getServer().getPluginManager().registerEvents(new ChatListener(this, settings, waveManager), this);

        PluginCommand command = getCommand("store");
        if (command != null) {
            StoreCommand executor = new StoreCommand(this, settings, waveManager, announcer);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("StoreAnnouncer enabled (Tebex command mode).");
    }

    @Override
    public void onDisable() {
        if (waveManager != null) {
            waveManager.stop(false);
        }
    }

    public void reloadAll() {
        settings.reload();
        getLogger().info("StoreAnnouncer configuration reloaded.");
    }
}