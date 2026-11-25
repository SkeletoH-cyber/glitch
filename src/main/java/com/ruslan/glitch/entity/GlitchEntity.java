package com.ruslan.glitch.entity;

import com.ruslan.glitch.network.ScarePayload;
import com.ruslan.glitch.world.GlitchDirector;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Глич: бессмертная сущность. Её нельзя убить, можно только изгнать ритуалом (4 якоря на кресты). */
public class GlitchEntity extends HostileEntity {
    /** Сколько тиков он ещё пробудет в мире, потом "рассыпется" и исчезнет (до следующего появления). */
    public int lifeTicks = 1200;
    private int hopCooldown = 60;

    public GlitchEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createGlitchAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 7.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        World w = this.getWorld();
        if (w.isClient) {
            if (this.random.nextInt(3) == 0) {
                w.addParticle(ParticleTypes.REVERSE_PORTAL, this.getParticleX(0.5), this.getRandomBodyY(),
                        this.getParticleZ(0.5), 0, 0, 0);
            }
            return;
        }
        ServerWorld sw = (ServerWorld) w;
        if (--lifeTicks <= 0 || this.getY() < sw.getBottomY() - 10) {
            vanish(sw);
            return;
        }
        if (--hopCooldown <= 0) {
            hopCooldown = 50 + this.random.nextInt(100);
            glitchHop(sw);
        }
    }

    /** "Глич-прыжок": мгновенно оказывается рядом с целью. */
    private void glitchHop(ServerWorld sw) {
        LivingEntity target = this.getTarget();
        if (target == null || this.squaredDistanceTo(target) < 25) return;
        BlockPos spot = GlitchDirector.findSpotAround(sw, target.getBlockPos(), 3, 6);
        if (spot == null) return;
        GlitchDirector.puff(sw, this.getX(), this.getY(), this.getZ());
        this.getNavigation().stop();
        this.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, this.getYaw(), 0f);
        this.lookAtEntity(target, 360f, 360f);
        sw.playSound(null, spot, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.4f);
        GlitchDirector.puff(sw, this.getX(), this.getY(), this.getZ());
    }

    private void vanish(ServerWorld sw) {
        GlitchDirector.puff(sw, this.getX(), this.getY(), this.getZ());
        this.discard();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // /kill админа работает, всё остальное бессильно: Глич лишь "дёргается" и перескакивает
        if (source.isOf(DamageTypes.GENERIC_KILL)) return super.damage(source, amount);
        if (!this.getWorld().isClient && this.random.nextInt(3) == 0) {
            glitchHop((ServerWorld) this.getWorld());
        }
        return false;
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean ok = super.tryAttack(target);
        if (ok && target instanceof ServerPlayerEntity sp) {
            ServerPlayNetworking.send(sp, new ScarePayload(14, 1));
            sp.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0));
        }
        return ok;
    }

    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance effect) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_ENDERMAN_STARE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ENDERMAN_SCREAM;
    }
}
