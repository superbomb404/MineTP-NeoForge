package ljsure.cn.MineTP.minetp;


import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.concurrent.CompletableFuture;

@Mod(MineTPMod.MODID)
public class MineTPMod {
    public static final String MODID = "minetp";

    public MineTPMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /tpw 命令
        dispatcher.register(Commands.literal("tpw")
                .then(Commands.argument("params", StringArgumentType.greedyString())
                        .suggests(this::suggestPlayers)
                        .executes(this::handleTpw))
                .executes(ctx -> {
                    ctx.getSource().sendSystemMessage(Component.literal("§c用法: /tpw [玩家名] 或 /tpw [x] [y] [z]"));
                    return 0;
                })
        );

        // /killl 命令
        dispatcher.register(Commands.literal("killl")
                .executes(this::handleKilll)
        );
    }

    private CompletableFuture<Suggestions> suggestPlayers(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        MinecraftServer server = context.getSource().getServer();
        server.getPlayerList().getPlayers().forEach(player ->
                builder.suggest(player.getGameProfile().getName()));
        return builder.buildFuture();
    }

    private int handleTpw(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String params = StringArgumentType.getString(context, "params");
        String[] args = params.split(" ");
        MinecraftServer server = source.getServer();

        // 玩家传送模式
        if (args.length == 1) {
            ServerPlayer target = server.getPlayerList().getPlayerByName(args[0]);
            if (target == null) {
                source.sendSystemMessage(Component.literal("§c玩家不存在或不在线！"));
                return 0;
            }

            String command = String.format("tp %s %s", player.getScoreboardName(), target.getScoreboardName());
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);

            server.getPlayerList().getPlayers().forEach(p ->
                    p.sendSystemMessage(Component.literal(String.format("§a 已将 %s 传送到 %s",
                            player.getScoreboardName(),
                            target.getScoreboardName())))
            );
            return Command.SINGLE_SUCCESS;
        }

        // 坐标传送模式
        if (args.length == 3) {
            try {
                Vec3 currentPos = player.position();
                double x = parseCoordinate(currentPos.x, args[0]);
                double y = parseCoordinate(currentPos.y, args[1]);
                double z = parseCoordinate(currentPos.z, args[2]);

                // 距离检查
                double distance = currentPos.distanceTo(new Vec3(x, y, z));
                if (distance > 1500) {
                    source.sendSystemMessage(Component.literal("§c超出最大传送半径（1500格）！"));
                    return 0;
                }

                String command = String.format("tp %s %.2f %.2f %.2f",
                        player.getScoreboardName(), x, y, z);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);

                server.getPlayerList().getPlayers().forEach(p ->
                        p.sendSystemMessage(Component.literal(String.format("§a 已将 %s 传送到坐标 (%.1f, %.1f, %.1f)",
                                player.getScoreboardName(), x, y, z)))
                );
                return Command.SINGLE_SUCCESS;
            } catch (NumberFormatException e) {
                source.sendSystemMessage(Component.literal("§c坐标格式不正确！"));
            }
        }

        source.sendSystemMessage(Component.literal("§c参数错误！用法: /tpw [玩家名] 或 /tpw [x] [y] [z]"));
        return 0;
    }

    private double parseCoordinate(double base, String input) throws NumberFormatException {
        if (input.startsWith("~")) {
            return base + (input.length() > 1 ? Double.parseDouble(input.substring(1)) : 0);
        }
        return Double.parseDouble(input);
    }

    private int handleKilll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        MinecraftServer server = source.getServer();

        String command = String.format("kill %s", player.getScoreboardName());
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);

        server.getPlayerList().getPlayers().forEach(p ->
                p.sendSystemMessage(Component.literal(String.format("§c %s 选择了自杀...", player.getScoreboardName())))
        );
        return Command.SINGLE_SUCCESS;
    }
}