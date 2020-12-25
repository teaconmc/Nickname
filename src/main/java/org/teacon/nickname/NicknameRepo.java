package org.teacon.nickname;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NicknameRepo {
    private static final Map<UUID, String> NICKS = new HashMap<>();
    private static final Gson GSON = new Gson();
    private static final Type TYPE_TOKEN = new TypeToken<Map<UUID, String>>() {
    }.getType();

    public static void load() throws IOException {
        Path nicknameStore = Paths.get(".", "nicknames.json");
        if (Files.exists(nicknameStore)) {
            NICKS.putAll(GSON.fromJson(Files.newBufferedReader(
                    nicknameStore, StandardCharsets.UTF_8), TYPE_TOKEN));
        }
    }

    public static void save() throws IOException {
        Files.write(Paths.get(".", "nicknames.json"), GSON.toJson(NICKS).getBytes(StandardCharsets.UTF_8));
    }

    public static String setNick(UUID uuid, String nick) {
        return NICKS.put(uuid, nick);
    }

    public static Optional<String> lookup(UUID uuid) {
        return Optional.ofNullable(NICKS.get(uuid));
    }

    public static void clearNick(UUID uuid) {
        NICKS.remove(uuid);
    }
}
