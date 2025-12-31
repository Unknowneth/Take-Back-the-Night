package com.notunanancyowen.herobrine.neoforge;

import com.notunanancyowen.herobrine.TakeBackTheNight;
import com.notunanancyowen.herobrine.client.HerobrineEntityRenderer;
import com.notunanancyowen.herobrine.entities.HerobrineEntity;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(TakeBackTheNight.MOD_ID)
public final class TakeBackTheNightNeoForge {
    public TakeBackTheNightNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        TakeBackTheNight.init();
        modEventBus.addListener(this::registerStuff);
        modEventBus.addListener(this::registerAttributes);
        NeoForge.EVENT_BUS.addListener(this::registerSummon);
        modContainer.registerConfig(ModConfig.Type.COMMON, TakeBackTheNightCommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, TakeBackTheNightClientConfig.SPEC);
    }
    private void registerStuff(final RegisterEvent event) {
        event.register(RegistryKeys.ENTITY_TYPE, registry -> registry.register(Identifier.of(TakeBackTheNight.MOD_ID, "herobrine"), TakeBackTheNight.HEROBRINE = EntityType.Builder.<HerobrineEntity>create(HerobrineEntity::new, SpawnGroup.MONSTER).dimensions(0.6F, 1.8F).eyeHeight(1.62F).build("herobrine")));
    }
    private void registerAttributes(final EntityAttributeCreationEvent event) {
        event.put(TakeBackTheNight.HEROBRINE, HerobrineEntity.createHerobrineAttributes().build());
    }
    private void registerSummon(final BlockEvent.EntityPlaceEvent event) {
        if(event.getEntity() instanceof PlayerEntity player && player.getWorld() instanceof World world && !player.isSpectator() && !player.isSneaking() && event.getPlacedAgainst().isIn(BlockTags.INFINIBURN_NETHER)) {
            var pos = event.getPos();
            if(!(world.getBlockState(pos).getBlock() instanceof AbstractFireBlock)) world.setBlockState(pos, AbstractFireBlock.getState(world, pos));
            for(int i = -1; i <= 1; i++) for(int j = -1; j <= 1; j++) if(!world.getBlockState(pos.add(i, -2, j)).isIn(BlockTags.BEACON_BASE_BLOCKS)) return;
            if(world.getBlockState(pos.down().east()).getBlock() instanceof TorchBlock && world.getBlockState(pos.down().west()).getBlock() instanceof TorchBlock && world.getBlockState(pos.down().south()).getBlock() instanceof TorchBlock && world.getBlockState(pos.down().north()).getBlock() instanceof TorchBlock) {
                if(HerobrineEntity.activeHerobrine != -1) return;
                HerobrineEntity herobrine = TakeBackTheNight.HEROBRINE.create(world);
                if(herobrine == null) return;
                herobrine.refreshPositionAndAngles(pos, 0F, 90F);
                world.spawnEntity(herobrine);
            }
        }
    }
    @EventBusSubscriber(modid = TakeBackTheNight.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent public static void addRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(TakeBackTheNight.HEROBRINE, HerobrineEntityRenderer::new);
        }
    }
}
