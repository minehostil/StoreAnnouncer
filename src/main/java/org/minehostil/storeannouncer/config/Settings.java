package org.minehostil.storeannouncer.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Settings {

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private boolean showHead;
    private String announceSound;
    private String storeLink;
    private String headerFormat;
    private String productFormat;
    private String priceFormat;
    private String storeLine;

    private boolean waveAutoStart;
    private int waveDuration;
    private String waveStartSound;
    private String waveStartMessage;
    private String waveEndMessage;
    private List<String> rewardCommands;
    private int rewardMaxPerPlayer;
    private int rewardCooldownSeconds;
    private boolean firstBonusEnabled;
    private List<String> firstBonusCommands;
    private String rewardSound;
    private String rewardMessage;
    private boolean rewardModeContains;
    private String chatFormat;
    private List<String> colors;

    private String msgUsage;
    private String msgUsageAdvertise;
    private String msgReloaded;
    private String msgAnnounced;
    private String msgWaveStopped;
    private String msgHeadHover;

    public Settings(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        showHead = c.getBoolean("announcer.head.enabled", true);
        announceSound = c.getString("announcer.sound", "entity.player.levelup");
        storeLink = c.getString("announcer.store-link", "");
        headerFormat = c.getString("announcer.header",
                "<dark_gray>«</dark_gray> <bold><white>%player%</white></bold> <dark_gray>▶</dark_gray> <green><bold>has purchased in our store</bold></green>");
        productFormat = c.getString("announcer.product",
                "<dark_gray>•</dark_gray> <aqua><bold>%product%</bold></aqua> <dark_gray>x%quantity%</dark_gray>");
        priceFormat = c.getString("announcer.price",
                "<dark_gray>•</dark_gray> <gold>$%price%</gold>");
        storeLine = c.getString("announcer.store-line",
                "<click:open_url:'%link%'><gradient:#00FFA3:#F5D90A><bold>➥ Visit our store!</bold></gradient></click>");

        waveAutoStart = c.getBoolean("ggwave.auto-start", true);
        waveDuration = c.getInt("ggwave.duration-seconds", 30);
        waveStartSound = c.getString("ggwave.start-sound", "entity.ender_dragon.growl");
        waveStartMessage = c.getString("ggwave.start-message", "");
        waveEndMessage = c.getString("ggwave.end-message", "");
        rewardModeContains = "CONTAINS".equalsIgnoreCase(c.getString("ggwave.reward.mode", "EXACT"))
                || "CONTIENE".equalsIgnoreCase(c.getString("ggwave.reward.mode", "EXACT"));
        rewardCommands = c.getStringList("ggwave.reward.commands");
        rewardMaxPerPlayer = Math.max(1, c.getInt("ggwave.reward.max-per-player", 3));
        rewardCooldownSeconds = Math.max(0, c.getInt("ggwave.reward.cooldown-seconds", 5));
        firstBonusEnabled = c.getBoolean("ggwave.reward.first-time-bonus.enabled", true);
        firstBonusCommands = c.getStringList("ggwave.reward.first-time-bonus.commands");
        rewardSound = c.getString("ggwave.reward.sound", "entity.experience_orb.pickup");
        rewardMessage = c.getString("ggwave.reward.message", "");

        chatFormat = c.getString("ggwave.chat-format",
                "{color}<bold>%name%</bold> <dark_gray>»</dark_gray> <white>%message%</white>");
        if (!chatFormat.contains("%message%")) {
            plugin.getLogger().warning("ggwave.chat-format does not contain %message%, using default format.");
            chatFormat = "{color}<bold>%name%</bold> <dark_gray>»</dark_gray> <white>%message%</white>";
        }

        colors = c.getStringList("ggwave.colors");
        if (colors.isEmpty()) {
            colors = List.of("#FF5555", "#FFAA00", "#FFFF55", "#55FF55", "#55FFFF", "#5555FF", "#FF55FF");
        }

        msgUsage = c.getString("messages.usage",
                "<red>Usage:</red> <gray>/store <reload | advertise | ggwave></gray>");
        msgUsageAdvertise = c.getString("messages.usage-advertise",
                "<red>Usage:</red> <gray>/store advertise <player> <product...> [quantity] [price]</gray>");
        msgReloaded = c.getString("messages.reloaded",
                "<green>StoreAnnouncer configuration reloaded.</green>");
        msgAnnounced = c.getString("messages.announced",
                "<green>Announcement sent.</green>");
        msgWaveStopped = c.getString("messages.wave-stopped",
                "<green>GG Wave stopped.</green>");
        msgHeadHover = c.getString("messages.head-hover",
                "<gradient:#FFD700:#FF8C00><bold>Purchase made by %player%!</bold></gradient>");
    }

    /** Convierte un texto MiniMessage a Component. */
    public Component parse(String text) {
        return miniMessage.deserialize(text);
    }

    public boolean isShowHead() { return showHead; }
    public String getAnnounceSound() { return announceSound; }
    public String getStoreLink() { return storeLink; }
    public String getHeader() { return headerFormat; }
    public String getProduct() { return productFormat; }
    public String getPriceFormat() { return priceFormat; }
    public String getStoreLine() { return storeLine; }
    public boolean isWaveAutoStart() { return waveAutoStart; }
    public int getWaveDuration() { return waveDuration; }
    public String getWaveStartSound() { return waveStartSound; }
    public String getWaveStartMessage() { return waveStartMessage; }
    public String getWaveEndMessage() { return waveEndMessage; }
    public boolean isRewardModeContains() { return rewardModeContains; }
    public List<String> getRewardCommands() { return rewardCommands; }
    public int getRewardMaxPerPlayer() { return rewardMaxPerPlayer; }
    public int getRewardCooldownSeconds() { return rewardCooldownSeconds; }
    public boolean isFirstBonusEnabled() { return firstBonusEnabled; }
    public List<String> getFirstBonusCommands() { return firstBonusCommands; }
    public String getRewardSound() { return rewardSound; }
    public String getRewardMessage() { return rewardMessage; }
    public String getChatFormat() { return chatFormat; }
    public List<String> getColors() { return colors; }
    public String getMsgUsage() { return msgUsage; }
    public String getMsgUsageAdvertise() { return msgUsageAdvertise; }
    public String getMsgReloaded() { return msgReloaded; }
    public String getMsgAnnounced() { return msgAnnounced; }
    public String getMsgWaveStopped() { return msgWaveStopped; }
    public String getMsgHeadHover() { return msgHeadHover; }
}