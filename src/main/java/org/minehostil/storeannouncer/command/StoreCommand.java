package org.minehostil.storeannouncer.command;

import org.minehostil.storeannouncer.StoreAnnouncerPlugin;
import org.minehostil.storeannouncer.announcer.PurchaseAnnouncer;
import org.minehostil.storeannouncer.config.Settings;
import org.minehostil.storeannouncer.gg.GGWaveManager;
import org.minehostil.storeannouncer.purchase.Purchase;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class StoreCommand implements CommandExecutor, TabCompleter {

    private final StoreAnnouncerPlugin plugin;
    private final Settings settings;
    private final GGWaveManager waveManager;
    private final PurchaseAnnouncer announcer;

    public StoreCommand(StoreAnnouncerPlugin plugin, Settings settings,
                        GGWaveManager waveManager, PurchaseAnnouncer announcer) {
        this.plugin = plugin;
        this.settings = settings;
        this.waveManager = waveManager;
        this.announcer = announcer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(settings.parse(settings.getMsgUsage()));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(settings.parse(settings.getMsgReloaded()));
            }
            // "anunciar" se mantiene como alias de compatibilidad
            case "advertise", "anunciar" -> {
                if (args.length < 3) {
                    sender.sendMessage(settings.parse(settings.getMsgUsageAdvertise()));
                    return true;
                }
                String player = args[1];

                // Detects [quantity] [price] numeric values at the end
                // (compatible with: store advertise {username} {packageName} {quantity} {price})
                int end = args.length;
                Double price = null;
                Integer quantity = null;
                if (isNumber(args[end - 1])) {
                    price = Double.parseDouble(args[end - 1]);
                    end--;
                }
                if (end - 1 >= 3 && isNumber(args[end - 1])) {
                    quantity = Integer.parseInt(args[end - 1]);
                    end--;
                }
                String product = String.join(" ", Arrays.copyOfRange(args, 2, end));

                announcer.announce(new Purchase(player, null, product,
                        quantity != null ? quantity : 1,
                        price != null ? price : 0.0D));
                sender.sendMessage(settings.parse(settings.getMsgAnnounced()));
            }
            case "ggwave" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("stop")) {
                    waveManager.stop(true);
                    sender.sendMessage(settings.parse(settings.getMsgWaveStopped()));
                    return true;
                }
                int seconds = parseIntOr(args.length >= 2 ? args[1] : null, settings.getWaveDuration());
                waveManager.start(seconds);
            }
            default -> sender.sendMessage(settings.parse(settings.getMsgUsage()));
        }
        return true;
    }

    private boolean isNumber(String value) {
        return value.matches("\\d+(\\.\\d+)?");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "advertise", "ggwave"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("ggwave")) {
            return filter(List.of("15", "30", "60", "stop"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String start) {
        String lower = start.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.startsWith(lower)).toList();
    }

    private int parseIntOr(String value, int fallback) {
        if (value == null) return fallback;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return fallback; }
    }
}