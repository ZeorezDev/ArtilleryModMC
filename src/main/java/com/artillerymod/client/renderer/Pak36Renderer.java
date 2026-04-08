package com.artillerymod.client.renderer;

import com.artillerymod.client.ObjModel;
import com.artillerymod.entity.Pak36Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for the 3.7 cm Pak 36 entity using a real 3-D OBJ model.
 *
 * ─────────────────────────────────────────────────────────────────────
 * MODEL
 * ─────────────────────────────────────────────────────────────────────
 *   Source : Pak40.fbx  →  Pak 40 model.obj  (Blender export)
 *   Stored : assets/artillerymod/geo/pak36.obj
 *
 *   Two material groups:
 *     pak40_chassis  →  body, wheels, trail legs, shield
 *     pak40_canon    →  barrel assembly
 *
 * ─────────────────────────────────────────────────────────────────────
 * TEXTURES
 * ─────────────────────────────────────────────────────────────────────
 *   assets/artillerymod/textures/entity/pak36/chassis.png
 *   assets/artillerymod/textures/entity/pak36/canon.png
 *   (converted from pak40_*_BaseColor.jpg by the convertAndCopyAssets task)
 *
 * ─────────────────────────────────────────────────────────────────────
 * ORIENTATION
 * ─────────────────────────────────────────────────────────────────────
 *   Verified from OBJ vertex data:
 *     Min-Z vertices (Z≈-2.95): Y≈0.9 (high), X≈0 (centred) → barrel/muzzle
 *     Max-Z vertices (Z≈+3.45): Y≈0 (low), X≈±1.1 (wide)    → trail legs
 *
 *   Barrel extends toward -Z in OBJ space.
 *   AbstractArtilleryRenderer already maps local -Z → aimed direction
 *   via its (180 − entityYaw) rotation.  No additional flip needed.
 *
 * ─────────────────────────────────────────────────────────────────────
 * BARREL PITCH ANIMATION
 * ─────────────────────────────────────────────────────────────────────
 *   Not yet applied to the OBJ model — the trunnion pivot point
 *   (where the barrel rotates on the chassis) needs to be read from
 *   the model first.  The gun still rotates correctly in yaw (horizontal).
 *   Barrel elevation animation can be added once the pivot coords are known.
 * ─────────────────────────────────────────────────────────────────────
 */
public class Pak36Renderer extends AbstractArtilleryRenderer<Pak36Entity> {

    private static final ResourceLocation MODEL =
            new ResourceLocation("artillerymod", "geo/pak36.obj");
    private static final ResourceLocation TEX_CHASSIS =
            new ResourceLocation("artillerymod", "textures/entity/pak36/chassis.png");
    private static final ResourceLocation TEX_CANON =
            new ResourceLocation("artillerymod", "textures/entity/pak36/canon.png");

    /** Lazily initialised on first render; safe because rendering is single-threaded. */
    private static volatile ObjModel model;

    public Pak36Renderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    /**
     * Scale 1.4× to match the human-sized block model this replaces.
     * At this scale the PAK 40 OBJ (native ~1.32 bl wide) becomes ~1.85 bl wide.
     */
    @Override
    protected float getModelScale() { return 1.4f; }

    @Override
    protected void renderGunParts(Pak36Entity entity, float partialTick,
                                   PoseStack ps, MultiBufferSource buf,
                                   int light, int overlay) {
        // Load on first render (resource manager is ready by the time any entity renders)
        ObjModel m = model;
        if (m == null) {
            m = model = ObjModel.load(MODEL);
        }

        // ── Positioning ───────────────────────────────────────────────────────
        // Barrel already faces local -Z (no extra rotation needed).
        // Translate so the model is centred on X/Z and sits on the ground.
        ps.translate(m.offsetX, m.offsetY, m.offsetZ);

        // ── Draw chassis (body, wheels, trail legs, shield) ───────────────────
        // entityCutoutNoCull disables backface culling so all faces are visible
        // regardless of winding order in the exported OBJ.
        m.renderChassis(ps,
                buf.getBuffer(RenderType.entityCutoutNoCull(TEX_CHASSIS)),
                light, overlay);

        // ── Draw cannon (barrel assembly) ─────────────────────────────────────
        // No barrel pitch rotation applied yet — see class-level javadoc.
        m.renderCanon(ps,
                buf.getBuffer(RenderType.entityCutoutNoCull(TEX_CANON)),
                light, overlay);
    }
}
