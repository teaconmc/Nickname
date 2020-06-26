package org.teacon.nickname;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public final class Hooks {
    public static ITextComponent getPlayerBaseName(PlayerEntity player) {
        return new StringTextComponent(NicknameMod.NICKS.getOrDefault(
            player.getGameProfile().getId(), 
            player.getGameProfile().getName()));
    }
}