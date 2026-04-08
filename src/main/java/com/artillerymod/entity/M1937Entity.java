package com.artillerymod.entity;

import com.artillerymod.registry.ModEntities;
import com.artillerymod.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 45 mm anti-tank gun M1937 (53-K) – Soviet anti-tank gun, introduced 1937.
 *
 * Historical characteristics reflected in gameplay parameters:
 *   - Calibre:          45 mm
 *   - Weight in action: ~560 kg  → heavier, larger carriage
 *   - Rate of fire:     ~15–20 rpm (practical, well-trained crew)
 *   - Compatible ammo:  BR-240, BR-240P
 *
 * Loading ticks: 72 (≈3.6 s @ 20 TPS) – Soviet 45 mm was known for a
 *   relatively brisk loading drill; crew ergonomics were slightly better
 *   than the Pak 36 for repeat shots.
 * Cooldown ticks: 16 (0.8 s).
 *
 * Elevation range: -20° (up) to +5° (down).
 * Recoil: 0.45 blocks – heavier shell, longer recoil stroke.
 */
public class M1937Entity extends AbstractArtilleryEntity {

    public M1937Entity(EntityType<? extends M1937Entity> type, Level level) {
        super(type, level);
    }

    /** Convenience factory used by the entity type builder. */
    public M1937Entity(Level level) {
        this(ModEntities.M1937.get(), level);
    }

    // ── AbstractArtilleryEntity configuration ────────────────────────────────

    @Override
    protected int getMaxHealth() { return 35; }

    @Override
    protected int getLoadingTicks() { return 72; }

    @Override
    protected int getCooldownTicks() { return 16; }

    @Override
    protected float getMaxElevation() { return -20f; }

    @Override
    protected float getMaxDepression() { return 5f; }

    @Override
    protected float getMaxRecoil() { return 0.45f; }

    /**
     * The 53-K only accepts its own 45 mm ammunition.
     */
    @Override
    protected boolean isCompatibleAmmo(AmmoType ammo) {
        return ammo == AmmoType.BR_240
                || ammo == AmmoType.BR_240P;
    }

    @Override
    protected ItemStack getDropItem() {
        return new ItemStack(ModItems.M1937_ITEM.get());
    }

    /**
     * Muzzle at OBJ max-X = 34.4865, centered: 34.4865 - 1.7343 = 32.75 units.
     * After 90° rotation (+X → -Z) and scale 0.06: 32.75 × 0.06 ≈ 1.97 blocks forward.
     */
    @Override
    protected float getMuzzleForwardOffset() { return 2.0f; }

    /**
     * Muzzle Y center at max-X vertices ≈ 12.23 OBJ units.
     * After centering (+0.79) and scale 0.06: 13.02 × 0.06 ≈ 0.78 blocks above ground.
     */
    @Override
    protected float getMuzzleHeightOffset() { return 0.8f; }
}
