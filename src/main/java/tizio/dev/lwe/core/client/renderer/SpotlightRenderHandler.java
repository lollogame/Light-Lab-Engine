package tizio.dev.lwe.core.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import tizio.dev.lwe.core.MainClass;
import tizio.dev.lwe.api.SpotlightAPI;
import tizio.dev.lwe.core.client.ClientShaders;
import tizio.dev.lwe.api.data.SpotlightInstance;

@Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SpotlightRenderHandler {

    private static boolean loggedError = false;
    private static boolean loggedBeamError = false;

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3f - 2f * t);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        DynamicLightUniforms.updateFrame();

        if (SpotlightAPI.getActiveSpotlights().isEmpty()) return;

        ShaderInstance shader = ClientShaders.getSpotlightShader();

        if (shader == null) {
            if (!loggedError) {
                MainClass.LOGGER.error("Spotlight Flare Shader is NULL!");
                loggedError = true;
            }
            return;
        }

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        Vec3 camLook = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableCull();

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tesselator = Tesselator.getInstance();

        RenderSystem.setShader(() -> shader);
        if (shader.safeGetUniform("CameraPos") != null)
            shader.safeGetUniform("CameraPos").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
        if (shader.safeGetUniform("CamDir") != null)
            shader.safeGetUniform("CamDir").set((float) camLook.x, (float) camLook.y, (float) camLook.z);

        Vector3f camUp = camera.getUpVector();
        Vector3f camLeft = camera.getLeftVector();

        for (SpotlightInstance light : SpotlightAPI.getActiveSpotlights()) {
            Vector3f absPos = light.getPosition();

            Vector3f lPos = new Vector3f(
                    (float) (absPos.x() - camPos.x),
                    (float) (absPos.y() - camPos.y),
                    (float) (absPos.z() - camPos.z)
            );

            Vector3f lDir = light.getDirection();
            Vector4f color = light.getColor();
            float size = light.getSize();

            float distSq = lPos.lengthSquared();

            float fovFade;
            float sideFade;

            if (distSq < 0.01f) {
                fovFade = 1f;
                sideFade = 1f;
            } else {
                Vector3f toLight = new Vector3f(lPos).normalize();
                Vector3f lookDir = new Vector3f((float) camLook.x, (float) camLook.y, (float) camLook.z).normalize();
                float cosAngle = toLight.dot(lookDir);
                fovFade = smoothstep(0.35f, 0.70f, cosAngle);

                Vector3f lightToCam = new Vector3f(lPos).negate().normalize();
                float backDot = lightToCam.dot(lDir);
                sideFade = smoothstep(-0.35f, 0.35f, backDot);
            }

            float visibility = fovFade * sideFade;
            if (visibility <= 0.001f) continue;

            float alphaMult = color.w() * visibility;

            if (shader.safeGetUniform("LightPos") != null)
                shader.safeGetUniform("LightPos").set(absPos.x(), absPos.y(), absPos.z());
            if (shader.safeGetUniform("LightDir") != null)
                shader.safeGetUniform("LightDir").set(lDir.x(), lDir.y(), lDir.z());
            if (shader.safeGetUniform("FadeParams") != null)
                shader.safeGetUniform("FadeParams").set(light.getMinFadeDist(), light.getMaxFadeDist());

            shader.apply();

            BufferBuilder buffer = tesselator.getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

            float halfSize = size * 0.85f;

            Vector3f v1 = new Vector3f(lPos).add(new Vector3f(camLeft).mul(-halfSize)).add(new Vector3f(camUp).mul(-halfSize));
            Vector3f v2 = new Vector3f(lPos).add(new Vector3f(camLeft).mul(halfSize)).add(new Vector3f(camUp).mul(-halfSize));
            Vector3f v3 = new Vector3f(lPos).add(new Vector3f(camLeft).mul(halfSize)).add(new Vector3f(camUp).mul(halfSize));
            Vector3f v4 = new Vector3f(lPos).add(new Vector3f(camLeft).mul(-halfSize)).add(new Vector3f(camUp).mul(halfSize));

            buffer.vertex(matrix, v1.x(), v1.y(), v1.z()).color(color.x(), color.y(), color.z(), alphaMult).uv(0.0f, 0.0f).endVertex();
            buffer.vertex(matrix, v2.x(), v2.y(), v2.z()).color(color.x(), color.y(), color.z(), alphaMult).uv(1.0f, 0.0f).endVertex();
            buffer.vertex(matrix, v3.x(), v3.y(), v3.z()).color(color.x(), color.y(), color.z(), alphaMult).uv(1.0f, 1.0f).endVertex();
            buffer.vertex(matrix, v4.x(), v4.y(), v4.z()).color(color.x(), color.y(), color.z(), alphaMult).uv(0.0f, 1.0f).endVertex();

            tesselator.end();
        }

        ShaderInstance beamShader = ClientShaders.getSpotlightBeamShader();

        if (beamShader == null) {
            if (!loggedBeamError) {
                MainClass.LOGGER.error("Spotlight Beam Shader is NULL!");
                loggedBeamError = true;
            }
        } else {
            RenderSystem.setShader(() -> beamShader);
            if (beamShader.safeGetUniform("CameraPos") != null)
                beamShader.safeGetUniform("CameraPos").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
            if (beamShader.safeGetUniform("CamDir") != null)
                beamShader.safeGetUniform("CamDir").set((float) camLook.x, (float) camLook.y, (float) camLook.z);

            for (SpotlightInstance light : SpotlightAPI.getActiveSpotlights()) {
                Vector3f absPos = light.getPosition();

                Vector3f lPos = new Vector3f(
                        (float) (absPos.x() - camPos.x),
                        (float) (absPos.y() - camPos.y),
                        (float) (absPos.z() - camPos.z)
                );

                Vector3f lDir = light.getDirection();
                Vector4f color = light.getColor();
                float beamLength = light.getVisualBeamLength();
                float beamRadius = light.getBeamRadius();

                if (beamShader.safeGetUniform("LightPos") != null)
                    beamShader.safeGetUniform("LightPos").set(absPos.x(), absPos.y(), absPos.z());

                if (beamShader.safeGetUniform("LightDir") != null)
                    beamShader.safeGetUniform("LightDir").set(lDir.x(), lDir.y(), lDir.z());

                if (beamShader.safeGetUniform("FadeParams") != null)
                    beamShader.safeGetUniform("FadeParams").set(light.getMinFadeDist(), light.getMaxFadeDist());

                if (beamShader.safeGetUniform("GoboIndex") != null)
                    beamShader.safeGetUniform("GoboIndex").set(light.getGoboIndex());

                beamShader.apply();
                GoboTextureManager.bind(beamShader.getId());

                Vector3f axis = new Vector3f(lDir).normalize();
                Vector3f toCam = new Vector3f(lPos).negate();
                if (toCam.lengthSquared() < 1.0E-6f) toCam.set((float) camLook.x, (float) camLook.y, (float) camLook.z).negate();
                toCam.normalize();

                float alignDot = toCam.dot(axis);
                float beamAlignFade = 1f - smoothstep(0.975f, 0.998f, alignDot);
                if (beamAlignFade <= 0.001f) continue;

                float beamAlpha = color.w() * beamAlignFade;

                Vector3f widthDir = new Vector3f();
                axis.cross(toCam, widthDir);
                if (widthDir.lengthSquared() < 1.0E-6f) {
                    axis.cross(camUp, widthDir);
                }
                widthDir.normalize();

                float halfWidth = beamRadius * 0.4f;

                Vector3f topCenter = new Vector3f(lPos);
                Vector3f baseCenter = new Vector3f(lPos).add(new Vector3f(axis).mul(beamLength));

                Vector3f topLeft   = new Vector3f(topCenter).add(new Vector3f(widthDir).mul(-halfWidth));
                Vector3f topRight  = new Vector3f(topCenter).add(new Vector3f(widthDir).mul(halfWidth));
                Vector3f baseRight = new Vector3f(baseCenter).add(new Vector3f(widthDir).mul(halfWidth));
                Vector3f baseLeft  = new Vector3f(baseCenter).add(new Vector3f(widthDir).mul(-halfWidth));

                BufferBuilder beamBuffer = tesselator.getBuilder();
                beamBuffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

                beamBuffer.vertex(matrix, topLeft.x(),  topLeft.y(),  topLeft.z()).color(color.x(), color.y(), color.z(), beamAlpha).uv(0.0f, 0.0f).endVertex();
                beamBuffer.vertex(matrix, topRight.x(), topRight.y(), topRight.z()).color(color.x(), color.y(), color.z(), beamAlpha).uv(1.0f, 0.0f).endVertex();
                beamBuffer.vertex(matrix, baseRight.x(), baseRight.y(), baseRight.z()).color(color.x(), color.y(), color.z(), beamAlpha).uv(1.0f, 1.0f).endVertex();
                beamBuffer.vertex(matrix, baseLeft.x(),  baseLeft.y(),  baseLeft.z()).color(color.x(), color.y(), color.z(), beamAlpha).uv(0.0f, 1.0f).endVertex();

                tesselator.end();
            }
        }

        poseStack.popPose();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}