package tizio.dev.lwe.core.mixin.client;

import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tizio.dev.lwe.core.client.renderer.DynamicLightUniforms;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {

    @Inject(method = "apply", at = @At("RETURN"))
    private void lwe$applyDynamicLightUniforms(CallbackInfo ci) {
        DynamicLightUniforms.apply((ShaderInstance) (Object) this);
    }
}
