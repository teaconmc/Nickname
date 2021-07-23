package org.teacon.nickname.handlers;

import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.teacon.nickname.NicknameRepo;

@Mod.EventBusSubscriber(modid = "nickname", value = Dist.DEDICATED_SERVER)
public final class NameFormatHandler {
    @SubscribeEvent
    public static void nameFormat(PlayerEvent.NameFormat event) {
        event.setDisplayname(new TextComponent(
                NicknameRepo.lookup(event.getPlayer().getUUID()).orElse(event.getPlayer().getGameProfile().getName())
        ));
    }
}
