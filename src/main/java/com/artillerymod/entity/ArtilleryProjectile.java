package com.artillerymod.entity;

import com.artillerymod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Artillery shell projectile – shared by both gun types.
 *
 * Ballistics are driven entirely by the {@link AmmoType}: each round has its
 * own velocity, gravity, damage, and area-blast flag.
 *
 * Physics notes:
 *   - Gravity deducted from Y velocity each tick (varies by ammo type).
 *   - Air drag: 0.1% per tick (negligible at game scales, but present).
 *   - Collision: hitscan-step each tick via ProjectileUtil so fast shells
 *     don't tunnel through thin walls.
 *   - The shell is INVISIBLE in flight (no renderer geometry).
 *
 * Ammo behaviour on impact:
 *   AP rounds (PZGR, PZGR_40, BR_240, BR_240P):
 *     • Pierce through up to MAX_PIERCE entities without stopping.
 *     • Each penetration reduces velocity by 30 % (energy loss).
 *     • Sparks + metallic clang on entity hit; no area blast.
 *     • Stopped by solid blocks (impact sound only).
 *
 *   HEAT round (STIELGRANATE_41):
 *     • Stops on first entity or block hit.
 *     • Creates a visible area explosion (power from AmmoType.blastRadius).
 */
public class ArtilleryProjectile extends Projectile {

    private static final EntityDataAccessor<Integer> DATA_AMMO_TYPE =
            SynchedEntityData.defineId(ArtilleryProjectile.class, EntityDataSerializers.INT);

    /** Ticks this shell has been alive – used for lifetime cutoff. */
    private int age = 0;

    /** Maximum lifetime before the shell despawns (200 ticks = 10 seconds). */
    private static final int MAX_AGE = 200;

    /**
     * Entity IDs that this AP shell has already penetrated.
     * Prevents the same entity from being hit multiple times while the
     * shell is still overlapping its hitbox.
     * Server-side only; not persisted to NBT (acceptable to reset on chunk reload).
     */
    private final Set<Integer> piercedIds = new HashSet<>();

    /** How many entities this shell has pierced so far. */
    private int pierceCount = 0;

    /** Maximum entities an AP round can punch through before stopping. */
    private static final int MAX_PIERCE = 5;

    public ArtilleryProjectile(EntityType<? extends ArtilleryProjectile> type, Level level) {
        super(type, level);
    }

    /** Factory constructor with immediate velocity assignment. */
    public ArtilleryProjectile(Level level, AmmoType ammo, double x, double y, double z,
                                double vx, double vy, double vz) {
        this(ModEntities.ARTILLERY_PROJECTILE.get(), level);
        this.setPos(x, y, z);
        this.setAmmoType(ammo);
        this.setDeltaMovement(vx, vy, vz);
    }

    // ── SynchedEntityData ─────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_AMMO_TYPE, AmmoType.NONE.getId());
    }

    public AmmoType getAmmoType() {
        return AmmoType.byId(entityData.get(DATA_AMMO_TYPE));
    }

    public void setAmmoType(AmmoType ammo) {
        entityData.set(DATA_AMMO_TYPE, ammo.getId());
    }

    // ── Tick / physics ────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            age++;
            if (age >= MAX_AGE) {
                this.discard();
                return;
            }
        }

        AmmoType ammo = getAmmoType();

        // Per-tick hitscan step (prevents tunnelling at high velocities)
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            this.onHit(hit);
            return; // Entity discarded in onHit; stop processing this tick
        }

        // Move
        Vec3 velocity = this.getDeltaMovement();
        this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);

        // Apply gravity
        float gravity = ammo != AmmoType.NONE ? ammo.getGravity() : 0.05f;
        this.setDeltaMovement(velocity.x, velocity.y - gravity, velocity.z);

        // Tiny air drag
        this.setDeltaMovement(getDeltaMovement().scale(0.999));

        // Update rotation to face direction of travel
        this.updateRotation();
    }

    /** Don't hit the firing entity or entities already pierced this flight. */
    @Override
    protected boolean canHitEntity(Entity candidate) {
        if (candidate == this.getOwner()) return false;
        if (piercedIds.contains(candidate.getId())) return false; // already penetrated
        if (!candidate.isPickable()) return false;
        if (candidate instanceof AbstractArtilleryEntity) return true;
        return super.canHitEntity(candidate);
    }

    // ── Hit handling ──────────────────────────────────────────────────────────

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;

        AmmoType ammo = getAmmoType();
        Entity target  = result.getEntity();

        // Apply damage
        DamageSource dmgSource = this.damageSources().thrown(this,
                this.getOwner() instanceof LivingEntity le ? le : this);
        target.hurt(dmgSource, ammo.getDamage());

        if (ammo.isAreaBlast()) {
            // ── HEAT round: area explosion, stop here ────────────────────────
            Vec3 knockVel = this.getDeltaMovement().normalize().scale(ammo.getKnockback() * 0.1);
            target.setDeltaMovement(target.getDeltaMovement().add(knockVel));
            doAreaBlast(this.position());
            this.discard();

        } else {
            // ── AP round: pierce through, continue flying ────────────────────
            // Record this entity so we don't hit it again while still overlapping
            piercedIds.add(target.getId());
            pierceCount++;

            // Energy loss per penetration: velocity reduced to 70 %
            this.setDeltaMovement(getDeltaMovement().scale(0.70));

            // Metallic impact sparks at the penetration point
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5,
                        target.getZ(), 12, 0.2, 0.2, 0.2, 0.15);
            }

            // Hard metallic clang
            level().playSound(null, target.blockPosition(),
                    SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0f,
                    1.4f + random.nextFloat() * 0.2f);

            // Stop piercing if maximum penetrations reached or shell too slow
            if (pierceCount >= MAX_PIERCE || getDeltaMovement().lengthSqr() < 0.01) {
                this.discard();
            }
            // Otherwise: stay alive and continue flying through
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide) return;

        AmmoType ammo = getAmmoType();

        if (ammo.isAreaBlast()) {
            // ── HEAT: visible area explosion on block impact ─────────────────
            doAreaBlast(result.getLocation());
        } else {
            // ── AP: hard stop, metallic impact sound + sparks ────────────────
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT,
                        result.getLocation().x, result.getLocation().y,
                        result.getLocation().z, 8, 0.1, 0.1, 0.1, 0.1);
            }
            level().playSound(null, BlockPos.containing(result.getLocation()),
                    SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5f, 1.2f);
        }

        this.discard();
    }

    /**
     * Controlled area blast for HEAT/shaped-charge round.
     * Power 2.5 matches roughly a creeper explosion.
     * MOB_GRIEF is respected; set explosion interaction to NONE to avoid
     * digging craters when mob griefing is disabled.
     *
     * Historically the Stielgranate 41 was a shaped-charge HEAT grenade,
     * so it creates a localised blast rather than wide terrain destruction.
     */
    private void doAreaBlast(Vec3 pos) {
        boolean doBreakBlocks = level().getGameRules()
                .getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);

        level().explode(
                this,
                pos.x, pos.y, pos.z,
                getAmmoType().getBlastRadius(),
                false,
                doBreakBlocks
                        ? Level.ExplosionInteraction.MOB
                        : Level.ExplosionInteraction.NONE
        );
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setAmmoType(AmmoType.byId(tag.getInt("AmmoType")));
        age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AmmoType", getAmmoType().getId());
        tag.putInt("Age", age);
    }
}
