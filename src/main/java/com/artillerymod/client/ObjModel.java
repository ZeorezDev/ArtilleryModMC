package com.artillerymod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight runtime OBJ loader for entity models.
 *
 * ─────────────────────────────────────────────────────────────────────
 * SUPPORTED OBJ DIRECTIVES
 * ─────────────────────────────────────────────────────────────────────
 *   v   x y z          Vertex position
 *   vt  u v            Texture coordinate  (V is flipped to OpenGL convention)
 *   vn  x y z          Vertex normal
 *   usemtl <name>      Switch material group  (only "pak40_chassis" / "pak40_canon")
 *   f   v/vt/vn ...    Face  (triangles or N-gons; N-gons are fan-triangulated)
 *
 * All other directives (mtllib, o, g, s, …) are silently ignored.
 *
 * ─────────────────────────────────────────────────────────────────────
 * VERTEX LAYOUT
 * ─────────────────────────────────────────────────────────────────────
 *   Geometry is pre-baked into a flat float array:
 *     [x, y, z,  u, v,  nx, ny, nz]  ×  vertex-count
 *
 *   This avoids per-frame index lookups and is passed directly to
 *   Minecraft's VertexConsumer in a tight loop.
 *
 * ─────────────────────────────────────────────────────────────────────
 * CENTERING
 * ─────────────────────────────────────────────────────────────────────
 *   After loading, offsetX/Y/Z hold the translation that should be
 *   applied before rendering so the model is:
 *     • centred on the X axis
 *     • sitting on Y = 0  (minimum Y of model → world ground level)
 *     • centred on the Z axis
 *
 *   Apply with  poseStack.translate(model.offsetX, model.offsetY, model.offsetZ)
 *   before calling renderChassis() / renderCanon().
 * ─────────────────────────────────────────────────────────────────────
 */
public class ObjModel {

    // Pre-baked vertex arrays: 8 floats per vertex [x,y,z, u,v, nx,ny,nz]
    private final float[] chassisVerts;
    private final float[] canonVerts;

    /** Translation that centres the model at the entity origin on the ground. */
    public final float offsetX, offsetY, offsetZ;

    private ObjModel(float[] chassisVerts, float[] canonVerts,
                     float ox, float oy, float oz) {
        this.chassisVerts = chassisVerts;
        this.canonVerts   = canonVerts;
        this.offsetX      = ox;
        this.offsetY      = oy;
        this.offsetZ      = oz;
    }

    // ── Loader ────────────────────────────────────────────────────────────────

    /**
     * Parse an OBJ file from the mod's resource manager.
     * The two expected material group names are hard-coded to the PAK 40 export.
     *
     * @param location  e.g. new ResourceLocation("artillerymod", "geo/pak36.obj")
     */
    public static ObjModel load(ResourceLocation location) {
        try (var is = Minecraft.getInstance().getResourceManager().open(location);
             var br = new BufferedReader(new InputStreamReader(is))) {

            // Raw geometry lists  (1-based indices in OBJ → store 0-based here)
            List<float[]> positions = new ArrayList<>(24_000);
            List<float[]> uvCoords  = new ArrayList<>(26_000);
            List<float[]> normals   = new ArrayList<>(24_000);

            // Per-material-group face lists.
            // Each entry = one triangle vertex: int[3] { posIdx, uvIdx, normIdx }
            // uvIdx / normIdx == -1 if absent.
            List<int[]> chassisFaces = new ArrayList<>(180_000);
            List<int[]> canonFaces   = new ArrayList<>(60_000);
            List<int[]> currentGroup = null;

            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() < 2) continue;
                char c0 = line.charAt(0);
                char c1 = line.charAt(1);

                if (c0 == 'v') {
                    if (c1 == ' ') {
                        // "v x y z"
                        String[] p = line.split("\\s+");
                        positions.add(new float[]{ pf(p[1]), pf(p[2]), pf(p[3]) });

                    } else if (c1 == 't') {
                        // "vt u v"
                        String[] p = line.split("\\s+");
                        uvCoords.add(new float[]{ pf(p[1]), pf(p[2]) });

                    } else if (c1 == 'n') {
                        // "vn nx ny nz"
                        String[] p = line.split("\\s+");
                        normals.add(new float[]{ pf(p[1]), pf(p[2]), pf(p[3]) });
                    }

                } else if (c0 == 'u' && line.startsWith("usemtl ")) {
                    String name = line.substring(7).trim();
                    if      (name.equals("pak40_chassis")) currentGroup = chassisFaces;
                    else if (name.equals("pak40_canon"))   currentGroup = canonFaces;
                    else                                   currentGroup = null;

                } else if (c0 == 'f' && c1 == ' ' && currentGroup != null) {
                    // "f v/vt/vn …"  — fan-triangulate from first vertex
                    String[] p = line.split("\\s+");
                    // p[0] = "f", p[1..n] = face vertices
                    int[] a = faceVert(p[1]);
                    int[] b = faceVert(p[2]);
                    for (int i = 3; i < p.length; i++) {
                        int[] c = faceVert(p[i]);
                        currentGroup.add(a);
                        currentGroup.add(b);
                        currentGroup.add(c);
                        b = c;
                    }
                }
            }

            // Compute bounding box for the centring offsets
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (float[] pos : positions) {
                if (pos[0] < minX) minX = pos[0];
                if (pos[0] > maxX) maxX = pos[0];
                if (pos[1] < minY) minY = pos[1];
                if (pos[2] < minZ) minZ = pos[2];
                if (pos[2] > maxZ) maxZ = pos[2];
            }
            // Translate so:  X centred,  Y minimum = 0 (on ground),  Z centred
            float ox = -(minX + maxX) * 0.5f;
            float oy = -minY;
            float oz = -(minZ + maxZ) * 0.5f;

            return new ObjModel(
                bake(chassisFaces, positions, uvCoords, normals),
                bake(canonFaces,   positions, uvCoords, normals),
                ox, oy, oz);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load OBJ model: " + location, e);
        }
    }

    /**
     * Parse one face-vertex token.
     * Formats: "v", "v/vt", "v/vt/vn", "v//vn"
     */
    private static int[] faceVert(String token) {
        String[] s = token.split("/", -1);
        int vi  = Integer.parseInt(s[0]) - 1;
        int vti = s.length > 1 && !s[1].isEmpty() ? Integer.parseInt(s[1]) - 1 : -1;
        int vni = s.length > 2 && !s[2].isEmpty() ? Integer.parseInt(s[2]) - 1 : -1;
        return new int[]{ vi, vti, vni };
    }

    /**
     * Flatten a face list into a pre-baked float array.
     * Each vertex becomes 8 consecutive floats: x y z  u v  nx ny nz
     *
     * OBJ UV V coordinate is bottom-up; OpenGL / Minecraft expects top-down.
     * We flip:  v_gl = 1.0 - v_obj
     */
    private static float[] bake(List<int[]> faces,
                                  List<float[]> positions,
                                  List<float[]> uvCoords,
                                  List<float[]> normals) {
        float[] data = new float[faces.size() * 8];
        for (int i = 0; i < faces.size(); i++) {
            int[]   f   = faces.get(i);
            float[] pos = positions.get(f[0]);
            float   u   = f[1] >= 0 ? uvCoords.get(f[1])[0]       : 0f;
            float   v   = f[1] >= 0 ? 1f - uvCoords.get(f[1])[1]  : 0f;  // V-flip
            float[] n   = f[2] >= 0 ? normals.get(f[2])            : new float[]{ 0f, 1f, 0f };
            int b = i * 8;
            data[b]   = pos[0];  data[b+1] = pos[1];  data[b+2] = pos[2];
            data[b+3] = u;       data[b+4] = v;
            data[b+5] = n[0];    data[b+6] = n[1];    data[b+7] = n[2];
        }
        return data;
    }

    private static float pf(String s) { return Float.parseFloat(s); }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /** Emit chassis geometry (body, wheels, trail legs, shield). */
    public void renderChassis(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        emit(chassisVerts, ps, vc, light, overlay);
    }

    /** Emit cannon geometry (barrel assembly). */
    public void renderCanon(PoseStack ps, VertexConsumer vc, int light, int overlay) {
        emit(canonVerts, ps, vc, light, overlay);
    }

    /**
     * Submit all vertices from a pre-baked array to a VertexConsumer.
     * The array layout (8 floats/vertex) matches the entitySolid render type's
     * POSITION_COLOR_TEX_OVERLAY_LIGHTMAP_UV_NORMAL vertex format.
     */
    private static void emit(float[] data, PoseStack ps,
                              VertexConsumer vc, int light, int overlay) {
        Matrix4f mat  = ps.last().pose();
        Matrix3f norm = ps.last().normal();
        int vertCount = data.length / 8;
        for (int i = 0; i < vertCount; i++) {
            int b = i * 8;
            vc.vertex(mat, data[b], data[b+1], data[b+2])
              .color(255, 255, 255, 255)
              .uv(data[b+3], data[b+4])
              .overlayCoords(overlay)
              .uv2(light)
              .normal(norm, data[b+5], data[b+6], data[b+7])
              .endVertex();
        }
    }
}
