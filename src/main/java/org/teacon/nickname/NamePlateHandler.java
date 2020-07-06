package org.teacon.nickname;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "nickname", value = Dist.CLIENT)
public final class NamePlateHandler {
    
    @SubscribeEvent
    public static void namePlate(RenderNameplateEvent event) {
        if (event.getEntity() instanceof PlayerEntity) {
            final PlayerEntity player = (PlayerEntity) event.getEntity();
            ClientPlayNetHandler connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                NetworkPlayerInfo playerInfo = connection.getPlayerInfo(player.getGameProfile().getId());
                if (playerInfo != null && playerInfo.getDisplayName() != null) {
                    event.setContent(playerInfo.getDisplayName().getFormattedText());
                }
            }
            
        }
    }
}