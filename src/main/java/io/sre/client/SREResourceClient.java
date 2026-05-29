package io.sre.client;

import io.sre.client.utils.VTModePlayerSkin;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public class SREResourceClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        VTModePlayerSkin.init();
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new SREResourceReloadListener());
    }

}
