package com.parkedge.client.mixin;

import com.parkedge.client.MovementController;
import com.parkedge.client.ParkSEdgeConfig;
import com.parkedge.client.PlayerState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void parkedge$cancelFallDamageOnRoll(float fallDistance, float damageMultiplier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled) return;

        if ((Object) this instanceof ClientPlayerEntity player) {
            MovementController.onFallLanding(fallDistance);
            // Если игрок выполняет кувырок (ROLLING) или сгруппировался (COILING), урон не наносится
            if (MovementController.state() == PlayerState.ROLLING || MovementController.state() == PlayerState.COILING) {
                player.fallDistance = 0;
                cir.setReturnValue(false); // Полная отмена урона
            }
        }
    }

    @Unique
    private double parkedge$prevX;
    @Unique
    private double parkedge$prevZ;

    // Включение/отключение ванильного спринта в зависимости от состояния мода
    @ModifyVariable(method = "setSprinting", at = @At("HEAD"), argsOnly = true)
    private boolean parkedge$noSprint(boolean sprinting) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        // Если мод выключен — полностью возвращаем ванильный спринт
        if (!cfg.modEnabled) {
            return sprinting;
        }
        if ((Object) this instanceof ClientPlayerEntity) {
            // Ctrl в воде даёт ванильный бег на несколько тиков, в остальное время бег выключен
            return sprinting && MovementController.isWaterSprint();
        }
        return sprinting;
    }

    // Пробел с A или D запускает рывок, перед уступом - залезание, перед стеной - скалолазание
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void parkedge$sidestepInsteadOfJump(CallbackInfo ci) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.modEnabled) return;

        if ((Object) this instanceof ClientPlayerEntity player) {
            if (MovementController.tryStartSidestep(player) || MovementController.tryStartClimb(player)
                    || MovementController.tryStartWallClimbFromJump(player)) {
                ci.cancel();
            }
        }
    }

    // Во время подката клавиши движения не действуют
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3d parkedge$lockInput(Vec3d input) {
        if ((Object) this instanceof ClientPlayerEntity && MovementController.isInputLocked()) {
            return Vec3d.ZERO;
        }
        return input;
    }

    // Перед движением: запоминаем позицию и даём плавно затормозить
    @Inject(method = "travel", at = @At("HEAD"))
    private void parkedge$beforeTravel(Vec3d input, CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity player) {
            parkedge$prevX = player.getX();
            parkedge$prevZ = player.getZ();
            MovementController.onPreTravel(player, input);
        }
    }

    // После движения ограничиваем скорость
    @Inject(method = "travel", at = @At("RETURN"))
    private void parkedge$afterTravel(Vec3d input, CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity player) {
            MovementController.limitSpeed(player, parkedge$prevX, parkedge$prevZ);
        }
    }
}