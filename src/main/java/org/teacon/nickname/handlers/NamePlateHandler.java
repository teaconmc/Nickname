package org.teacon.nickname.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "nickname", value = Dist.CLIENT)
public final class NamePlateHandler {

    @SubscribeEvent
    public static void namePlate(RenderNameplateEvent event) {
        if (event.getEntity() instanceof Player) {
            final Player player = (Player) event.getEntity();
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                PlayerInfo playerInfo = connection.getPlayerInfo(player.getGameProfile().getId());
                if (playerInfo != null && playerInfo.getTabListDisplayName() != null) {
                    event.setContent(playerInfo.getTabListDisplayName());
                }
            }
        }
    }
}