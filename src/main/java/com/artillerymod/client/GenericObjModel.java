package com.artillerymod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Generic runtime OBJ loader that supports an arbitrary set of named material groups.
 *
 * Unlike ObjModel (which is hard-coded for pak40_chassis / pak40_canon), this class
 * accepts any list of material names at load time and stores each group's triangles
 * in a separate pre-baked float[] array for efficient per-group rendering.
 *
 * Usage:
 * <pre>
 *   GenericObjModel m = GenericObjModel.load(location, "mat1", "mat2", "mat3");
 *   m.render("mat1", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX1)), light, overlay);
 *   m.render("mat2", ps, buf.getBuffer(RenderType.entityCutoutNoCull(TEX2)), light, overlay);
 * </pre>
 *
 * Centering offsets (offsetX/Y/Z) are computed from the full model bounding box
 * and should be applied via {@code poseStack.translate(m.offsetX, m.offsetY, m.offsetZ)}
 * before any render() call.
 */
public class GenericObjModel {

    /** Pre-baked vertex arrays keyed by material name. 8 floats/vertex: x y z u v nx ny nz */
    private final Map<String, float[]> groups;

    /** Translation that centres the model at the entity origin on the ground. */
    public final float offsetX, offsetY, offsetZ;

    private GenericObjModel(Map<String, float[]> groups, float ox, float oy, float oz) {
        this.groups  = groups;
        this.offsetX = ox;
        this.offsetY = oy;
        this.offsetZ = oz;
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Load an OBJ file and collect only the specified material groups.
     *
     * @param location   ResourceLocation of the .obj file
     * @param groupNames Material names to collect (usemtl tokens in the OBJ)
     */
    public static GenericObjModel load(ResourceLocation location, String... groupNames) {
        Set<String> wanted = new HashSet<>(Arrays.asList(groupNames));

        try (var is = Minecraft.getInstance().getResourceManager().open(location);
             var br = new BufferedReader(new InputStreamReader(is))) {

            List<float[]> positions = new ArrayList<>(20_000);
            List<float[]> uvCoords  = new ArrayList<>(22_000);
            List<float[]> normals   = new ArrayList<>(20_000);

            // One face list per collected group
            Map<String, List<int[]>> faceLists = new LinkedHashMap<>();
            for (String name : groupNames) faceLists.put(name, new ArrayList<>(50_000));

            List<int[]> currentGroup = null;

            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() < 2) continue;
                char c0 = line.charAt(0);
                char c1 = line.charAt(1);

                if (c0 == 'v') {
                    if (c1 == ' ') {
                        String[] p = line.split("\\s+");
                        positions.add(new float[]{ pf(p[1]), pf(p[2]), pf(p[3]) });
                    } else if (c1 == 't') {
                        String[] p = line.split("\\s+");
                        uvCoords.add(new float[]{ pf(p[1]), pf(p[2]) });
                    } else if (c1 == 'n') {
                        String[] p = line.split("\\s+");
                        normals.add(new float[]{ pf(p[1]), pf(p[2]), pf(p[3]) });
                    }

                } else if (c0 == 'u' && line.startsWith("usemtl ")) {
                    String name = line.substring(7).trim();
                    currentGroup = wanted.contains(name) ? faceLists.get(name) : null;

                } else if (c0 == 'f' && c1 == ' ' && currentGroup != null) {
                    String[] p = line.split("\\s+");
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

            // Bounding box for centring
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
            float ox = -(minX + maxX) * 0.5f;
            float oy = -minY;
            float oz = -(minZ + maxZ) * 0.5f;

            // Bake each group
            Map<String, float[]> baked = new LinkedHashMap<>();
            for (Map.Entry<String, List<int[]>> e : faceLists.entrySet()) {
                baked.put(e.getKey(), bake(e.getValue(), positions, uvCoords, normals));
            }

            return new GenericObjModel(baked, ox, oy, oz);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load OBJ model: " + location, e);
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Emit all triangles belonging to the named material group.
     * No-op if the group name was not collected during load.
     */
    public void render(String groupName, PoseStack ps, VertexConsumer vc, int light, int overlay) {
        float[] data = groups.get(groupName);
        if (data == null || data.length == 0) return;
        emit(data, ps, vc, light, overlay);
    }

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

    // ── OBJ parsing helpers ───────────────────────────────────────────────────

    private static int[] faceVert(String token) {
        String[] s = token.split("/", -1);
        int vi  = Integer.parseInt(s[0]) - 1;
        int vti = s.length > 1 && !s[1].isEmpty() ? Integer.parseInt(s[1]) - 1 : -1;
        int vni = s.length > 2 && !s[2].isEmpty() ? Integer.parseInt(s[2]) - 1 : -1;
        return new int[]{ vi, vti, vni };
    }

    private static float[] bake(List<int[]> faces,
                                 List<float[]> positions,
                                 List<float[]> uvCoords,
                                 List<float[]> normals) {

        // ── Winding-order correction ───────────────────────────────────────────
        // Some Maya/Blender exports mix CW and CCW triangles.
        // For each triangle: compute the geometric face normal via cross product
        // and compare it with the stated vertex normal.  If they are in opposite
        // hemispheres (dot < 0), swap vertices B and C to flip the winding to CCW.
        // This lets entitySolid (backface culling) render all faces correctly.
        for (int i = 0; i + 2 < faces.size(); i += 3) {
            int[] fa = faces.get(i);
            int[] fb = faces.get(i + 1);
            int[] fc = faces.get(i + 2);

            // Need a stated normal to compare against
            if (fa[2] < 0) continue;

            float[] pa = positions.get(fa[0]);
            float[] pb = positions.get(fb[0]);
            float[] pc = positions.get(fc[0]);

            // Edge vectors
            float e1x = pb[0] - pa[0], e1y = pb[1] - pa[1], e1z = pb[2] - pa[2];
            float e2x = pc[0] - pa[0], e2y = pc[1] - pa[1], e2z = pc[2] - pa[2];

            // Cross product → geometric face normal (not normalised, but direction is enough)
            float fnx = e1y * e2z - e1z * e2y;
            float fny = e1z * e2x - e1x * e2z;
            float fnz = e1x * e2y - e1y * e2x;

            float[] na = normals.get(fa[2]);
            float dot = fnx * na[0] + fny * na[1] + fnz * na[2];

            if (dot < 0f) {
                // Winding is CW — swap B ↔ C to make it CCW
                faces.set(i + 1, fc);
                faces.set(i + 2, fb);
            }
        }

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
}
