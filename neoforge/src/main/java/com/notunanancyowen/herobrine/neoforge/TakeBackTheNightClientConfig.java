package com.notunanancyowen.herobrine.neoforge;

import com.notunanancyowen.herobrine.TakeBackTheNight;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = TakeBackTheNight.MOD_ID, value = Dist.CLIENT)
public class TakeBackTheNightClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue SPR = BUILDER.define("special_visual_effects", TakeBackTheNight.SPECIAL_RENDER);
    private static final ModConfigSpec.BooleanValue SRP = BUILDER.define("show_riptide_effect", TakeBackTheNight.SHOW_RIPTIDE);
    private static final ModConfigSpec.BooleanValue SSA = BUILDER.define("show_stuck_arrows", TakeBackTheNight.SHOW_ARROWS);
    private static final ModConfigSpec.BooleanValue SEG = BUILDER.define("show_eye_glow", TakeBackTheNight.GLOWING_EYES);

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        try {
            TakeBackTheNight.SPECIAL_RENDER = SPR.getAsBoolean();
            TakeBackTheNight.SHOW_RIPTIDE = SRP.getAsBoolean();
            TakeBackTheNight.SHOW_ARROWS = SSA.getAsBoolean();
            TakeBackTheNight.GLOWING_EYES = SEG.getAsBoolean();
        }
        catch (Throwable ignore) {
        }
    }
}
