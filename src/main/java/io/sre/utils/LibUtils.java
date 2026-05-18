package io.sre.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class LibUtils {
    public boolean isLibLoaded() {
        return true;
    }

    public boolean isEntityAPlayer(Entity e) {
        return e instanceof Player;
    }

    public boolean isServerSide(Level world) {
        return !world.isClientSide;
    }
}
