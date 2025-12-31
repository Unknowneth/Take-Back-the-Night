package com.notunanancyowen.herobrine;

import com.notunanancyowen.herobrine.entities.HerobrineEntity;
import net.minecraft.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TakeBackTheNight {
    public static final String MOD_ID = "take_back_the_night";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static boolean SPECIAL_RENDER = true;
    public static boolean SHOW_RIPTIDE = true;
    public static boolean SHOW_ARROWS = true;
    public static boolean GLOWING_EYES = true;
    public static double HB_ATK = 6;
    public static double HB_HP = 500;
    public static double HB_MOV = 0.23;
    public static double HB_FR = 256;
    public static double HB_DEF = 10;
    public static EntityType<HerobrineEntity> HEROBRINE;
    public static void init() {
        LOGGER.info("Added Herobrine");
        // Write common init code here.
    }
}
