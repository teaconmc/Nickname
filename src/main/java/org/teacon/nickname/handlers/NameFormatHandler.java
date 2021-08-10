package org.teacon.nickname.handlers;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.teacon.nickname.NicknameRepo;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "nickname")
public final class NameFormatHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void nameFormat(PlayerEvent.NameFormat event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            final UUID uuid = event.getPlayer().getUniqueID();
            NicknameRepo.lookup(uuid).ifPresent(name -> event.setDisplayname(new StringTextComponent(name)));
        }
    }
}
