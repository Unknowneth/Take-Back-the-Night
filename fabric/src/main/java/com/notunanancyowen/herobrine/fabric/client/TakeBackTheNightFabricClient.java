package com.notunanancyowen.herobrine.fabric.client;

import com.notunanancyowen.herobrine.TakeBackTheNight;
import com.notunanancyowen.herobrine.client.HerobrineEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public final class TakeBackTheNightFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        String originalConfig = "#Exists as some shaders make him invisible\n"
                + "special_visual_effects = " + TakeBackTheNight.SPECIAL_RENDER + "\n"
                + "show_riptide_effect = " + TakeBackTheNight.SHOW_RIPTIDE + "\n"
                + "show_stuck_arrows = " + TakeBackTheNight.SHOW_ARROWS + "\n"
                + "show_eye_glow = " + TakeBackTheNight.GLOWING_EYES;
        long originalConfigSize = originalConfig.lines().count();
        try {
            File config = FabricLoader.getInstance().getConfigDir().resolve(TakeBackTheNight.MOD_ID + "-client.toml").toFile();
            if(config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
                configWriter.write(originalConfig);
            }
            boolean rebuild = false;
            try(FileReader configReader = new FileReader(config)) {
                BufferedReader reader = new BufferedReader(configReader);
                StringBuilder bob = new StringBuilder();
                String configText = null;
                int tries = -1;
                while(++tries <= originalConfigSize && (configText = reader.readLine()) != null) {
                    bob.append(configText);
                    bob.append(System.lineSeparator());
                }
                bob.deleteCharAt(bob.length() - 1);
                reader.close();
                bob.toString().lines().forEach(configContent -> {
                    if(!configContent.startsWith("#") && configContent.contains(" = ")) {
                        String[] configValues = configContent.split(" = ", 2);
                        boolean result = configValues[1].equals("true");
                        switch (configValues[0]) {
                            case "special_visual_effects" -> TakeBackTheNight.SPECIAL_RENDER = result;
                            case "show_riptide_effect" -> TakeBackTheNight.SHOW_RIPTIDE = result;
                            case "show_stuck_arrows" -> TakeBackTheNight.SHOW_ARROWS = result;
                            case "show_eye_glow" -> TakeBackTheNight.GLOWING_EYES = result;
                            default -> TakeBackTheNight.LOGGER.info("Unknown config value: \"" + configValues[0] + "\"");
                        }
                    }
                });
                rebuild = bob.toString().lines().count() != originalConfigSize;
            }
            if(rebuild && config.delete() && config.createNewFile()) try(FileWriter configWriter = new FileWriter(config)) {
                originalConfig.lines().forEach(configContent -> {
                    if(configContent.startsWith("#") || !configContent.contains("=")) {
                        try {
                            configWriter.append(configContent);
                            if(originalConfig.lines().toList().indexOf(configContent) < originalConfigSize - 1) configWriter.append(System.lineSeparator());
                        }
                        catch (Throwable ignore) {
                        }
                    }
                    String[] configValues = configContent.split(" = ", 2);
                    if(!configValues[0].startsWith("#")) try {
                        configContent = configValues[0] + " = ";
                        switch (configValues[0]) {
                            case "special_visual_effects" -> configContent += TakeBackTheNight.SPECIAL_RENDER;
                            case "show_riptide_effect" -> configContent += TakeBackTheNight.SHOW_RIPTIDE;
                            case "show_stuck_arrows" -> configContent += TakeBackTheNight.SHOW_ARROWS;
                            case "show_eye_glow" -> configContent += TakeBackTheNight.GLOWING_EYES;
                            default -> TakeBackTheNight.LOGGER.info("Unknown config value: \"" + configValues[0] + "\"");
                        }
                        configWriter.append(configContent);
                        if(originalConfig.lines().toList().indexOf(configContent) < originalConfigSize - 1) configWriter.append(System.lineSeparator());
                    }
                    catch (Throwable ignore) {
                        TakeBackTheNight.LOGGER.info("Failed to generate config");
                    }
                });
            }
        }
        catch (Throwable t) {
            TakeBackTheNight.LOGGER.info("Failed to generate config: " + t.getLocalizedMessage());
        }
        EntityRendererRegistry.register(TakeBackTheNight.HEROBRINE, HerobrineEntityRenderer::new);
    }
}
