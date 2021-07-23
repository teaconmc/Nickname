package org.teacon.nickname;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.Optional;
import java.util.UUID;


public final class NicknameCommand {

    private static final DynamicCommandExceptionType INVALID_UUID =
            new DynamicCommandExceptionType(o -> new TextComponent(String.format("Invalid uuid: %s", o)));
    private static final DynamicCommandExceptionType PLAYER_NOTFOUND =
            new DynamicCommandExceptionType(o -> new TextComponent(String.format("Player not found for %s", o)));
    // Permission nodes
    public static final String NICKNAME_MANAGE = "nickname.manage";
    public static final String NICKNAME_BYPASS_REVIEW = "nickname.bypass_review";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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

    private static boolean hasManagePerms(CommandSourceStack src) {
        if (src.source instanceof ServerPlayer) {
            return PermissionAPI.hasPermission((ServerPlayer) src.source, NICKNAME_MANAGE);
        }
        return src.hasPermission(3);
    }

    private static int review(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer reviewer = src.getPlayerOrException();
        NicknameReview.listRequests().forEach((entry) -> {
            UUID uuid = entry.getKey();
            String nick = entry.getValue();

            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()
                    .stream().filter(p -> p.getUUID().equals(uuid)).findAny().ifPresent(player ->
                    informReview(reviewer, player, nick));
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int approve(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        try {
            UUID uuid = UUID.fromString(ctx.getArgument("uuid", String.class));
            String nick = ctx.getArgument("nick", String.class);
            String name = getPlayerByUUID(uuid).orElseThrow(() -> PLAYER_NOTFOUND.create(uuid)).getGameProfile().getName();
            if (NicknameReview.approve(uuid, nick)) {
                ctx.getSource().sendSuccess(new TranslatableComponent("commands.nickname.nickname.review.approve", name), true);
            } else {
                ctx.getSource().sendSuccess(new TranslatableComponent("commands.nickname.nickname.review.error"), true);
            }
            getPlayerByUUID(uuid).ifPresent(p -> {
                p.refreshDisplayName();
                ServerLifecycleHooks.getCurrentServer().getPlayerList()
                        .broadcastAll(VanillaPacketUtils.displayNameUpdatePacketFor(p));
            });
            return Command.SINGLE_SUCCESS;
        } catch (Exception exception) {
            throw INVALID_UUID.create(ctx.getArgument("uuid", String.class));
        }
    }

    private static int deny(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        try {
            UUID uuid = UUID.fromString(ctx.getArgument("uuid", String.class));
            String name = getPlayerByUUID(uuid).orElseThrow(() -> PLAYER_NOTFOUND.create(uuid)).getGameProfile().getName();
            if (NicknameReview.deny(uuid)) {
                ctx.getSource().sendSuccess(new TranslatableComponent("commands.nickname.nickname.review.deny", name), true);
            } else {
                ctx.getSource().sendSuccess(new TranslatableComponent("commands.nickname.nickname.review.error"), true);
            }
            return Command.SINGLE_SUCCESS;
        } catch (Exception exception) {
            throw INVALID_UUID.create(ctx.getArgument("uuid", String.class));
        }
    }

    private static int changeNick(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (PermissionAPI.hasPermission(player, NICKNAME_BYPASS_REVIEW)) {
            return performChangeNick(ctx);
        } else {
            String nick = ctx.getArgument("nick", String.class);
            NicknameReview.request(player.getUUID(), nick);
            ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream().filter(p -> PermissionAPI.hasPermission(p, NICKNAME_MANAGE))
                    .forEach((reviewer) -> informReview(reviewer, player, nick));
            ctx.getSource().sendSuccess(new TranslatableComponent("commands.nickname.nickname.pending"), false);
            return Command.SINGLE_SUCCESS;
        }
    }

    private static int performChangeNick(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String current = context.getArgument("nick", String.class);
        ServerPlayer player = context.getSource().getPlayerOrException();
        String previous = NicknameRepo.setNick(player.getUUID(), current);
        context.getSource().sendSuccess(previous == null ? new TranslatableComponent("commands.nickname.nickname.set", current)
                : new TranslatableComponent("commands.nickname.nickname.changed", previous, current), true);
        context.getSource().getServer().getPlayerList().broadcastAll(VanillaPacketUtils.displayNameUpdatePacketFor(player));
        player.refreshDisplayName();
        return Command.SINGLE_SUCCESS;
    }

    private static int clearNick(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NicknameRepo.clearNick(player.getUUID());
        context.getSource().sendSuccess(new TranslatableComponent("commands.nickname.nickname.cleared", ObjectArrays.EMPTY_ARRAY), true);
        context.getSource().getServer().getPlayerList().broadcastAll(VanillaPacketUtils.displayNameUpdatePacketFor(player));
        player.refreshDisplayName();
        return Command.SINGLE_SUCCESS;
    }

    private static Optional<ServerPlayer> getPlayerByUUID(UUID uuid) {
        return Optional.ofNullable(ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid));
    }

    /**
     * Inform an online manager to review the change request
     *
     * @param reviewer  the reviewer to inform
     * @param requester the requester who wants to change their nickname
     * @param nick      the new nickname
     */
    private static void informReview(ServerPlayer reviewer, ServerPlayer requester, String nick) {
        UUID uuid = requester.getUUID();
        reviewer.displayClientMessage(new TextComponent("--------"), false);
        reviewer.displayClientMessage(new TextComponent(requester.getGameProfile().getName() + " wants to change their nick to \"" + nick + '"'), false);
        MutableComponent approve = new TextComponent("Approve");
        approve.setStyle(Style.EMPTY.withColor(TextColor.parseColor("#1fed67")).withClickEvent(new ClickActionApproveRequest(uuid, nick)));
        MutableComponent deny = new TextComponent("Deny");
        deny.setStyle(Style.EMPTY.withColor(TextColor.parseColor("#f01916")).withClickEvent(new ClickActionDenyRequest(uuid)));
        reviewer.displayClientMessage(new TextComponent("Operation: ").append(approve).append(" ").append(deny), false);
        reviewer.displayClientMessage(new TextComponent("--------"), false);
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
