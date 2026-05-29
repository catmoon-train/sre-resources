package io.sre.client.events;

import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 客户端玩家信息更新包事件的容器类。
 *
 * <p>
 * 该类提供了两个 Fabric 事件，用于监听
 * {@link net.minecraft.client.multiplayer.ClientPacketListener}
 * 中接收到的玩家列表更新（加入、更新、离开）数据包。这些事件仅在客户端触发，
 * 允许模组在玩家进入或离开 Tab 列表时执行自定义逻辑（如加载皮肤、记录日志等）。
 *
 * <p>
 * 事件基于 Fabric API 的 {@link Event} 机制，支持多个监听器同时注册。
 *
 * @see ClientboundPlayerInfoUpdatePacket
 * @see net.minecraft.client.multiplayer.PlayerInfo
 */
public class ClientPlayerInfoUpdatePacketEvents {

    /**
     * 玩家信息移除事件。
     *
     * <p>
     * 当服务器发送
     * {@link net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket}
     * 时触发（例如玩家离开服务器）。该事件会提供所有被移除玩家的 UUID 列表。
     *
     * <p>
     * 监听器应实现 {@link CPInfoRemovedImpl} 接口。
     */
    public static Event<CPInfoRemovedImpl> REMOVE = createArrayBacked(CPInfoRemovedImpl.class,
            listeners -> (uuids) -> {
                for (CPInfoRemovedImpl listener : listeners) {
                    listener.onPlayerInfosRemoved(uuids);
                }
            });

    /**
     * 玩家信息更新事件。
     *
     * <p>
     * 当服务器发送 {@link ClientboundPlayerInfoUpdatePacket} 时触发，
     * 该数据包可能包含以下动作：新增玩家、更新游戏模式、更新延迟、更新显示名称等。
     * 监听器会获得本次数据包中的所有动作类型（{@link ClientboundPlayerInfoUpdatePacket.Action}）
     * 以及受影响的 {@link PlayerInfo} 对象。
     *
     * <p>
     * 注意：一个数据包可能包含多个动作组合，例如同时包含 {@code ADD_PLAYER} 和 {@code UPDATE_LATENCY}。
     * 监听器应当检查动作集合以确定具体变更内容。
     *
     * <p>
     * 监听器应实现 {@link CPInfoUpdateImpl} 接口。
     */
    public static Event<CPInfoUpdateImpl> UPDATE = createArrayBacked(CPInfoUpdateImpl.class,
            listeners -> (action, profile) -> {
                for (CPInfoUpdateImpl listener : listeners) {
                    listener.onPlayerInfoUpdated(action, profile);
                }
            });

    /**
     * 玩家信息移除事件的监听器接口。
     */
    public static interface CPInfoRemovedImpl {
        /**
         * 当玩家信息被移除时调用。
         *
         * @param uuids 被移除玩家的 UUID 列表，每个 UUID 对应一个离开服务器的玩家。
         *              列表不可为 {@code null}，但可能为空（通常不会）。
         */
        void onPlayerInfosRemoved(List<UUID> uuids);
    }

    /**
     * 玩家信息更新事件的监听器接口。
     */
    public static interface CPInfoUpdateImpl {
        /**
         * 当玩家信息更新时调用。
         *
         * @param action  本次更新包含的所有动作类型，使用 {@link EnumSet} 表示。
         *                可能的值包括：{@code ADD_PLAYER}、{@code UPDATE_GAME_MODE}、
         *                {@code UPDATE_LATENCY}、{@code UPDATE_DISPLAY_NAME} 等。
         *                该集合不可为 {@code null}，但可能为空（通常不会）。
         * @param profile 受影响玩家的 {@link PlayerInfo} 对象，包含该玩家的名称、UUID、
         *                游戏模式、延迟等最新信息。不可为 {@code null}。
         */
        void onPlayerInfoUpdated(EnumSet<ClientboundPlayerInfoUpdatePacket.Action> action, PlayerInfo profile);
    }
}