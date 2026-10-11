package com.parkedge;

import com.parkedge.client.ParkSEdgeClient;
import com.parkedge.network.ServerMovementHandler;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;

public final class ParkEdge {
    public static final String MOD_ID = "parkedge";

    public static void init() {
        ServerMovementHandler.register();
        EnvExecutor.runInEnv(Env.CLIENT, () -> ParkSEdgeClient::init);
    }
}