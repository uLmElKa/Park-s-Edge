package com.parkedge.neoforge;

import com.parkedge.ParkEdge;
import com.parkedge.client.ParkSEdgeClient;
import com.parkedge.client.ParkSEdgeConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(ParkEdge.MOD_ID)
public final class ParkEdgeNeoForge {
    public ParkEdgeNeoForge() {
        // Инициализация общей логики
        ParkEdge.init();

        // Регистрируем клиентскую часть и экран только если запускается КЛИЕНТ
        if (FMLEnvironment.dist == Dist.CLIENT) {

            ModLoadingContext.get().registerExtensionPoint(
                    IConfigScreenFactory.class,
                    () -> (container, parent) -> ParkSEdgeConfig.createConfigScreen(parent)
            );
        }
    }
}