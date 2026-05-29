package io.sre.mixin.client;

import com.mojang.authlib.GameProfile;

import io.sre.client.events.ClientPlayerInfoUpdatePacketEvents;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

import java.util.Objects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    /**
     * 监听玩家信息更新（包含加入、游戏模式变化、延迟变化等）
     */
    @Shadow
    private boolean enforcesSecureChat() {
        throw new AssertionError("Shadow method should not be called directly");
    }

    @Inject(method = "handlePlayerInfoUpdate", at = @At("TAIL"))
    private void onPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {

        for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
            PlayerInfo playerInfo = new PlayerInfo((GameProfile) Objects.requireNonNull(entry.profile()),
                    this.enforcesSecureChat());
            ClientPlayerInfoUpdatePacketEvents.UPDATE.invoker().onPlayerInfoUpdated(packet.actions(), playerInfo);
        }
    }

    /**
     * 监听玩家移除（离开）
     */
    @Inject(method = "handlePlayerInfoRemove", at = @At("TAIL"))
    private void onPlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
        ClientPlayerInfoUpdatePacketEvents.REMOVE.invoker().onPlayerInfosRemoved(packet.profileIds());
    }
}
