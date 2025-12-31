package com.notunanancyowen.herobrine.client;

import com.notunanancyowen.herobrine.TakeBackTheNight;
import com.notunanancyowen.herobrine.entities.HerobrineEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.StuckArrowsFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

public class HerobrineEntityRenderer extends MobEntityRenderer<HerobrineEntity, HerobrineEntityModel<HerobrineEntity>> {
    private static final RenderLayer LAYER = RenderLayer.getEntityCutoutNoCull(Identifier.ofVanilla("textures/entity/guardian_beam.png"));
    public HerobrineEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new HerobrineEntityModel<>(context.getPart(EntityModelLayers.PLAYER)), 0.5F);
        if(TakeBackTheNight.SHOW_ARROWS) this.addFeature(new StuckArrowsFeatureRenderer<>(context, this));
        if(TakeBackTheNight.GLOWING_EYES) this.addFeature(new HerobrineEyesFeatureRenderer<>(this));
        if(TakeBackTheNight.SHOW_RIPTIDE) this.addFeature(new HerobrineRiptideAttackFeatureRenderer<>(this, context.getModelLoader()));
    }
    @Override public Identifier getTexture(HerobrineEntity entity) {
        return Identifier.of(TakeBackTheNight.MOD_ID, "textures/entity/herobrine.png");
    }
    @Override protected void setupTransforms(HerobrineEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, float scale) {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);
        if(entity.getDataTracker().get(HerobrineEntity.FLIP_TIME) != 0) {
            int f = entity.getDataTracker().get(HerobrineEntity.FLIP_TIME);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-(f - Math.signum(f) * tickDelta) * 36F), 0.0F, entity.getHeight() / entity.getScaleFactor() / 2.0F, 0.0F);
        }
        matrices.scale(0.95F, 0.95F, 0.95F);
    }
    @Override public void render(HerobrineEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        if(!TakeBackTheNight.SPECIAL_RENDER) {
            super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
            if(livingEntity.getDataTracker().get(HerobrineEntity.ATTACK_MODE) == 10) handleLaserBeam(livingEntity, g, matrixStack, vertexConsumerProvider);
            return;
        }
        matrixStack.push();
        livingEntity.tickDelta = g;
        this.model.handSwingProgress = this.getHandSwingProgress(livingEntity, g);
        this.model.riding = livingEntity.hasVehicle();
        this.model.child = livingEntity.isBaby();
        float h = MathHelper.lerpAngleDegrees(g, livingEntity.prevBodyYaw, livingEntity.bodyYaw);
        float j = MathHelper.lerpAngleDegrees(g, livingEntity.prevHeadYaw, livingEntity.headYaw);
        float k = j - h;
        if (livingEntity.hasVehicle() && livingEntity.getVehicle() instanceof LivingEntity livingEntity2) {
            h = MathHelper.lerpAngleDegrees(g, livingEntity2.prevBodyYaw, livingEntity2.bodyYaw);
            k = j - h;
            float l = MathHelper.wrapDegrees(k);
            if (l < -85.0F) {
                l = -85.0F;
            }

            if (l >= 85.0F) {
                l = 85.0F;
            }

            h = j - l;
            if (l * l > 2500.0F) {
                h += l * 0.2F;
            }

            k = j - h;
        }

        float m = MathHelper.lerp(g, livingEntity.prevPitch, livingEntity.getPitch());
        if (shouldFlipUpsideDown(livingEntity)) {
            m *= -1.0F;
            k *= -1.0F;
        }

        k = MathHelper.wrapDegrees(k);
        if (livingEntity.isInPose(EntityPose.SLEEPING)) {
            Direction direction = livingEntity.getSleepingDirection();
            if (direction != null) {
                float n = livingEntity.getEyeHeight(EntityPose.STANDING) - 0.1F;
                matrixStack.translate(-direction.getOffsetX() * n, 0.0F, -direction.getOffsetZ() * n);
            }
        }

        float lx = livingEntity.getScale();
        matrixStack.scale(lx, lx, lx);
        float n = this.getAnimationProgress(livingEntity, g);
        this.setupTransforms(livingEntity, matrixStack, n, h, g, lx);
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        this.scale(livingEntity, matrixStack, g);
        matrixStack.translate(0.0F, -1.501F, 0.0F);
        float o = 0.0F;
        float p = 0.0F;
        if (!livingEntity.hasVehicle() && livingEntity.isAlive()) {
            o = livingEntity.limbAnimator.getSpeed(g);
            p = livingEntity.limbAnimator.getPos(g);
            if (livingEntity.isBaby()) {
                p *= 3.0F;
            }

            if (o > 1.0F) {
                o = 1.0F;
            }
        }

        this.model.animateModel(livingEntity, p, o, g);
        this.model.setAngles(livingEntity, p, o, n, k, m);
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        boolean bl = this.isVisible(livingEntity);
        boolean bl2 = !bl && !livingEntity.isInvisibleTo(minecraftClient.player);
        boolean bl3 = minecraftClient.hasOutline(livingEntity);
        RenderLayer renderLayer = this.getRenderLayer(livingEntity, bl, bl2, bl3);
        if (renderLayer != null) {
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);
            int q = getOverlay(livingEntity, this.getAnimationCounter(livingEntity, g));
            float tp = livingEntity.getDataTracker().get(HerobrineEntity.TP_TIME);
            if(tp > 0F) tp += g;
            if(tp < 0F) tp = 1F;
            else tp = (float)Math.cos(tp * 0.1F * Math.PI);
            float alpha = (bl2 ? 0.15F : 1F) * (tp * 0.5F + 0.5F) * tp;
            int color = ColorHelper.Argb.fromFloats(alpha, 1.0F, tp, 1.0F);
            this.model.render(matrixStack, vertexConsumer, i, q, color);
            this.model.aura = true;
            float fb = livingEntity.getDataTracker().get(HerobrineEntity.ATTACK_TIME);
            switch (livingEntity.getDataTracker().get(HerobrineEntity.ATTACK_MODE)) {
                case 1 -> {
                    fb += g;
                    fb = (float)Math.sin(fb / 30F * Math.PI);
                    this.model.render(matrixStack, vertexConsumer, 15728640, getOverlay(livingEntity, Math.max(fb, 0F)), ColorHelper.Argb.fromFloats(alpha * fb, 0F, fb, fb * 0.8F));
                }
                case 2 -> {
                    if(fb < 60F) {
                        fb += g;
                        fb = (float)Math.sin(fb / 60F * Math.PI);
                        this.model.render(matrixStack, vertexConsumer, 15728640, getOverlay(livingEntity, Math.max(fb, 0F)), ColorHelper.Argb.fromFloats(alpha * fb, fb * 0.8F, fb, 0F));
                    }
                }
                case 4, 7 -> {
                    if(fb < 60F) {
                        fb += g;
                        fb = (float)Math.sin(fb / 60F * Math.PI);
                        this.model.render(matrixStack, vertexConsumer, 15728640, getOverlay(livingEntity, Math.max(fb, 0F)), ColorHelper.Argb.fromFloats(alpha * fb, fb, 0F, fb * 0.8F));
                    }
                }
                case 6 -> {
                    if(fb < 80F) {
                        fb += g;
                        fb = (float)Math.sin(fb / 80F * Math.PI);
                        this.model.render(matrixStack, vertexConsumer, 15728640, getOverlay(livingEntity, Math.max(fb, 0F)), ColorHelper.Argb.fromFloats(alpha * fb, fb, fb, fb));
                    }
                }
            }
            this.model.aura = false;
        }
        if (!livingEntity.isSpectator()) {
            for (FeatureRenderer<HerobrineEntity, HerobrineEntityModel<HerobrineEntity>> featureRenderer : this.features) {
                featureRenderer.render(matrixStack, vertexConsumerProvider, i, livingEntity, p, o, g, n, k, m);
            }
        }
        if (this.hasLabel(livingEntity)) {
            this.renderLabelIfPresent(livingEntity, livingEntity.getDisplayName(), matrixStack, vertexConsumerProvider, i, g);
        }
        matrixStack.pop();
        if(livingEntity.getDataTracker().get(HerobrineEntity.ATTACK_MODE) == 10) handleLaserBeam(livingEntity, g, matrixStack, vertexConsumerProvider);
    }
    @Override public boolean shouldRender(HerobrineEntity livingEntity, Frustum frustum, double d, double e, double f) {
        if (super.shouldRender(livingEntity, frustum, d, e, f)) {
            return true;
        } else {
            if (livingEntity.getDataTracker().get(HerobrineEntity.ATTACK_MODE) == 10) {
                Vec3d vec3d = new Vec3d(livingEntity.getDataTracker().get(HerobrineEntity.LASER_TARGET));
                Vec3d vec3d2 = this.fromLerpedPosition(livingEntity, livingEntity.getStandingEyeHeight(), 1.0F);
                return frustum.isVisible(new Box(vec3d2.x, vec3d2.y, vec3d2.z, vec3d.x, vec3d.y, vec3d.z));
            }
            return false;
        }
    }
    private void handleLaserBeam(HerobrineEntity livingEntity, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
        float j = livingEntity.getDataTracker().get(HerobrineEntity.ATTACK_TIME);
        if(j < 5) return;
        j += g;
        float h = Math.min(j * 0.025F, 1F);
        float k = j * 0.5F % 1.0F;
        float l = livingEntity.getStandingEyeHeight();
        matrixStack.push();
        matrixStack.translate(0.0F, l, 0.0F);
        Vec3d vec3d = new Vec3d(livingEntity.getDataTracker().get(HerobrineEntity.LASER_TARGET));
        Vec3d vec3d2 = this.fromLerpedPosition(livingEntity, l, g);
        Vec3d vec3d3 = vec3d.subtract(vec3d2);
        float m = (float)(vec3d3.length() + 1.0);
        vec3d3 = vec3d3.normalize();
        float n = (float)Math.acos(vec3d3.y);
        float o = (float)Math.atan2(vec3d3.z, vec3d3.x);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((float) (Math.PI / 2) - o) * (180.0F / (float)Math.PI)));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n * (180.0F / (float)Math.PI)));
        float q = j * 0.05F * -1.5F;
        float r = h * h;
        int s = 64 + (int)(r * 191.0F);
        int t = 32 + (int)(r * 191.0F);
        int u = 128 - (int)(r * 64.0F);
        float x = MathHelper.cos(q + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
        float y = MathHelper.sin(q + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
        float z = MathHelper.cos(q + (float) (Math.PI / 4)) * 0.282F;
        float aa = MathHelper.sin(q + (float) (Math.PI / 4)) * 0.282F;
        float ab = MathHelper.cos(q + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
        float ac = MathHelper.sin(q + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
        float ad = MathHelper.cos(q + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
        float ae = MathHelper.sin(q + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
        float af = MathHelper.cos(q + (float) Math.PI) * 0.2F;
        float ag = MathHelper.sin(q + (float) Math.PI) * 0.2F;
        float ah = MathHelper.cos(q + 0.0F) * 0.2F;
        float ai = MathHelper.sin(q + 0.0F) * 0.2F;
        float aj = MathHelper.cos(q + (float) (Math.PI / 2)) * 0.2F;
        float ak = MathHelper.sin(q + (float) (Math.PI / 2)) * 0.2F;
        float al = MathHelper.cos(q + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
        float am = MathHelper.sin(q + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
        float aq = -1.0F + k;
        float ar = m * 2.5F + aq;
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(LAYER);
        MatrixStack.Entry entry = matrixStack.peek();
        vertex(vertexConsumer, entry, af, m, ag, s, t, u, 0.4999F, ar);
        vertex(vertexConsumer, entry, af, 0.0F, ag, s, t, u, 0.4999F, aq);
        vertex(vertexConsumer, entry, ah, 0.0F, ai, s, t, u, 0.0F, aq);
        vertex(vertexConsumer, entry, ah, m, ai, s, t, u, 0.0F, ar);
        vertex(vertexConsumer, entry, aj, m, ak, s, t, u, 0.4999F, ar);
        vertex(vertexConsumer, entry, aj, 0.0F, ak, s, t, u, 0.4999F, aq);
        vertex(vertexConsumer, entry, al, 0.0F, am, s, t, u, 0.0F, aq);
        vertex(vertexConsumer, entry, al, m, am, s, t, u, 0.0F, ar);
        float as = 0.0F;
        if (livingEntity.age % 2 == 0) as = 0.5F;
        vertex(vertexConsumer, entry, x, m, y, s, t, u, 0.5F, as + 0.5F);
        vertex(vertexConsumer, entry, z, m, aa, s, t, u, 1.0F, as + 0.5F);
        vertex(vertexConsumer, entry, ad, m, ae, s, t, u, 1.0F, as);
        vertex(vertexConsumer, entry, ab, m, ac, s, t, u, 0.5F, as);
        matrixStack.pop();
    }
    private Vec3d fromLerpedPosition(LivingEntity entity, double yOffset, float delta) {
        double d = MathHelper.lerp(delta, entity.lastRenderX, entity.getX());
        double e = MathHelper.lerp(delta, entity.lastRenderY, entity.getY()) + yOffset;
        double f = MathHelper.lerp(delta, entity.lastRenderZ, entity.getZ());
        return new Vec3d(d, e, f);
    }
    private static void vertex(VertexConsumer vertexConsumer, MatrixStack.Entry matrix, float x, float y, float z, int red, int green, int blue, float u, float v) {
        vertexConsumer.vertex(matrix, x, y, z).color(red, green, blue, 255).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(matrix, 0.0F, 1.0F, 0.0F);
    }
    @Override protected float getAnimationCounter(HerobrineEntity entity, float tickDelta) {
        if(entity.getDataTracker().get(HerobrineEntity.ATTACK_MODE) == 3 && entity.getDataTracker().get(HerobrineEntity.ATTACK_TIME) > 0 && entity.getDataTracker().get(HerobrineEntity.ATTACK_TIME) < 40) return (float)Math.abs(Math.sin(entity.getDataTracker().get(HerobrineEntity.ATTACK_TIME) / 6F * Math.PI));
        return super.getAnimationCounter(entity, tickDelta);
    }
}
