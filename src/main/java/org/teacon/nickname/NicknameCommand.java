package org.teacon.nickname;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.UUID;

public final class NicknameCommand {

    private static final DynamicCommandExceptionType INVALID_UUID =
            new DynamicCommandExceptionType(o -> new StringTextComponent(String.format("Invalid uuid: %s", o)));
    // Permission nodes
    public static final String NICKNAME_MANAGE = "nickname.manage";
    public static final String NICKNAME_BYPASS_REVIEW = "nickname.bypass_review";

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("nick")
                .then(Commands.argument("nick", StringArgumentType.greedyString())
                        .executes(NicknameCommand::changeNick))
                .then(Commands.literal("review").requires(NicknameCommand::hasManagePerms)
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .then(Commands.argument("nick", StringArgumentType.greedyString())
                                        .executes(NicknameCommand::review))))
                .then(Commands.literal("approve").requires(NicknameCommand::hasManagePerms)
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .executes(NicknameCommand::approve)))
                .then(Commands.literal("deny").requires(NicknameCommand::hasManagePerms)
                        .executes(NicknameCommand::deny))
                .executes(NicknameCommand::clearNick));
    }

    private static boolean hasManagePerms(CommandSource src) {
        try {
            return PermissionAPI.hasPermission(src.asPlayer(), NICKNAME_MANAGE);
        } catch (Exception e) {
            return false;
        }
    }

    private static int review(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource src = ctx.getSource();
        ServerPlayerEntity reviewer = src.asPlayer();
        NicknameReview.listRequests().forEach((entry) -> {
            UUID uuid = entry.getKey();
            String nick = entry.getValue();

            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()
                    .stream().filter(p -> p.getUniqueID().equals(uuid)).findAny().ifPresent(player ->
                    informReview(reviewer, player, nick));
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int approve(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        try {
            UUID uuid = UUID.fromString(ctx.getArgument("uuid", String.class));
            String nick = ctx.getArgument("nick", String.class);
            NicknameReview.approve(uuid, nick);
            return Command.SINGLE_SUCCESS;
        } catch (Exception exception) {
            throw INVALID_UUID.create(ctx.getArgument("uuid", String.class));
        }
    }

    private static int deny(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        try {
            UUID uuid = UUID.fromString(ctx.getArgument("uuid", String.class));
            NicknameReview.deny(uuid);
            return Command.SINGLE_SUCCESS;
        } catch (Exception exception) {
            throw INVALID_UUID.create(ctx.getArgument("uuid", String.class));
        }
    }

    private static int changeNick(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        ServerPlayerEntity player = ctx.getSource().asPlayer();
        if (PermissionAPI.hasPermission(player, NICKNAME_BYPASS_REVIEW)) {
            return performChangeNick(ctx);
        } else {
            String nick = ctx.getArgument("nick", String.class);
            NicknameReview.request(player.getUniqueID(), nick);
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream().filter(p -> PermissionAPI.hasPermission(p, NICKNAME_MANAGE))
                    .forEach((reviewer) -> informReview(reviewer, player, nick));
            return Command.SINGLE_SUCCESS;
        }
    }

    private static int performChangeNick(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String current = context.getArgument("nick", String.class);
        ServerPlayerEntity player = context.getSource().asPlayer();
        String previous = NicknameRepo.setNick(player.getUniqueID(), current);
        context.getSource().sendFeedback(previous == null ? new TranslationTextComponent("commands.nickname.nickname.set", current)
                : new TranslationTextComponent("commands.nickname.nickname.changed", previous, current), true);
        context.getSource().getServer().getPlayerList().sendPacketToAllPlayers(VanillaPacketUtils.displayNameUpdatePacketFor(player));
        player.refreshDisplayName();
        return Command.SINGLE_SUCCESS;
    }

    private static int clearNick(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().asPlayer();
        NicknameRepo.clearNick(player.getUniqueID());
        context.getSource().sendFeedback(new TranslationTextComponent("commands.nickname.nickname.cleared", ObjectArrays.EMPTY_ARRAY), true);
        context.getSource().getServer().getPlayerList().sendPacketToAllPlayers(VanillaPacketUtils.displayNameUpdatePacketFor(player));
        player.refreshDisplayName();
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Inform an online manager to review the change request
     * @param reviewer the reviewer to inform
     * @param requester the requester who wants to change their nickname
     * @param nick the new nickname
     */
    private static void informReview(ServerPlayerEntity reviewer, ServerPlayerEntity requester, String nick) {
        UUID uuid = requester.getUniqueID();
        reviewer.sendStatusMessage(new StringTextComponent("--------"), false);
        reviewer.sendStatusMessage(new StringTextComponent(requester.getGameProfile().getName() + " wants to change their nick to \"" + nick + '"'), false);
        IFormattableTextComponent approve = new StringTextComponent("Approve");
        approve.setStyle(Style.EMPTY.setColor(Color.fromHex("#1fed67")).setClickEvent(new ClickActionApproveRequest(uuid, nick)));
        IFormattableTextComponent deny = new StringTextComponent("Deny");
        deny.setStyle(Style.EMPTY.setColor(Color.fromHex("#f01916")).setClickEvent(new ClickActionDenyRequest(uuid)));
        reviewer.sendStatusMessage(new StringTextComponent("Operation: ").append(approve).appendString(" ").append(deny), false);
        reviewer.sendStatusMessage(new StringTextComponent("--------"), false);
    }

    private static class ClickActionApproveRequest extends ClickEvent {

        public ClickActionApproveRequest(UUID uuid, String nick) {
            super(Action.RUN_COMMAND, String.format("/nick approve %s %s", uuid.toString(), nick));
        }
    }

    private static class ClickActionDenyRequest extends ClickEvent {

        public ClickActionDenyRequest(UUID uuid) {
            super(Action.RUN_COMMAND, String.format("/nick deny %s", uuid.toString()));
        }
    }
}
