package com.texturepipeline.engine;

import com.texturepipeline.model.TextureImage;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批量处理器 — 多线程并行处理多张纹理。
 *
 * <p>游戏开发常见场景：导入一整组高度图，一次性批量烘焙法线贴图；
 * 或对一整组纹理批量生成 Mipmap。使用固定线程池并行处理。</p>
 */
public class BatchProcessor {

    private final ExecutorService executor;
    private final int threadCount;

    /** 创建批量处理器，线程数 = CPU 核心数 */
    public BatchProcessor() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public BatchProcessor(int threadCount) {
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    /**
     * 批量生成法线贴图。
     *
     * @param images   输入高度图列表
     * @param strength 法线强度
     * @param wrap     是否循环平铺边界
     * @param callback 进度回调（每完成一张触发）
     * @return 生成的法线贴图列表
     */
    public List<TextureImage> batchNormalMaps(List<TextureImage> images,
                                               float strength,
                                               boolean wrap,
                                               ProgressCallback callback) {
        NormalMapGenerator.EdgeMode mode = wrap
                ? NormalMapGenerator.EdgeMode.WRAP
                : NormalMapGenerator.EdgeMode.CLAMP;

        AtomicInteger completed = new AtomicInteger(0);
        int total = images.size();
        List<TextureImage> results = new ArrayList<>();

        List<java.util.concurrent.Future<TextureImage>> futures = new ArrayList<>();

        for (TextureImage img : images) {
            futures.add(executor.submit(() -> {
                BufferedImage result = NormalMapGenerator.generate(
                        img.getImage(), strength, mode);

                String resultName = img.getName().replaceFirst("\\.[^.]+$", "")
                        + "_normal.png";
                TextureImage tex = new TextureImage(resultName, result);

                int done = completed.incrementAndGet();
                if (callback != null) {
                    callback.onProgress(done, total, img.getName());
                }
                return tex;
            }));
        }

        // 收集结果（保持顺序）
        for (var future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add(null); // 标记失败
            }
        }
        return results;
    }

    /**
     * 批量生成 Mipmap。
     *
     * @param images   输入纹理列表
     * @param callback 进度回调
     * @return 每张图的 Mipmap 链列表
     */
    public List<List<BufferedImage>> batchMipmaps(List<TextureImage> images,
                                                   ProgressCallback callback) {
        AtomicInteger completed = new AtomicInteger(0);
        int total = images.size();
        List<List<BufferedImage>> results = new ArrayList<>();

        List<java.util.concurrent.Future<List<BufferedImage>>> futures = new ArrayList<>();

        for (TextureImage img : images) {
            futures.add(executor.submit(() -> {
                List<BufferedImage> mips = MipmapGenerator.generate(img.getImage());
                int done = completed.incrementAndGet();
                if (callback != null) {
                    callback.onProgress(done, total, img.getName());
                }
                return mips;
            }));
        }

        for (var future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add(null);
            }
        }
        return results;
    }

    /** 关闭线程池 */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public int getThreadCount() { return threadCount; }

    /** 进度回调接口 */
    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int completed, int total, String currentName);
    }
}
