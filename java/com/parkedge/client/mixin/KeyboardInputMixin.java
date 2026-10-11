package com.parkedge.client.mixin;

import com.parkedge.client.ParkSEdgeConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    // В 1.21.4 метод tick() не принимает аргументов! Оставляем только CallbackInfo ci
    @Inject(method = "tick", at = @At("TAIL"))
    private void parkedge$noVanillaSneak(CallbackInfo ci) {
        if (!ParkSEdgeConfig.get().disableVanillaSneak) return;
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null || p.hasVehicle() || p.getAbilities().flying) return;

        // Ваша рабочая логика отключения sneak
    }
}