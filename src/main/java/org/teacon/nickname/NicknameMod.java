package org.teacon.nickname;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("nickname")
@Mod.EventBusSubscriber(modid = "nickname")
public final class NicknameMod {

    private static final Logger LOGGER = LogManager.getLogger("Nickname");

    public NicknameMod() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(
            () -> FMLNetworkConstants.IGNORESERVERONLY, (serverVer, isDedicated) -> true));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        NicknameCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void serverStart(FMLServerStartingEvent event) {
        try {
            NicknameRepo.load();
        } catch (Exception e) {
            LOGGER.error("Failed to read existed nickname data, details: ", e);
        }
    }

    @SubscribeEvent
    public static void serverStop(FMLServerStoppingEvent event) {
        try {
            NicknameRepo.save();
        } catch (Exception e) {
            LOGGER.error("Failed to save nickname data, details: ", e);
        }
    }

    // If a player joins server, sync their display name to everyone who is currently in the server.
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            final ServerPlayerEntity thePlayer = (ServerPlayerEntity) event.getEntity();
            final SPlayerListItemPacket packet = new SPlayerListItemPacket(SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, thePlayer);
            thePlayer.server.getPlayerList().sendPacketToAllPlayers(packet);
        }
    }
}