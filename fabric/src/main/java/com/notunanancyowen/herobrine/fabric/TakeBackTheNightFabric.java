package com.notunanancyowen.herobrine.fabric;

import com.notunanancyowen.herobrine.entities.HerobrineEntity;
import net.fabricmc.api.ModInitializer;

import com.notunanancyowen.herobrine.TakeBackTheNight;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public final class TakeBackTheNightFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        String originalConfig = "#All of these accept decimal places\n"
                + "maximum_health = " + TakeBackTheNight.HB_HP + "\n"
                + "attack_damage = " + TakeBackTheNight.HB_ATK + "\n"
                + "armor_points = " + TakeBackTheNight.HB_DEF + "\n"
                + "movement_speed = " + TakeBackTheNight.HB_MOV + "\n"
                + "follow_range = " + TakeBackTheNight.HB_FR;
        long originalConfigSize = originalConfig.lines().count();
        try {
            File config = FabricLoader.getInstance().getConfigDir().resolve(TakeBackTheNight.MOD_ID + "-common.toml").toFile();
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
                        for(int i = 0; i < configValues[1].length(); i++) if(configValues[1].charAt(i) != '.' && !Character.isDigit(configValues[1].charAt(i))) return;
                        double result = Double.parseDouble(configValues[1]);
                        switch (configValues[0]) {
                            case "maximum_health" -> TakeBackTheNight.HB_HP = result;
                            case "attack_damage" -> TakeBackTheNight.HB_ATK = result;
                            case "armor_points" -> TakeBackTheNight.HB_DEF = result;
                            case "movement_speed" -> TakeBackTheNight.HB_MOV = result;
                            case "follow_range" -> TakeBackTheNight.HB_FR = result;
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
                            case "maximum_health" -> configContent += TakeBackTheNight.HB_HP;
                            case "attack_damage" -> configContent += TakeBackTheNight.HB_ATK;
                            case "armor_points" -> configContent += TakeBackTheNight.HB_DEF;
                            case "movement_speed" -> configContent += TakeBackTheNight.HB_MOV;
                            case "follow_range" -> configContent += TakeBackTheNight.HB_FR;
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
        TakeBackTheNight.init();
        TakeBackTheNight.HEROBRINE = Registry.register(
                Registries.ENTITY_TYPE,
                Identifier.of(TakeBackTheNight.MOD_ID, "herobrine"),
                EntityType.Builder.create(HerobrineEntity::new, SpawnGroup.MONSTER).dimensions(0.6F, 1.8F).eyeHeight(1.62F).build("herobrine")
        );
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if(!player.isSpectator() && !player.isSneaking() && world.getBlockState(hitResult.getBlockPos()).isIn(BlockTags.INFINIBURN_NETHER)) {
                var pos = hitResult.getBlockPos().up();
                if(!(world.getBlockState(pos).getBlock() instanceof AbstractFireBlock)) world.setBlockState(pos, AbstractFireBlock.getState(world, pos));
                for(int i = -1; i <= 1; i++) for(int j = -1; j <= 1; j++) if(!world.getBlockState(pos.add(i, -2, j)).isIn(BlockTags.BEACON_BASE_BLOCKS)) return ActionResult.PASS;
                if(world.getBlockState(pos.down().east()).getBlock() instanceof TorchBlock && world.getBlockState(pos.down().west()).getBlock() instanceof TorchBlock && world.getBlockState(pos.down().south()).getBlock() instanceof TorchBlock && world.getBlockState(pos.down().north()).getBlock() instanceof TorchBlock) {
                    if(HerobrineEntity.activeHerobrine != -1) return ActionResult.FAIL;
                    HerobrineEntity herobrine = TakeBackTheNight.HEROBRINE.create(world);
                    if(herobrine == null) return ActionResult.PASS;
                    herobrine.refreshPositionAndAngles(pos, 0F, 90F);
                    world.spawnEntity(herobrine);
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
        FabricDefaultAttributeRegistry.register(TakeBackTheNight.HEROBRINE, HerobrineEntity.createHerobrineAttributes());
    }
}
