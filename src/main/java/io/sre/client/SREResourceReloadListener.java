package io.sre.client;

import io.sre.client.utils.VTModePlayerSkin;
import io.sre.resource_lib.SREResource;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class SREResourceReloadListener implements SimpleSynchronousResourceReloadListener {

    @Override
    public ResourceLocation getFabricId() {
        return SREResource.SREId("player_skin");
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        VTModePlayerSkin.reload(resourceManager);
    }

}
