package tizio.dev.lwe.core.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL20;
import tizio.dev.lwe.api.SpotlightAPI;

public final class DynamicLightUniforms {

    private static int cachedPointCount;
    private static int cachedSpotCount;

    public static void updateFrame() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            cachedPointCount = 0;
            cachedSpotCount = 0;
            return;
        }

        if (minecraft.isPaused()) {
            return;
        }

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        cachedPointCount = LightDataTexture.updatePointLights(camera);
        cachedSpotCount = LightDataTexture.updateSpotLights(SpotlightAPI.getActiveSpotlights(), camera);
    }

    public static void forceUpdate() {
        updateFrame();
    }

    @Deprecated
    public static void resetFrameFlag() {
    }

    public static void apply(ShaderInstance shader) {
        String name = shader.getName();
        if (!isBlockShader(name)) {
            return;
        }

        int programId = shader.getId();
        LightDataTexture.bindTextures(programId);
        setCountUniforms(programId, cachedPointCount, cachedSpotCount);
    }

    private static void setCountUniforms(int programId, int pointCount, int spotCount) {
        int locPoint = GL20.glGetUniformLocation(programId, "LwePointCount");
        if (locPoint != -1) {
            GL20.glUniform1i(locPoint, pointCount);
        }
        int locSpot = GL20.glGetUniformLocation(programId, "LweSpotCount");
        if (locSpot != -1) {
            GL20.glUniform1i(locSpot, spotCount);
        }
    }

    private static boolean isBlockShader(String name) {
        return name.endsWith("rendertype_solid")
                || name.endsWith("rendertype_cutout")
                || name.endsWith("rendertype_cutout_mipped")
                || name.endsWith("rendertype_entity_cutout_no_cull_z_offset")
                || name.endsWith("rendertype_entity_no_outline")
                || name.endsWith("rendertype_translucent")
                || name.endsWith("rendertype_entity_solid")
                || name.endsWith("rendertype_entity_cutout")
                || name.endsWith("rendertype_entity_cutout_no_cull")
                || name.endsWith("rendertype_entity_translucent")
                || name.endsWith("rendertype_entity_translucent_cull")
                || name.endsWith("rendertype_armor_cutout_no_cull")
                || name.endsWith("rendertype_tripwire");
    }
}
