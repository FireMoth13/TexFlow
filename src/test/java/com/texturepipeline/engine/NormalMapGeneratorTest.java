package com.texturepipeline.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NormalMapGenerator 单元测试
 */
class NormalMapGeneratorTest {

    /** 创建平坦高度图（所有像素同灰度值） */
    private static BufferedImage flatHeightMap(int w, int h, int gray) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int argb = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, argb);
            }
        }
        return img;
    }

    /** 创建带斜面的高度图：左半低，右半高 */
    private static BufferedImage slopedHeightMap(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int gray = x < w / 2 ? 64 : 192;
                int argb = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
                img.setRGB(x, y, argb);
            }
        }
        return img;
    }

    @Test
    @DisplayName("平坦高度图生成默认法线 (128,128,255) — 相当于 (0,0,1)")
    void flatHeightMapProducesDefaultNormal() {
        // 所有像素同值 → 梯度为 0 → 法线 (0,0,1) → 编码后 (128,128,255)
        BufferedImage flat = flatHeightMap(16, 16, 128);
        BufferedImage result = NormalMapGenerator.generate(flat, NormalMapGenerator.EdgeMode.CLAMP);

        // 取中心区域像素（避开边界影响）
        int pixel = result.getRGB(8, 8);
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;

        assertEquals(128, r, 1, "平坦高度图 → 法线 R 应为 128 (无水平倾斜)");
        assertEquals(128, g, 1, "平坦高度图 → 法线 G 应为 128 (无垂直倾斜)");
        assertEquals(255, b, 1, "平坦高度图 → 法线 B 应接近 255 (朝上)");
    }

    @Test
    @DisplayName("左低右高的斜面 → 法线 R 分量 > 128（法线朝右）")
    void slopedHeightMapRComponent() {
        // 左黑右白 → 水平梯度正方向 → 法线 X 分量 > 0 → R > 128
        BufferedImage sloped = slopedHeightMap(16, 16);
        BufferedImage result = NormalMapGenerator.generate(sloped, NormalMapGenerator.EdgeMode.CLAMP);

        // 取分界线附近的像素
        int pixel = result.getRGB(8, 8);
        int r = (pixel >> 16) & 0xFF;

        assertTrue(r > 128, "左低右高斜面应使 R 分量 > 128，实际为 " + r);
    }

    @Test
    @DisplayName("Strength 越大凹凸感越强（法线越偏离默认方向）")
    void higherStrengthProducesStrongerBump() {
        BufferedImage sloped = slopedHeightMap(16, 16);

        BufferedImage weakBump = NormalMapGenerator.generate(sloped, 1.0f,
                NormalMapGenerator.EdgeMode.CLAMP);
        BufferedImage strongBump = NormalMapGenerator.generate(sloped, 10.0f,
                NormalMapGenerator.EdgeMode.CLAMP);

        // dZ = 1.0/strength，strength 越大 → dZ 越小 → 法线越平 → 偏离默认值越大
        int pixelWeak = weakBump.getRGB(8, 8);
        int pixelStrong = strongBump.getRGB(8, 8);

        int rWeak = (pixelWeak >> 16) & 0xFF;
        int rStrong = (pixelStrong >> 16) & 0xFF;

        // strength=10 的 R 分量偏离 128 更大（法线更倾斜）
        int deviationWeak = Math.abs(rWeak - 128);
        int deviationStrong = Math.abs(rStrong - 128);

        assertTrue(deviationStrong > deviationWeak,
                "Strength=10 时 R 偏离度 (" + deviationStrong 
                + ") 应大于 Strength=1 时 (" + deviationWeak + ")");
    }

    @Test
    @DisplayName("输出格式为 TYPE_INT_ARGB")
    void outputIsArgb() {
        BufferedImage flat = flatHeightMap(8, 8, 128);
        BufferedImage result = NormalMapGenerator.generate(flat,
                NormalMapGenerator.EdgeMode.CLAMP);

        assertEquals(BufferedImage.TYPE_INT_ARGB, result.getType());
    }

    @Test
    @DisplayName("输出尺寸与输入相同")
    void outputSameSizeAsInput() {
        BufferedImage height = flatHeightMap(32, 24, 128);
        BufferedImage result = NormalMapGenerator.generate(height,
                NormalMapGenerator.EdgeMode.CLAMP);

        assertEquals(32, result.getWidth());
        assertEquals(24, result.getHeight());
    }

    @Test
    @DisplayName("CLAMP 模式不抛异常")
    void clampModeWorks() {
        BufferedImage flat = flatHeightMap(4, 4, 128);
        assertDoesNotThrow(() ->
                NormalMapGenerator.generate(flat, NormalMapGenerator.EdgeMode.CLAMP));
    }

    @Test
    @DisplayName("WRAP 模式不抛异常")
    void wrapModeWorks() {
        BufferedImage flat = flatHeightMap(4, 4, 128);
        assertDoesNotThrow(() ->
                NormalMapGenerator.generate(flat, NormalMapGenerator.EdgeMode.WRAP));
    }

    @Test
    @DisplayName("便捷方法使用默认 strength=2.0")
    void defaultStrengthIs2() {
        BufferedImage flat = flatHeightMap(4, 4, 192);
        BufferedImage explicit = NormalMapGenerator.generate(flat, 2.0f,
                NormalMapGenerator.EdgeMode.CLAMP);
        BufferedImage defaulted = NormalMapGenerator.generate(flat,
                NormalMapGenerator.EdgeMode.CLAMP);

        // 两者应生成相同结果
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                assertEquals(explicit.getRGB(x, y), defaulted.getRGB(x, y),
                        "默认 strength 便捷方法应与显式指定 2.0 结果一致");
            }
        }
    }
}
