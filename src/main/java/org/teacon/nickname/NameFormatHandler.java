package org.teacon.nickname;

import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "nickname", value = Dist.DEDICATED_SERVER)
public class NameFormatHandler {
    @SubscribeEvent
    public static void nameFormat(PlayerEvent.NameFormat event) {
        event.setDisplayname(new StringTextComponent(
                NicknameRepo.lookup(event.getPlayer().getUniqueID()).orElse(event.getPlayer().getGameProfile().getName())
        ));
    }
}
