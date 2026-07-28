package tizio.dev.lwe.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tizio.dev.lwe.core.MainClass;
import tizio.dev.lwe.api.SpotlightAPI;
import tizio.dev.lwe.api.data.SpotlightInstance;

@Mod.EventBusSubscriber(modid = MainClass.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SpotlightRaytracer {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        for (SpotlightInstance light : SpotlightAPI.getActiveSpotlights()) {
            updateBeamLength(level, light);
        }
    }

    private static void updateBeamLength(Level level, SpotlightInstance light) {
        float maxFadeDist = light.getMaxFadeDist();

        Vec3 start = new Vec3(
                light.getPosition().x(),
                light.getPosition().y(),
                light.getPosition().z()
        );

        Vec3 dir = new Vec3(
                light.getDirection().x(),
                light.getDirection().y(),
                light.getDirection().z()
        );

        Vec3 end = start.add(dir.scale(maxFadeDist));

        ClipContext ctx = new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
        );

        BlockHitResult result = level.clip(ctx);

        float hitDistance;
        if (result.getType() != HitResult.Type.MISS) {
            hitDistance = (float) start.distanceTo(result.getLocation());
        } else {
            hitDistance = maxFadeDist;
        }

        light.setVisualBeamLength(Math.min(hitDistance, maxFadeDist) * 1.35f);
    }
}
