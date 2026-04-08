package com.artillerymod.network;

import com.artillerymod.ArtilleryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Central networking registry for Artillery Mod.
 *
 * One SimpleChannel handles all client↔server packets.
 * Call {@link #register()} once during mod initialisation.
 */
public final class ModNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ArtilleryMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int nextId = 0;

    /** Register all packets. Must be called on both logical sides. */
    public static void register() {
        CHANNEL.registerMessage(
                nextId++,
                HitchRequestPacket.class,
                HitchRequestPacket::encode,
                HitchRequestPacket::decode,
                HitchRequestPacket::handle
        );
    }

    private ModNetwork() {}
}
