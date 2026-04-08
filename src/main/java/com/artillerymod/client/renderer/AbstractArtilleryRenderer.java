package com.artillerymod.client.renderer;

import com.artillerymod.entity.AbstractArtilleryEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla-only artillery entity renderer.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * COORDINATE SYSTEM
 * ═══════════════════════════════════════════════════════════════════════
 *
 *  When render() is called, the PoseStack origin is already at the
 *  entity's interpolated world position (bottom-centre of bounding box,
 *  i.e. ground level).  After the yaw rotation applied here:
 *
 *    X+ = gun's local right
 *    Y+ = up
 *    Z+ = gun's local rear  (trail direction)
 *    Z- = gun's local front (barrel / muzzle direction)
 *
 *  All coordinates in subclass renderGunParts() are relative to this
 *  origin.  No extra global translation is applied.
 *
 * ═══════════════════════════════════════════════════════════════════════
 * VANILLA-ONLY RENDERING
 * ═══════════════════════════════════════════════════════════════════════
 *
 *  Every part is drawn with BlockRenderDispatcher.renderSingleBlock() or
 *  ItemRenderer.renderStatic().  No custom model files or textures exist.
 *
 *  renderBlock(state, tx, ty, tz, w, h, d) places a block whose:
 *    • bottom-left-front corner is at (tx, ty, tz)
 *    • size is (w × h × d)
 *  Use tx = cx - w/2  and  tz = cz - d/2  to centre on (cx, *, cz).
 *
 * ═══════════════════════════════════════════════════════════════════════
 */
public abstract class AbstractArtilleryRenderer<T extends AbstractArtilleryEntity>
        extends EntityRenderer<T> {

    protected final BlockRenderDispatcher blockRenderer;
    protected final ItemRenderer          itemRenderer;

    protected AbstractArtilleryRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blockRenderer  = Minecraft.getInstance().getBlockRenderer();
        this.itemRenderer   = ctx.getItemRenderer();
        this.shadowStrength = 0.8f;
        // Shadow radius is updated per-subclass after getModelScale() is known.
        this.shadowRadius   = getModelScale() * 0.6f;
    }

    /**
     * Uniform scale factor applied to the entire gun model so it appears
     * human-sized (≈ player model, 1.8 blocks) in the world.
     *
     * Pak 36  → 1.4  (light, compact German gun – wheel ~0.78 bl, waist-high shield)
     * 53-K    → 1.6  (heavier Soviet carriage  – wheel ~1.02 bl, chest-high shield)
     *
     * Override in subclasses to tune per-gun.
     */
    protected abstract float getModelScale();

    // ── Main render entry point ───────────────────────────────────────────────

    @Override
    public void render(T entity, float entityYaw, float partialTick,
                       PoseStack ps, MultiBufferSource buf, int light) {

        ps.pushPose();

        // Rotate the entire assembly so the barrel faces the gun's current yaw.
        // We use entity.getYRot() directly (not the interpolated entityYaw parameter)
        // to stay in sync with the server-side projectile direction calculation which
        // also uses entity.getYRot().
        //
        // Minecraft yaw convention:
        //   yaw=0   → facing SOUTH (+Z)
        //   yaw=90  → facing WEST  (-X)
        //   yaw=180 → facing NORTH (-Z)
        //   yaw=270 → facing EAST  (+X)
        //
        // Our model has the barrel along local -Z (NORTH end_rod).
        // Rotation = -(yaw) maps local -Z onto the correct world direction:
        //   yaw=0  → 0°  rotation → local -Z stays at world -Z? No…
        //
        // Actually the correct mapping for local -Z → bullet direction:
        //   bullet_x = -sin(yaw),  bullet_z = cos(yaw)
        //   at yaw=0: bullet goes (0,0,+1) = SOUTH
        //   rotating local -Z = (0,0,-1) by (180-yaw):
        //     yaw=0 → 180° → (0,0,+1) = SOUTH  ✓
        //   This is the vanilla boat/minecart convention.
        //
        // Use entity.getYRot() directly to avoid interpolation lag.
        float gunYaw = entity.getYRot();
        ps.mulPose(Axis.YP.rotationDegrees(180.0f - gunYaw));

        // Scale the entire assembly to human/player-model size.
        // Applied after yaw so the scale is uniform in all axes regardless
        // of the gun's facing direction.
        float s = getModelScale();
        ps.scale(s, s, s);

        // Recoil: push the whole gun slightly rearward (+Z, in scaled model space)
        // when fired.  The world-space recoil distance = recoilOffset × modelScale.
        float recoil = entity.getRecoilOffset();
        if (recoil > 0f) {
            ps.translate(0, 0, recoil);
        }

        renderGunParts(entity, partialTick, ps, buf, light, OverlayTexture.NO_OVERLAY);

        ps.popPose();

        super.render(entity, entityYaw, partialTick, ps, buf, light);
    }

    /**
     * Subclasses implement this to draw their specific parts.
     * Origin (0, 0, 0) = entity centre at ground level, after yaw rotation.
     */
    protected abstract void renderGunParts(T entity, float partialTick,
                                            PoseStack ps, MultiBufferSource buf,
                                            int light, int overlay);

    // ── Rendering helpers ─────────────────────────────────────────────────────

    /**
     * Renders a block with its bottom-left-front corner at (tx, ty, tz)
     * and size (w × h × d).
     *
     * To centre a block horizontally at (cx, cz) use tx = cx - w/2, tz = cz - d/2.
     */
    protected void renderBlock(BlockState state,
                                float tx, float ty, float tz,
                                float w,  float h,  float d,
                                PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(tx, ty, tz);
        ps.scale(w, h, d);
        blockRenderer.renderSingleBlock(state, ps, buf, light, overlay);
        ps.popPose();
    }

    /**
     * Renders a block centred at (cx, *, cz), bottom at ty.
     * Convenience wrapper around renderBlock().
     */
    protected void renderBlockCentered(BlockState state,
                                        float cx, float ty, float cz,
                                        float w,  float h,  float d,
                                        PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        renderBlock(state, cx - w * 0.5f, ty, cz - d * 0.5f, w, h, d, ps, buf, light, overlay);
    }

    /**
     * Renders a block with an additional rotation applied around the Y axis
     * before scaling.  Useful for angled trail legs.
     */
    protected void renderBlockRotY(BlockState state,
                                    float cx, float ty, float cz,
                                    float w,  float h,  float d,
                                    float rotYDeg,
                                    PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(cx, ty, cz);
        ps.mulPose(Axis.YP.rotationDegrees(rotYDeg));
        ps.translate(-w * 0.5f, 0, 0);   // keep centred on the pivot after rotation
        ps.scale(w, h, d);
        blockRenderer.renderSingleBlock(state, ps, buf, light, overlay);
        ps.popPose();
    }

    /** Renders an item stack at a given position with uniform scale. */
    protected void renderItem(ItemStack stack,
                               float cx, float cy, float cz, float scale,
                               PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(cx, cy, cz);
        ps.scale(scale, scale, scale);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ps, buf, null, 0);
        ps.popPose();
    }

    // ── EntityRenderer requirement ────────────────────────────────────────────

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        // Unused – all rendering done via BlockRenderDispatcher / ItemRenderer
        return new ResourceLocation("minecraft", "textures/block/iron_block.png");
    }
}
