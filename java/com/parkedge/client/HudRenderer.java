package com.parkedge.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Рисует HUD со скоростью и свой прицел. */
public final class HudRenderer {
    private HudRenderer() {}

    public static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        if (mc.player == null || mc.options.hudHidden || !cfg.hudEnabled) return;

        List<String> lines = new ArrayList<>(3);
        if (cfg.hudShowSpeed) {
            double horizKmh = MovementController.hudKmh();
            double vertMps = mc.player.getVelocity().y * 20.0; // вертикальная скорость в м/с
            lines.add(String.format("Speed: %.1f km/h (V: %.1f m/s)", horizKmh, vertMps));
        }
        if (cfg.hudShowMomentum) {
            lines.add("Inertia: " + Math.round(MovementController.momentum() * 100f) + "%");
        }
        if (cfg.hudShowState) {
            lines.add("State: " + MovementController.state());
        }
        if (lines.isEmpty()) return;

        int color = 0xFF000000 | cfg.hudColor;
        context.getMatrices().push();
        context.getMatrices().translate(cfg.hudX, cfg.hudY, 0);
        context.getMatrices().scale((float) cfg.hudScale, (float) cfg.hudScale, 1f);
        int y = 0;
        for (String line : lines) {
            context.drawTextWithShadow(mc.textRenderer, line, 0, y, color);
            y += mc.textRenderer.fontHeight + 2;
        }
        context.getMatrices().pop();
    }

    /** Маленькая точка с обводкой, как в Mirror's Edge. Вызывается из InGameHudMixin. */
    public static void renderCrosshair(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.options.getPerspective().isFirstPerson()) return;

        ParkSEdgeConfig cfg = ParkSEdgeConfig.get();
        int size = cfg.crosshairSize;
        int outline = cfg.crosshairOutline;
        int outer = size + 2 * outline;
        int x = (context.getScaledWindowWidth() - outer) / 2;
        int y = (context.getScaledWindowHeight() - outer) / 2;

        if (outline > 0) {
            fillDot(context, x, y, outer, 0xFF000000 | cfg.crosshairOutlineColor);
        }
        fillDot(context, x + outline, y + outline, size, 0xFF000000 | cfg.crosshairColor);
    }

    /** Квадрат со срезанными углами, чтобы точка выглядела круглой. */
    private static void fillDot(DrawContext c, int x, int y, int size, int color) {
        if (size < 4) {
            c.fill(x, y, x + size, y + size, color);
            return;
        }
        c.fill(x, y + 1, x + size, y + size - 1, color);
        c.fill(x + 1, y, x + size - 1, y + size, color);
    }
}