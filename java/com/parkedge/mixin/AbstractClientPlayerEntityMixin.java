package com.parkedge.mixin;

import com.parkedge.client.MovementController;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getFovMultiplier", at = @At("RETURN"), cancellable = true)
    private void parkedge$softFov(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * MovementController.fovCorrection());
    }
}