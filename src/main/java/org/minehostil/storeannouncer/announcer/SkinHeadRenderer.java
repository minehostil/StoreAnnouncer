package org.minehostil.storeannouncer.announcer;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.minehostil.storeannouncer.config.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Dibuja la cara del skin (8x8 píxeles) como arte ASCII en el chat:
 * un carácter █ coloreado por cada píxel real del skin.
 */
public final class SkinHeadRenderer {

    private static final int SIZE = 8;
    private static final char BLOCK = '█';
    private static final long CACHE_TTL_MS = 10 * 60_000L;
    private static final int FACE_X = 8, FACE_Y = 8;   // cara base del skin
    private static final int HAT_X = 40, HAT_Y = 8;    // capa del sombrero

    private final Settings settings;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Map<String, CachedFace> cache = new ConcurrentHashMap<>();

    public SkinHeadRenderer(Settings settings) {
        this.settings = settings;
    }

    public CompletableFuture<List<Component>> renderFace(String playerName, UUID knownUuid, Player online) {
        String key = playerName.toLowerCase(Locale.ROOT);
        CachedFace cached = cache.get(key);
        if (cached != null && System.currentTimeMillis() - cached.time() < CACHE_TTL_MS) {
            return CompletableFuture.completedFuture(cached.face());
        }
        return CompletableFuture
                .supplyAsync(() -> fetchFace(playerName, knownUuid, online))
                .completeOnTimeout(null, 15, TimeUnit.SECONDS)
                .thenApply(face -> {
                    List<Component> result = face != null ? face : fallbackFace(playerName);
                    if (cache.size() > 512) cache.clear();
                    cache.put(key, new CachedFace(result, System.currentTimeMillis()));
                    return result;
                });
    }

    /** Hilo aparte (hace peticiones HTTP). Nunca lanza excepciones. */
    private List<Component> fetchFace(String name, UUID uuid, Player online) {
        try {
            String skinUrl = skinUrlOf(name, uuid, online);
            if (skinUrl != null) {
                BufferedImage skin = downloadImage(skinUrl);
                List<Component> face = skin != null ? buildFace(extractPixels(skin), name) : null;
                if (face != null) return face;
            }
            // Plan B: avatar 8x8 ya recortado (incluye la capa)
            BufferedImage avatar = downloadImage("https://mc-heads.net/avatar/" + encode(name) + "/8");
            if (avatar != null) {
                List<Component> face = buildFace(extractPixels(avatar), name);
                if (face != null) return face;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ---------- localizar la URL del skin ----------

    private String skinUrlOf(String name, UUID uuid, Player online) {
        try {
            // 1) Online: la textura ya está en su perfil (sin red extra)
            if (online != null) {
                String value = texturesValueOf(online.getPlayerProfile());
                if (value != null) return decodeSkinUrl(value);
            }

            // 2) UUID conocido o guardado en la caché de Bukkit
            String undashed = uuid != null ? uuid.toString().replace("-", "") : null;
            if (undashed == null) {
                OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
                if (cached != null) undashed = cached.getUniqueId().toString().replace("-", "");
            }
            // 3) Última opción: API pública de Mojang
            if (undashed == null) undashed = lookupUuid(name);
            if (undashed == null) return null;

            String body = get("https://sessionserver.mojang.com/session/minecraft/profile/" + undashed);
            if (body == null) return null;
            JsonObject profile = JsonParser.parseString(body).getAsJsonObject();
            if (!profile.has("properties")) return null;
            for (var element : profile.getAsJsonArray("properties")) {
                JsonObject property = element.getAsJsonObject();
                if ("textures".equals(property.get("name").getAsString())) {
                    return decodeSkinUrl(property.get("value").getAsString());
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String texturesValueOf(PlayerProfile profile) {
        for (ProfileProperty property : profile.getProperties()) {
            if ("textures".equals(property.getName())) {
                return property.getValue();
            }
        }
        return null;
    }

    private String decodeSkinUrl(String texturesBase64) {
        try {
            String json = new String(Base64.getDecoder().decode(texturesBase64), StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private String lookupUuid(String name) {
        String body = get("https://api.mojang.com/users/profiles/minecraft/" + encode(name));
        if (body == null || body.isBlank()) return null;
        try {
            return JsonParser.parseString(body).getAsJsonObject().get("id").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    // ---------- imagen -> píxeles ----------

    private BufferedImage downloadImage(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "StoreAnnouncer/1.0")
                    .GET().build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) return null;
            return ImageIO.read(new ByteArrayInputStream(response.body()));
        } catch (Exception e) {
            return null;
        }
    }

    private String get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("User-Agent", "StoreAnnouncer/1.0")
                    .GET().build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) return null;
            return new String(response.body(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private int[][] extractPixels(BufferedImage img) {
        if (img == null) return null;

        // Avatar ya recortado 8x8
        if (img.getWidth() == SIZE && img.getHeight() == SIZE) {
            return readAll(img);
        }
        // Skin completo (64x64 o 64x32): cara + capa del sombrero
        if (img.getWidth() == 64 && (img.getHeight() == 64 || img.getHeight() == 32)) {
            int[][] pixels = new int[SIZE][SIZE];
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int base = img.getRGB(FACE_X + x, FACE_Y + y);
                    int hat = img.getHeight() == 64 ? img.getRGB(HAT_X + x, HAT_Y + y) : 0;
                    pixels[y][x] = alpha(hat) >= 128 ? hat : base;
                }
            }
            return pixels;
        }
        // Cualquier otro tamaño: escalar a 8x8
        BufferedImage scaled = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.drawImage(img, 0, 0, SIZE, SIZE, null);
        g.dispose();
        return readAll(scaled);
    }

    private int[][] readAll(BufferedImage img) {
        int[][] pixels = new int[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                pixels[y][x] = img.getRGB(x, y);
            }
        }
        return pixels;
    }

    // ---------- píxeles -> componentes ----------

    private List<Component> buildFace(int[][] pixels, String hoverName) {
        if (pixels == null) return null;
        List<Component> rows = new ArrayList<>(SIZE);
        for (int y = 0; y < SIZE; y++) {
            TextComponent.Builder row = Component.text()
                    .hoverEvent(HoverEvent.showText(
                            settings.parse(settings.getMsgHeadHover().replace("%player%", hoverName))));
            int runColor = Integer.MIN_VALUE;
            StringBuilder run = new StringBuilder();
            for (int x = 0; x < SIZE; x++) {
                int argb = pixels[y][x];
                if (alpha(argb) < 128) argb = 0xFFC8C8C8; // píxel transparente -> gris
                int rgb = argb & 0xFFFFFF;
                if (rgb != runColor) {
                    if (run.length() > 0) {
                        row.append(Component.text(run.toString(), TextColor.color(runColor)));
                    }
                    runColor = rgb;
                    run.setLength(0);
                }
                run.append(BLOCK);
            }
            if (run.length() > 0) {
                row.append(Component.text(run.toString(), TextColor.color(runColor)));
            }
            rows.add(row.build());
        }
        return rows;
    }

    /** Cara gris genérica si no se pudo obtener el skin. */
    private List<Component> fallbackFace(String hoverName) {
        int[][] pixels = new int[SIZE][SIZE];
        for (int[] row : pixels) Arrays.fill(row, 0xFF4A4A4A);
        pixels[2][2] = pixels[2][5] = 0xFF9A9A9A; // ojos
        pixels[3][2] = pixels[3][5] = 0xFF9A9A9A;
        pixels[5][3] = pixels[5][4] = 0xFF333333; // boca
        return buildFace(pixels, hoverName);
    }

    private int alpha(int argb) {
        return argb >>> 24;
    }

    private record CachedFace(List<Component> face, long time) {
    }
}