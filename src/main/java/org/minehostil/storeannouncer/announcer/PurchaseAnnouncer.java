package org.minehostil.storeannouncer.announcer;

import org.minehostil.storeannouncer.StoreAnnouncerPlugin;
import org.minehostil.storeannouncer.config.Settings;
import org.minehostil.storeannouncer.gg.GGWaveManager;
import org.minehostil.storeannouncer.purchase.Purchase;
import org.minehostil.storeannouncer.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
        // Líneas de texto que irán a la par de la cabeza
        List<Component> textLines = new ArrayList<>();
        textLines.add(settings.parse(settings.getHeader().replace("%player%", name)));
        textLines.add(settings.parse(settings.getProduct()
                .replace("%product%", purchase.producto())
                .replace("%quantity%", String.valueOf(purchase.cantidad()))));
        if (!settings.getPriceFormat().isEmpty() && purchase.precio() > 0) {
            textLines.add(settings.parse(settings.getPriceFormat()
                    .replace("%price%", formatPrice(purchase.precio()))));
        }
        String link = settings.getStoreLink();
        if (link != null && !link.isEmpty() && !settings.getStoreLine().isEmpty()) {
            textLines.add(settings.parse(settings.getStoreLine().replace("%link%", link)));
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (headRows == null) {
                // Sin cabeza: solo las líneas de texto, una debajo de otra
                for (Component line : textLines) {
                    player.sendMessage(line);
                }
            } else {
                // Centra el texto verticalmente respecto a la cabeza
                int start = Math.max(0, (headRows.size() - textLines.size()) / 2);
                for (int i = 0; i < headRows.size(); i++) {
                    int textIndex = i - start;
                    if (textIndex >= 0 && textIndex < textLines.size()) {
                        player.sendMessage(Component.empty()
                                .append(headRows.get(i))
                                .append(Component.text("   "))
                                .append(textLines.get(textIndex)));
                    } else {
                        player.sendMessage(headRows.get(i));
                    }
                }
            }
        }

        plugin.getLogger().info("Purchase announced: " + name + " -> " + purchase.producto()
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