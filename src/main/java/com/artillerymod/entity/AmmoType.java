package com.artillerymod.entity;

import com.artillerymod.registry.ModItems;
import net.minecraft.world.item.Item;

/**
 * Defines all ammunition types with their ballistic and damage characteristics.
 *
 * Historical notes are included inline.
 *
 * Velocity is in blocks/tick (~20 ticks/sec).  Real muzzle velocities
 * are far higher; these values are scaled for Minecraft gameplay feel.
 * Gravity multiplier is subtracted from Y velocity each tick (0.05 = standard
 * ThrowableProjectile gravity).
 */
public enum AmmoType {

    NONE(0, "none", 0f, 0f, 0f, 0f, false, 0f),

    // ── 3.7 cm Pak 36 rounds ────────────────────────────────────────────────

    /**
     * PzGr. – Standard armour-piercing round.
     * Historical MV ≈ 745 m/s.  Solid AP with blunt cap.
     * Good damage, moderate velocity in-game.
     */
    PZGR(1, "pzgr", 4.0f, 0.04f, 22f, 3.5f, false, 0f),

    /**
     * PzGr. 40 – Sub-calibre tungsten-core APCR.
     * Historical MV ≈ 1020 m/s.  Faster, higher pen, lighter shell
     * → slightly lower post-penetration damage in-game.
     */
    PZGR_40(2, "pzgr_40", 5.5f, 0.03f, 28f, 2.5f, false, 0f),

    /**
     * Stielgranate 41 – Over-muzzle HEAT (shaped-charge) grenade.
     * Historical MV ≈ 110 m/s.  Very slow, very high penetration.
     * Small area blast on impact (concrete-breacher role).
     * Historically used as a stopgap against heavier armour.
     * Gravity set near-zero so the round flies flat (rocket-assisted feel).
     */
    STIELGRANATE_41(3, "stielgranate_41", 3.0f, 0.002f, 40f, 5.0f, true, 2.5f),

    // ── 45 mm M1937 (53-K) rounds ───────────────────────────────────────────

    /**
     * BR-240 – Standard full-calibre AP round for the 53-K.
     * Also documented as 53-BR-240.
     * Historical MV ≈ 760 m/s.  Solid performance, good damage.
     */
    BR_240(4, "br_240", 4.2f, 0.04f, 25f, 4.0f, false, 0f),

    /**
     * BR-240P – Sub-calibre APCR ('P' = podkaliberny / sub-calibre).
     * Tungsten-carbide core; higher MV ≈ 1070 m/s.
     * Higher penetration, lighter shell → slightly lower post-pen damage.
     */
    BR_240P(5, "br_240p", 5.8f, 0.03f, 32f, 3.0f, false, 0f);

    // ── Fields ───────────────────────────────────────────────────────────────

    /** Numeric ID used for EntityDataAccessor syncing. */
    private final int id;
    /** Registry name fragment – matches the Item registry key. */
    private final String registryName;
    /** Initial speed in blocks/tick. */
    private final float velocity;
    /** Gravity deducted from Y-velocity each tick. */
    private final float gravity;
    /** Direct-hit damage in half-hearts. */
    private final float damage;
    /** Knockback strength applied to hit entity. */
    private final float knockback;
    /** Whether this round produces a small area blast on impact. */
    private final boolean areaBlast;
    /** Blast radius in blocks (only relevant when areaBlast == true). */
    private final float blastRadius;

    AmmoType(int id, String registryName, float velocity, float gravity,
             float damage, float knockback, boolean areaBlast, float blastRadius) {
        this.id = id;
        this.registryName = registryName;
        this.velocity = velocity;
        this.gravity = gravity;
        this.damage = damage;
        this.knockback = knockback;
        this.areaBlast = areaBlast;
        this.blastRadius = blastRadius;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public int getId()          { return id; }
    public String getRegistryName() { return registryName; }
    public float getVelocity()  { return velocity; }
    public float getGravity()   { return gravity; }
    public float getDamage()    { return damage; }
    public float getKnockback() { return knockback; }
    public boolean isAreaBlast(){ return areaBlast; }
    public float getBlastRadius(){ return blastRadius; }

    /**
     * Returns the corresponding Item for this ammo type, or null for NONE.
     * Uses a switch to avoid circular class-loading issues with ModItems.
     */
    public Item getItem() {
        return switch (this) {
            case PZGR            -> ModItems.PZGR.get();
            case PZGR_40         -> ModItems.PZGR_40.get();
            case STIELGRANATE_41 -> ModItems.STIELGRANATE_41.get();
            case BR_240          -> ModItems.BR_240.get();
            case BR_240P         -> ModItems.BR_240P.get();
            default              -> null;
        };
    }

    public static AmmoType byId(int id) {
        for (AmmoType t : values()) {
            if (t.id == id) return t;
        }
        return NONE;
    }
}
