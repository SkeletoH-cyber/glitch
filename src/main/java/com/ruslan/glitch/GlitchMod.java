package com.ruslan.glitch;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.ruslan.glitch.entity.GlitchEntity;
import com.ruslan.glitch.item.ModItems;
import com.ruslan.glitch.network.ScarePayload;
import com.ruslan.glitch.world.GlitchDirector;
import com.ruslan.glitch.world.GlitchState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GlitchMod implements ModInitializer {
    public static final String MOD_ID = "glitchmod";

    public static final EntityType<GlitchEntity> GLITCH = Registry.register(
            Registries.ENTITY_TYPE,
            id("glitch"),
            EntityType.Builder.create(GlitchEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.6f, 1.95f)
                    .makeFireImmune()
                    .build("glitch"));

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(ScarePayload.ID, ScarePayload.CODEC);
        FabricDefaultAttributeRegistry.register(GLITCH, GlitchEntity.createGlitchAttributes());
        ModItems.register();

        ServerTickEvents.END_WORLD_TICK.register(GlitchDirector::tick);

        // Мир, разрушенный Гличем, больше не открывается.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (GlitchState.get(server).destroyed) {
                handler.disconnect(Text.literal("§4Этот мир уничтожен Гличем.\n§7Войти в него больше нельзя."));
            }
        });

        // Тестовые команды (нужен op): /glitch stage N | spawn | cross | banish | status | reset
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("glitch")
                        .requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("stage")
                                .then(CommandManager.argument("n", IntegerArgumentType.integer(0, 5))
                                        .executes(ctx -> {
                                            int n = IntegerArgumentType.getInteger(ctx, "n");
                                            GlitchState.get(ctx.getSource().getServer()).jumpToStage(ctx.getSource().getServer(), n);
                                            ctx.getSource().sendFeedback(() -> Text.literal("Стадия Глича: " + n), false);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("spawn").executes(ctx -> {
                            GlitchDirector.spawnGlitchNear(ctx.getSource().getPlayerOrThrow(), 8);
                            return 1;
                        }))
                        .then(CommandManager.literal("cross").executes(ctx -> {
                            var p = ctx.getSource().getPlayerOrThrow();
                            var world = (net.minecraft.server.world.ServerWorld) p.getWorld();
                            GlitchState st = GlitchState.get(ctx.getSource().getServer());
                            boolean ok = GlitchDirector.spawnCrossNear(world, st, p, world.getTime());
                            ctx.getSource().sendFeedback(() -> Text.literal(ok ? "Крест создан" : "Не нашёл места"), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("banish").executes(ctx -> {
                            GlitchDirector.banish(ctx.getSource().getServer());
                            return 1;
                        }))
                        .then(CommandManager.literal("status").executes(ctx -> {
                            GlitchState st = GlitchState.get(ctx.getSource().getServer());
                            String msg = "Стадия " + st.stage + ", якорей " + st.ritualProgress + "/" + GlitchState.ANCHORS_NEEDED
                                    + ", крестов в мире " + st.crosses.size() + ", изгнан: " + st.banished;
                            ctx.getSource().sendFeedback(() -> Text.literal(msg), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("reset").executes(ctx -> {
                            GlitchState.get(ctx.getSource().getServer()).reset();
                            ctx.getSource().sendFeedback(() -> Text.literal("Глич сброшен."), false);
                            return 1;
                        }))));
    }
}
