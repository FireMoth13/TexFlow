package com.texturepipeline.engine;

import java.awt.image.BufferedImage;

/**
 * 法线贴图生成器 — 从灰度高度图烘焙切线空间法线贴图。
 *
 * <p>算法：Sobel 3×3 算子计算像素的水平梯度和垂直梯度，
 * 加上深度分量组成法线向量，归一化后编码到 RGB。
 * 结果图呈淡紫色（默认法线 (0,0,1) 的 OpenGL 编码）。</p>
 *
 * <p>vibe coding 要点：
 * - Sobel 核的数学原理见 MCP 搜索结果（gamedev.stackexchange）
 * - 边界处理支持 Clamp（边缘复制）和 Wrap（循环平铺）两种模式
 * - 灰度值取 RGB 三通道平均（美术通常用灰度高度图）</p>
 */
public class NormalMapGenerator {

    /** 默认法线强度（值越小凹凸越深） */
    private static final float DEFAULT_STRENGTH = 2.0f;

    /** 边界处理模式 */
    public enum EdgeMode {
        /** 边缘复制 — 超出边界的像素用最近有效像素 */
        CLAMP,
        /** 循环平铺 — 用于无缝纹理 */
        WRAP
    }

    /**
     * 从高度图生成法线贴图。
     *
     * @param heightMap 灰度高度图（白=高，黑=低）
     * @param strength  法线强度，值越小凹凸越深，推荐 0.5~10.0
     * @param edgeMode  边界处理模式
     * @return 法线贴图（R=法线X，G=法线Y，B=法线Z，A=255）
     */
    public static BufferedImage generate(BufferedImage heightMap,
                                          float strength,
                                          EdgeMode edgeMode) {
        int w = heightMap.getWidth();
        int h = heightMap.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // 采集 3×3 邻域 8 个方向的灰度
                float tl = sampleGrayscale(heightMap, x - 1, y - 1, edgeMode);
                float t  = sampleGrayscale(heightMap, x,     y - 1, edgeMode);
                float tr = sampleGrayscale(heightMap, x + 1, y - 1, edgeMode);
                float l  = sampleGrayscale(heightMap, x - 1, y,     edgeMode);
                float r  = sampleGrayscale(heightMap, x + 1, y,     edgeMode);
                float bl = sampleGrayscale(heightMap, x - 1, y + 1, edgeMode);
                float b  = sampleGrayscale(heightMap, x,     y + 1, edgeMode);
                float br = sampleGrayscale(heightMap, x + 1, y + 1, edgeMode);

                // Sobel 水平梯度：右边减左边（加权）
                float dX = (tr + 2.0f * r + br) - (tl + 2.0f * l + bl);
                // Sobel 垂直梯度：下边减上边（加权）
                float dY = (bl + 2.0f * b + br) - (tl + 2.0f * t + tr);
                // 深度分量：强度越小 → 法线越陡 → 凹凸越明显
                float dZ = 1.0f / strength;

                // 归一化为单位向量
                float length = (float) Math.sqrt(dX * dX + dY * dY + dZ * dZ);
                float nx = dX / length;
                float ny = dY / length;
                float nz = dZ / length;

                // 编码到 RGB：法线分量 [-1, 1] → [0, 255]
                int rEnc = clampToByte((nx + 1.0f) * 127.5f);
                int gEnc = clampToByte((ny + 1.0f) * 127.5f);
                int bEnc = clampToByte((nz + 1.0f) * 127.5f);

                int argb = (0xFF << 24) | (rEnc << 16) | (gEnc << 8) | bEnc;
                result.setRGB(x, y, argb);
            }
        }
        return result;
    }

    /** 便捷方法：使用默认参数 */
    public static BufferedImage generate(BufferedImage heightMap, EdgeMode edgeMode) {
        return generate(heightMap, DEFAULT_STRENGTH, edgeMode);
    }

    /**
     * 采样 3×3 邻域中某一点的灰度值（0.0~1.0）。
     * 坐标越界时根据 edgeMode 处理。
     */
    private static float sampleGrayscale(BufferedImage img,
                                          int x, int y,
                                          EdgeMode edgeMode) {
        int w = img.getWidth();
        int h = img.getHeight();

        switch (edgeMode) {
            case CLAMP:
                x = Math.max(0, Math.min(x, w - 1));
                y = Math.max(0, Math.min(y, h - 1));
                break;
            case WRAP:
                x = ((x % w) + w) % w; // 正确处理负数
                y = ((y % h) + h) % h;
                break;
        }

        int rgb = img.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        // 取平均灰度
        return (r + g + b) / (3.0f * 255.0f);
    }

    /** 把一个 float 钳制到 byte 范围 */
    private static int clampToByte(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }
}
