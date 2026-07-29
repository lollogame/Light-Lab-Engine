package tizio.dev.lle.core.client.renderer;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import tizio.dev.lle.api.data.SpotlightInstance;

import java.nio.FloatBuffer;
import java.util.Collection;

public class LightDataTexture {
    public static final int MAX_LIGHTS = 2048;
    public static final int DATA_MULTIPLIER = 4;

    private static int pointTextureId = -1;
    private static int spotTextureId = -1;

    private static FloatBuffer pointBuffer;
    private static FloatBuffer spotBuffer;

    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;

        pointBuffer = BufferUtils.createFloatBuffer(MAX_LIGHTS * DATA_MULTIPLIER * 4);
        spotBuffer = BufferUtils.createFloatBuffer(MAX_LIGHTS * DATA_MULTIPLIER * 4);

        int previousUnit = beginRawUnit(GL13.GL_TEXTURE10);

        pointTextureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, pointTextureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, MAX_LIGHTS * DATA_MULTIPLIER, 1, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);

        GL13.glActiveTexture(GL13.GL_TEXTURE11);
        spotTextureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, spotTextureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, MAX_LIGHTS * DATA_MULTIPLIER, 1, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (FloatBuffer) null);

        endRawUnit(previousUnit);

        initialized = true;
    }

    public static int updatePointLights(Vec3 camera) {
        return 0;
    }

    public static int updateSpotLights(Collection<SpotlightInstance> lights, Vec3 camera) {
        if (!initialized) initialize();

        int count = 0;
        spotBuffer.clear();

        for (SpotlightInstance light : lights) {
            if (count >= MAX_LIGHTS) break;

            Vector3f absPos = light.getPosition();
            Vec3 relative = new Vec3(absPos.x() - camera.x, absPos.y() - camera.y, absPos.z() - camera.z);

            Vector3f direction = light.getDirection();
            Vector4f color = light.getColor();

            float beamLength = light.getBeamLength();
            float beamRadius = light.getBeamRadius() * 0.7f;

            float outerAngleDegrees = (float) Math.toDegrees(Math.atan2(beamRadius, beamLength));
            float innerAngleDegrees = outerAngleDegrees * 0.6f;

            float innerCos = (float) Math.cos(Math.toRadians(innerAngleDegrees));
            float outerCos = (float) Math.cos(Math.toRadians(outerAngleDegrees));

            float intensity = color.w() * 10.0f;
            float falloff = 1.5f;

            spotBuffer.put((float) relative.x).put((float) relative.y).put((float) relative.z).put(beamLength);
            spotBuffer.put(color.x()).put(color.y()).put(color.z()).put(intensity);
            spotBuffer.put(direction.x()).put(direction.y()).put(direction.z()).put(falloff);
            spotBuffer.put(innerCos).put(outerCos).put((float) light.getGoboIndex()).put(0f);

            count++;
        }

        spotBuffer.flip();
        if (count > 0) {
            int previousUnit = beginRawUnit(GL13.GL_TEXTURE11);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, spotTextureId);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                    count * DATA_MULTIPLIER, 1,
                    GL11.GL_RGBA, GL11.GL_FLOAT, spotBuffer);
            endRawUnit(previousUnit);
        }

        return count;
    }

    public static void bindTextures(int programId) {
        if (!initialized) initialize();

        int previousUnit = beginRawUnit(GL13.GL_TEXTURE10);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, pointTextureId);
        int locPoint = GL20.glGetUniformLocation(programId, "PointData");
        if (locPoint != -1) {
            GL20.glUniform1i(locPoint, 10);
        }

        GL13.glActiveTexture(GL13.GL_TEXTURE11);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, spotTextureId);
        int locSpot = GL20.glGetUniformLocation(programId, "SpotData");
        if (locSpot != -1) {
            GL20.glUniform1i(locSpot, 11);
        }

        endRawUnit(previousUnit);

        GoboTextureManager.bind(programId);
    }

    private static int beginRawUnit(int unit) {
        int previous = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(unit);
        return previous;
    }

    private static void endRawUnit(int previous) {
        GL13.glActiveTexture(previous);
    }
}