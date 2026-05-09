package com.texturepipeline.engine;

import com.texturepipeline.model.TextureImage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 纹理格式转换器 — PNG ↔ WebP 格式互转。
 *
 * <p>游戏开发场景：PNG 是美术交换格式，WebP 提供更高压缩比
 * 适合运行时贴图，减少包体和显存占用。
 * 使用 ImageIO SPI 机制，依赖 webp-imageio 原生库（内置在 jar 中）。</p>
 */
public class FormatConverter {

    /** 默认 WebP 有损压缩质量 (0~100) */
    public static final float DEFAULT_QUALITY = 80f;

    /**
     * 将 BufferedImage 导出为 WebP。
     *
     * @param image    源图像
     * @param output   输出路径
     * @param quality  压缩质量 0~100（越高越清晰，文件越大）
     */
    public static void toWebP(BufferedImage image, Path output, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality / 100f);
        }

        File outFile = output.toFile();
        outFile.getParentFile().mkdirs();

        try (FileImageOutputStream fos = new FileImageOutputStream(outFile)) {
            writer.setOutput(fos);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    /**
     * 便捷方法：使用默认质量导出 WebP。
     */
    public static void toWebP(BufferedImage image, Path output) throws IOException {
        toWebP(image, output, DEFAULT_QUALITY);
    }

    /**
     * 将 TextureImage 导出为 WebP（保留原始文件名，仅改扩展名）。
     */
    public static Path toWebP(TextureImage texture, Path outputDir, float quality) throws IOException {
        String name = texture.getName().replaceFirst("\\.[^.]+$", "") + ".webp";
        Path output = outputDir.resolve(name);
        toWebP(texture.getImage(), output, quality);
        return output;
    }

    /**
     * 批量转换：多个 TextureImage → WebP。
     *
     * @param images    输入纹理列表
     * @param outputDir 输出目录
     * @param quality   压缩质量
     * @param callback  进度回调
     * @return 成功导出的文件路径列表
     */
    public static List<Path> batchToWebP(List<TextureImage> images,
                                          Path outputDir,
                                          float quality,
                                          BatchProcessor.ProgressCallback callback) throws IOException {
        List<Path> results = new ArrayList<>();
        int total = images.size();
        for (int i = 0; i < total; i++) {
            TextureImage img = images.get(i);
            Path out = toWebP(img, outputDir, quality);
            results.add(out);
            if (callback != null) {
                callback.onProgress(i + 1, total, img.getName());
            }
        }
        return results;
    }

    /** 获取 WebP 压缩质量描述 */
    public static String qualityDescription(float quality) {
        if (quality >= 95) return "无损/近无损";
        if (quality >= 75) return "高质量";
        if (quality >= 50) return "中等质量";
        return "高压缩";
    }
}
