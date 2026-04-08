package com.artillerymod.client;

import com.artillerymod.ArtilleryMod;
import com.artillerymod.client.renderer.ArtilleryProjectileRenderer;
import com.artillerymod.client.renderer.M1937Renderer;
import com.artillerymod.client.renderer.Pak36Renderer;
import com.artillerymod.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-only event subscriber.
 *
 * Registers entity renderers on the mod event bus.
 * These renderers use only vanilla BlockRenderDispatcher and ItemRenderer
 * calls – no custom model files of any kind.
 */
@Mod.EventBusSubscriber(modid = ArtilleryMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        ModKeyBindings.register(event);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PAK_36.get(),           Pak36Renderer::new);
        event.registerEntityRenderer(ModEntities.M1937.get(),            M1937Renderer::new);
        event.registerEntityRenderer(ModEntities.ARTILLERY_PROJECTILE.get(), ArtilleryProjectileRenderer::new);
    }
}
