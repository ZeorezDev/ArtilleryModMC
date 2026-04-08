package com.artillerymod;

import com.artillerymod.network.ModNetwork;
import com.artillerymod.registry.ModCreativeTab;
import com.artillerymod.registry.ModEntities;
import com.artillerymod.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ArtilleryMod.MODID)
public class ArtilleryMod {

    public static final String MODID = "artillerymod";

    public ArtilleryMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTab.CREATIVE_MODE_TABS.register(modBus);

        // Register client↔server network channel and packets
        ModNetwork.register();
    }
}
