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
import net.minecraft.network.play.server.SPlayerListItemPacket;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.Optional;
import java.util.UUID;

public final class NicknameCommand {

    private static final DynamicCommandExceptionType INVALID_UUID =
            new DynamicCommandExceptionType(o -> new StringTextComponent(String.format("Invalid uuid: %s", o)));
    private static final DynamicCommandExceptionType PLAYER_NOTFOUND =
            new DynamicCommandExceptionType(o -> new StringTextComponent(String.format("Player not found for %s", o)));
    // Permission nodes
    public static final String NICKNAME_MANAGE = "nickname.manage";
    public static final String NICKNAME_BYPASS_REVIEW = "nickname.bypass_review";

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("nick")
                .then(Commands.argument("nick", StringArgumentType.greedyString())
                        .executes(NicknameCommand::changeNick))
                .executes(NicknameCommand::clearNick));
        dispatcher.register(Commands.literal("nick-review")
                .requires(NicknameCommand::hasManagePerms)
                .then(Commands.literal("approve")
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .then(Commands.argument("nick", StringArgumentType.greedyString())
                                        .executes(NicknameCommand::approve))))
                .then(Commands.literal("deny").requires(NicknameCommand::hasManagePerms)
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .executes(NicknameCommand::deny)))
                .executes(NicknameCommand::review));
    }

    private static boolean hasManagePerms(CommandSource src) {
        if (src.source instanceof ServerPlayerEntity) {
            return PermissionAPI.hasPermission((ServerPlayerEntity) src.source, NICKNAME_MANAGE);
        }
        return src.hasPermissionLevel(3);
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
            String name = getPlayerByUUID(uuid).orElseThrow(() -> PLAYER_NOTFOUND.create(uuid)).getGameProfile().getName();
            if (NicknameReview.approve(uuid, nick)) {
                ctx.getSource().sendFeedback(new TranslationTextComponent("commands.nickname.nickname.review.approve", name), true);
            } else {
                ctx.getSource().sendFeedback(new TranslationTextComponent("commands.nickname.nickname.review.error"), true);
            }
            getPlayerByUUID(uuid).ifPresent(p -> {
                PlayerList playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();
                p.refreshDisplayName();
                playerList.sendPacketToAllPlayers(new SPlayerListItemPacket(SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, p));
            });
            return Command.SINGLE_SUCCESS;
        } catch (Exception exception) {
            throw INVALID_UUID.create(ctx.getArgument("uuid", String.class));
        }
    }

    private static int deny(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        try {
            UUID uuid = UUID.fromString(ctx.getArgument("uuid", String.class));
            String name = getPlayerByUUID(uuid).orElseThrow(() -> PLAYER_NOTFOUND.create(uuid)).getGameProfile().getName();
            if (NicknameReview.deny(uuid)) {
                ctx.getSource().sendFeedback(new TranslationTextComponent("commands.nickname.nickname.review.deny", name), true);
            } else {
                ctx.getSource().sendFeedback(new TranslationTextComponent("commands.nickname.nickname.review.error"), true);
            }
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
            ctx.getSource().sendFeedback(new TranslationTextComponent("commands.nickname.nickname.pending"), false);
            return Command.SINGLE_SUCCESS;
        }
    }

    private static int performChangeNick(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String current = context.getArgument("nick", String.class);
        ServerPlayerEntity player = context.getSource().asPlayer();
        String previous = NicknameRepo.setNick(player.getUniqueID(), current);
        context.getSource().sendFeedback(previous == null ? new TranslationTextComponent("commands.nickname.nickname.set", current)
                : new TranslationTextComponent("commands.nickname.nickname.changed", previous, current), true);
        PlayerList playerList = context.getSource().getServer().getPlayerList();
        player.refreshDisplayName();
        playerList.sendPacketToAllPlayers(new SPlayerListItemPacket(SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, player));
        return Command.SINGLE_SUCCESS;
    }

    private static int clearNick(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().asPlayer();
        NicknameRepo.clearNick(player.getUniqueID());
        context.getSource().sendFeedback(new TranslationTextComponent("commands.nickname.nickname.cleared", ObjectArrays.EMPTY_ARRAY), true);
        PlayerList playerList = context.getSource().getServer().getPlayerList();
        player.refreshDisplayName();
        playerList.sendPacketToAllPlayers(new SPlayerListItemPacket(SPlayerListItemPacket.Action.UPDATE_DISPLAY_NAME, player));
        return Command.SINGLE_SUCCESS;
    }

    private static Optional<ServerPlayerEntity> getPlayerByUUID(UUID uuid) {
        return Optional.ofNullable(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(uuid));
    }

    /**
     * Inform an online manager to review the change request
     *
     * @param reviewer  the reviewer to inform
     * @param requester the requester who wants to change their nickname
     * @param nick      the new nickname
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
            super(Action.RUN_COMMAND, String.format("/nick-review approve %s %s", uuid.toString(), nick));
        }
    }

    private static class ClickActionDenyRequest extends ClickEvent {

        public ClickActionDenyRequest(UUID uuid) {
            super(Action.RUN_COMMAND, String.format("/nick-review deny %s", uuid.toString()));
        }
    }
}
