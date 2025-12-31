package com.notunanancyowen.herobrine.client;

import com.notunanancyowen.herobrine.entities.HerobrineEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class HerobrineRiptideAttackFeatureRenderer<T extends HerobrineEntity> extends FeatureRenderer<T, HerobrineEntityModel<T>> {
    public static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/trident_riptide.png");
    private final ModelPart aura;
    public HerobrineRiptideAttackFeatureRenderer(FeatureRendererContext<T, HerobrineEntityModel<T>> context, EntityModelLoader loader) {
        super(context);
        ModelPart modelPart = loader.getModelPart(EntityModelLayers.SPIN_ATTACK);
        this.aura = modelPart.getChild("box");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild("box", ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -16.0F, -8.0F, 16.0F, 32.0F, 16.0F), ModelTransform.NONE);
        return TexturedModelData.of(modelData, 64, 64);
    }

    public void render(
            MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, T livingEntity, float f, float g, float h, float j, float k, float l
    ) {
        if (livingEntity.isUsingRiptide()) {
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));

            for (int m = 0; m < 3; m++) {
                matrixStack.push();
                float n = j * -(45 + m * 5);
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n));
                float o = 0.75F * m;
                matrixStack.scale(o, o, o);
                matrixStack.translate(0.0F, -0.2F + 0.6F * m, 0.0F);
                this.aura.render(matrixStack, vertexConsumer, i, OverlayTexture.DEFAULT_UV);
                matrixStack.pop();
            }
        }
    }
}
