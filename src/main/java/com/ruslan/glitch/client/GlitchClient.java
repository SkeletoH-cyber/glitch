package com.ruslan.glitch.client;

import com.ruslan.glitch.GlitchMod;
import com.ruslan.glitch.network.ScarePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Random;

public class GlitchClient implements ClientModInitializer {
    private static final Identifier SCARE_FACE = GlitchMod.id("textures/gui/glitch_scare.png");
    private static final Random RAND = new Random();

    private static int ticksLeft = 0;
    private static int total = 1;
    private static int variant = 0;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(GlitchMod.GLITCH, GlitchRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(ScarePayload.ID, (payload, context) ->
                trigger(context.client(), payload.durationTicks(), payload.variant()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ticksLeft > 0) ticksLeft--;
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
    }

    private static void trigger(MinecraftClient client, int duration, int v) {
        ticksLeft = Math.max(1, duration);
        total = ticksLeft;
        variant = v;
        var sm = client.getSoundManager();
        switch (v) {
            case 0 -> {
                sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 1.0f));
                sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_ENDERMAN_SCREAM, 0.5f, 1.0f));
            }
            case 1 -> sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_ENDERMAN_STARE, 0.6f, 1.0f));
            case 2 -> sm.play(PositionedSoundInstance.master(SoundEvents.ENTITY_WARDEN_ROAR, 0.5f, 1.0f));
            default -> sm.play(PositionedSoundInstance.master(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.0f));
        }
    }

    private static void bands(DrawContext ctx, int w, int h, int count, int maxAlpha) {
        int[] colors = {0xFFFFFF, 0x00FFFF, 0xFF00FF, 0xFF0000, 0x000000};
        for (int i = 0; i < count; i++) {
            int y = RAND.nextInt(h);
            int bh = 1 + RAND.nextInt(Math.max(2, h / 25));
            int x = RAND.nextInt(w / 2);
            int bw = w / 4 + RAND.nextInt(w / 2);
            int alpha = 40 + RAND.nextInt(maxAlpha);
            ctx.fill(x, y, Math.min(w, x + bw), Math.min(h, y + bh), (alpha << 24) | colors[RAND.nextInt(colors.length)]);
        }
    }

    private static void render(DrawContext ctx) {
        if (ticksLeft <= 0) return;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        float progress = ticksLeft / (float) total; // 1 -> 0

        switch (variant) {
            case 0 -> { // скример: чёрный фон + лицо Глича с помехами
                ctx.fill(0, 0, w, h, 0xFF000000);
                if (ticksLeft % 5 != 0) { // моргание лица
                    int size = Math.min(w, h);
                    int ox = (w - size) / 2 + RAND.nextInt(13) - 6;
                    int oy = (h - size) / 2 + RAND.nextInt(13) - 6;
                    ctx.drawTexture(SCARE_FACE, ox, oy, size, size, 0, 0, 256, 256, 256, 256);
                }
                bands(ctx, w, h, 14, 120);
            }
            case 1 -> bands(ctx, w, h, 6 + RAND.nextInt(6), 90); // короткие помехи
            case 2 -> { // финал: тяжёлые помехи и темнота
                int dark = (int) ((1f - progress) * 140);
                ctx.fill(0, 0, w, h, (Math.min(200, dark + 30) << 24));
                bands(ctx, w, h, 30, 160);
            }
            default -> { // вспышка победы
                int alpha = (int) (progress * 230);
                ctx.fill(0, 0, w, h, (alpha << 24) | 0xFFFFFF);
            }
        }
    }
}
