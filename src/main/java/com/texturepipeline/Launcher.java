package com.texturepipeline;

import javafx.application.Application;

/**
 * 启动器 — jpackage 打包入口。
 *
 * <p>不直接继承 Application，避免模块系统在 classpath/module-path 混合模式下
 * 反射查找失败。显式传入 App.class 确保 JavaFX 正确识别启动类。</p>
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
