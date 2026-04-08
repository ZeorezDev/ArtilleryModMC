package com.artillerymod.client;

import com.artillerymod.ArtilleryMod;
import com.artillerymod.network.HitchRequestPacket;
import com.artillerymod.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side handler that detects the hitch keybind and sends the
 * corresponding packet to the server.
 *
 * Subscribed on the FORGE (game) event bus so it receives per-tick input
 * events even when no screen is open.
 */
@Mod.EventBusSubscriber(
        modid = ArtilleryMod.MODID,
        bus   = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class HorseHitchHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();

        // Do nothing if no player is in-world or a screen is blocking input
        if (mc.player == null || mc.screen != null) return;

        if (ModKeyBindings.HITCH_KEY.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new HitchRequestPacket());
        }
    }

    private HorseHitchHandler() {}
}
