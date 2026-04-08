package com.artillerymod.client.renderer;

import com.artillerymod.entity.ArtilleryProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Artillery shell projectile renderer — intentionally invisible.
 *
 * The shell has realistic ballistic physics (gravity arc, air drag) but
 * no visible geometry: only the explosion/impact effect is seen.
 * This matches the feel of real anti-tank fire where the shell itself
 * is invisible to the naked eye at game distances.
 */
public class ArtilleryProjectileRenderer extends EntityRenderer<ArtilleryProjectile> {

    public ArtilleryProjectileRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    /** Render nothing – shell is invisible in flight. */
    @Override
    public void render(ArtilleryProjectile entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Intentionally empty – no visual representation for the shell.
    }

    @Override
    public ResourceLocation getTextureLocation(ArtilleryProjectile entity) {
        return new ResourceLocation("minecraft", "textures/misc/blank.png");
    }
}
