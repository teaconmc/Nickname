package org.teacon.nickname;

import com.google.common.collect.ImmutableSet;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public final class NicknameReview {

    private static final ConcurrentHashMap<UUID, String> requests = new ConcurrentHashMap<>();

    public static void request(UUID uuid, String nick) {
        requests.put(uuid, nick);
    }

    public static boolean approve(UUID uuid, String nick) {
        if (!requests.containsKey(uuid)) return false;
        if (!requests.get(uuid).equals(nick)) return false;
        requests.remove(uuid);
        NicknameRepo.setNick(uuid, nick);
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
                .filter(p -> p.getUniqueID().equals(uuid)).findAny().ifPresent((player) ->
                player.sendStatusMessage(
                        new TranslationTextComponent("commands.nickname.nickname.approved", nick), false));
        return true;
    }

    public static boolean deny(UUID uuid) {
        if (!requests.containsKey(uuid)) return false;
        requests.remove(uuid);
        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
                .filter(p -> p.getUniqueID().equals(uuid)).findAny().ifPresent((player) ->
                player.sendStatusMessage(
                        new TranslationTextComponent("commands.nickname.nickname.denied"), false));
        return true;
    }

    public static Collection<Map.Entry<UUID, String>> listRequests() {
        return ImmutableSet.copyOf(requests.entrySet());
    }
}
