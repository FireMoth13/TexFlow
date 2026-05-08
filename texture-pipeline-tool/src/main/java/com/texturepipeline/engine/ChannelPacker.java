package com.texturepipeline.engine;

import java.awt.image.BufferedImage;

/**
 * 通道打包器 — 将多张灰度图合并到一张纹理的 RGB 通道中。
 *
 * <p>游戏纹理工作流核心：PBR 材质常用 MRAO 打包 ——
 * R 通道 = Metallic（金属度）、G 通道 = Roughness（粗糙度）、
 * B 通道 = Ambient Occlusion（环境光遮蔽）。
 * 这样一张纹理存三种信息，节省显存和采样次数。</p>
 *
 * <p>算法思路（vibe coding 要点）：
 * 逐像素遍历，从三张输入图取对应像素的灰度值，写入输出图的 RGB 通道。
 * 如果只提供 1-2 张图，缺失的通道填默认值（白=1.0）。</p>
 */
public class ChannelPacker {

    /** 合并 R、G、B 三张图到一张输出纹理 */
    public static BufferedImage pack(BufferedImage rChannel,
                                      BufferedImage gChannel,
                                      BufferedImage bChannel) {
        int w = 0, h = 0;
        if (rChannel != null) { w = rChannel.getWidth(); h = rChannel.getHeight(); }
        if (gChannel != null) { w = Math.max(w, gChannel.getWidth()); h = Math.max(h, gChannel.getHeight()); }
        if (bChannel != null) { w = Math.max(w, bChannel.getWidth()); h = Math.max(h, bChannel.getHeight()); }

        if (w == 0 || h == 0) {
            throw new IllegalArgumentException("至少需要提供一张输入图");
        }

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = sampleChannel(rChannel, x, y);
                int g = sampleChannel(gChannel, x, y);
                int b = sampleChannel(bChannel, x, y);

                // 打包到 RGB，Alpha 置为不透明
                int argb = (0xFF << 24) | (r << 16) | (g << 8) | b;
                result.setRGB(x, y, argb);
            }
        }
        return result;
    }

    /** 从单通道图中采样灰度值，越界或 null 返回 255（白=1.0 即无效果） */
    private static int sampleChannel(BufferedImage img, int x, int y) {
        if (img == null) return 255;
        if (x >= img.getWidth() || y >= img.getHeight()) return 255;

        int rgb = img.getRGB(x, y);
        // 取灰度（RGB 三通道取平均，美术通常用灰度图所以三通道相同）
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r + g + b) / 3;
    }

    /** 便捷方法：从文件名展示打包结果说明 */
    public static String describe(String rName, String gName, String bName) {
        return String.format("R=%s | G=%s | B=%s",
                rName != null ? rName : "white(1.0)",
                gName != null ? gName : "white(1.0)",
                bName != null ? bName : "white(1.0)");
    }
}
