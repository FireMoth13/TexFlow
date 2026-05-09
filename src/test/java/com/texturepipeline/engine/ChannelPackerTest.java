package com.texturepipeline.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChannelPacker 单元测试
 */
class ChannelPackerTest {

    /** 创建一个指定灰度值的 4×4 测试图 */
    private static BufferedImage grayImage(int gray) {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        int argb = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                img.setRGB(x, y, argb);
            }
        }
        return img;
    }

    @Test
    @DisplayName("三张图打包到 RGB 通道")
    void packThreeChannels() {
        BufferedImage r = grayImage(200);  // R=200
        BufferedImage g = grayImage(100);  // G=100
        BufferedImage b = grayImage(50);   // B=50

        BufferedImage result = ChannelPacker.pack(r, g, b);

        // 检查中间像素
        int pixel = result.getRGB(2, 2);
        int actualR = (pixel >> 16) & 0xFF;
        int actualG = (pixel >> 8) & 0xFF;
        int actualB = pixel & 0xFF;
        int actualA = (pixel >> 24) & 0xFF;

        assertEquals(200, actualR, "R 通道值应为 200");
        assertEquals(100, actualG, "G 通道值应为 100");
        assertEquals(50, actualB, "B 通道值应为 50");
        assertEquals(255, actualA, "Alpha 应不透明");
    }

    @Test
    @DisplayName("null 通道默认填白色(255)")
    void nullChannelDefaultsToWhite() {
        BufferedImage r = grayImage(200);

        // 只提供 R 通道，G 和 B 应为 null → 填白
        BufferedImage result = ChannelPacker.pack(r, null, null);

        int pixel = result.getRGB(2, 2);
        int actualR = (pixel >> 16) & 0xFF;
        int actualG = (pixel >> 8) & 0xFF;
        int actualB = pixel & 0xFF;

        assertEquals(200, actualR, "R 通道应来自输入");
        assertEquals(255, actualG, "G 通道应为默认白");
        assertEquals(255, actualB, "B 通道应为默认白");
    }

    @Test
    @DisplayName("全部 null 抛异常")
    void allNullThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ChannelPacker.pack(null, null, null));
    }

    @Test
    @DisplayName("输入图尺寸不一致时取最大尺寸")
    void differentSizesUsesMaxDimensions() {
        BufferedImage small = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        small.setRGB(0, 0, 0xFFFF0000);
        BufferedImage large = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        large.setRGB(0, 0, 0xFF00FF00);

        BufferedImage result = ChannelPacker.pack(small, large, null);

        assertEquals(8, result.getWidth(), "宽度应取最大值");
        assertEquals(8, result.getHeight(), "高度应取最大值");
    }

    @Test
    @DisplayName("输出图类型为 TYPE_INT_ARGB")
    void outputIsArgb() {
        BufferedImage r = grayImage(128);
        BufferedImage result = ChannelPacker.pack(r, null, null);

        assertEquals(BufferedImage.TYPE_INT_ARGB, result.getType(),
                "输出图必须是 TYPE_INT_ARGB 格式");
    }

    @Test
    @DisplayName("describe 方法格式正确")
    void describeFormat() {
        String desc = ChannelPacker.describe("metal.png", "rough.png", "ao.png");
        assertEquals("R=metal.png | G=rough.png | B=ao.png", desc);

        String desc2 = ChannelPacker.describe("metal.png", null, null);
        assertTrue(desc2.contains("white(1.0)"));
    }
}
