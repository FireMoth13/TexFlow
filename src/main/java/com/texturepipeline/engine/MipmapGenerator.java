package com.texturepipeline.engine;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Mipmap 生成器 — 为纹理生成多级缩小的 Mipmap 链。
 *
 * <p>为什么需要 Mipmap：游戏渲染时，远处的物体用低分辨率纹理，避免摩尔纹和
 * 采样性能问题。Mipmap 链是原图的 1/2、1/4、1/8... 系列缩小版本。</p>
 *
 * <p>算法思路（vibe coding 要点）：
 * 每级 Mipmap 是上一级画布长宽各除以 2，用 Java 2D 的 BILINEAR 插值缩放。
 * 一直缩到 1px 为止。结果返回 List，索引 0 是原图。</p>
 */
public class MipmapGenerator {

    /** 生成完整的 Mipmap 链，level 0 = 原图 */
    public static List<BufferedImage> generate(BufferedImage source) {
        List<BufferedImage> mips = new ArrayList<>();
        mips.add(source); // level 0

        int w = source.getWidth();
        int h = source.getHeight();

        while (w > 1 || h > 1) {
            w = Math.max(1, w / 2);
            h = Math.max(1, h / 2);
            BufferedImage mip = downsample(mips.get(mips.size() - 1), w, h);
            mips.add(mip);
        }
        return mips;
    }

    /** 双线性插值缩小到目标尺寸 */
    private static BufferedImage downsample(BufferedImage src, int targetW, int targetH) {
        BufferedImage dst = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        return dst;
    }

    /** 生成指定数量的 Mipmap 级别 */
    public static List<BufferedImage> generate(BufferedImage source, int maxLevels) {
        List<BufferedImage> fullChain = generate(source);
        if (fullChain.size() <= maxLevels) return fullChain;
        return fullChain.subList(0, maxLevels);
    }

    /** 计算总内存占用（字节） */
    public static long estimateMemory(List<BufferedImage> mips) {
        long total = 0;
        for (BufferedImage mip : mips) {
            // TYPE_INT_ARGB = 4 bytes per pixel
            total += (long) mip.getWidth() * mip.getHeight() * 4;
        }
        return total;
    }
}
