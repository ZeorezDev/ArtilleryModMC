package com.artillerymod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Configurable keybindings for Artillery Mod.
 *
 * All bindings appear under the "Artillery Mod" category in Minecraft's
 * Options → Controls screen and can be rebound freely.
 */
public final class ModKeyBindings {

    /**
     * H — Hitch / unhitch the nearest artillery piece to / from a horse.
     *
     * Historical basis: WW2 gun crews used a standardised quick-hitch
     * drill that could connect or disconnect a gun from its horse team
     * in under 60 seconds.  The single-button toggle reflects this.
     */
    public static final KeyMapping HITCH_KEY = new KeyMapping(
            "key.artillerymod.hitch",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.artillerymod"
    );

    /** Called from {@link ClientSetup} on the MOD event bus. */
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(HITCH_KEY);
    }

    private ModKeyBindings() {}
}
