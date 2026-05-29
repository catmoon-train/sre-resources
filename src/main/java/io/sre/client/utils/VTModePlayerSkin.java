package io.sre.client.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.sre.client.events.ClientPlayerInfoUpdatePacketEvents;
import io.sre.resource_lib.SREResource;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;

@Environment(EnvType.CLIENT)
public class VTModePlayerSkin {
    public static Gson GSON = new Gson();
    public static final List<LocalPlayerSkin> LOCAL_VT_PLAYER_SKINS = new ArrayList<>();
    public static final HashMap<UUID, LocalPlayerSkin> UID2SKINS = new HashMap<>();
    private static int skinId = 0;

    public static LocalPlayerSkin getPlayerSkin(Player player) {
        return getPlayerSkin(player.getUUID());
    }

    public static LocalPlayerSkin getPlayerSkin(UUID uid) {
        if (UID2SKINS.containsKey(uid)) {
            return UID2SKINS.get(uid);
        } else {
            return getANewPlayerSkinAndCache(uid);
        }
    }

    public static LocalPlayerSkin getANewPlayerSkinAndCache(UUID uid) {
        var sk = getAPlayerSkin();
        UID2SKINS.put(uid, sk);
        return sk;
    }

    public static LocalPlayerSkin getAPlayerSkin() {
        return getASkin();
    }

    public static LocalPlayerSkin getASkin() {
        if (LOCAL_VT_PLAYER_SKINS.size() == 0) {
            return null;
        }
        if (skinId >= LOCAL_VT_PLAYER_SKINS.size()) {
            skinId = 0;
        }
        LocalPlayerSkin result = LOCAL_VT_PLAYER_SKINS.get(skinId);
        skinId++;
        return result;
    }

    public static void reload() {
        LOCAL_VT_PLAYER_SKINS.clear();
        LOCAL_VT_PLAYER_SKINS.addAll(loadSkinLists());
    }

    public static void init() {
        registerEvents();
    }

    private static void registerEvents() {
        ClientPlayerInfoUpdatePacketEvents.UPDATE.register((action, playerinfo) -> {
            if (action.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
                if (playerinfo.getProfile() != null) {
                    var id = playerinfo.getProfile().getId();
                    UID2SKINS.put(id,
                            getAPlayerSkin());
                }
            }
        });
        ClientPlayerInfoUpdatePacketEvents.REMOVE.register((uuids) -> {
            if (uuids == null)
                return;
            for (var uid : uuids) {
                UID2SKINS.remove(uid);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            UID2SKINS.clear();
        });
    }

    public static class LocalPlayerSkin {
        public String path;
        public boolean is_slim;

        public LocalPlayerSkin(String path, boolean is_slim) {
            this.path = path;
            this.is_slim = is_slim;
        }

        public PlayerSkin toPlayerSkin() {
            return toPlayerSkin(true);
        }

        /**
         * 将当前 LocalPlayerSkin 对象转换为 Minecraft 原生的 PlayerSkin 对象。
         * 
         * @return 转换后的 PlayerSkin 实例，如果路径无效则返回 null（或根据需求处理）
         */
        public PlayerSkin toPlayerSkin(boolean secure) {
            try {
                ResourceLocation textureLocation = ResourceLocation.parse(path);
                PlayerSkin.Model model = is_slim ? PlayerSkin.Model.SLIM : PlayerSkin.Model.WIDE;
                // 构造 PlayerSkin：纹理位置，纹理URL(null)，披风(null)，鞘翅(null)，模型，是否安全(false)
                return new PlayerSkin(textureLocation, null, null, null, model, secure);
            } catch (Exception e) {
                // 路径解析失败时的降级处理，可根据需要改为抛出异常或返回默认皮肤
                return null;
            }
        }
    }

    final static String LIST_FILE_NAME = "player_skins.json";

    private static List<LocalPlayerSkin> loadSkinList(Minecraft minecraft, String namespace) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(namespace, LIST_FILE_NAME);
        List<LocalPlayerSkin> results = new ArrayList<>();
        try {
            Optional<Resource> res = minecraft.getResourceManager().getResource(loc);
            if (res.isPresent()) {
                try (InputStream is = res.get().open();
                        InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    JsonArray jsonArray = GSON.fromJson(reader, JsonArray.class);
                    results.clear();
                    for (JsonElement ele : jsonArray) {
                        results.add(GSON.fromJson(ele, LocalPlayerSkin.class));
                    }
                    return results;
                }
            }
        } catch (Exception e) {
            SREResource.LOGGER.error("[SRE-RESOURCE] '" + namespace + "/player_skins.json' failed to load.", e);
        }
        return List.of();
    }

    private static List<LocalPlayerSkin> loadSkinLists() {
        SREResource.LOGGER.info("Loading custom vt-mode player skins...");
        final Minecraft minecraft = Minecraft.getInstance();
        final ResourceManager manager = minecraft.getResourceManager();
        Set<String> namespaces = manager.getNamespaces();
        List<LocalPlayerSkin> results = new ArrayList<>();
        for (String namespace : namespaces) {
            results.addAll(loadSkinList(minecraft, namespace));
        }
        if (results.isEmpty()) {
            SREResource.LOGGER.info(
                    "No custom vt-mode player skins found! Use default skins. (Defined in '<namespace>/player_skins.json' in your resource pack)",
                    results.size());
            return getDefaultSkins();
        }
        SREResource.LOGGER.info("Loaded custom vt-mode player skins Successfully! Found {} skins.", results.size());

        return results;
    }

    private static List<LocalPlayerSkin> getDefaultSkins() {
        return List.of(
                new LocalPlayerSkin("minecraft:textures/entity/player/slim/alex.png", false),
                new LocalPlayerSkin("minecraft:textures/entity/player/slim/ari.png", false),
                new LocalPlayerSkin("minecraft:textures/entity/player/slim/efe.png", false),
                new LocalPlayerSkin("minecraft:textures/entity/player/slim/kai.png", false),
                new LocalPlayerSkin("minecraft:textures/entity/player/slim/makena.png", false),
                new LocalPlayerSkin("minecraft:textures/entity/player/slim/noor.png", false),
                new LocalPlayerSkin("minecraft:textures/entity/player/slim/steve.png", false),
                new LocalPlayerSkin("minecraft:textures/entity/player/slim/sunny.png", false),
                new LocalPlayerSkin("minecraft:textures/entity/player/slim/zuri.png", false),
                new LocalPlayerSkin("minecraft:textures/entity/player/wide/alex.png", true),
                new LocalPlayerSkin("minecraft:textures/entity/player/wide/ari.png", true),
                new LocalPlayerSkin("minecraft:textures/entity/player/wide/efe.png", true),
                new LocalPlayerSkin("minecraft:textures/entity/player/wide/kai.png", true),
                new LocalPlayerSkin("minecraft:textures/entity/player/wide/makena.png", true),
                new LocalPlayerSkin("minecraft:textures/entity/player/wide/noor.png", true),
                new LocalPlayerSkin("minecraft:textures/entity/player/wide/steve.png", true),
                new LocalPlayerSkin("minecraft:textures/entity/player/wide/sunny.png", true),
                new LocalPlayerSkin("minecraft:textures/entity/player/wide/zuri.png", true));
    }
}
