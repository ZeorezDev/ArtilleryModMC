package com.artillerymod.registry;

import com.artillerymod.ArtilleryMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArtilleryMod.MODID);

    public static final RegistryObject<CreativeModeTab> ARTILLERY_TAB =
            CREATIVE_MODE_TABS.register("artillery_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.artillerymod.artillery"))
                            // Use iron block as the tab icon – no custom textures required.
                            .icon(() -> new ItemStack(ModItems.PAK36_ITEM.get()))
                            .displayItems((parameters, output) -> {
                                // Artillery placement items
                                output.accept(ModItems.PAK36_ITEM.get());
                                output.accept(ModItems.M1937_ITEM.get());
                                // Pak 36 ammo
                                output.accept(ModItems.PZGR.get());
                                output.accept(ModItems.PZGR_40.get());
                                output.accept(ModItems.STIELGRANATE_41.get());
                                // 53-K ammo
                                output.accept(ModItems.BR_240.get());
                                output.accept(ModItems.BR_240P.get());
                            })
                            .build());
}
