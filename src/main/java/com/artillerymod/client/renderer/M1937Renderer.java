package com.artillerymod.client.renderer;

import com.artillerymod.client.GenericObjModel;
import com.artillerymod.entity.M1937Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for the 45 mm anti-tank gun M1937 (53-K) using a real 3-D OBJ model.
 *
 * ─────────────────────────────────────────────────────────────────────
 * MODEL
 * ─────────────────────────────────────────────────────────────────────
 *   Source : Soviet artillery.obj  (Maya export, units in cm-scale)
 *   Stored : assets/artillerymod/geo/m1937.obj
 *
 *   Four material groups:
 *     lambert2SG  →  main chassis / hull
 *     lambert3SG  →  secondary chassis details
 *     lambert4SG  →  wheels
 *     lambert5SG  →  barrel assembly
 *
 * ─────────────────────────────────────────────────────────────────────
 * TEXTURES
 * ─────────────────────────────────────────────────────────────────────
 *   assets/artillerymod/textures/entity/m1937/lambert2.png
 *   assets/artillerymod/textures/entity/m1937/lambert3.png
 *   assets/artillerymod/textures/entity/m1937/lambert4.png
 *   assets/artillerymod/textures/entity/m1937/lambert5.png
 *
 * ─────────────────────────────────────────────────────────────────────
 * ORIENTATION
 * ─────────────────────────────────────────────────────────────────────
 *   Verified from OBJ vertex data:
 *     Max-X vertices (X≈34.5): Y∈[10.9, 13.6], Z∈[-1.3, +1.3] → narrow/high = muzzle
 *     Min-X vertices (X≈-31):  Y∈[4.9, 5.1],  Z∈[-15.7,+15.7] → wide/low   = trail legs
 *
 *   Barrel extends toward +X in OBJ space.
 *   AbstractArtilleryRenderer maps local -Z → aimed direction via (180-yaw) rotation.
 *   An additional +90° Y rotation inside renderGunParts maps model +X → local -Z. ✓
 *
 * ─────────────────────────────────────────────────────────────────────
 * SCALE
 * ─────────────────────────────────────────────────────────────────────
 *   Model X span ≈ 65.5 units.  At scale 0.06: total length ≈ 3.9 blocks.
 *   Muzzle height after centering ≈ 13.0 units × 0.06 ≈ 0.78 blocks.
 * ─────────────────────────────────────────────────────────────────────
 */
public class M1937Renderer extends AbstractArtilleryRenderer<M1937Entity> {

    private static final ResourceLocation MODEL =
            new ResourceLocation("artillerymod", "geo/m1937.obj");
    private static final ResourceLocation TEX_2 =
            new ResourceLocation("artillerymod", "textures/entity/m1937/lambert2.png");
    private static final ResourceLocation TEX_3 =
            new ResourceLocation("artillerymod", "textures/entity/m1937/lambert3.png");
    private static final ResourceLocation TEX_4 =
            new ResourceLocation("artillerymod", "textures/entity/m1937/lambert4.png");
    private static final ResourceLocation TEX_5 =
            new ResourceLocation("artillerymod", "textures/entity/m1937/lambert5.png");

    private static volatile GenericObjModel model;

    public M1937Renderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    /**
     * Scale 0.06 — model is exported in centimetre-scale Maya units.
     * At this scale the gun spans ≈ 3.9 blocks (barrel + trails).
     */
    @Override
    protected float getModelScale() { return 0.06f; }

    @Override
    protected void renderGunParts(M1937Entity entity, float partialTick,
                                   PoseStack ps, MultiBufferSource buf,
                                   int light, int overlay) {
        GenericObjModel m = model;
        if (m == null) {
            m = model = GenericObjModel.load(MODEL,
                    "lambert2SG", "lambert3SG", "lambert4SG", "lambert5SG");
        }

        // ── Align barrel to aimed direction ───────────────────────────────────
        // Model barrel extends toward +X.
        // AbstractArtilleryRenderer has already applied (180-gunYaw) rotation so
        // local -Z = aimed direction.  A +90° Y rotation maps model +X → local -Z.
        ps.mulPose(Axis.YP.rotationDegrees(90f));

        // ── Centre model on entity origin ─────────────────────────────────────
        ps.translate(m.offsetX, m.offsetY, m.offsetZ);

        // ── Draw all four material groups ─────────────────────────────────────
        // entitySolid enables backface culling (only outward-facing CCW faces render).
        // This is correct for a Blender-exported model with consistent CCW winding.
        m.render("lambert2SG", ps, buf.getBuffer(RenderType.entitySolid(TEX_2)), light, overlay);
        m.render("lambert3SG", ps, buf.getBuffer(RenderType.entitySolid(TEX_3)), light, overlay);
        m.render("lambert4SG", ps, buf.getBuffer(RenderType.entitySolid(TEX_4)), light, overlay);
        m.render("lambert5SG", ps, buf.getBuffer(RenderType.entitySolid(TEX_5)), light, overlay);
    }
}
