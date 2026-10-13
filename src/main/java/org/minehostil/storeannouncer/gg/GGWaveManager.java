package org.minehostil.storeannouncer.gg;

import org.minehostil.storeannouncer.StoreAnnouncerPlugin;
import org.minehostil.storeannouncer.config.Settings;
import org.minehostil.storeannouncer.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GGWaveManager {

    private final StoreAnnouncerPlugin plugin;
    private final Settings settings;

    private volatile boolean active;
    private volatile long endMillis;
    private final Map<UUID, WavePlayer> participants = new ConcurrentHashMap<>();
    private BukkitTask endTask;

    public GGWaveManager(StoreAnnouncerPlugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void start(int seconds) {
        long now = System.currentTimeMillis();
        if (active) {
            // Si ya hay una wave activa, se extiende sin borrar el progreso
            endMillis += seconds * 1000L;
        } else {
            participants.clear();
            active = true;
            endMillis = now + seconds * 1000L;
            if (!settings.getWaveStartMessage().isEmpty()) {
                Bukkit.broadcast(settings.parse(settings.getWaveStartMessage()
                        .replace("%time%", String.valueOf(seconds))));
            }
            SoundUtil.playAll(Bukkit.getOnlinePlayers(), settings.getWaveStartSound());
        }
        long remainingTicks = Math.max(1L, (endMillis - now) / 50L);
        if (endTask != null) {
            endTask.cancel();
        }
        endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> stop(true), remainingTicks);
    }

    public void stop(boolean announceEnd) {
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }
        boolean wasActive = active;
        active = false;
        participants.clear();
        if (wasActive && announceEnd && !settings.getWaveEndMessage().isEmpty()) {
            Bukkit.broadcast(settings.parse(settings.getWaveEndMessage()));
        }
    }

    public boolean isActive() {
        return active && System.currentTimeMillis() <= endMillis;
    }

    public WavePlayer getWavePlayer(UUID uuid) {
        return participants.computeIfAbsent(uuid, id -> new WavePlayer());
    }

    public static final class WavePlayer {
        private int ggCount;
        private int rewardsGiven;
        private long lastRewardMillis;

        public int getGgCount() { return ggCount; }
        public void incrementGg() { this.ggCount++; }
        public int getRewardsGiven() { return rewardsGiven; }
        public void setRewardsGiven(int rewardsGiven) { this.rewardsGiven = rewardsGiven; }
        public long getLastRewardMillis() { return lastRewardMillis; }
        public void setLastRewardMillis(long lastRewardMillis) { this.lastRewardMillis = lastRewardMillis; }
    }
}