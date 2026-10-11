package com.parkedge.mixin;

import com.parkedge.network.ServerMovementHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void cancelRollFallDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        // 1. Проверяем, является ли урон уроном от падения
        if (source.isOf(DamageTypes.FALL)) {

            // 2. Проверяем, активен ли перекат на сервере
            if (ServerMovementHandler.isRolling(player.getUuid())) {

                // Сбрасываем дистанцию падения
                player.fallDistance = 0.0F;

                // Полностью отменяем нанесение урона (метод damage возвращает false)
                cir.setReturnValue(false);
            }
        }
    }
}