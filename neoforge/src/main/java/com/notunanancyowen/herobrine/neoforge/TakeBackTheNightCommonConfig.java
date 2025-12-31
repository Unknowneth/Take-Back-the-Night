package com.notunanancyowen.herobrine.neoforge;

import com.notunanancyowen.herobrine.TakeBackTheNight;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = TakeBackTheNight.MOD_ID)
public class TakeBackTheNightCommonConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.DoubleValue HP = BUILDER.defineInRange("maximum_health", TakeBackTheNight.HB_HP, 0, 1024);
    private static final ModConfigSpec.DoubleValue ATK = BUILDER.defineInRange("attack_damage", TakeBackTheNight.HB_ATK, 0, 1024);
    private static final ModConfigSpec.DoubleValue DEF = BUILDER.defineInRange("armor_points", TakeBackTheNight.HB_DEF, 0, 1024);
    private static final ModConfigSpec.DoubleValue MOV = BUILDER.defineInRange("movement_speed", TakeBackTheNight.HB_MOV, 0, 1024);
    private static final ModConfigSpec.DoubleValue FR = BUILDER.defineInRange("follow_range", TakeBackTheNight.HB_FR, 0, 1024);

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        try {
            TakeBackTheNight.HB_HP = HP.getAsDouble();
            TakeBackTheNight.HB_ATK = ATK.getAsDouble();
            TakeBackTheNight.HB_DEF = DEF.getAsDouble();
            TakeBackTheNight.HB_MOV = MOV.getAsDouble();
            TakeBackTheNight.HB_FR = FR.getAsDouble();
        }
        catch (Throwable ignore) {
        }
    }
}
