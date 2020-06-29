package org.teacon.nickname;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.util.text.TranslationTextComponent;

public final class NicknameCommand {

    public NicknameCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("nickname").redirect(
            dispatcher.register(Commands.literal("nick").then(
                Commands.argument("nick", StringArgumentType.greedyString())
                    .executes(NicknameCommand::changeNick))
                .executes(NicknameCommand::clearNick))));
    }

    private static int changeNick(CommandContext<CommandSource> context) {
        try {
            String current = context.getArgument("nick", String.class);
            ServerPlayerEntity player = context.getSource().asPlayer();
            String previous = NicknameMod.NICKS.put(player.getGameProfile().getId(), current);
            context.getSource().sendFeedback(previous == null ? new TranslationTextComponent("commands.nickname.nickname.set", current)
                : new TranslationTextComponent("commands.nickname.nickname.changed", previous, current), true);
            player.connection.sendPacket(new SPlayerListItemPacket(SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, player));
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
            player.connection.sendPacket(new SPlayerListItemPacket(SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, player));
            return Command.SINGLE_SUCCESS;
        } catch (CommandSyntaxException e) {
            context.getSource().sendErrorMessage(new TranslationTextComponent("commands.nickname.nickname.error", ObjectArrays.EMPTY_ARRAY));
            return -1;
        }
    }

}
