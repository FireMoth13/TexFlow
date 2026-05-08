package com.texturepipeline.engine;

import com.texturepipeline.model.TextureImage;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 流水线引擎 — 纹理处理的核心编排器。
 *
 * <p>设计思路（2026 vibe coding）：
 * 每个处理步骤是一个独立的阶段，输入 TextureImage → 输出 TextureImage。
 * 使用 CompletableFuture 实现异步处理（不阻塞 UI 线程）。
 * 可插拔的阶段设计方便后续扩展到压缩、图集等功能。</p>
 */
public class PipelineEngine {

    /** 通道打包结果包装 */
    public static class PackResult {
        public BufferedImage packedImage;
        public String description;
    }

    /** 异步执行通道打包 */
    public static CompletableFuture<TaskResult> packChannelsAsync(
            TextureImage rChannel,
            TextureImage gChannel,
            TextureImage bChannel) {

        return CompletableFuture.supplyAsync(() -> {
            BufferedImage r = rChannel != null ? rChannel.getImage() : null;
            BufferedImage g = gChannel != null ? gChannel.getImage() : null;
            BufferedImage b = bChannel != null ? bChannel.getImage() : null;

            BufferedImage result = ChannelPacker.pack(r, g, b);
            String desc = ChannelPacker.describe(
                    rChannel != null ? rChannel.getName() : null,
                    gChannel != null ? gChannel.getName() : null,
                    bChannel != null ? bChannel.getName() : null);

            String resultName = "packed_" +
                    (rChannel != null ? "M" : "") +
                    (gChannel != null ? "R" : "") +
                    (bChannel != null ? "A" : "") +
                    ".png";

            TextureImage packed = new TextureImage(resultName, result);
            return new TaskResult("ChannelPack", packed, "通道打包完成: " + desc);
        });
    }

    /** 异步生成 Mipmap 链 */
    public static CompletableFuture<TaskResult> generateMipmapsAsync(TextureImage source) {
        return CompletableFuture.supplyAsync(() -> {
            List<BufferedImage> mips = MipmapGenerator.generate(source.getImage());
            long memoryBytes = MipmapGenerator.estimateMemory(mips);

            List<TextureImage> mipTextures = new ArrayList<>();
            for (int i = 0; i < mips.size(); i++) {
                String mipName = source.getName().replaceFirst("\\.[^.]+$", "")
                        + "_mip" + i + ".png";
                mipTextures.add(new TextureImage(mipName, mips.get(i)));
            }

            return new TaskResult("MipmapGenerate", mipTextures,
                    String.format("生成 %d 级 Mipmap，总内存 %s",
                            mips.size(), formatBytes(memoryBytes)));
        });
    }

    /** 异步生成法线贴图 */
    public static CompletableFuture<TaskResult> generateNormalMapAsync(
            TextureImage heightMap,
            float strength,
            boolean wrap) {

        return CompletableFuture.supplyAsync(() -> {
            NormalMapGenerator.EdgeMode mode = wrap
                    ? NormalMapGenerator.EdgeMode.WRAP
                    : NormalMapGenerator.EdgeMode.CLAMP;

            BufferedImage result = NormalMapGenerator.generate(
                    heightMap.getImage(), strength, mode);

            String resultName = heightMap.getName().replaceFirst("\\.[^.]+$", "")
                    + "_normal.png";

            TextureImage normalTex = new TextureImage(resultName, result);
            return new TaskResult("NormalMapGenerate", normalTex,
                    String.format("法线贴图生成完成 (strength=%.1f, mode=%s)",
                            strength, mode));
        });
    }

    /** 处理结果 */
    public static class TaskResult {
        public final String taskType;
        public final List<TextureImage> outputs;
        public final String message;

        public TaskResult(String taskType, TextureImage singleOutput, String message) {
            this.taskType = taskType;
            this.outputs = List.of(singleOutput);
            this.message = message;
        }

        public TaskResult(String taskType, List<TextureImage> outputs, String message) {
            this.taskType = taskType;
            this.outputs = outputs;
            this.message = message;
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
