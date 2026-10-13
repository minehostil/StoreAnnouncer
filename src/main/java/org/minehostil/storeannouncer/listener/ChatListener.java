package org.minehostil.storeannouncer.listener;

import org.minehostil.storeannouncer.StoreAnnouncerPlugin;
import org.minehostil.storeannouncer.config.Settings;
import org.minehostil.storeannouncer.gg.GGWaveManager;
import org.minehostil.storeannouncer.util.SoundUtil;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Locale;

public final class ChatListener implements Listener {

    private final StoreAnnouncerPlugin plugin;
    private final Settings settings;
    private final GGWaveManager waveManager;

    public ChatListener(StoreAnnouncerPlugin plugin, Settings settings, GGWaveManager waveManager) {
        this.plugin = plugin;
        this.settings = settings;
        this.waveManager = waveManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!waveManager.isActive()) {
            return;
        }

        Player player = event.getPlayer();
        GGWaveManager.WavePlayer wavePlayer = waveManager.getWavePlayer(player.getUniqueId());
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (isGG(plain)) {
            wavePlayer.incrementGg();
            if (canReward(wavePlayer)) {
                wavePlayer.setRewardsGiven(wavePlayer.getRewardsGiven() + 1);
                wavePlayer.setLastRewardMillis(System.currentTimeMillis());
                boolean firstBonus = settings.isFirstBonusEnabled() && wavePlayer.getRewardsGiven() == 1;
                int done = wavePlayer.getRewardsGiven();
                int max = settings.getRewardMaxPerPlayer();
                // Las recompensas se ejecutan en el hilo principal
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        giveReward(player, firstBonus, done, max);
                    }
                });
            }
        }

        // Formato especial para quienes ya escribieron gg: el color rota con cada "gg"
        if (wavePlayer.getGgCount() > 0) {
            List<String> colors = settings.getColors();
            String hex = colors.get((wavePlayer.getGgCount() - 1) % colors.size());
            event.renderer(ChatRenderer.viewerUnaware((source, displayName, message) ->
                    buildFormat(source.getName(), message, hex)));
        }
    }

    private boolean isGG(String message) {
        String normalized = message.toLowerCase(Locale.ROOT).replaceAll("[!¡¿? .,]", "");
        if (settings.isRewardModeContains()) {
            return normalized.contains("gg");
        }
        return normalized.equals("gg");
    }

    private boolean canReward(GGWaveManager.WavePlayer wavePlayer) {
        if (wavePlayer.getRewardsGiven() >= settings.getRewardMaxPerPlayer()) {
            return false;
        }
        long cooldownMillis = settings.getRewardCooldownSeconds() * 1000L;
        return System.currentTimeMillis() - wavePlayer.getLastRewardMillis() >= cooldownMillis;
    }

    private void giveReward(Player player, boolean firstBonus, int done, int max) {
        runCommands(player, settings.getRewardCommands());
        if (firstBonus) {
            runCommands(player, settings.getFirstBonusCommands());
        }
        SoundUtil.play(player, settings.getRewardSound());
        player.sendMessage(settings.parse(settings.getRewardMessage()
                .replace("%done%", String.valueOf(done))
                .replace("%max%", String.valueOf(max))));
    }

    private void runCommands(Player player, List<String> commands) {
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    command.replace("%player%", player.getName()));
        }
    }

    private Component buildFormat(String name, Component message, String hex) {
        String[] parts = settings.getChatFormat().split("%message%", -1);
        String left = (parts.length > 0 ? parts[0] : "")
                .replace("{color}", "<" + hex + ">")
                .replace("%name%", name);
        String right = (parts.length > 1 ? parts[1] : "")
                .replace("{color}", "<" + hex + ">")
                .replace("%name%", name);
        return settings.parse(left).append(message).append(settings.parse(right));
    }
}