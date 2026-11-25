package com.ruslan.glitch.client;

import com.ruslan.glitch.GlitchMod;
import com.ruslan.glitch.entity.GlitchEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.Random;

public class GlitchRenderer extends BipedEntityRenderer<GlitchEntity, BipedEntityModel<GlitchEntity>> {
    private static final Identifier TEXTURE = GlitchMod.id("textures/entity/glitch.png");

    public GlitchRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public void render(GlitchEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // Глич-дрожание: иногда модель на мгновение сдвигается и "ломается"
        Random r = new Random(entity.age / 2L * 31L + entity.getId());
        if (r.nextInt(100) < 30) {
            matrices.translate((r.nextDouble() - 0.5) * 0.25, 0, (r.nextDouble() - 0.5) * 0.25);
            matrices.scale(1.0f + (r.nextFloat() - 0.5f) * 0.3f, 1.0f + r.nextFloat() * 0.25f, 1.0f);
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(GlitchEntity entity) {
        return TEXTURE;
    }
}
