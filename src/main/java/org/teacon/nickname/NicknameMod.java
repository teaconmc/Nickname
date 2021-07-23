package org.teacon.nickname;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmllegacy.network.FMLNetworkConstants;
import net.minecraftforge.fmlserverevents.FMLServerStartingEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static net.minecraftforge.fmllegacy.network.FMLNetworkConstants.IGNORESERVERONLY;

@Mod("nickname")
@Mod.EventBusSubscriber(modid = "nickname")
public final class NicknameMod {

    private static final Logger LOGGER = LogManager.getLogger("Nickname");

    public NicknameMod() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, ()->new IExtensionPoint.DisplayTest(()->FMLNetworkConstants.IGNORESERVERONLY, (remote, isServer)-> true));
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
        if (event.getEntity() instanceof ServerPlayer) {
            final ServerPlayer thePlayer = (ServerPlayer) event.getEntity();
            thePlayer.server.getPlayerList().broadcastAll(VanillaPacketUtils.displayNameUpdatePacketFor(thePlayer));
        }
    }
}