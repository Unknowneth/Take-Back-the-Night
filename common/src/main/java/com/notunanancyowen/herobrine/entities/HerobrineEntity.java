package com.notunanancyowen.herobrine.entities;

import com.notunanancyowen.herobrine.TakeBackTheNight;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HerobrineEntity extends HostileEntity {
    public static int activeHerobrine = -1;
    public float tickDelta;
    private final ServerBossBar bossBar = (ServerBossBar)new ServerBossBar(this.getDisplayName(), BossBar.Color.BLUE, BossBar.Style.PROGRESS).setDarkenSky(true).setThickenFog(true);
    public static final TrackedData<Integer> TP_TIME = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> FLIP_TIME = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> ATTACK_TIME = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Integer> ATTACK_MODE = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final TrackedData<Vector3f> LASER_TARGET = DataTracker.registerData(HerobrineEntity.class, TrackedDataHandlerRegistry.VECTOR3F);
    public float previousAttackTime;
    public HerobrineEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 500;
    }
    public static DefaultAttributeContainer.Builder createHerobrineAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, TakeBackTheNight.HB_ATK)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, TakeBackTheNight.HB_MOV)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, TakeBackTheNight.HB_FR)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, TakeBackTheNight.HB_HP)
                .add(EntityAttributes.GENERIC_ARMOR, TakeBackTheNight.HB_DEF)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TP_TIME, -100);
        builder.add(FLIP_TIME, 0);
        builder.add(ATTACK_TIME, 0);
        builder.add(ATTACK_MODE, 0);
        builder.add(LASER_TARGET, new Vector3f());
    }
    public void fancyTeleport(Vec3d pos) {
        if(getWorld() instanceof ServerWorld server) {
            server.spawnParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.1);
            server.spawnParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 6, 0.1, 0.1, 0.1, 0.1);
        }
        playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1F, 1F);
        getDataTracker().set(TP_TIME, 10);
        //front port of OG tp code from pre 1.21
        if (this.getWorld() instanceof ServerWorld) {
            ChunkPos chunkPos = new ChunkPos(BlockPos.ofFloored(pos.x, pos.y, pos.z));
            ((ServerWorld)this.getWorld()).getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 0, this.getId());
            this.getWorld().getChunk(chunkPos.x, chunkPos.z);
            this.requestTeleport(pos.x, pos.y, pos.z);
        }
    }
    private void conjureFangs(double x, double y, double maxY, double z, float yaw, int warmup) {
        BlockPos blockPos = BlockPos.ofFloored(x, y, z);
        boolean bl = false;
        double d = 0.0;
        do {
            BlockPos blockPos2 = blockPos.down();
            BlockState blockState = getWorld().getBlockState(blockPos2);
            if (blockState.isSideSolidFullSquare(getWorld(), blockPos2, Direction.UP)) {
                if (!getWorld().isAir(blockPos)) {
                    BlockState blockState2 = getWorld().getBlockState(blockPos);
                    VoxelShape voxelShape = blockState2.getCollisionShape(getWorld(), blockPos);
                    if (!voxelShape.isEmpty()) d = voxelShape.getMax(Direction.Axis.Y);
                }
                bl = true;
                break;
            }
            blockPos = blockPos.down();
        } while (blockPos.getY() >= MathHelper.floor(maxY) - 1);
        if (bl) {
            getWorld().spawnEntity(new EvokerFangsEntity(getWorld(), x, blockPos.getY() + d, z, yaw, warmup, this));
            getWorld().emitGameEvent(GameEvent.ENTITY_PLACE, new Vec3d(x, blockPos.getY() + d, z), GameEvent.Emitter.of(this));
        }
    }
    private void blockWave(int x, int y, int z) {
        BlockPos blockPos = BlockPos.ofFloored(x, y, z);
        if(getWorld().getBlockState(blockPos).isIn(BlockTags.AIR)) return;
        boolean bl = false;
        int d = 0;
        do {
            BlockPos blockPos2 = blockPos.down();
            BlockState blockState = getWorld().getBlockState(blockPos2);
            if (blockState.isSideSolidFullSquare(getWorld(), blockPos2, Direction.UP)) {
                if (!getWorld().isAir(blockPos)) {
                    BlockState blockState2 = getWorld().getBlockState(blockPos);
                    VoxelShape voxelShape = blockState2.getCollisionShape(getWorld(), blockPos);
                    if (!voxelShape.isEmpty()) d = (int)voxelShape.getMax(Direction.Axis.Y);
                }
                bl = true;
                break;
            }
            blockPos = blockPos.down();
        } while (blockPos.getY() >= 63);
        if (bl) {
            var f = FallingBlockEntity.spawnFromBlock(getWorld(), blockPos.up(d - 1), getWorld().getBlockState(blockPos.up(d - 1)));
            f.shouldDupe = false;
            f.setVelocity(0, 0.2, 0);
            f.velocityDirty = true;
            f.velocityModified = true;
            getWorld().getOtherEntities(this, f.getBoundingBox().expand(0, 1, 0)).forEach(e -> {
                if(e.getId() == f.getId()) return;
                e.addVelocity(f.getVelocity());
                e.velocityDirty = true;
                e.velocityModified = true;
                e.damage(getDamageSources().fallingBlock(this), 15);
            });
            getWorld().emitGameEvent(GameEvent.ENTITY_PLACE, new Vec3d(x, blockPos.getY() + d, z), GameEvent.Emitter.of(this));
        }
    }
    @Override public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("FinishedSpawning", getDataTracker().get(TP_TIME) >= 0);
        nbt.putInt("FlipTime", getDataTracker().get(FLIP_TIME));
        nbt.putInt("AttackTime", getDataTracker().get(ATTACK_TIME));
        nbt.putInt("AttackMode", getDataTracker().get(ATTACK_MODE));
    }
    @Override public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if(nbt.getBoolean("FinishedSpawning") && getDataTracker().get(TP_TIME) < 0) getDataTracker().set(TP_TIME, 0);
        getDataTracker().set(FLIP_TIME, nbt.getInt("FlipTime"));
        getDataTracker().set(ATTACK_TIME, nbt.getInt("AttackTime"));
        getDataTracker().set(ATTACK_MODE, nbt.getInt("AttackMode"));
    }

    @Override public void onTrackedDataSet(TrackedData<?> data) {
        if(data.equals(ATTACK_TIME) && getDataTracker().get(ATTACK_TIME) == 0) getDataTracker().set(ATTACK_MODE, 0);
        if(data.equals(ATTACK_MODE) && getDataTracker().get(ATTACK_MODE) >= 1) getDataTracker().set(ATTACK_TIME, 1);
        super.onTrackedDataSet(data);
    }

    @Override protected void initGoals() {
        goalSelector.add(0, new Goal() {
            private int flipTime = 0;
            private int lastAttack2 = -1;
            private int lastAttack1 = -1;
            @Override public boolean canStart() {
                int t = getDataTracker().get(TP_TIME);
                if(t < -10) {
                    getLookControl().lookAt(getX(), getY(), getZ(), 0, -MathHelper.clamp(20 * (t + 50), -60, 60));
                    return false;
                }
                return getTarget() != null;
            }
            @Override public void stop() {
                getDataTracker().set(ATTACK_TIME, 0);
                getDataTracker().set(ATTACK_MODE, 0);
                getPassengerList().forEach(Entity::discard);
            }
            @Override public boolean shouldRunEveryTick() {
                return true;
            }
            @Override public void tick() {
                if(getTarget() == null) return;
                int i = getDataTracker().get(ATTACK_TIME);
                switch (getDataTracker().get(ATTACK_MODE)) {
                    case 1 -> {
                        getLookControl().lookAt(getTarget(), 0, 90);
                        if(i == 1) fancyTeleport(getTarget().getPos().subtract(getRotationVector(0, getYaw()).multiply(6)).add(0, 3, 0));
                        Vec3d particleOrigin = getEyePos().add(0, -0.3, 0);
                        if(i == 6 && getWorld() instanceof ServerWorld server) for(int j = 2; j < 16; j++) {
                            if(j == 2) playSound(SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 1F, 1F);
                            Vec3d particleSpawn = particleOrigin.add(getRotationVector().multiply(j));
                            server.spawnParticles(ParticleTypes.SONIC_BOOM, particleSpawn.x, particleSpawn.y, particleSpawn.z, 1, 0, 0, 0, 0);
                        }
                        if(i == 14) for(int j = 2; j < 16; j++) {
                            if(j == 2) playSound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1F, 1F);
                            Vec3d particleSpawn = particleOrigin.add(getRotationVector().multiply(j));
                            if(getTarget().squaredDistanceTo(particleSpawn) < 3) tryAttack(getTarget());
                        }
                        setNoGravity(i <= 22);
                        getDataTracker().set(ATTACK_TIME, i >= 30 ? 0 : i + 1);
                    }
                    case 2 -> {
                        setNoGravity(i < 59);
                        getLookControl().lookAt(getTarget(), i < 30 ? 90 : 0, 90);
                        if(i < 30) lookAtEntity(getTarget(), 30, 30);
                        if(i < 20) {
                            if(distanceTo(getTarget()) > 16) setVelocity(0, 1, 0);
                            else setVelocity(getRotationVector(0, getYaw()).add(0, -1, 0).multiply(-1));
                            velocityDirty = velocityModified = true;
                        }
                        else if(i == 20) {
                            refreshPosition();
                            playSound(SoundEvents.ENTITY_GHAST_WARN, 1F, 1F);
                            FireballEntity fireball = new FireballEntity(getWorld(), HerobrineEntity.this, Vec3d.ZERO, getWorld().getDifficulty().getId());
                            if(fireball.startRiding(HerobrineEntity.this)) getWorld().spawnEntity(fireball);
                        }
                        else if(i == 40 && getFirstPassenger() instanceof FireballEntity fireball) {
                            playSound(SoundEvents.ENTITY_GHAST_SHOOT, 10F, 1F);
                            fireball.stopRiding();
                            fireball.setVelocity(HerobrineEntity.this, getPitch(), getYaw(), 0, 2, 0);
                            fireball.velocityDirty = true;
                            fireball.velocityModified = true;
                        }
                        else if(i > 60) {
                            setVelocity(getRotationVector().multiply(0.4));
                            getLookControl().lookAt(getTarget(), 90, 90);
                            lookAtEntity(getTarget(), 90, 90);
                        }
                        else {
                            if(getWorld() instanceof ServerWorld server) {
                                server.spawnParticles(ParticleTypes.SMALL_FLAME, getX(), getY(), getZ(), 3, 0.1, 0.1, 0.1, 0.1);
                                server.spawnParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 3, 0.1, 0.1, 0.1, 0.1);
                                server.spawnParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 3, 0.1, 0.1, 0.1, 0.1);
                            }
                            setVelocity(Vec3d.ZERO);
                        }
                        getDataTracker().set(ATTACK_TIME, i >= 100 ? 0 : i + 1);
                    }
                    case 3 -> {
                        setNoGravity(i < 50);
                        if(hasNoGravity()) setVelocity(Vec3d.ZERO);
                        if(i == 1) {
                            fancyTeleport(getTarget().getEyePos());
                            playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 3F, 1F);
                        }
                        if(i == 40) getWorld().createExplosion(HerobrineEntity.this, getX(), getY(), getZ(), getWorld().getDifficulty().getId() + 3, World.ExplosionSourceType.MOB);
                        getDataTracker().set(ATTACK_TIME, i >= 60 ? 0 : i + 1);
                    }
                    case 4 -> {
                        setNoGravity(i < 88);
                        getLookControl().lookAt(getTarget(), 90, 90);
                        lookAtEntity(getTarget(), 90, 90);
                        if(!hasNoGravity()) setVelocity(getRotationVector().multiply(0.25));
                        else setVelocity(0, i < 15 ? 1 : 0, 0);
                        if(i <= 15 || !hasNoGravity()) velocityDirty = velocityModified = true;
                        if(i % 6 == 0 && i >= 18 && i <= 48) {
                            playSound(SoundEvents.ENTITY_SHULKER_SHOOT, 3F, 1F);
                            ShulkerBulletEntity shulk = new ShulkerBulletEntity(getWorld(), HerobrineEntity.this, null, Direction.Axis.pickRandomAxis(getRandom()));
                            shulk.setNoGravity(true);
                            shulk.setYaw(getRandom().nextBetween(-180, 180));
                            shulk.setPitch(getRandom().nextBetween(-90, 90));
                            if(shulk.startRiding(HerobrineEntity.this, true)) getWorld().spawnEntity(shulk);
                            if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.FLASH, shulk.getX(), shulk.getY(), shulk.getZ(), 0, 0, 0, 0, 0);
                        }
                        getDataTracker().set(ATTACK_TIME, i >= 120 ? 0 : i + 1);
                    }
                    case 5 -> {
                        if(i == 20) {
                            for(int k = 0; k < 4; k++) for(int j = 0; j < 8 * k; j++) conjureFangs(getX() + Math.cos(j / (8F * k) * Math.PI * 2) * k * 4, getY(), 16,getZ() + Math.sin(j / (8F * k) * Math.PI * 2) * k * 4, j / (8F * k) * 360, 10 * k);
                            playSound(SoundEvents.ENTITY_EVOKER_CAST_SPELL, 3F, 1F);
                            if(getWorld() instanceof ServerWorld server) server.spawnParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 16, 0.2, 0.1, 0.2, 0.4);
                        }
                        if(i > 20 && (i - 5) % 10 == 0) conjureFangs(getTarget().getX(), getY(), 64, getTarget().getZ(), getYaw(), 10);
                        getDataTracker().set(ATTACK_TIME, i >= 60 ? 0 : i + 1);
                    }
                    case 6 -> {
                        setNoGravity(i < 68);
                        getLookControl().lookAt(getTarget(), 90, 90);
                        lookAtEntity(getTarget(), 90, 90);
                        if(!hasNoGravity()) setVelocity(getRotationVector().multiply(0.25));
                        else setVelocity(0, i < 15 ? 1 : 0, 0);
                        if(i % 6 == 0 && i >= 18 && i <= 48) {
                            if(getWorld() instanceof ServerWorld server) {
                                server.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 9, 0.1, 0.1, 0.1, 0.1);
                                server.spawnParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 9, 0.1, 0.1, 0.1, 0.1);
                            }
                            playSound(SoundEvents.ENTITY_WITHER_SHOOT, 3F, 1F);
                            Vec3d randomDir = getRotationVector(getRandom().nextBetween(-90, 10), getRandom().nextBetween(-180, 180));
                            WitherSkullEntity skull = new WitherSkullEntity(getWorld(), HerobrineEntity.this, randomDir);
                            skull.setCharged(i % 12 == 0);
                            skull.setPosition(getEyePos());
                            getWorld().spawnEntity(skull);
                        }
                        if(i == 65) {
                            for(Entity e : getWorld().getEntitiesByClass(WitherSkullEntity.class, getBoundingBox().expand(128), w -> HerobrineEntity.this.equals(w.getOwner()) && w.age > 0)) {
                                Vec3d toTarget = getTarget().getPos().subtract(e.getPos()).normalize();
                                WitherSkullEntity skull = new WitherSkullEntity(getWorld(), HerobrineEntity.this, toTarget);
                                skull.setPosition(e.getPos());
                                skull.setCharged(e instanceof WitherSkullEntity w && w.isCharged());
                                getWorld().spawnEntity(skull);
                                e.discard();
                            }
                            playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 3F, 1F);
                        }
                        getDataTracker().set(ATTACK_TIME, i >= 80 ? 0 : i + 1);
                    }
                    case 7 -> {
                        setNoGravity(i < 59);
                        getLookControl().lookAt(getTarget(), i < 30 ? 90 : 0, 90);
                        if(i < 30) lookAtEntity(getTarget(), 30, 30);
                        if(i < 20) {
                            if(distanceTo(getTarget()) > 24) setVelocity(0, 0.25, 0);
                            else setVelocity(getRotationVector(0, getYaw()).add(0, -0.5, 0).multiply(-0.5));
                            velocityDirty = velocityModified = true;
                        }
                        else if(i == 20) {
                            for(int j = 0; j < 3; j++) {
                                DragonFireballEntity fireball = new DragonFireballEntity(getWorld(), HerobrineEntity.this, Vec3d.ZERO);
                                if(fireball.startRiding(HerobrineEntity.this, true)) getWorld().spawnEntity(fireball);
                            }
                            refreshPosition();
                            playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1F, 1F);
                        }
                        else if(i == 40) {
                            AtomicInteger k = new AtomicInteger(-1);
                            getPassengerList().forEach(e -> {
                                if(e instanceof DragonFireballEntity fireball) {
                                    fireball.stopRiding();
                                    fireball.setVelocity(HerobrineEntity.this, getPitch() + 5, getYaw() + k.get() * 30, 0, 1, 0);
                                    fireball.accelerationPower = 0.02;
                                    fireball.velocityDirty = true;
                                    fireball.velocityModified = true;
                                    k.set(k.get() + 1);
                                }
                            });
                            playSound(SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, 10F, 1F);
                        }
                        else if(i > 60) {
                            setVelocity(getRotationVector().multiply(0.4));
                            getLookControl().lookAt(getTarget(), 90, 90);
                            lookAtEntity(getTarget(), 90, 90);
                        }
                        else {
                            if(getWorld() instanceof ServerWorld server) {
                                server.spawnParticles(ParticleTypes.DRAGON_BREATH, getX(), getY(), getZ(), 3, 0.1, 0.1, 0.1, 0.1);
                                server.spawnParticles(ParticleTypes.PORTAL, getX(), getY(), getZ(), 3, 0.1, 0.1, 0.1, 0.1);
                                server.spawnParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 3, 0.1, 0.1, 0.1, 0.1);
                            }
                            setVelocity(Vec3d.ZERO);
                        }
                        getDataTracker().set(ATTACK_TIME, i >= 80 ? 0 : i + 1);
                    }
                    case 8 -> {
                        getLookControl().lookAt(getTarget(), 1, 90);
                        setNoGravity(i < 70);
                        if(i == 1) fancyTeleport(getTarget().getPos().subtract(getRotationVector(0, getYaw()).multiply(6)).add(0, 12, 0));
                        if(i % 6 == 0 && i >= 18 && i <= 60) {
                            PotionEntity potion = new PotionEntity(getWorld(), HerobrineEntity.this);
                            potion.setVelocity(getRotationVector().multiply(2));
                            potion.setItem(PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.HARMING));
                            if(getWorld().spawnEntity(potion)) swingHand(i % 12 == 0 ? Hand.OFF_HAND : Hand.MAIN_HAND);
                        }
                        getDataTracker().set(ATTACK_TIME, i >= 80 ? 0 : i + 1);
                    }
                    case 9 -> {
                        if(i == 1 && distanceTo(getTarget()) < 8) {
                            getDataTracker().set(FLIP_TIME, 10);
                            jumping = true;
                            setBodyYaw(getHeadYaw());
                            lookAtEntity(getTarget(), 90F, 1F);
                            getMoveControl().strafeTo(-1F, 0F);
                            setVelocity(getRotationVector(-10F, getYaw() + 180).multiply(1.75F));
                        }
                        if(getDataTracker().get(FLIP_TIME) != 0) getMoveControl().strafeTo(-1F, 0F);
                        if(i >= 60) {
                            riptideTicks = 0;
                            setNoGravity(false);
                            setVelocity(Vec3d.ZERO);
                        }
                        else if(i % 20 < 10) {
                            getLookControl().lookAt(getTarget(), 90F, 90F);
                            lookAtEntity(getTarget(), 30F, 90F);
                            if(hasNoGravity()) setVelocity(getVelocity().multiply(0.8F));
                        }
                        else if(i % 20 == 10) {
                            playSound((i == 10 ? SoundEvents.ITEM_TRIDENT_RIPTIDE_1 : i == 30 ? SoundEvents.ITEM_TRIDENT_RIPTIDE_2 : SoundEvents.ITEM_TRIDENT_RIPTIDE_3).value(), i / 20F + 1.5F, 0F);
                            setNoGravity(true);
                            setVelocity(getRotationVector().multiply(Math.max(distanceTo(getTarget()) * 0.25F, 2F)));
                            setPos(getX(), getEyeY(), getZ());
                            riptideTicks = 10;
                            riptideAttackDamage = 15F;
                            setLivingFlag(USING_RIPTIDE_FLAG, true);
                        }
                        getDataTracker().set(ATTACK_TIME, i >= 60 ? 0 : i + 1);
                    }
                    case 10 -> {
                        if(i == 1) {
                            playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 3F, 1F);
                            setNoGravity(true);
                            fancyTeleport(getTarget().getPos().add(0, 10, 0).subtract(getRotationVector(0, getYaw()).multiply(16)));
                            getDataTracker().set(LASER_TARGET, new Vec3d(getX(), getTarget().getY(), getZ()).add(getRotationVector(0, getYaw())).toVector3f());
                        }
                        Vec3d laserTarget = new Vec3d(getDataTracker().get(LASER_TARGET));
                        BlockPos blockPos = BlockPos.ofFloored(laserTarget);
                        boolean bl = false;
                        double d = 0.0;
                        do {
                            BlockPos blockPos2 = blockPos.down();
                            BlockState blockState = getWorld().getBlockState(blockPos2);
                            if (blockState.isSideSolidFullSquare(getWorld(), blockPos2, Direction.UP)) {
                                if (!getWorld().isAir(blockPos)) {
                                    BlockState blockState2 = getWorld().getBlockState(blockPos);
                                    VoxelShape voxelShape = blockState2.getCollisionShape(getWorld(), blockPos);
                                    if (!voxelShape.isEmpty()) d = voxelShape.getMax(Direction.Axis.Y);
                                }
                                bl = true;
                                break;
                            }
                            blockPos = blockPos.down();
                        } while (blockPos.getY() >= 63);
                        if(bl) getDataTracker().set(LASER_TARGET, laserTarget.add(0, d, 0).toVector3f());
                        if(i > 5) {
                            getDataTracker().set(LASER_TARGET, laserTarget.add(getRotationVector(0, getYaw())).toVector3f());
                            getWorld().createExplosion(HerobrineEntity.this, laserTarget.getX(), laserTarget.getY(), laserTarget.getZ(), 1 + (getWorld().getDifficulty().getId() - 1) / 2F, World.ExplosionSourceType.MOB);
                        }
                        else if(i == 5) playSound(SoundEvents.ENTITY_GUARDIAN_ATTACK, 3F, 1F);
                        getLookControl().lookAt(laserTarget);
                        if(i == 40) setNoGravity(false);
                        getDataTracker().set(ATTACK_TIME, i >= 40 ? 0 : i + 1);
                    }
                    case 11 -> {
                        if(i == 1) {
                            flipTime = -1;
                            getDataTracker().set(FLIP_TIME, -20);
                            setNoDrag(true);
                            setVelocity(getRotationVector(-40, getYaw()));
                        }
                        if(isOnGround() && hasNoDrag()) {
                            flipTime = i;
                            setNoDrag(false);
                        }
                        if(i > flipTime && flipTime > -1) {
                            int h = i - flipTime - 14;
                            if(h % 2 == 0) for(int j = -h / 2; j <= h / 2; j++) {
                                blockWave(getBlockX() + h / 2, getBlockY() - 1, getBlockZ() + j);
                                blockWave(getBlockX() - h / 2, getBlockY() - 1, getBlockZ() + j);
                                blockWave(getBlockX() + j, getBlockY() - 1, getBlockZ() + h / 2);
                                blockWave(getBlockX() + j, getBlockY() - 1, getBlockZ() - h / 2);
                            }
                        }
                        if(i == 40) {
                            flipTime = -1;
                            setNoDrag(false);
                        }
                        getDataTracker().set(ATTACK_TIME, i >= 40 ? 0 : i + 1);
                    }
                    default -> {
                        if(i >= 30) {
                            boolean phase2 = getHealth() < getMaxHealth() / 2;
                            boolean phase3 = getHealth() < getMaxHealth() / 4;
                            int attackCount = phase2 ? 6 : 5;
                            int l = getRandom().nextInt(attackCount);
                            while(l == lastAttack1 || l == lastAttack2) l = getRandom().nextInt(attackCount);
                            lastAttack2 = lastAttack1;
                            lastAttack1 = l;
                            switch(lastAttack1) {
                                case 0 -> getDataTracker().set(ATTACK_MODE, phase3 ? 11 : 1);
                                case 1 -> getDataTracker().set(ATTACK_MODE, phase2 ? 7 : 2);
                                case 2 -> getDataTracker().set(ATTACK_MODE, phase3 ? 10 : 3);
                                case 3 -> getDataTracker().set(ATTACK_MODE, phase2 ? 6 : 4);
                                case 4 -> getDataTracker().set(ATTACK_MODE, phase2 ? 8 : 5);
                                case 5 -> getDataTracker().set(ATTACK_MODE, 9);
                            }
                            getNavigation().stop();
                        }
                        else {
                            getDataTracker().set(ATTACK_TIME, i + 1);
                            getLookControl().lookAt(getTarget());
                            getNavigation().startMovingTo(getTarget(), 1.25);
                        }
                    }
                }
            }
        });
        targetSelector.add(0, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }
    @Override protected void tickRiptide(Box a, Box b) {
        Box box = a.union(b);
        List<Entity> list = getWorld().getOtherEntities(this, box);
        list.forEach(c -> {
            if(c instanceof LivingEntity l && tryAttack(l)) {
                LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(getWorld());
                if(lightning != null) {
                    lightning.setPosition(getPos());
                    lightning.setCosmetic(true);
                    getWorld().spawnEntity(lightning);
                }
                l.setOnFireForTicks(100);
            }
        });
        if(getWorld() instanceof ServerWorld serverWorld) serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 3, 0.1, 0.1, 0.1, 0.1);
        if(!this.getWorld().isClient && this.riptideTicks <= 0) {
            this.setLivingFlag(USING_RIPTIDE_FLAG, false);
            this.riptideAttackDamage = 0.0F;
            this.riptideStack = null;
        }
    }

    @Override protected void updatePassengerPosition(Entity passenger, PositionUpdater positionUpdater) {
        super.updatePassengerPosition(passenger, positionUpdater);
        if((passenger instanceof FireballEntity || passenger instanceof DragonFireballEntity) && getDataTracker().get(ATTACK_TIME) >= 20) {
            Vec3d fireballPos = getEyePos().subtract(0, -(getDataTracker().get(ATTACK_TIME) - 20) * 0.025, 0).add(getRotationVector(-Math.min(getDataTracker().get(ATTACK_TIME) - 20, 20) * 4.5F, bodyYaw).multiply(0.4));
            positionUpdater.accept(passenger, fireballPos.x, fireballPos.y, fireballPos.z);
        }
        if(passenger instanceof ShulkerBulletEntity) {
            Random random1 = Random.create(passenger.getUuid().getLeastSignificantBits());
            Random random2 = Random.create(passenger.getUuid().getMostSignificantBits());
            float rotationOffset = random1.nextFloat();
            int randomDirection = random2.nextBoolean() ? -passenger.age : passenger.age;
            float rotationOffset2 = random2.nextFloat();
            int randomDirection2 = random1.nextBoolean() ? passenger.age : -passenger.age;
            double pitchOffset = Math.cos(Math.PI * randomDirection2 / 40F + (getPitch() * Math.PI / 180) + rotationOffset2);
            Vec3d fireballPos = getEyePos().add((float)Math.cos(Math.PI * randomDirection / 40F + (getYaw() * Math.PI / 180) + rotationOffset) * passenger.age / 10F * pitchOffset, passenger.age / 10F * pitchOffset, (float)Math.sin(Math.PI * randomDirection / 40F + (getYaw() * Math.PI / 180) + rotationOffset) * passenger.age / 10F * pitchOffset);
            positionUpdater.accept(passenger, fireballPos.x, fireballPos.y, fireballPos.z);
            if(passenger.age > 40) {
                passenger.dismountVehicle();
                passenger.refreshPositionAndAngles(fireballPos.x, fireballPos.y, fireballPos.z, 0, 0);
                if(getTarget() != null) passenger.setVelocity(getTarget().getPos().subtract(fireballPos).normalize());
                else passenger.setVelocity(fireballPos.subtract(getPos()).normalize());
                passenger.velocityDirty = true;
                passenger.velocityModified = true;
            }
        }
    }
    @Override public boolean damage(DamageSource source, float amount) {
        if(getDataTracker().get(TP_TIME) < 0) return false;
        if(source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.UNATTRIBUTED_FIREBALL) || source.isOf(DamageTypes.FIREBALL) || source.isOf(DamageTypes.CRAMMING) || source.isOf(DamageTypes.CACTUS) || source.isOf(DamageTypes.BAD_RESPAWN_POINT) || source.isOf(DamageTypes.DROWN) || source.isOf(DamageTypes.HOT_FLOOR) || source.isOf(DamageTypes.LAVA) || source.isOf(DamageTypes.ON_FIRE) || source.isOf(DamageTypes.IN_FIRE) || source.isOf(DamageTypes.IN_WALL) || source.isOf(DamageTypes.LIGHTNING_BOLT)) return false;
        if(source.getSource() instanceof HostileEntity || source.getAttacker() instanceof HostileEntity) return false;
        return super.damage(source, amount);
    }
    @Override public void tick() {
        super.tick();
        activeHerobrine = getId();
        int f = getDataTracker().get(FLIP_TIME);
        if(f != 0) getDataTracker().set(FLIP_TIME, f - MathHelper.sign(f));
        int t = getDataTracker().get(TP_TIME);
        if(t > 0) getDataTracker().set(TP_TIME, t - 1);
        else if(t < 0) {
            if(t == -100) {
                LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(getWorld());
                if(lightning != null) {
                    lightning.setPosition(getPos());
                    lightning.setCosmetic(true);
                    getWorld().spawnEntity(lightning);
                }
            }
            if(t == -1) bossBar.setVisible(true);
            getDataTracker().set(TP_TIME, t + 1);
        }
        bossBar.setPercent(getHealth() / getMaxHealth());
        if(getTarget() instanceof ServerPlayerEntity serverPlayer) bossBar.addPlayer(serverPlayer);
    }
    @Override protected void updatePostDeath() {
        if(deathTime < 45) {
            setYaw(0);
            if(!getWorld().isClient()) deathTime++;
            if(deathTime == 1) {
                getDataTracker().set(ATTACK_TIME, 0);
                getDataTracker().set(ATTACK_MODE, 3);
            }
            int i = getDataTracker().get(ATTACK_TIME);
            setPitch(i > 40 ? -(i - 40) * 6 : Math.min(30, i * 2));
            getDataTracker().set(ATTACK_TIME, i + 1);
            setVelocity(0, 0.1, 0);
            setNoGravity(true);
            return;
        }
        getWorld().createExplosion(this, getX(), getY(), getZ(), 6, World.ExplosionSourceType.NONE);
        activeHerobrine = -1;
        bossBar.clearPlayers();
        bossBar.setVisible(false);
        bossBar.setDarkenSky(false);
        bossBar.setThickenFog(false);
        deathTime = 20;
        super.updatePostDeath();
    }
    @Override public void onRemoved() {
        activeHerobrine = -1;
        bossBar.clearPlayers();
        bossBar.setVisible(false);
        bossBar.setDarkenSky(false);
        bossBar.setThickenFog(false);
        super.onRemoved();
    }
    @Override public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
        this.bossBar.setVisible(getDataTracker().get(TP_TIME) >= 0);
    }
    @Override public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }
}
