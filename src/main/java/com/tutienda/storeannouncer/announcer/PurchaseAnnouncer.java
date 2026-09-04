package com.tutienda.storeannouncer.announcer;

import com.tutienda.storeannouncer.StoreAnnouncerPlugin;
import com.tutienda.storeannouncer.config.Settings;
import com.tutienda.storeannouncer.gg.GGWaveManager;
import com.tutienda.storeannouncer.purchase.Purchase;
import com.tutienda.storeannouncer.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PurchaseAnnouncer {

    private final StoreAnnouncerPlugin plugin;
    private final Settings settings;
    private final GGWaveManager waveManager;
    private final SkinHeadRenderer headRenderer;

    public PurchaseAnnouncer(StoreAnnouncerPlugin plugin, Settings settings,
                             GGWaveManager waveManager, SkinHeadRenderer headRenderer) {
        this.plugin = plugin;
        this.settings = settings;
        this.waveManager = waveManager;
        this.headRenderer = headRenderer;
    }

    /** Se llama desde el comando que ejecuta Tebex (hilo principal). */
    public void announce(Purchase purchase) {
        Player online = Bukkit.getPlayerExact(purchase.playerName());
        String name = online != null ? online.getName() : purchase.playerName();

        UUID uuid = null;
        try {
            if (purchase.uuid() != null && !purchase.uuid().isBlank()) {
                uuid = UUID.fromString(purchase.uuid());
            }
        } catch (IllegalArgumentException ignored) {
        }
        if (uuid == null && online != null) {
            uuid = online.getUniqueId();
        }

        CompletableFuture<List<Component>> face = settings.isShowHead()
                ? headRenderer.renderFace(name, uuid, online)
                : CompletableFuture.completedFuture(null);

        face.thenAccept(rows -> {
            try {
                Bukkit.getScheduler().runTask(plugin, () -> broadcast(purchase, name, rows));
            } catch (IllegalStateException ignored) {
                // Plugin deshabilitándose durante la descarga
            }
        });
    }

    private void broadcast(Purchase purchase, String name, List<Component> headRows) {
        // Bloque 1: cabeza 8x8 encima
        // Bloque 2: "Player ▶ Purchased:" + producto + enlace
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (headRows != null) {
                for (Component row : headRows) {
                    player.sendMessage(row);
                }
            }
            player.sendMessage(settings.parse(settings.getHeader().replace("%player%", name)));
            player.sendMessage(settings.parse(settings.getProduct()
                    .replace("%producto%", purchase.producto())
                    .replace("%cantidad%", String.valueOf(purchase.cantidad()))));
            if (!settings.getPriceFormat().isEmpty() && purchase.precio() > 0) {
                player.sendMessage(settings.parse(settings.getPriceFormat()
                        .replace("%precio%", formatPrice(purchase.precio()))));
            }
            String link = settings.getStoreLink();
            if (link != null && !link.isEmpty() && !settings.getStoreLine().isEmpty()) {
                player.sendMessage(settings.parse(settings.getStoreLine().replace("%link%", link)));
            }
        }

        plugin.getLogger().info("Compra anunciada: " + name + " -> " + purchase.producto()
                + " x" + purchase.cantidad() + " ($" + formatPrice(purchase.precio()) + ")");

        SoundUtil.playAll(Bukkit.getOnlinePlayers(), settings.getAnnounceSound());

        if (settings.isWaveAutoStart()) {
            waveManager.start(settings.getWaveDuration());
        }
    }

    private String formatPrice(double price) {
        if (price == Math.floor(price) && !Double.isInfinite(price)) {
            return String.valueOf((long) price);
        }
        return String.format(Locale.US, "%.2f", price);
    }
}
