package com.parkedge.client.mixin;

import com.parkedge.client.MovementController;
import com.parkedge.client.ParkSEdgeConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    public abstract Vec3d getPos();

    @Inject(method = "update", at = @At("TAIL"))
    private void parkedge$tilt(BlockView area, Entity e, boolean tp, boolean inv, float td, CallbackInfo ci) {
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.cameraEnabled) return;

        Quaternionf q = ((Camera) (Object) this).getRotation();

        // Поворот Roll (ось Z) и Pitch (ось X)
        q.rotateZ((float) Math.toRadians(MovementController.camRoll(td)));
        q.rotateX((float) Math.toRadians(MovementController.camPitch(td)));

        // Кувырок 360° при приземлении
        float rollProg = MovementController.rollProgress();
        if (rollProg > 0.001f) {
            q.rotateX((float) Math.toRadians(-rollProg * 360.0f));
        }

        // Вертикальное смещение камеры Y (Slide, Coil, Landing, Climb, Bobbing)
        float offsetY = MovementController.camOffsetY(td);
        if (Math.abs(offsetY) > 0.0001f) {
            Vec3d pos = getPos();
            setPos(pos.x, pos.y + offsetY, pos.z);
        }
    }
}