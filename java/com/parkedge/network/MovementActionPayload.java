package com.parkedge.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MovementActionPayload(MovementAction action, boolean active) implements CustomPayload {
    /** Сколько тиков после нажатия shift в воздухе кувырок защищает от урона (и на клиенте, и на сервере). */
    public static final int ROLL_BUFFER_TICKS = 10;

    public static final CustomPayload.Id<MovementActionPayload> ID = new CustomPayload.Id<>(Identifier.of("parkedge", "movement_action"));

    public static final PacketCodec<RegistryByteBuf, MovementActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.indexed(i -> MovementAction.values()[i], MovementAction::ordinal), MovementActionPayload::action,
            PacketCodecs.BOOL, MovementActionPayload::active,
            MovementActionPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}