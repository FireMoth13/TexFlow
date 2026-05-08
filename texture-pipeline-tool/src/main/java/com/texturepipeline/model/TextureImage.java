package com.texturepipeline.model;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 纹理数据对象 — 封装游戏纹理的像素数据、元信息和处理状态。
 * 字段设计：不可变像素数据 + 可变元信息（类似 DTO + Entity 混合模式）。
 */
public class TextureImage {
    private final Path sourcePath;
    private final String name;
    private final BufferedImage image;
    private final int width;
    private final int height;
    private String status;       // pending, processing, done, error
    private String errorMessage; // 处理失败时的错误信息

    public TextureImage(Path sourcePath) throws IOException {
        this.sourcePath = sourcePath;
        this.name = sourcePath.getFileName().toString();
        File file = sourcePath.toFile();
        if (!file.exists()) {
            throw new IOException("文件不存在: " + sourcePath);
        }
        // 读取为兼容格式 (ARGB)，保证像素处理一致性
        BufferedImage raw = ImageIO.read(file);
        if (raw == null) {
            throw new IOException("无法读取图片: " + sourcePath);
        }
        this.image = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
        this.image.getGraphics().drawImage(raw, 0, 0, null);
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.status = "pending";
    }

    /** 用一个已有的 BufferedImage 创建（用于处理结果） */
    public TextureImage(String name, BufferedImage image) {
        this.sourcePath = null;
        this.name = name;
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.status = "done";
    }

    // --- getters ---

    public Path getSourcePath() { return sourcePath; }
    public String getName() { return name; }
    public BufferedImage getImage() { return image; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }

    public void setStatus(String status) { this.status = status; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    /** 把纹理保存到指定路径 */
    public void saveTo(Path outputPath, String format) throws IOException {
        File out = outputPath.toFile();
        out.getParentFile().mkdirs();
        ImageIO.write(image, format, out);
    }

    @Override
    public String toString() {
        return String.format("%s (%dx%d) [%s]", name, width, height, status);
    }
}
