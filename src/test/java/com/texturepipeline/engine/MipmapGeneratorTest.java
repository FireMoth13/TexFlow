package com.texturepipeline.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MipmapGenerator 单元测试
 */
class MipmapGeneratorTest {

    /** 创建指定尺寸的测试图（填满随机/固定像素无所谓，只测尺寸逻辑） */
    private static BufferedImage sizedImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        // 填点非零像素，避免纯黑
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, 0xFF808080);
            }
        }
        return img;
    }

    @Test
    @DisplayName("64×64 图应生成 7 级 Mipmap (64→32→16→8→4→2→1)")
    void generateFullChain() {
        BufferedImage src = sizedImage(64, 64);
        List<BufferedImage> mips = MipmapGenerator.generate(src);

        assertEquals(7, mips.size(), "64×64 应有 7 级 (log2(64)+1)");
        assertEquals(64, mips.get(0).getWidth());
        assertEquals(64, mips.get(0).getHeight());
        assertEquals(32, mips.get(1).getWidth());
        assertEquals(32, mips.get(1).getHeight());
        assertEquals(16, mips.get(2).getWidth());
        assertEquals(8, mips.get(3).getWidth());
        assertEquals(4, mips.get(4).getWidth());
        assertEquals(2, mips.get(5).getWidth());
        assertEquals(1, mips.get(6).getWidth());
    }

    @Test
    @DisplayName("256×256 图应生成 9 级 Mipmap")
    void generate256Chain() {
        BufferedImage src = sizedImage(256, 256);
        List<BufferedImage> mips = MipmapGenerator.generate(src);

        assertEquals(9, mips.size(), "256×256 应有 9 级");
        assertEquals(256, mips.get(0).getWidth());
        assertEquals(1, mips.get(mips.size() - 1).getWidth());
    }

    @Test
    @DisplayName("所有 Mipmap 级别均为 TYPE_INT_ARGB")
    void allLevelsAreArgb() {
        BufferedImage src = sizedImage(32, 32);
        List<BufferedImage> mips = MipmapGenerator.generate(src);

        for (int i = 0; i < mips.size(); i++) {
            assertEquals(BufferedImage.TYPE_INT_ARGB, mips.get(i).getType(),
                    "Mipmap level " + i + " 应为 TYPE_INT_ARGB");
        }
    }

    @Test
    @DisplayName("maxLevels 参数截断 Mipmap 链")
    void maxLevelsTruncates() {
        BufferedImage src = sizedImage(64, 64);
        List<BufferedImage> mips = MipmapGenerator.generate(src, 3);

        assertEquals(3, mips.size(), "maxLevels=3 应只返回前 3 级");
        assertEquals(64, mips.get(0).getWidth());
        assertEquals(32, mips.get(1).getWidth());
        assertEquals(16, mips.get(2).getWidth());
    }

    @Test
    @DisplayName("maxLevels 大于实际级数时返回全部")
    void maxLevelsLargerThanAvailable() {
        BufferedImage src = sizedImage(8, 8);
        // 8→4→2→1 = 4 级，maxLevels=10
        List<BufferedImage> mips = MipmapGenerator.generate(src, 10);

        assertEquals(4, mips.size(), "不应超过最大可用级数");
    }

    @Test
    @DisplayName("1×1 图只生成 1 级")
    void singlePixelImage() {
        BufferedImage src = sizedImage(1, 1);
        List<BufferedImage> mips = MipmapGenerator.generate(src);

        assertEquals(1, mips.size());
        assertEquals(1, mips.get(0).getWidth());
    }

    @Test
    @DisplayName("内存估算值正确")
    void estimateMemory() {
        BufferedImage src = sizedImage(64, 64);
        List<BufferedImage> mips = MipmapGenerator.generate(src);
        long bytes = MipmapGenerator.estimateMemory(mips);

        // 64²+32²+16²+8²+4²+2²+1² = 5461 像素 × 4 字节 = 21844
        long expected = (long) (64 * 64 + 32 * 32 + 16 * 16 + 8 * 8 + 4 * 4 + 2 * 2 + 1 * 1) * 4;
        assertEquals(expected, bytes, "总内存应等于各 Mip 像素数 × 4 字节");
    }
}
