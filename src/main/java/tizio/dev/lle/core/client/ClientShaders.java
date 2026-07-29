package tizio.dev.lle.core.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.lle.core.MainClass;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientShaders {
    private static ShaderInstance spotlightShader;
    private static ShaderInstance spotlightBeamShader;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        new ResourceLocation(MainClass.MODID, "spotlight_flare"),
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                shader -> spotlightShader = shader
        );
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        new ResourceLocation(MainClass.MODID, "spotlight_beam"),
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                shader -> spotlightBeamShader = shader
        );
    }

    public static ShaderInstance getSpotlightShader() {
        return spotlightShader;
    }

    public static ShaderInstance getSpotlightBeamShader() {
        return spotlightBeamShader;
    }
}