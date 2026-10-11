package com.parkedge.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Streak effect: визуализация высокой скорости. */
public final class StreakOverlay {

    private static float strength = 0f;

    private StreakOverlay() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (!cfg.streakEnabled) return;

        double startKmh = cfg.streakStartKmh;
        double maxKmh = Math.max(startKmh + 1.0, cfg.maxSpeedKmh);

        float t = (float) MathHelper.clamp((MovementController.hudKmh() - startKmh) / (maxKmh - startKmh), 0.0, 1.0);
        float target = t * t * (3f - 2f * t);
        strength += (target - strength) * Math.min(1f, tickCounter.getLastFrameDuration() * 0.2f);
        if (strength < 0.01f) return;

        double scale = mc.getWindow().getScaleFactor();
        float px = (float) (1.0 / scale);
        float diag = (float) Math.hypot(mc.getWindow().getFramebufferWidth(),
                mc.getWindow().getFramebufferHeight()) / 2f;
        double time = Util.getMeasuringTimeMs() / 1000.0;

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(ctx.getScaledWindowWidth() / 2f, ctx.getScaledWindowHeight() / 2f, 0f);
        m.scale(px, px, 1f);

        int count = cfg.streakCount;
        float maxAlpha = (float) cfg.streakMaxAlpha;

        for (int i = 0; i < count; i++) {
            float angle = rnd(i, 1) * (float) (Math.PI * 2);
            float speed = 0.5f + rnd(i, 2);
            float phase = rnd(i, 3);
            float len = (float) (diag * (0.15f + 0.25f * rnd(i, 4)) * cfg.streakLengthMult);
            int width = Math.max(1, (int) (rnd(i, 5) * cfg.streakWidth));

            float prog = (float) ((time * speed * 0.6 + phase) % 1.0);
            float r0 = diag * (0.25f + 0.75f * prog);
            float alpha = maxAlpha * strength * (float) Math.sin(Math.PI * prog);
            if (alpha <= 0.01f) continue;

            m.push();
            m.multiply(RotationAxis.POSITIVE_Z.rotation(angle - (float) (Math.PI / 2)));
            int head = ((int) (alpha * 255f) << 24) | 0xFFFFFF;
            ctx.fillGradient(0, (int) r0, width, (int) (r0 + len), 0x00FFFFFF, head);
            m.pop();
        }
        m.pop();
    }

    private static float rnd(int i, int salt) {
        double x = Math.sin(i * 12.9898 + salt * 78.233) * 43758.5453;
        return (float) (x - Math.floor(x));
    }
}