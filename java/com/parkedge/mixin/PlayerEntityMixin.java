package com.parkedge.mixin;

import com.parkedge.client.MovementController;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "updatePose", at = @At("TAIL"))
    private void parkedge$slidePose(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (MovementController.isSlidingFor(self.getUuid())) {
            self.setPose(EntityPose.SWIMMING);
        }
    }
}