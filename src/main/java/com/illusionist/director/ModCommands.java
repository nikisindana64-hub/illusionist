package com.illusionist.director;

import com.illusionist.IllusionistMod;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /illusionist summon [kind|nightmare] | act <1-4> | trick <имя> | event <имя> | status
 * (для тестов, нужен уровень доступа 2)
 */
@EventBusSubscriber(modid = IllusionistMod.MODID)
public class ModCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("illusionist")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("summon")
                        .executes(ctx -> {
                            PerformanceDirector.spawnMagicianNear(ctx.getSource().getPlayerOrException(), true);
                            return 1;
                        })
                        .then(Commands.literal("kind").executes(ctx -> {
                            PerformanceDirector.spawnKindMagicianNear(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("nightmare").executes(ctx -> {
                            PerformanceDirector.spawnMagicianNear(ctx.getSource().getPlayerOrException(), true);
                            return 1;
                        })))
                .then(Commands.literal("act")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int act = IntegerArgumentType.getInteger(ctx, "level");
                                    PerformanceDirector.setAct(player, act);
                                    PerformanceDirector.announceAct(player, act);
                                    return 1;
                                })))
                .then(Commands.literal("trick")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(PerformanceDirector.TRICKS, b))
                                .executes(ctx -> {
                                    PerformanceDirector.triggerTrick(ctx.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(ctx, "name").toLowerCase());
                                    return 1;
                                })))
                .then(Commands.literal("event")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(PerformanceDirector.EVENTS, b))
                                .executes(ctx -> {
                                    PerformanceDirector.triggerEvent(ctx.getSource().getPlayerOrException(),
                                            StringArgumentType.getString(ctx, "name").toLowerCase());
                                    return 1;
                                })))
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.sendSystemMessage(Component.literal("§4[Фокусник]§r Акт: §c" + PerformanceDirector.getAct(player)
                            + "§r | Напряжение: §e" + PerformanceDirector.getTension(player) + "%"));
                    return 1;
                })));
    }
}
