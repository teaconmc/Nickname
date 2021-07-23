package org.teacon.nickname;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public final class VanillaPacketUtils {

    private static final Logger LOGGER = LogManager.getLogger("Nickname");
    private static final Marker MARKER = MarkerManager.getMarker("Packet");

    private static final Field DISPLAY_NAME;

    static {
        DISPLAY_NAME = ObfuscationReflectionHelper.findField(ClientboundPlayerInfoPacket.class, "entries");
    }

    @SuppressWarnings("unchecked")
    public static ClientboundPlayerInfoPacket displayNameUpdatePacketFor(ServerPlayer player) {
        final ClientboundPlayerInfoPacket packet = new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME, Collections.emptyList());
        try {
            List<ClientboundPlayerInfoPacket.PlayerUpdate> playerData = (List<ClientboundPlayerInfoPacket.PlayerUpdate>) DISPLAY_NAME.get(packet);
            playerData.add(new ClientboundPlayerInfoPacket.PlayerUpdate(player.getGameProfile(), player.latency, player.gameMode.getGameModeForPlayer(), player.getDisplayName()));
        } catch (Exception e) {
            LOGGER.warn(MARKER, "Failed to construct PlayerListItemPacket, nickname will be out of sync. Check debug.log for more information.");
            LOGGER.debug(MARKER, "Details: ", e);
        }
        return packet;
    }
}