package com.texturepipeline.ui;

import com.texturepipeline.model.TextureImage;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 操作历史管理器 — 支持撤销/重做。
 *
 * <p>基于状态栈：每次操作前保存当前预览状态，
 * 撤销时回退到上一个状态，重做时恢复到下一个状态。</p>
 */
public class HistoryManager {

    private final Deque<TextureImage> undoStack = new ArrayDeque<>();
    private final Deque<TextureImage> redoStack = new ArrayDeque<>();

    /** 最大历史深度 */
    private static final int MAX_HISTORY = 50;

    /**
     * 在执行新操作前调用，保存当前状态到撤销栈。
     *
     * @param currentState 当前预览状态（操作前）
     */
    public void pushState(TextureImage currentState) {
        if (currentState == null) return;

        undoStack.push(cloneImage(currentState));
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
        // 新操作使重做栈失效
        redoStack.clear();
    }

    /**
     * 执行撤销，返回上一个状态。
     *
     * @return 上一个状态，如果没有可撤销的操作则返回 null
     */
    public TextureImage undo(TextureImage currentState) {
        if (undoStack.isEmpty()) return null;

        // 保存当前状态到重做栈
        redoStack.push(cloneImage(currentState));

        return undoStack.pop();
    }

    /**
     * 执行重做，返回下一个状态。
     *
     * @return 下一个状态，如果没有可重做的操作则返回 null
     */
    public TextureImage redo(TextureImage currentState) {
        if (redoStack.isEmpty()) return null;

        // 保存当前状态到撤销栈
        undoStack.push(cloneImage(currentState));

        return redoStack.pop();
    }

    /** 是否有可撤销的操作 */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** 是否有可重做的操作 */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** 清空历史 */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    /** 深拷贝 TextureImage（复制 BufferedImage） */
    private TextureImage cloneImage(TextureImage src) {
        java.awt.image.BufferedImage srcImg = src.getImage();
        java.awt.image.BufferedImage copy = new java.awt.image.BufferedImage(
                srcImg.getWidth(), srcImg.getHeight(), srcImg.getType());
        copy.getGraphics().drawImage(srcImg, 0, 0, null);
        return new TextureImage(src.getName(), copy);
    }
}
