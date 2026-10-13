package org.minehostil.storeannouncer.util;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class SoundUtil {

    private SoundUtil() {
    }

    public static void play(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Key key = Key.key(soundName.toLowerCase(Locale.ROOT));
            player.playSound(Sound.sound(key, Sound.Source.MASTER, 1.0f, 1.0f));
        } catch (Exception e) {
            // Sonido inválido en la config: se ignora
        }
    }

    public static void playAll(Iterable<? extends Player> players, String soundName) {
        if (soundName == null || soundName.isEmpty()) return;
        for (Player player : players) {
            play(player, soundName);
        }
    }
}