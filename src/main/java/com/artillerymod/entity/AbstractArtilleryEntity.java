package com.artillerymod.entity;

import com.artillerymod.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * Base class for all artillery entities.
 *
 * ─────────────────────────────────────────────────────────────────────
 * INTERACTION REFERENCE
 * ─────────────────────────────────────────────────────────────────────
 *
 *   Shift + Right-click (ammo in hand)   → load one round
 *   Shift + Right-click (empty hand)     → show HP and breech status
 *   Right-click         (empty hand)     → fire (if LOADED)
 *   Left-click          (empty hand)     → pick gun back up into inventory
 *   Left-click          (weapon/tool)    → deal damage to the gun
 *   Attack damage (explosion, etc.)      → deal damage to the gun
 *
 * ─────────────────────────────────────────────────────────────────────
 * HEALTH & DESTRUCTION
 * ─────────────────────────────────────────────────────────────────────
 *
 *   Each gun has a hidden HP pool (Pak36 = 30, 53-K = 35).
 *   When HP reaches zero the gun:
 *     1. Triggers a small explosion (visual + sound, no terrain damage)
 *     2. Scatters fire and smoke particles around the wreck
 *     3. Drops its placement item so the player can recover it
 *
 *   Left-clicking with an empty hand always picks the gun up safely
 *   (0 damage, gun returned to inventory).
 * ─────────────────────────────────────────────────────────────────────
 */
public abstract class AbstractArtilleryEntity extends Entity {

    // ── Synced data ───────────────────────────────────────────────────────────

    private static final EntityDataAccessor<Integer> DATA_BREECH_STATE =
            SynchedEntityData.defineId(AbstractArtilleryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AMMO_TYPE =
            SynchedEntityData.defineId(AbstractArtilleryEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RELOAD_TIMER =
            SynchedEntityData.defineId(AbstractArtilleryEntity.class, EntityDataSerializers.INT);
    /** Barrel pitch in degrees – synced so renderer shows correct elevation. */
    private static final EntityDataAccessor<Float> DATA_BARREL_PITCH =
            SynchedEntityData.defineId(AbstractArtilleryEntity.class, EntityDataSerializers.FLOAT);
    /** Recoil offset in blocks – decays each tick, synced for renderer. */
    private static final EntityDataAccessor<Float> DATA_RECOIL =
            SynchedEntityData.defineId(AbstractArtilleryEntity.class, EntityDataSerializers.FLOAT);
    /** Current HP – synced so action-bar display is accurate on client. */
    private static final EntityDataAccessor<Integer> DATA_HEALTH =
            SynchedEntityData.defineId(AbstractArtilleryEntity.class, EntityDataSerializers.INT);

    /**
     * Entity ID of the horse currently towing this gun, or -1 if not towed.
     * Synced to clients so they can know the tow state (e.g. for rendering).
     */
    private static final EntityDataAccessor<Integer> DATA_TOW_HORSE_ID =
            SynchedEntityData.defineId(AbstractArtilleryEntity.class, EntityDataSerializers.INT);

    // ── Tow (horse-drawn) constant ────────────────────────────────────────────

    /**
     * Fixed distance (blocks) from the horse's centre to the gun's entity origin
     * along the horse's reverse-facing direction.
     *
     * Horse body ≈ 0.7 bl centre→rear + a small coupling gap ≈ 1.3 bl total.
     * This places the front of the gun flush against the horse's back end.
     */
    private static final double ATTACH_OFFSET = 1.3;

    // ── Abstract configuration ────────────────────────────────────────────────

    protected abstract int getMaxHealth();

    /**
     * LOADING phase (ticks):
     *   Pak 36  → 92 ticks  (~4.6 s)
     *   53-K    → 72 ticks  (~3.6 s)
     */
    protected abstract int getLoadingTicks();

    /**
     * COOLDOWN phase (ticks, post-fire case extraction):
     *   Pak 36  → 20 ticks
     *   53-K    → 16 ticks
     */
    protected abstract int getCooldownTicks();

    /** Max upward elevation (negative = up).  Pak 36: -25°, 53-K: -20° */
    protected abstract float getMaxElevation();

    /** Max downward depression (positive = down).  Both: +5° */
    protected abstract float getMaxDepression();

    /** Max recoil offset in blocks.  Pak 36: 0.35, 53-K: 0.45 */
    protected abstract float getMaxRecoil();

    /**
     * Distance along the barrel (in world blocks) from the entity origin to
     * the muzzle tip.  Used to spawn the projectile at the correct position.
     *
     * Block-model guns (M1937):  ~1.4 bl
     * OBJ-model guns  (Pak 36):  ~2.0 bl  (muzzle at Z≈-1.425 × scale 1.4)
     */
    protected float getMuzzleForwardOffset() { return 1.4f; }

    /**
     * Height of the muzzle above the entity's Y origin (ground level), in
     * world blocks, at zero barrel pitch.
     *
     * Block-model guns (M1937):  0.9 bl
     * OBJ-model guns  (Pak 36):  0.6 bl  (barrel centroid Y≈0.43 × scale 1.4)
     */
    protected float getMuzzleHeightOffset() { return 0.9f; }

    // ── Constructor ───────────────────────────────────────────────────────────

    protected AbstractArtilleryEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    // ── SynchedEntityData ─────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_BREECH_STATE, BreechState.EMPTY.getId());
        entityData.define(DATA_AMMO_TYPE,    AmmoType.NONE.getId());
        entityData.define(DATA_RELOAD_TIMER, 0);
        entityData.define(DATA_BARREL_PITCH, 0f);
        entityData.define(DATA_RECOIL,       0f);
        entityData.define(DATA_HEALTH,       0); // initialised after construction
        entityData.define(DATA_TOW_HORSE_ID, -1); // -1 = not towed
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    public BreechState getBreechState() {
        return BreechState.byId(entityData.get(DATA_BREECH_STATE));
    }

    public void setBreechState(BreechState state) {
        entityData.set(DATA_BREECH_STATE, state.getId());
    }

    public AmmoType getAmmoType() {
        return AmmoType.byId(entityData.get(DATA_AMMO_TYPE));
    }

    public void setAmmoType(AmmoType type) {
        entityData.set(DATA_AMMO_TYPE, type.getId());
    }

    public int getReloadTimer() {
        return entityData.get(DATA_RELOAD_TIMER);
    }

    public void setReloadTimer(int ticks) {
        entityData.set(DATA_RELOAD_TIMER, ticks);
    }

    public float getBarrelPitch() {
        return entityData.get(DATA_BARREL_PITCH);
    }

    public void setBarrelPitch(float pitch) {
        entityData.set(DATA_BARREL_PITCH, pitch);
    }

    public float getRecoilOffset() {
        return entityData.get(DATA_RECOIL);
    }

    public void setRecoilOffset(float offset) {
        entityData.set(DATA_RECOIL, offset);
    }

    public int getHealth() {
        return entityData.get(DATA_HEALTH);
    }

    private void setHealth(int hp) {
        entityData.set(DATA_HEALTH, hp);
    }

    // ── Tow accessors ─────────────────────────────────────────────────────────

    /** Returns the entity ID of the towing horse, or -1 if not currently towed. */
    public int getTowHorseId() {
        return entityData.get(DATA_TOW_HORSE_ID);
    }

    private void setTowHorseId(int id) {
        entityData.set(DATA_TOW_HORSE_ID, id);
    }

    /**
     * Attaches this gun to the horse with the given entity ID.
     * Called server-side from {@link com.artillerymod.network.HitchRequestPacket}.
     */
    public void attachToHorse(int horseEntityId) {
        setTowHorseId(horseEntityId);
    }

    /**
     * Removes the tow link.  Safe to call even when not currently towed.
     */
    public void detachFromHorse() {
        setTowHorseId(-1);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Called once when the entity enters the world for the first time.
     * We initialise HP here (after the entity type's abstract method is callable).
     */
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide && getHealth() == 0) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide && getTowHorseId() != -1) {
            // ── Towed: rigid attachment, horse drives everything ──────────
            tickTow();
            // No gravity or move() — position is set directly by tickTow().
        } else {
            // ── Free-standing: normal gravity + terrain movement ──────────
            if (!this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
            } else {
                this.setDeltaMovement(Vec3.ZERO);
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
        }

        if (!level().isClientSide) {
            tickBreech();
        }
    }

    // ── Horse-tow: rigid attachment (server-only) ─────────────────────────────

    /**
     * Positions the gun exactly behind the horse every tick.
     *
     * The gun is treated as a rigidly-coupled attachment rather than a towed
     * trailer so it moves in perfect synchrony with the horse:
     *
     *   • Gun position = horse centre − horse-forward × ATTACH_OFFSET
     *   • Gun Y        = horse.getY()  (same ground level, no floating)
     *   • Gun yaw      = horse yaw     (barrel faces direction of travel)
     *
     * Because setPos() is called every tick on the server, entity-tracking
     * packets keep the client position up-to-date within one update interval.
     */
    private void tickTow() {
        int horseId = getTowHorseId();
        if (horseId == -1) return;

        Entity entity = level().getEntity(horseId);
        if (!(entity instanceof AbstractHorse horse) || !horse.isAlive()) {
            detachFromHorse();
            return;
        }

        // Direction horse is facing (Minecraft yaw: 0 = South, 90 = West)
        float  horseYaw = horse.getYRot();
        double yawRad   = Math.toRadians(horseYaw);
        double forwardX = -Math.sin(yawRad);   // unit vector forward
        double forwardZ =  Math.cos(yawRad);

        // Gun sits directly behind the horse's body centre
        double gunX = horse.getX() - forwardX * ATTACH_OFFSET;
        double gunZ = horse.getZ() - forwardZ * ATTACH_OFFSET;
        double gunY = horse.getY();   // stay at the horse's ground level

        this.setPos(gunX, gunY, gunZ);
        this.setYRot(horseYaw + 180f); // barrel points forward (away from horse)
        this.setDeltaMovement(Vec3.ZERO); // suppress residual physics
    }

    // ── Breech state machine (server-only) ────────────────────────────────────

    private void tickBreech() {
        BreechState state = getBreechState();

        if (state == BreechState.LOADING) {
            int timer = getReloadTimer() - 1;
            if (timer <= 0) {
                setBreechState(BreechState.LOADED);
                setReloadTimer(0);
                level().playSound(null, this.blockPosition(),
                        SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.2f);
            } else {
                setReloadTimer(timer);
            }

        } else if (state == BreechState.FIRING) {
            // Projectile already spawned in tryFire(); transition to cooldown.
            setBreechState(BreechState.COOLDOWN);
            setReloadTimer(getCooldownTicks());
            setRecoilOffset(getMaxRecoil());
            level().playSound(null, this.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0f,
                    0.6f + random.nextFloat() * 0.2f);
            level().playSound(null, this.blockPosition(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.5f, 0.5f);

        } else if (state == BreechState.COOLDOWN) {
            int timer = getReloadTimer() - 1;
            if (timer <= 0) {
                setBreechState(BreechState.EMPTY);
                setReloadTimer(0);
                level().playSound(null, this.blockPosition(),
                        SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0f, 0.8f);
            } else {
                setReloadTimer(timer);
            }
        }

        // Decay recoil
        float recoil = getRecoilOffset();
        if (recoil > 0f) {
            setRecoilOffset(Math.max(0f, recoil - 0.03f));
        }
    }

    // ── Projectile spawning ───────────────────────────────────────────────────

    private void spawnProjectile(Player owner) {
        AmmoType ammo = getAmmoType();
        if (ammo == AmmoType.NONE) return;

        ArtilleryProjectile shell = new ArtilleryProjectile(
                ModEntities.ARTILLERY_PROJECTILE.get(), level());
        shell.setAmmoType(ammo);
        if (owner != null) shell.setOwner(owner);

        float yawRad   = (float) Math.toRadians(this.getYRot());
        float pitchRad = (float) Math.toRadians(this.getBarrelPitch());
        double vx = -Math.sin(yawRad) * Math.cos(pitchRad);
        double vy = -Math.sin(pitchRad);
        double vz =  Math.cos(yawRad) * Math.cos(pitchRad);

        double fwd = getMuzzleForwardOffset();
        double mx = this.getX() + vx * fwd;
        // Spawn Y is fixed at the barrel centerline height regardless of pitch.
        // The vy component of the initial velocity handles the trajectory arc;
        // we must NOT multiply vy × fwd here because fwd is large (~4.5 bl) and
        // even a tiny downward pitch would push the spawn below ground level.
        double my = this.getY() + getMuzzleHeightOffset();
        double mz = this.getZ() + vz * fwd;
        shell.setPos(mx, my, mz);

        float speed = ammo.getVelocity();
        shell.setDeltaMovement(vx * speed, vy * speed, vz * speed);

        ((ServerLevel) level()).addFreshEntity(shell);
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    @Nonnull
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);

        if (player.isSecondaryUseActive()) {
            // Shift + right-click with ammo → load
            if (held.getItem() instanceof com.artillerymod.item.AmmoItem ammoItem) {
                return tryLoadAmmo(player, held, ammoItem.getAmmoType());
            }
            // Shift + right-click empty hand → show HP / status
            if (held.isEmpty()) {
                showStatus(player);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Right-click empty hand → fire
        if (held.isEmpty()) {
            return tryFire(player);
        }

        return InteractionResult.PASS;
    }

    private void showStatus(Player player) {
        int hp    = getHealth();
        int maxHp = getMaxHealth();

        // Build a simple health bar: ■ filled, □ empty
        int barLen    = 10;
        int filled    = Math.round((float) hp / maxHp * barLen);
        StringBuilder bar = new StringBuilder("§c");
        for (int i = 0; i < barLen; i++) {
            if (i == filled) bar.append("§7");
            bar.append(i < filled ? "■" : "□");
        }

        String breechDesc = switch (getBreechState()) {
            case EMPTY    -> "§7Empty";
            case LOADING  -> "§e Loading (" + getReloadTimer() + "t)";
            case LOADED   -> "§aLoaded: §f" + getAmmoType().getRegistryName().toUpperCase()
                                                            .replace('_', ' ');
            case FIRING   -> "§cFIRING";
            case COOLDOWN -> "§6Cooling (" + getReloadTimer() + "t)";
        };

        player.displayClientMessage(
                Component.literal("§6HP: §f" + hp + "§7/§f" + maxHp
                        + " " + bar + "   §6Breech: " + breechDesc),
                true);
    }

    private InteractionResult tryLoadAmmo(Player player, ItemStack held, AmmoType ammo) {
        if (!isCompatibleAmmo(ammo)) {
            player.displayClientMessage(
                    Component.translatable("artillerymod.hint.wrong_ammo"), true);
            return InteractionResult.FAIL;
        }
        if (getBreechState() != BreechState.EMPTY) {
            player.displayClientMessage(
                    Component.translatable("artillerymod.hint.breech_busy"), true);
            return InteractionResult.FAIL;
        }

        if (!player.isCreative()) held.shrink(1);

        setAmmoType(ammo);
        setBreechState(BreechState.LOADING);
        setReloadTimer(getLoadingTicks());

        level().playSound(null, this.blockPosition(),
                SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.4f);
        player.displayClientMessage(
                Component.translatable("artillerymod.hint.loading",
                        Component.translatable("item.artillerymod." + ammo.getRegistryName())),
                true);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult tryFire(Player player) {
        if (getBreechState() != BreechState.LOADED) {
            String key = switch (getBreechState()) {
                case EMPTY    -> "artillerymod.hint.no_ammo";
                case LOADING  -> "artillerymod.hint.still_loading";
                case COOLDOWN -> "artillerymod.hint.cooling";
                default       -> "artillerymod.hint.not_ready";
            };
            player.displayClientMessage(Component.translatable(key), true);
            return InteractionResult.FAIL;
        }

        this.setYRot(player.getYRot());
        setBarrelPitch(Mth.clamp(player.getXRot(), getMaxElevation(), getMaxDepression()));
        spawnProjectile(player);
        setAmmoType(AmmoType.NONE);
        setBreechState(BreechState.FIRING);
        return InteractionResult.SUCCESS;
    }

    protected abstract boolean isCompatibleAmmo(AmmoType ammo);

    // ── Damage, pickup, and destruction ───────────────────────────────────────

    @Override
    public boolean isPickable() { return true; }

    /**
     * Handles all damage and the left-click pickup mechanic.
     *
     * Left-click with EMPTY main hand → safe pickup into inventory (no damage).
     * Left-click with any item / tool  → deal damage to the gun.
     * Any other source (explosion, etc.) → deal damage to the gun.
     *
     * When HP drops to 0 the gun explodes and is replaced by a dropped item.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || this.isRemoved()) return false;

        // ── Left-click pickup ──────────────────────────────────────────────
        if (source.getEntity() instanceof Player player
                && player.getMainHandItem().isEmpty()
                && source == this.damageSources().playerAttack(player)) {
            // Cannot pick up while towed — unhitch first
            if (getTowHorseId() != -1) {
                player.displayClientMessage(
                        Component.literal("§7[Artillery] §cUnhitch from horse first (press H)."),
                        true);
                return false;
            }
            pickupGun(player);
            return true;
        }

        // ── Normal damage ──────────────────────────────────────────────────
        int newHp = getHealth() - (int) Math.max(1, amount);
        setHealth(Math.max(0, newHp));

        if (getHealth() <= 0) {
            destroyWithExplosion();
        }
        return true;
    }

    /**
     * Returns the gun to the player's inventory (or drops it if full).
     * Plays an item-pickup sound as feedback.
     */
    private void pickupGun(Player player) {
        // Ensure tow is removed before the entity is discarded
        detachFromHorse();
        ItemStack item = getDropItem();
        if (!player.getInventory().add(item)) {
            // Inventory full – drop at the player's position
            player.drop(item, false);
        }
        level().playSound(null, player.blockPosition(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                0.2f, ((random.nextFloat() - random.nextFloat()) * 0.7f + 1.0f) * 2.0f);
        this.discard();
    }

    /**
     * Destroys the gun with a dramatic explosion effect:
     *   • Small explosion (power 2.0, no terrain damage, respects mob-grief rule)
     *   • Flame particles scattered around the chassis
     *   • Large smoke column
     *   • Secondary crackle sounds
     *
     * The placement item is dropped so the player can collect the wreck.
     */
    private void destroyWithExplosion() {
        double x = getX(), y = getY() + 0.6, z = getZ();

        // Drop the item and remove the entity BEFORE the explosion so that
        // when Level.explode() sweeps nearby entities for blast damage it
        // finds isRemoved() == true on this entity and skips it entirely.
        // Without this, the explosion re-enters hurt() → destroyWithExplosion()
        // → StackOverflowError.
        this.spawnAtLocation(getDropItem());
        this.discard();

        // Main explosion (no block damage)
        level().explode(null, x, y, z, 2.0f, false, Level.ExplosionInteraction.NONE);

        if (level() instanceof ServerLevel serverLevel) {
            // Scattered fire bursts
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    x, y, z, 35, 0.55, 0.40, 0.55, 0.12);
            // Large smoke column
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    x, y + 0.5, z, 20, 0.40, 0.60, 0.40, 0.03);
            // Sparks
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    x, y, z, 12, 0.50, 0.30, 0.50, 0.0);
            // Secondary smaller explosions (scattered)
            for (int i = 0; i < 4; i++) {
                double ox = (random.nextDouble() - 0.5) * 1.2;
                double oy = random.nextDouble() * 0.8;
                double oz = (random.nextDouble() - 0.5) * 1.2;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        x + ox, y + oy, z + oz, 1, 0, 0, 0, 0);
            }
        }

        // Extra crackle and fire sounds
        level().playSound(null, this.blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 3.0f, 0.7f);
        level().playSound(null, this.blockPosition(),
                SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.5f, 0.8f);
    }

    protected abstract ItemStack getDropItem();

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        int savedHp = tag.getInt("Health");
        setHealth(savedHp > 0 ? savedHp : getMaxHealth());
        setBreechState(BreechState.byId(tag.getInt("BreechState")));
        setAmmoType(AmmoType.byId(tag.getInt("AmmoType")));
        setReloadTimer(tag.getInt("ReloadTimer"));
        setBarrelPitch(tag.getFloat("BarrelPitch"));
        this.setYRot(tag.getFloat("GunYaw"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Health",      getHealth());
        tag.putInt("BreechState", getBreechState().getId());
        tag.putInt("AmmoType",    getAmmoType().getId());
        tag.putInt("ReloadTimer", getReloadTimer());
        tag.putFloat("BarrelPitch", getBarrelPitch());
        tag.putFloat("GunYaw",      this.getYRot());
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    @Override public boolean isPushable() { return false; }
    @Override public boolean isOnFire()   { return false; }
}
