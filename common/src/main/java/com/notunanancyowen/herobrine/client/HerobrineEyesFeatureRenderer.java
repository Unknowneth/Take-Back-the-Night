package com.notunanancyowen.herobrine.client;

import com.notunanancyowen.herobrine.TakeBackTheNight;
import com.notunanancyowen.herobrine.entities.HerobrineEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.util.Identifier;

public class HerobrineEyesFeatureRenderer<T extends HerobrineEntity> extends EyesFeatureRenderer<T, HerobrineEntityModel<T>>
{
    private static final RenderLayer SKIN = RenderLayer.getEyes(Identifier.of(TakeBackTheNight.MOD_ID, "textures/entity/herobrine_eyes.png"));

    public HerobrineEyesFeatureRenderer(FeatureRendererContext<T, HerobrineEntityModel<T>> featureRendererContext) {
        super(featureRendererContext);
    }
    @Override
    public RenderLayer getEyesTexture() {
        return SKIN;
    }
}
