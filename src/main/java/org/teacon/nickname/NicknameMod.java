package org.teacon.nickname;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod("nickname")
@Mod.EventBusSubscriber(modid = "nickname")
public final class NicknameMod {

    private static final Gson GSON = new Gson();

    private static final Type TYPE_TOKEN = new TypeToken<Map<UUID, String>>() {}.getType();

    private static final Logger LOGGER = LogManager.getLogger("Nickname");

    static final HashMap<UUID, String> NICKS = new HashMap<>();

    @SubscribeEvent
    public static void serverStart(FMLServerStartingEvent event) {
        new NicknameCommand(event.getCommandDispatcher());
        try {
            Path nicknameStore = Paths.get(".", "nicknames.json");
            if (Files.exists(nicknameStore)) {
                NICKS.putAll(GSON.fromJson(Files.newBufferedReader(
                    nicknameStore, StandardCharsets.UTF_8), TYPE_TOKEN));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read existed nickname data, details: ", e);
        }
    }

    @SubscribeEvent
    public static void serverStop(FMLServerStoppingEvent event) {
        try {
            Files.write(Paths.get(".", "nicknames.json"), GSON.toJson(NICKS).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.error("Failed to save nickname data, details: ", e);
        }
    }
}