package com.parkedge.network;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ServerMovementHandler {
    private static final Set<UUID> SLIDING_PLAYERS = new HashSet<>();
    private static final Map<UUID, Integer> ROLL_UNTIL = new HashMap<>();
    private static int serverTick = 0;

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.c2s(), MovementActionPayload.ID, MovementActionPayload.CODEC, (payload, context) -> {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getPlayer();
            MovementAction action = payload.action();
            boolean active = payload.active();

            context.queue(() -> {
                switch (action) {
                    case SLIDE -> {
                        if (active) {
                            SLIDING_PLAYERS.add(player.getUuid());
                        } else {
                            SLIDING_PLAYERS.remove(player.getUuid());
                        }
                    }
                    case ROLL -> {
                        if (active) {
                            ROLL_UNTIL.put(player.getUuid(), serverTick + MovementActionPayload.ROLL_BUFFER_TICKS);
                        }
                    }
                    case RESET_FALL -> player.fallDistance = 0.0f;
                }
            });
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            SLIDING_PLAYERS.remove(player.getUuid());
            ROLL_UNTIL.remove(player.getUuid());
        });

        // Событие тика сервера через Architectury
        TickEvent.SERVER_POST.register(server -> {
            serverTick++;
            ROLL_UNTIL.values().removeIf(until -> until < serverTick);

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (SLIDING_PLAYERS.contains(player.getUuid())) {
                    player.setPose(EntityPose.SWIMMING);
                    player.calculateDimensions();
                }
            }
        });
    }

    public static boolean isSliding(UUID id) {
        return SLIDING_PLAYERS.contains(id);
    }

    public static boolean isRolling(UUID id) {
        Integer until = ROLL_UNTIL.get(id);
        return until != null && serverTick <= until;
    }
}