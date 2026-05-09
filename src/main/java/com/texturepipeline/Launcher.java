package com.texturepipeline;

/**
 * 启动器 — 不继承 Application，避免 JavaFX 模块检查。
 * jpackage 配合时使用此类作为入口。
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
