package com.tutienda.storeannouncer;

import com.tutienda.storeannouncer.announcer.PurchaseAnnouncer;
import com.tutienda.storeannouncer.announcer.SkinHeadRenderer;
import com.tutienda.storeannouncer.command.StoreCommand;
import com.tutienda.storeannouncer.config.Settings;
import com.tutienda.storeannouncer.gg.GGWaveManager;
import com.tutienda.storeannouncer.listener.ChatListener;
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
        this.announcer = new PurchaseAnnouncer(this, settings, waveManager, new SkinHeadRenderer());

        getServer().getPluginManager().registerEvents(new ChatListener(this, settings, waveManager), this);

        PluginCommand command = getCommand("tienda");
        if (command != null) {
            StoreCommand executor = new StoreCommand(this, settings, waveManager, announcer);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getLogger().info("StoreAnnouncer activado (modo Tebex por comandos).");
    }

    @Override
    public void onDisable() {
        if (waveManager != null) {
            waveManager.stop(false);
        }
    }

    public void reloadAll() {
        settings.reload();
        getLogger().info("Configuración recargada.");
    }
}
