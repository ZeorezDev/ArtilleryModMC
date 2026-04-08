package com.artillerymod.registry;

import com.artillerymod.ArtilleryMod;
import com.artillerymod.entity.ArtilleryProjectile;
import com.artillerymod.entity.M1937Entity;
import com.artillerymod.entity.Pak36Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ArtilleryMod.MODID);

    /**
     * 3.7 cm Pak 36 – German light anti-tank gun, 1936
     * Sized to approximate a compact wheeled AT gun in world space.
     */
    // Pak 36 visual at scale 1.4:  width ≈ 2.52×1.4 = 3.53 bl,  height ≈ 1.22×1.4 = 1.71 bl
    public static final RegistryObject<EntityType<Pak36Entity>> PAK_36 =
            ENTITIES.register("pak_36", () ->
                    EntityType.Builder.<Pak36Entity>of(Pak36Entity::new, MobCategory.MISC)
                            .sized(3.5F, 1.8F)
                            .clientTrackingRange(64)
                            .updateInterval(3)
                            .build("pak_36"));

    /**
     * 45 mm anti-tank gun M1937 (53-K) – Soviet AT gun, 1937
     * Slightly larger bounding box to reflect the larger calibre and carriage.
     */
    // M1937 OBJ at scale 0.06: Z-width=33.1×0.06=2.0bl, X-length=65.5×0.06=3.9bl, Y=1.36bl
    // After 90° rotation barrel→Z, the length becomes the Z-depth in world space.
    public static final RegistryObject<EntityType<M1937Entity>> M1937 =
            ENTITIES.register("m1937", () ->
                    EntityType.Builder.<M1937Entity>of(M1937Entity::new, MobCategory.MISC)
                            .sized(4.0F, 1.4F)
                            .clientTrackingRange(64)
                            .updateInterval(3)
                            .build("m1937"));

    /**
     * Shared artillery shell projectile entity.
     * Ammo type is carried in the entity's NBT/synced data.
     */
    public static final RegistryObject<EntityType<ArtilleryProjectile>> ARTILLERY_PROJECTILE =
            ENTITIES.register("artillery_projectile", () ->
                    EntityType.Builder.<ArtilleryProjectile>of(ArtilleryProjectile::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("artillery_projectile"));
}
