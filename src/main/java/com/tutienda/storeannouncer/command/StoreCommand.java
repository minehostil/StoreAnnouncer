package com.tutienda.storeannouncer.command;

import com.tutienda.storeannouncer.StoreAnnouncerPlugin;
import com.tutienda.storeannouncer.announcer.PurchaseAnnouncer;
import com.tutienda.storeannouncer.config.Settings;
import com.tutienda.storeannouncer.gg.GGWaveManager;
import com.tutienda.storeannouncer.purchase.Purchase;
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
            sender.sendMessage(settings.parse("<red>Uso:</red> <gray>/tienda <reload | anunciar | ggwave></gray>"));
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(settings.parse("<green>StoreAnnouncer recargado.</green>"));
            }
            case "anunciar" -> {
                if (args.length < 3) {
                    sender.sendMessage(settings.parse(
                            "<red>Uso:</red> <gray>/tienda anunciar <jugador> <producto...> [cantidad] [precio]</gray>"));
                    return true;
                }
                String player = args[1];

                // Detecta [cantidad] [precio] numéricos al final
                // (compatible con: tienda anunciar {username} {packageName} {quantity} {price})
                int end = args.length;
                Double precio = null;
                Integer cantidad = null;
                if (isNumber(args[end - 1])) {
                    precio = Double.parseDouble(args[end - 1]);
                    end--;
                }
                if (end - 1 >= 3 && isNumber(args[end - 1])) {
                    cantidad = Integer.parseInt(args[end - 1]);
                    end--;
                }
                String producto = String.join(" ", Arrays.copyOfRange(args, 2, end));

                announcer.announce(new Purchase(player, null, producto,
                        cantidad != null ? cantidad : 1,
                        precio != null ? precio : 0.0D));
                sender.sendMessage(settings.parse("<green>Anuncio enviado.</green>"));
            }
            case "ggwave" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("stop")) {
                    waveManager.stop(true);
                    sender.sendMessage(settings.parse("<green>GG Wave detenida.</green>"));
                    return true;
                }
                int seconds = parseIntOr(args.length >= 2 ? args[1] : null, settings.getWaveDuration());
                waveManager.start(seconds);
            }
            default -> sender.sendMessage(settings.parse(
                    "<red>Uso:</red> <gray>/tienda <reload | anunciar | ggwave></gray>"));
        }
        return true;
    }

    private boolean isNumber(String value) {
        return value.matches("\\d+(\\.\\d+)?");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "anunciar", "ggwave"), args[0]);
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
