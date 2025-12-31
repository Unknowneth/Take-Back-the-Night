package com.notunanancyowen.herobrine.client;

import com.notunanancyowen.herobrine.entities.HerobrineEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

public class HerobrineEntityModel<T extends HerobrineEntity> extends PlayerEntityModel<T> {
    public boolean aura;
    public HerobrineEntityModel(ModelPart root) {
        super(root, false);
    }
    private static float lerpSelf(float a, float b) {
        if(MinecraftClient.getInstance().isPaused()) return a;
        if(b < a) return 0F;
        return MathHelper.lerp(0.4F / (Math.max(MinecraftClient.getInstance().getCurrentFps(), 1) / 60F), a, b);
    }
    @Override public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        if(aura) {
            getBodyParts().forEach(m -> m.scale(new Vector3f(0.1F)));
            getHeadParts().forEach(m -> m.scale(new Vector3f(0.1F)));
        }
        super.render(matrices, vertices, light, overlay, color);
        if(aura) {
            getBodyParts().forEach(m -> m.scale(new Vector3f(-0.1F)));
            getHeadParts().forEach(m -> m.scale(new Vector3f(-0.1F)));
        }
    }
    @Override public void setAngles(T livingEntity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        super.setAngles(livingEntity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        float tickDelta = livingEntity.tickDelta;
        float pi = (float)Math.PI;
        float pitch = headPitch / 180F * pi;
        float clp = livingEntity.getDataTracker().get(HerobrineEntity.TP_TIME);
        if(clp < 0) if(clp > -10) {
            clp += tickDelta;
            clp /= -10F;
            rightArm.pitch = leftArm.pitch = -pi * 0.6F * clp;
            leftArm.yaw = -pi / 2F * clp;
            rightArm.yaw = pi / 2F * clp;
        }
        else if(clp > -50) {
            clp += 50;
            if(clp / 5F < 1F) clp += tickDelta;
            clp /= 5F;
            if(clp > 1F) clp = 1F;
            rightArm.pitch = leftArm.pitch = -pi * 0.6F * clp;
            leftArm.yaw = -pi / 2F * clp;
            rightArm.yaw = pi / 2F * clp;
        }
        else if(clp > -100) {
            clp += 50;
            if(clp / -5F < 1F) clp += tickDelta;
            clp /= -5F;
            if(clp > 1F) clp = 1F;
            body.pitch += pi / 8F * clp;
            rightArm.pitch = pi / -8F * clp;
            rightArm.yaw = pi / -8F * clp;
            leftArm.pitch = pi / 8F * clp;
            leftArm.yaw = pi / 8F * clp;
            leftLeg.pitch = pi / 4F * clp;
            rightLeg.pitch = pi / 8F * clp;
            leftLeg.translate(new Vector3f(0F, 0F, 2F * clp));
            rightLeg.translate(new Vector3f(0F, -clp, clp));
        }
        int attackMode = livingEntity.getDataTracker().get(HerobrineEntity.ATTACK_MODE);
        if(attackMode <= 0) return;
        clp = livingEntity.previousAttackTime = lerpSelf(livingEntity.previousAttackTime, livingEntity.getDataTracker().get(HerobrineEntity.ATTACK_TIME));
        if(clp > 0) switch(attackMode) {
            case 1 -> {
                if(clp < 10) {
                    clp /= 5F;
                    if(clp > 1F) clp = 1F;
                    rightArm.pitch = leftArm.pitch = -pi / 2F * clp;
                    leftArm.yaw = -pi / 2F * clp;
                    rightArm.yaw = pi / 2F * clp;
                }
                else if(clp < 20) {
                    clp -= 10;
                    clp /= 5F;
                    clp -= 0.5F;
                    if(clp < 0F) clp = 0F;
                    rightArm.pitch = leftArm.pitch = -pi / 2F + pitch * clp;
                    leftArm.yaw = -pi / 2F + pi * 0.6F * clp;
                    rightArm.yaw = pi / 2F - pi * 0.6F * clp;
                }
                else if(clp < 30) {
                    clp -= 20;
                    clp /= 5F;
                    if(clp > 1F) clp = 1F;
                    rightArm.pitch = leftArm.pitch = (-pi / 2F + pitch) * (1F - clp);
                    leftArm.yaw = (-pi / 2F + pi * 0.6F) * (1F - clp);
                    rightArm.yaw = (pi / 2F - pi * 0.6F) * (1F - clp);
                }
            }
            case 2 -> {
                if(clp < 20) {
                    clp /= 20F;
                    rightArm.pitch = leftArm.pitch = -pi / 2F * clp;
                    leftArm.yaw = -pi / 2F * clp;
                    rightArm.yaw = pi / 2F * clp;
                    rightLeg.pitch = leftLeg.pitch = -pi / 10F * clp;
                }
                else if(clp < 40) {
                    clp -= 20;
                    clp /= 20F;
                    rightArm.pitch = leftArm.pitch = -pi / 2F - pi / 2F * clp;
                    leftArm.yaw = -pi / 2F * (1F - clp);
                    rightArm.yaw = pi / 2F * (1F - clp);
                    rightLeg.pitch = leftLeg.pitch = -pi / 10F * (1F - clp);
                }
                else if(clp < 60) {
                    clp -= 40;
                    clp /= 10F;
                    if(clp > 1F) clp = 1F;
                    rightArm.pitch = leftArm.pitch = -pi * (1F - clp);
                }
            }
            case 3 -> {
                if(clp < 40) {
                    clp /= 40F;
                    body.pitch += pi / 8F * clp;
                    rightArm.pitch = leftArm.pitch = -pi * 0.75F * clp;
                    leftArm.yaw = pi / 4F * clp;
                    rightArm.yaw = -pi / 4F * clp;
                    rightLeg.pitch = leftLeg.pitch = pi / 4F * clp;
                    leftLeg.translate(new Vector3f(0F, 4F * -clp, -2F * clp));
                    rightLeg.translate(new Vector3f(0F, 4F * -clp, -2F * clp));
                }
                else if(clp < 50) {
                    clp -= 40;
                    clp /= 5F;
                    if(clp > 1F) clp = 1F;
                    body.pitch += pi / 8F * (1F - clp);
                    rightArm.pitch = leftArm.pitch = -pi * MathHelper.lerp(clp, 0.75F, 0.5F);
                    leftArm.yaw = MathHelper.lerp(clp, pi / 4F * clp, -pi * 0.55F);
                    rightArm.yaw = MathHelper.lerp(clp, -pi / 4F * clp, pi * 0.55F);
                    rightLeg.pitch = leftLeg.pitch = pi / MathHelper.lerp(clp, 4F, 8F);
                    leftLeg.translate(new Vector3f(0F, 4F * -(1F - clp), -2F * (1F - clp)));
                    rightLeg.translate(new Vector3f(0F, 4F * -(1F - clp), -2F * (1F - clp)));
                }
                else if(clp < 60) {
                    clp -= 50;
                    clp /= 10F;
                    rightArm.pitch = leftArm.pitch = -pi * MathHelper.lerp(clp, 0.5F, 0F);
                    leftArm.yaw = MathHelper.lerp(clp, -pi * 0.55F, 0F);
                    rightArm.yaw = MathHelper.lerp(clp, pi * 0.55F, 0F);
                    rightLeg.pitch = leftLeg.pitch = MathHelper.lerp(clp, pi / 8F, 0F);
                }
            }
            case 4 -> {
                if(clp < 45) {
                    clp /= 15F;
                    if(clp > 1F) clp = 1F;
                    rightArm.pitch = leftArm.pitch = -pi / 2F * clp;
                    leftArm.yaw = -pi / 2F * clp;
                    rightArm.yaw = pi / 2F * clp;
                    rightLeg.pitch = pi / 6F * clp;
                    rightLeg.translate(new Vector3f(0F, 4F * -clp, -6F * clp));
                }
                else if(clp < 60) {
                    clp -= 45;
                    clp /= 15F;
                    clp = 1F - clp;
                    rightArm.pitch = leftArm.pitch = -pi / 2F * clp;
                    leftArm.yaw = -pi / 2F * clp;
                    rightArm.yaw = pi / 2F * clp;
                    rightLeg.pitch = pi / 6F * clp;
                    rightLeg.translate(new Vector3f(0F, 4F * -clp, -6F * clp));
                }
            }
            case 5 -> {
                if(clp < 20) {
                    clp /= 20F;
                    body.yaw -= pi / 6F * clp;
                    rightArm.pitch = leftArm.pitch = -pi * clp;
                    leftArm.yaw = -pi / 2F * clp;
                    rightArm.yaw = pi / 2F * clp;
                    rightLeg.pitch = pi / 6F * clp;
                    rightLeg.translate(new Vector3f(0F, 4F * -clp, -6F * clp));
                }
                else if(clp < 40) {
                    clp -= 20;
                    clp /= 5F;
                    if(clp > 1F) clp = 1F;
                    body.yaw -= pi / 6F * (1F - clp);
                    rightArm.pitch = leftArm.pitch = MathHelper.lerp(clp, -pi, pi / 4F);
                    leftArm.yaw = -pi / 2F * (1F - clp);
                    rightArm.yaw = pi / 2F * (1F - clp);
                    rightLeg.pitch = pi / 6F * (1F - clp);
                    rightLeg.translate(new Vector3f(0F, 4F * -(1F - clp), -MathHelper.lerp(clp, 6F, 4F)));
                }
                else if(clp < 60) {
                    clp -= 40;
                    clp /= 20F;
                    rightArm.pitch = leftArm.pitch = MathHelper.lerp(clp, pi / 4F, 0F);
                    rightLeg.translate(new Vector3f(0F, 0F, -4F * (1F - clp)));
                }
            }
            case 6 -> {
                if(clp < 45) {
                    clp /= 15F;
                    if(clp > 1F) clp = 1F;
                    rightArm.pitch = leftArm.pitch = -pi * clp;
                    leftArm.yaw = -pi / 2F * clp;
                    rightArm.yaw = pi / 2F * clp;
                    rightLeg.pitch = pi / 6F * clp;
                    rightLeg.translate(new Vector3f(0F, 4F * -clp, -6F * clp));
                }
                else if(clp < 60) {
                    clp -= 45;
                    clp /= 15F;
                    clp = 1F - clp;
                    rightArm.pitch = leftArm.pitch = MathHelper.lerp(clp, pi / -2F + pitch, -pi);
                    leftArm.yaw = -pi / 2F * clp;
                    rightArm.yaw = pi / 2F * clp;
                    rightLeg.pitch = pi / 6F;
                    rightLeg.translate(new Vector3f(0F, -4F, -6F));
                }
                else if(clp < 80) {
                    clp -= 60;
                    clp /= 20F;
                    clp = 1F - clp;
                    rightArm.pitch = leftArm.pitch = pi / -2F * clp + pitch * clp;
                    rightLeg.pitch = pi / 6F * clp;
                    rightLeg.translate(new Vector3f(0F, 4F * -clp, -6F * clp));
                }
            }
            case 7 -> {
                if(clp < 20) {
                    clp /= 20F;
                    rightArm.pitch = leftArm.pitch = -pi / 2F * clp;
                    leftArm.yaw = -pi / 2F * clp;
                    rightArm.yaw = pi / 2F * clp;
                    leftLeg.pitch = -pi / 10F * clp;
                    rightLeg.pitch = pi / 6F * clp;
                    rightLeg.translate(new Vector3f(0F, 4F * -clp, -6F * clp));
                }
                else if(clp < 40) {
                    clp -= 20;
                    clp /= 20F;
                    rightArm.pitch = leftArm.pitch = -pi / 2F - pi / 2F * clp;
                    leftArm.yaw = -pi / 2F * (1F - clp);
                    rightArm.yaw = pi / 2F * (1F - clp);
                    leftLeg.pitch = -pi / 10F * (1F - clp);
                    rightLeg.pitch = pi / 6F;
                    rightLeg.translate(new Vector3f(0F, -4F, -6F));
                }
                else if(clp < 60) {
                    clp -= 40;
                    clp /= 10F;
                    if(clp > 1F) clp = 1F;
                    rightArm.pitch = leftArm.pitch = -pi * (1F - clp);
                    rightLeg.pitch = pi / 6F * (1F - clp);
                    rightLeg.translate(new Vector3f(0F, 4F * -(1F - clp), -6F * (1F - clp)));
                }
            }
            case 8 -> {
                if(handSwingProgress > 0F) {
                    rightArm.pitch = 0F;
                    rightArm.yaw = 0F;
                    rightArm.roll = 0F;
                    leftArm.pitch = 0F;
                    leftArm.yaw = 0F;
                    leftArm.roll = 0F;
                    boolean right = livingEntity.getMainArm() == Arm.RIGHT;
                    if(livingEntity.preferredHand == Hand.OFF_HAND) right = !right;
                    float smoothStep = 3 * handSwingProgress * handSwingProgress - 2 * handSwingProgress * handSwingProgress * handSwingProgress;
                    if(right) rightArm.pitch = smoothStep * pi * 2F;
                    else leftArm.pitch = smoothStep * pi * 2F;
                }
            }
            case 11 -> {
                if(clp < 10) {
                    clp /= 10F;
                    body.yaw -= pi / 6F * clp;
                    rightArm.pitch = leftArm.pitch = -pi * clp;
                    leftArm.yaw = -pi / 2F * clp;
                    rightArm.yaw = pi / 2F * clp;
                    rightLeg.pitch = pi / 6F * clp;
                    rightLeg.translate(new Vector3f(0F, 4F * -clp, -6F * clp));
                }
                else if(clp < 30) {
                    clp -= 10;
                    clp /= 5F;
                    if(clp > 1F) clp = 1F;
                    body.yaw -= pi / 6F * (1F - clp);
                    rightArm.pitch = leftArm.pitch = MathHelper.lerp(clp, -pi, pi / 4F);
                    leftArm.yaw = -pi / 2F * (1F - clp);
                    rightArm.yaw = pi / 2F * (1F - clp);
                    rightLeg.pitch = pi / 6F * (1F - clp);
                    rightLeg.translate(new Vector3f(0F, 4F * -(1F - clp), -MathHelper.lerp(clp, 6F, 4F)));
                }
                else if(clp < 40) {
                    clp -= 30;
                    clp /= 10F;
                    rightArm.pitch = leftArm.pitch = MathHelper.lerp(clp, pi / 4F, 0F);
                    rightLeg.translate(new Vector3f(0F, 0F, -4F * (1F - clp)));
                }
            }
        }
    }
    @Override public void animateModel(T livingEntity, float limbAngle, float limbDistance, float tickDelta) {
        super.animateModel(livingEntity, limbAngle, limbDistance, tickDelta);
        livingEntity.tickDelta = tickDelta;
    }
}
