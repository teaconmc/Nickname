package org.teacon.nickname;

import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public final class NicknameCommand {

    private static final Logger LOGGER = LogManager.getLogger("Nickname");

    public NicknameCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("nick").then(
                Commands.argument("nick", StringArgumentType.greedyString())
                    .executes(NicknameCommand::changeNick))
                .executes(NicknameCommand::clearNick));
    }

    private static int changeNick(CommandContext<CommandSource> context) {
        try {
            String current = context.getArgument("nick", String.class);
            ServerPlayerEntity player = context.getSource().asPlayer();
            String previous = NicknameMod.NICKS.put(player.getGameProfile().getId(), current);
            context.getSource().sendFeedback(previous == null ? new TranslationTextComponent("commands.nickname.nickname.set", current)
                : new TranslationTextComponent("commands.nickname.nickname.changed", previous, current), true);
            context.getSource().getServer().getPlayerList().sendPacketToAllPlayers(constructPacketFor(player));
            return Command.SINGLE_SUCCESS;
        } catch (CommandSyntaxException e) {
            context.getSource().sendErrorMessage(new TranslationTextComponent("commands.nickname.nickname.error", ObjectArrays.EMPTY_ARRAY));
            return -1;
        }
    }

    private static int clearNick(CommandContext<CommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().asPlayer();
            NicknameMod.NICKS.remove(player.getGameProfile().getId());
            context.getSource().sendFeedback(new TranslationTextComponent("commands.nickname.nickname.cleared", ObjectArrays.EMPTY_ARRAY), true);
            context.getSource().getServer().getPlayerList().sendPacketToAllPlayers(constructPacketFor(player));
            return Command.SINGLE_SUCCESS;
        } catch (CommandSyntaxException e) {
            context.getSource().sendErrorMessage(new TranslationTextComponent("commands.nickname.nickname.error", ObjectArrays.EMPTY_ARRAY));
            return -1;
        }
    }

    private static SPlayerListItemPacket constructPacketFor(ServerPlayerEntity player) {
        final SPlayerListItemPacket packet = new SPlayerListItemPacket(SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, Collections.emptyList());
        try {
            List<SPlayerListItemPacket.AddPlayerData> playerData = ObfuscationReflectionHelper.getPrivateValue(SPlayerListItemPacket.class, packet, "field_179769_b");
            playerData.add(packet.new AddPlayerData(player.getGameProfile(), player.ping, player.interactionManager.getGameType(), player.getDisplayName()));
        } catch (Exception e) {
            LOGGER.warn("Failed to construct PlayerListItemPacket, nickname will be out of sync. Check debug.log for more information.");
            LOGGER.debug("Details: {}", e);
        }
        return packet;
    }

}
