package io.sre.mixin.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.Util;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

/**
 * 流式音频（stream=true 的音乐/环境音）默认在声音引擎线程上从 mod jar 的
 * ZipFileSystem 惰性读取。该线程在声音系统重启时会被 interrupt，NIO 的
 * ClosedByInterruptException 会连带关闭整个 jar 底层的 FileChannel，
 * 之后从同一 jar 加载任何资源（纹理、音效）都会报 ClosedChannelException，
 * 直到游戏重启。
 * <p>
 * 修复方式：打开流时先把整个 ogg 读入内存，播放期间不再触碰 jar 的文件通道。
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @Shadow
    @Final
    private ResourceProvider resourceManager;

    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void sre$streamFromMemory(ResourceLocation id, boolean looping,
            CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        cir.setReturnValue(CompletableFuture.supplyAsync(() -> {
            try (InputStream in = this.resourceManager.open(id)) {
                InputStream mem = new ByteArrayInputStream(in.readAllBytes());
                return looping
                        ? new LoopingAudioStream(JOrbisAudioStream::new, mem)
                        : new JOrbisAudioStream(mem);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, Util.backgroundExecutor()));
    }
}
