package org.teacon.nickname;

import com.google.common.collect.ImmutableSet;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NicknameReview {

    private static final ConcurrentHashMap<UUID, String> requests = new ConcurrentHashMap<>();

    public static void request(UUID uuid, String nick) {
        requests.put(uuid, nick);
    }

    public static void approve(UUID uuid, String nick) {
        requests.remove(uuid);
        NicknameRepo.setNick(uuid, nick);
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
                .filter(p -> p.getUniqueID().equals(uuid)).findAny().ifPresent((player) ->
                player.sendStatusMessage(
                        new TranslationTextComponent("commands.nickname.nickname.approved", nick), false));
    }

    public static void deny(UUID uuid) {
        requests.remove(uuid);
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
                .filter(p -> p.getUniqueID().equals(uuid)).findAny().ifPresent((player) ->
                player.sendStatusMessage(
                        new TranslationTextComponent("commands.nickname.nickname.denied"), false));
    }

    public static Collection<Map.Entry<UUID, String>> listRequests() {
        return ImmutableSet.copyOf(requests.entrySet());
    }
}
