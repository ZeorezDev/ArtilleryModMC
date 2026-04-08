package com.artillerymod.registry;

import com.artillerymod.ArtilleryMod;
import com.artillerymod.entity.AmmoType;
import com.artillerymod.item.AmmoItem;
import com.artillerymod.item.ArtilleryPlacerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ArtilleryMod.MODID);

    // ── Artillery placement items ────────────────────────────────────────────

    /** Placement item for the 3.7 cm Pak 36 */
    public static final RegistryObject<Item> PAK36_ITEM =
            ITEMS.register("pak_36", () ->
                    new ArtilleryPlacerItem(ModEntities.PAK_36,
                            new Item.Properties().stacksTo(1)));

    /** Placement item for the 45 mm anti-tank gun M1937 (53-K) */
    public static final RegistryObject<Item> M1937_ITEM =
            ITEMS.register("m1937", () ->
                    new ArtilleryPlacerItem(ModEntities.M1937,
                            new Item.Properties().stacksTo(1)));

    // ── Pak 36 ammunition ────────────────────────────────────────────────────

    /**
     * PzGr. – Panzergranate, standard armour-piercing round for the Pak 36.
     * Steel core, full-calibre AP.
     */
    public static final RegistryObject<Item> PZGR =
            ITEMS.register("pzgr", () ->
                    new AmmoItem(AmmoType.PZGR, new Item.Properties().stacksTo(16)));

    /**
     * PzGr. 40 – Panzergranate 40, sub-calibre tungsten-core APCR round.
     * Higher penetration, lighter shell, slightly lower post-penetration damage.
     */
    public static final RegistryObject<Item> PZGR_40 =
            ITEMS.register("pzgr_40", () ->
                    new AmmoItem(AmmoType.PZGR_40, new Item.Properties().stacksTo(8)));

    /**
     * Stielgranate 41 – Stick grenade (rifle-grenade style large HEAT round).
     * Fitted over the muzzle; very slow flight, very high penetration on impact.
     * Historically used when normal AP rounds were insufficient against heavier armour.
     */
    public static final RegistryObject<Item> STIELGRANATE_41 =
            ITEMS.register("stielgranate_41", () ->
                    new AmmoItem(AmmoType.STIELGRANATE_41, new Item.Properties().stacksTo(4)));

    // ── 53-K ammunition ──────────────────────────────────────────────────────

    /**
     * BR-240 – Bronya-Razrushayushchiy 240, standard full-calibre AP round
     * for the 45 mm anti-tank gun M1937 (53-K).
     * (Also referred to as 53-BR-240 in Soviet documentation.)
     */
    public static final RegistryObject<Item> BR_240 =
            ITEMS.register("br_240", () ->
                    new AmmoItem(AmmoType.BR_240, new Item.Properties().stacksTo(16)));

    /**
     * BR-240P – Sub-calibre APCR round (tungsten-carbide core, 'P' for podkaliberny).
     * Higher muzzle velocity and penetration than BR-240, rarer and more expensive.
     */
    public static final RegistryObject<Item> BR_240P =
            ITEMS.register("br_240p", () ->
                    new AmmoItem(AmmoType.BR_240P, new Item.Properties().stacksTo(8)));
}
