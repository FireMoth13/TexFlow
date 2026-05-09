package com.texturepipeline.engine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * 纹理压缩器 — 纯 Java 实现 BC1 (DXT1) GPU 块压缩。
 *
 * <p>BC1 是 DirectX 时代最经典的纹理压缩格式，至今所有桌面/主机 GPU 均原生支持。
 * 算法原理：将图像按 4×4 像素块分割，每块找到颜色极值（min/max），
 * 生成 4 色调色板（两端点 + 2 个线性插值），每个像素用 2-bit 索引指向调色板。
 * 压缩比固定 8:1（RGBA→64bit/block）。</p>
 *
 * <p>算法参考：Rich Geldreich (BC1 编码器原作者)、nothings/stb_dxt、DirectXTex</p>
 */
public class TextureCompressor {

    // DDS 魔数
    private static final int DDS_MAGIC = 0x20534444; // "DDS "

    // DDS 像素格式标志
    private static final int DDPF_FOURCC = 0x4;

    // DDS 头标志
    private static final int DDSD_CAPS = 0x1;
    private static final int DDSD_HEIGHT = 0x2;
    private static final int DDSD_WIDTH = 0x4;
    private static final int DDSD_PIXELFORMAT = 0x1000;
    private static final int DDSD_LINEARSIZE = 0x80000;

    // DDS 功能标志
    private static final int DDSCAPS_TEXTURE = 0x1000;

    /**
     * 将 BufferedImage 压缩为 BC1 DXT1 格式并写入 DDS 文件。
     *
     * @param image  输入 RGBA 图像（尺寸须为 4 的倍数）
     * @param output 输出 .dds 文件路径
     */
    public static void compressToDDS(BufferedImage image, Path output) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();

        // BC1 要求 4×4 对齐
        if (width % 4 != 0 || height % 4 != 0) {
            throw new IllegalArgumentException(
                    "BC1 压缩要求宽高为 4 的倍数，当前: " + width + "x" + height);
        }

        // 读取 RGBA 像素
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        // 压缩所有 4×4 块
        int blocksX = width / 4;
        int blocksY = height / 4;
        int blockCount = blocksX * blocksY;
        byte[] compressed = new byte[blockCount * 8];

        for (int by = 0; by < blocksY; by++) {
            for (int bx = 0; bx < blocksX; bx++) {
                compressBlock(pixels, width, bx * 4, by * 4, compressed,
                        (by * blocksX + bx) * 8);
            }
        }

        // 写入 DDS 文件
        writeDDS(output, compressed, width, height);
    }

    /**
     * 压缩单个 4×4 像素块为 BC1 格式（8 字节）。
     *
     * <p>输出格式：
     * [0-1] c0 RGB565 (little-endian)
     * [2-3] c1 RGB565 (little-endian)
     * [4-7] 16 个 2-bit 索引，每个像素映射到 {c0, c1, c2, c3}</p>
     */
    static void compressBlock(int[] pixels, int stride,
                               int bx, int by, byte[] out, int outOffset) {
        // 收集 16 个像素
        int[] block = new int[16];
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                block[y * 4 + x] = pixels[(by + y) * stride + (bx + x)];
            }
        }

        // 找出 min/max 颜色（用 RGB 亮度排序）
        int minColor = block[0] & 0x00FFFFFF;
        int maxColor = minColor;

        for (int i = 1; i < 16; i++) {
            int c = block[i] & 0x00FFFFFF;
            if (colorLuminance(c) < colorLuminance(minColor)) minColor = c;
            if (colorLuminance(c) > colorLuminance(maxColor)) maxColor = c;
        }

        // 根据 min/max 关系决定色板编码顺序
        int c0, c1;
        if (minColor > maxColor) {
            // swap: BC1 要求 c0 > c1 用于 opaque 模式
            c0 = maxColor;
            c1 = minColor;
        } else {
            c0 = maxColor;
            c1 = minColor;
        }

        // 量化到 RGB565
        int c0_565 = rgbTo565(c0);
        int c1_565 = rgbTo565(c1);

        // 生成 4 色调色板
        int[] palette = new int[4];
        palette[0] = rgb565To888(c0_565);
        palette[1] = rgb565To888(c1_565);

        if (c0_565 > c1_565) {
            // 标准 opaque 模式: c2 = 2/3*c0 + 1/3*c1,  c3 = 1/3*c0 + 2/3*c1
            palette[2] = interpolateColor(palette[0], palette[1], 2, 3);
            palette[3] = interpolateColor(palette[0], palette[1], 1, 3);
        } else {
            // c0 ≤ c1: 1-bit alpha 模式 (c2=c3 transparent black)
            palette[2] = interpolateColor(palette[0], palette[1], 1, 2);
            palette[3] = 0; // transparent
        }

        // 为每个像素分配最近调色板索引
        int indices = 0;
        for (int i = 0; i < 16; i++) {
            int pixel = block[i] & 0x00FFFFFF;
            int bestIdx = 0;
            int bestDist = Integer.MAX_VALUE;
            for (int j = 0; j < 4; j++) {
                int dist = colorDistance(pixel, palette[j]);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestIdx = j;
                }
            }
            indices |= (bestIdx << (i * 2));
        }

        // 写入输出（little-endian）
        out[outOffset]     = (byte) (c0_565 & 0xFF);
        out[outOffset + 1] = (byte) ((c0_565 >> 8) & 0xFF);
        out[outOffset + 2] = (byte) (c1_565 & 0xFF);
        out[outOffset + 3] = (byte) ((c1_565 >> 8) & 0xFF);
        out[outOffset + 4] = (byte) (indices & 0xFF);
        out[outOffset + 5] = (byte) ((indices >> 8) & 0xFF);
        out[outOffset + 6] = (byte) ((indices >> 16) & 0xFF);
        out[outOffset + 7] = (byte) ((indices >> 24) & 0xFF);
    }

    // --- 辅助方法 ---

    /** RGB 亮度（加权和，用于 min/max 排序） */
    private static int colorLuminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r + g * 2 + b; // 粗略亮度
    }

    /** RGB888 → RGB565 */
    private static int rgbTo565(int rgb) {
        int r = ((rgb >> 16) & 0xFF) >> 3;
        int g = ((rgb >> 8)  & 0xFF) >> 2;
        int b = (rgb & 0xFF) >> 3;
        return (r << 11) | (g << 5) | b;
    }

    /** RGB565 → RGB888 */
    private static int rgb565To888(int c565) {
        int r = ((c565 >> 11) & 0x1F) << 3;
        int g = ((c565 >> 5)  & 0x3F) << 2;
        int b = (c565 & 0x1F) << 3;
        // 扩展低 bit 减少量化误差
        r |= (r >> 5);
        g |= (g >> 6);
        b |= (b >> 5);
        return (r << 16) | (g << 8) | b;
    }

    /** 线性插值：c0 * (num - den) / den + c1 * num / den */
    private static int interpolateColor(int c0, int c1, int num, int den) {
        int r = ((c0 >> 16) & 0xFF) * (den - num) / den + ((c1 >> 16) & 0xFF) * num / den;
        int g = ((c0 >> 8)  & 0xFF) * (den - num) / den + ((c1 >> 8)  & 0xFF) * num / den;
        int b = (c0 & 0xFF) * (den - num) / den + (c1 & 0xFF) * num / den;
        return (r << 16) | (g << 8) | b;
    }

    /** RGB 欧几里得距离平方 */
    private static int colorDistance(int c1, int c2) {
        int dr = ((c1 >> 16) & 0xFF) - ((c2 >> 16) & 0xFF);
        int dg = ((c1 >> 8)  & 0xFF) - ((c2 >> 8)  & 0xFF);
        int db = (c1 & 0xFF) - (c2 & 0xFF);
        return dr * dr + dg * dg + db * db;
    }

    /**
     * 写入 DDS 文件头 + 压缩数据。
     *
     * <p>DDS 文件结构：
     * Magic (4B) | DDS_HEADER (124B) | Compressed Blocks</p>
     */
    private static void writeDDS(Path output, byte[] compressed,
                                  int width, int height) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);

        int blocksX = Math.max(1, width / 4);
        int blocksY = Math.max(1, height / 4);
        int pitch = Math.max(1, blocksX * 8); // BC1 block = 8 bytes

        // Magic
        header.putInt(DDS_MAGIC);

        // dwSize (header size = 124)
        header.putInt(124);

        // dwFlags
        header.putInt(DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH
                | DDSD_PIXELFORMAT | DDSD_LINEARSIZE);

        // dwHeight, dwWidth
        header.putInt(height);
        header.putInt(width);

        // dwPitchOrLinearSize
        header.putInt(blocksX * blocksY * 8);

        // dwDepth (unused)
        header.putInt(0);

        // dwMipMapCount (no mipmaps in compressed file)
        header.putInt(1);

        // dwReserved1[11] = 0
        for (int i = 0; i < 11; i++) header.putInt(0);

        // DDS_PIXELFORMAT (32 bytes)
        header.putInt(32); // dwSize
        header.putInt(DDPF_FOURCC); // dwFlags
        header.putInt(0x31545844);  // dwFourCC = "DXT1"
        header.putInt(0); // dwRGBBitCount
        // dwRBitMask, dwGBitMask, dwBBitMask, dwABitMask = 0 (for DXT1)
        header.putInt(0);
        header.putInt(0);
        header.putInt(0);
        header.putInt(0);

        // dwCaps
        header.putInt(DDSCAPS_TEXTURE);
        // dwCaps2-4
        header.putInt(0);
        header.putInt(0);
        header.putInt(0);

        // dwReserved2
        header.putInt(0);

        // 写入文件
        header.flip();
        try (OutputStream fos = Files.newOutputStream(output);
             WritableByteChannel channel = Channels.newChannel(fos)) {
            channel.write(header);
            channel.write(ByteBuffer.wrap(compressed));
        }
    }

    /** 获取压缩后的大小估算 (bytes) */
    public static int compressedSize(int width, int height) {
        return Math.max(1, (width / 4) * (height / 4) * 8);
    }
}
