package tizio.dev.lwe.core.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import tizio.dev.lwe.core.MainClass;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GoboTextureManager {

    public static final int MAX_GOBOS = LightDataTexture.MAX_LIGHTS;
    private static final int GOBO_TEXTURE_UNIT = GL13.GL_TEXTURE12;
    private static final int GOBO_TEXTURE_SLOT = 12;

    private static final List<ResourceLocation> registeredGobos = new ArrayList<>();
    private static int arrayTextureId = -1;
    private static boolean dirty = true;

    private GoboTextureManager() {}

    public static synchronized int register(ResourceLocation texture) {
        int existing = registeredGobos.indexOf(texture);
        if (existing != -1) return existing;

        if (registeredGobos.size() >= MAX_GOBOS) {
            MainClass.LOGGER.error("Gobo texture limit reached (" + MAX_GOBOS + "), ignored: " + texture);
            return -1;
        }

        registeredGobos.add(texture);
        dirty = true;
        return registeredGobos.size() - 1;
    }

    public static synchronized void bind(int programId) {
        if (dirty) {
            rebuild();
        }

        if (arrayTextureId == -1) return;

        int previousUnit = beginRawUnit(GOBO_TEXTURE_UNIT);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, arrayTextureId);
        int loc = GL20.glGetUniformLocation(programId, "LweGoboArray");
        if (loc != -1) {
            GL20.glUniform1i(loc, GOBO_TEXTURE_SLOT);
        }
        endRawUnit(previousUnit);
    }

    private static void rebuild() {
        dirty = false;

        if (registeredGobos.isEmpty()) return;

        List<NativeImage> images = new ArrayList<>();
        int width = 0;
        int height = 0;

        for (ResourceLocation location : registeredGobos) {
            NativeImage image = loadImage(location);
            if (image != null && width == 0) {
                width = image.getWidth();
                height = image.getHeight();
            }
            images.add(image);
        }

        if (width == 0 || height == 0) {
            for (NativeImage image : images) {
                if (image != null) image.close();
            }
            return;
        }

        if (arrayTextureId != -1) {
            GL11.glDeleteTextures(arrayTextureId);
            arrayTextureId = -1;
        }

        int previousUnit = beginRawUnit(GOBO_TEXTURE_UNIT);

        arrayTextureId = GL11.glGenTextures();
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, arrayTextureId);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

        GL30.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL11.GL_RGBA, width, height, images.size(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

        for (int layer = 0; layer < images.size(); layer++) {
            NativeImage image = images.get(layer);
            if (image == null) continue;

            if (image.getWidth() != width || image.getHeight() != height) {
                MainClass.LOGGER.error("Mismatched gobo dimensions (layer " + layer + "), texture skipped.");
                image.close();
                continue;
            }

            uploadLayer(image, layer, width, height);
            image.close();
        }

        endRawUnit(previousUnit);
    }

    private static NativeImage loadImage(ResourceLocation location) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            MainClass.LOGGER.error("Texture gobo not found: " + location);
            return null;
        }

        try (InputStream stream = resource.get().open()) {
            return NativeImage.read(stream);
        } catch (IOException e) {
            MainClass.LOGGER.error("Unable to load gobo texture: " + location);
            return null;
        }
    }

    private static void uploadLayer(NativeImage image, int layer, int width, int height) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int abgr = image.getPixelRGBA(x, y);
                buffer.put((byte) (abgr & 0xFF));
                buffer.put((byte) ((abgr >> 8) & 0xFF));
                buffer.put((byte) ((abgr >> 16) & 0xFF));
                buffer.put((byte) ((abgr >> 24) & 0xFF));
            }
        }
        buffer.flip();

        GL30.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, width, height, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
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
