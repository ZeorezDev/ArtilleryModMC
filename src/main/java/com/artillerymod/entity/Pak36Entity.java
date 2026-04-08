package com.artillerymod.entity;

import com.artillerymod.registry.ModEntities;
import com.artillerymod.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 3.7 cm Pak 36 – German anti-tank gun, introduced 1936.
 *
 * Historical characteristics reflected in gameplay parameters:
 *   - Calibre:          37 mm
 *   - Weight in action: ~432 kg  → lighter, more compact → shorter loading time
 *   - Rate of fire:     ~15–20 rpm (practical)
 *   - Compatible ammo:  PzGr., PzGr. 40, Stielgranate 41
 *
 * Loading ticks: 92 (≈4.6 s @ 20 TPS) – within historical 3–4 s crew drill time.
 * Cooldown ticks: 20 (1 s) – case extraction + breech re-opening.
 *
 * Elevation range: -25° (up) to +5° (down).
 * Recoil: 0.35 blocks – lighter gun, shorter recoil stroke.
 */
public class Pak36Entity extends AbstractArtilleryEntity {

    public Pak36Entity(EntityType<? extends Pak36Entity> type, Level level) {
        super(type, level);
    }

    /** Convenience factory used by the entity type builder. */
    public Pak36Entity(Level level) {
        this(ModEntities.PAK_36.get(), level);
    }

    // ── AbstractArtilleryEntity configuration ────────────────────────────────

    @Override
    protected int getMaxHealth() { return 30; }

    @Override
    protected int getLoadingTicks() { return 92; }

    @Override
    protected int getCooldownTicks() { return 20; }

    @Override
    protected float getMaxElevation() { return -25f; }

    @Override
    protected float getMaxDepression() { return 5f; }

    @Override
    protected float getMaxRecoil() { return 0.35f; }

    /**
     * OBJ muzzle position (verified from vertex data):
     *   Z range [-2.9548, 3.4542] → center = +0.2497 → offsetZ = -0.2497
     *   Muzzle (min Z = -2.9548): centered → -3.2045 in local Z (forward direction)
     *   × scale 1.4 → 4.49 blocks forward from entity origin
     */
    @Override
    protected float getMuzzleForwardOffset() { return 4.5f; }

    /**
     * Muzzle Y in OBJ ≈ 0.91 (verified from min-Z vertices, Y ≈ 0.81-0.99).
     * After offsetY (≈+0.015) and scale 1.4: ≈ 1.28 blocks above entity ground Y.
     * In-game blue line was still too low at 1.3 → bumped to 1.8.
     */
    @Override
    protected float getMuzzleHeightOffset() { return 1.8f; }

    /**
     * The Pak 36 only accepts its own 37 mm ammunition.
     * STIELGRANATE_41 is also a 37 mm-family round (fitted over the muzzle).
     */
    @Override
    protected boolean isCompatibleAmmo(AmmoType ammo) {
        return ammo == AmmoType.PZGR
                || ammo == AmmoType.PZGR_40
                || ammo == AmmoType.STIELGRANATE_41;
    }

    @Override
    protected ItemStack getDropItem() {
        return new ItemStack(ModItems.PAK36_ITEM.get());
    }
}
