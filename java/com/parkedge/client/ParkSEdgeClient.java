package com.parkedge.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ParkSEdgeClient {
	public static KeyBinding quickTurnKey;

	public static void init() {
		AutoConfig.register(ParkSEdgeConfig.class, GsonConfigSerializer::new);

		// Клавиша Quick Turn (по умолчанию: Q)
		quickTurnKey = new KeyBinding(
				"key.parkedge.quick_turn",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_Q,
				"category.parkedge"
		);
		KeyMappingRegistry.register(quickTurnKey);

		ClientTickEvent.CLIENT_POST.register(client -> {
			if (client.player != null) MovementController.tick(client.player);
		});
		ClientGuiEvent.RENDER_HUD.register((ctx, tickCounter) -> {
			StreakOverlay.render(ctx, tickCounter);
			HudRenderer.renderHud(ctx, tickCounter);
		});
	}
}