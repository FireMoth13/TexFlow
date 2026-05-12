package com.texturepipeline;

import com.texturepipeline.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 纹理流水线工具 — 入口类。
 *
 * <p>游戏开发者用于：
 * 1. 通道打包：把 Metallic / Roughness / AO 三张灰度图合并为一张 PBR 纹理
 * 2. Mipmap 生成：自动生成完整的 Mipmap 链
 * 3. 批处理流水线（后续扩展）</p>
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        MainWindow mainWindow = new MainWindow();
        Scene scene = new Scene(mainWindow.getRoot(), 1200, 800);

        // 加载样式
        String css = getClass().getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("TexFlow v0.1 — PBR 纹理处理器");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
