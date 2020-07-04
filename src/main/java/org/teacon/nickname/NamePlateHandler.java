package org.teacon.nickname;

import net.minecraft.client.Minecraft;
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
            event.setContent(Minecraft.getInstance().getConnection().getPlayerInfo(event.getEntity().getUniqueID()).getDisplayName().getFormattedText());
        }
    }
}