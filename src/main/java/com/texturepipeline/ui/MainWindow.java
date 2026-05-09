package com.texturepipeline.ui;

import com.texturepipeline.engine.BatchProcessor;
import com.texturepipeline.engine.FormatConverter;
import com.texturepipeline.engine.PipelineEngine;
import com.texturepipeline.engine.TextureCompressor;
import com.texturepipeline.model.TextureImage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 主窗口 — 工具的核心 UI。
 *
 * <p>布局：左侧文件列表 + 中间预览 + 右侧控制面板 + 底部日志。
 * 采用 BorderPane 骨架 + HBox（中左中右）经典的桌面工具布局。</p>
 */
public class MainWindow {

    private final BorderPane root;
    private final VBox fileListPanel;
    private VBox dropZone; // 文件拖放区域，动态更新
    private final ImagePreview imagePreview;
    private final PipelinePanel pipelinePanel;
    private final TextArea logArea;
    private final List<TextureImage> loadedImages = new ArrayList<>();

    // 通道选择下拉框
    private final ComboBox<String> rChannelCombo;
    private final ComboBox<String> gChannelCombo;
    private final ComboBox<String> bChannelCombo;

    public MainWindow() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e2e;");

        // === 顶部工具栏 ===
        ToolBar toolbar = createToolbar();
        root.setTop(toolbar);

        // === 左侧：文件列表 ===
        fileListPanel = createFileListPanel();

        // === 中间：图像预览 ===
        imagePreview = new ImagePreview();

        // === 右侧：控制面板 ===
        pipelinePanel = new PipelinePanel(this);
        rChannelCombo = pipelinePanel.getRChannelCombo();
        gChannelCombo = pipelinePanel.getGChannelCombo();
        bChannelCombo = pipelinePanel.getBChannelCombo();

        // === 底部：日志 ===
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(5);
        logArea.setStyle("-fx-control-inner-background: #181825; -fx-text-fill: #cdd6f4;");
        logArea.setPromptText("处理日志...");

        // 中间区域：文件列表 | 预览 | 控制面板
        SplitPane centerSplit = new SplitPane();
        centerSplit.getItems().addAll(fileListPanel, imagePreview.getRoot(), pipelinePanel.getRoot());
        centerSplit.setDividerPositions(0.20, 0.65);

        root.setCenter(centerSplit);
        root.setBottom(logArea);

        log("纹理流水线工具已就绪。拖拽图片到左侧列表，或点击\"导入图片\"。");
        log("支持的操作：通道打包(MRAO)、Mipmap 生成。");
    }

    private ToolBar createToolbar() {
        Button importBtn = new Button("📁 导入图片");
        importBtn.setOnAction(e -> importImages());

        Button clearBtn = new Button("🗑 清空列表");
        clearBtn.setOnAction(e -> clearAll());

        ToolBar toolbar = new ToolBar(importBtn, clearBtn);
        toolbar.setStyle("-fx-background-color: #313244;");
        return toolbar;
    }

    private VBox createFileListPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(8));
        panel.setStyle("-fx-background-color: #181825;");

        Label title = new Label("📋 已加载纹理");
        title.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold;");

        // 拖拽区域 — 存为字段，方便后续动态刷新
        dropZone = new VBox(8);
        dropZone.setPadding(new Insets(12));
        dropZone.setStyle("-fx-border-color: #45475a; -fx-border-width: 2; -fx-border-radius: 8; "
                + "-fx-border-style: dashed; -fx-background-color: #1e1e2e;");
        dropZone.setPrefHeight(120);

        Label dropLabel = new Label("拖拽图片到此处");
        dropLabel.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 14px;");
        dropLabel.setWrapText(true);
        dropZone.getChildren().add(dropLabel);

        // === 拖拽导入事件 ===
        dropZone.setOnDragOver((DragEvent event) -> {
            if (event.getGestureSource() != dropZone
                    && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".png") || name.endsWith(".jpg")
                            || name.endsWith(".jpeg") || name.endsWith(".tga")
                            || name.endsWith(".bmp")) {
                        loadImage(file.toPath());
                    }
                }
                success = !db.getFiles().isEmpty();
            }
            event.setDropCompleted(success);
            event.consume();
        });

        ScrollPane scrollPane = new ScrollPane(dropZone);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(150);

        panel.getChildren().addAll(title, scrollPane);
        return panel;
    }

    private void importImages() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择纹理文件");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.tga", "*.bmp"));
        List<File> files = chooser.showOpenMultipleDialog(root.getScene().getWindow());
        if (files != null) {
            for (File file : files) {
                loadImage(file.toPath());
            }
        }
    }

    void onImportFolder() {
        var chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("选择纹理文件夹");
        File dir = chooser.showDialog(root.getScene().getWindow());
        if (dir == null) return;

        File[] files = dir.listFiles((f, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg") || lower.endsWith(".tga")
                    || lower.endsWith(".bmp");
        });

        if (files == null || files.length == 0) {
            log("⚠ 文件夹中没有找到图片文件");
            return;
        }

        log("导入文件夹: " + dir.getAbsolutePath() + " (" + files.length + " 张)");
        for (File file : files) {
            loadImage(file.toPath());
        }
    }

    private void loadImage(Path path) {
        try {
            TextureImage tex = new TextureImage(path);
            loadedImages.add(tex);
            refreshFileList();
            refreshChannelCombos();
            log("加载: " + tex);
            // 自动预览第一张
            if (loadedImages.size() == 1) {
                imagePreview.show(tex);
            }
        } catch (IOException e) {
            log("错误: 无法加载 " + path.getFileName() + " - " + e.getMessage());
        }
    }

    private void refreshFileList() {
        dropZone.getChildren().clear();

        for (int i = 0; i < loadedImages.size(); i++) {
            TextureImage tex = loadedImages.get(i);
            Label item = new Label((i + 1) + ". " + tex.getName() + "  (" + tex.getWidth() + "x" + tex.getHeight() + ")");
            item.setStyle("-fx-text-fill: #a6adc8; -fx-cursor: hand; -fx-padding: 2 0;");
            final int idx = i;
            item.setOnMouseClicked(e -> {
                imagePreview.show(loadedImages.get(idx));
                log("预览: " + loadedImages.get(idx));
            });
            dropZone.getChildren().add(item);
        }

        if (loadedImages.isEmpty()) {
            Label emptyLabel = new Label("拖拽图片到此处");
            emptyLabel.setStyle("-fx-text-fill: #6c7086;");
            dropZone.getChildren().add(emptyLabel);
        }
    }

    private void refreshChannelCombos() {
        String currentR = rChannelCombo.getValue();
        String currentG = gChannelCombo.getValue();
        String currentB = bChannelCombo.getValue();

        List<String> names = new ArrayList<>();
        names.add("(无 — 白=1.0)");
        for (TextureImage t : loadedImages) {
            names.add(t.getName());
        }

        rChannelCombo.getItems().setAll(names);
        gChannelCombo.getItems().setAll(names);
        bChannelCombo.getItems().setAll(names);

        if (currentR != null && names.contains(currentR)) rChannelCombo.setValue(currentR);
        else rChannelCombo.setValue(names.get(0));
        if (currentG != null && names.contains(currentG)) gChannelCombo.setValue(currentG);
        else gChannelCombo.setValue(names.get(0));
        if (currentB != null && names.contains(currentB)) bChannelCombo.setValue(currentB);
        else bChannelCombo.setValue(names.get(0));
    }

    void onPackChannels() {
        if (loadedImages.isEmpty()) {
            log("⚠ 请先导入图片");
            return;
        }
        TextureImage r = findImageByName(getComboValue(rChannelCombo));
        TextureImage g = findImageByName(getComboValue(gChannelCombo));
        TextureImage b = findImageByName(getComboValue(bChannelCombo));

        if (r == null && g == null && b == null) {
            log("⚠ 至少选择一张图片用于通道打包");
            return;
        }

        log("开始通道打包...");
        PipelineEngine.packChannelsAsync(r, g, b)
                .thenAccept(result -> Platform.runLater(() -> {
                    log("✓ " + result.message);
                    if (!result.outputs.isEmpty()) {
                        imagePreview.show(result.outputs.get(0));
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> log("✗ 打包失败: " + ex.getMessage()));
                    return null;
                });
    }

    void onGenerateMipmaps() {
        TextureImage selected = imagePreview.getCurrentTexture();
        if (selected == null && !loadedImages.isEmpty()) {
            selected = loadedImages.get(0);
        }
        if (selected == null) {
            log("⚠ 请先导入并选择一张图片");
            return;
        }

        log("开始生成 Mipmap: " + selected.getName());
        PipelineEngine.generateMipmapsAsync(selected)
                .thenAccept(result -> Platform.runLater(() -> {
                    log("✓ " + result.message);
                    // 显示第一级 Mipmap（原图）在预览中
                    if (!result.outputs.isEmpty()) {
                        imagePreview.show(result.outputs.get(0));
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> log("✗ Mipmap 生成失败: " + ex.getMessage()));
                    return null;
                });
    }

    void onGenerateNormalMap() {
        TextureImage selected = imagePreview.getCurrentTexture();
        if (selected == null && !loadedImages.isEmpty()) {
            selected = loadedImages.get(0);
        }
        if (selected == null) {
            log("⚠ 请先导入并选择一张高度图");
            return;
        }

        String modeText = pipelinePanel.getEdgeModeCombo().getValue();
        boolean wrap = modeText != null && modeText.startsWith("Wrap");
        float strength = (float) pipelinePanel.getStrengthSlider().getValue();

        log("开始生成法线贴图: " + selected.getName()
                + " (strength=" + strength + ", mode=" + (wrap ? "Wrap" : "Clamp") + ")");

        PipelineEngine.generateNormalMapAsync(selected, strength, wrap)
                .thenAccept(result -> Platform.runLater(() -> {
                    log("✓ " + result.message);
                    if (!result.outputs.isEmpty()) {
                        imagePreview.show(result.outputs.get(0));
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> log("✗ 法线生成失败: " + ex.getMessage()));
                    return null;
                });
    }

    void onExport() {
        TextureImage current = imagePreview.getCurrentTexture();
        if (current == null) {
            log("⚠ 没有可导出的纹理");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出纹理");
        chooser.setInitialFileName(current.getName());
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG 图片", "*.png"));
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try {
                current.saveTo(file.toPath(), "png");
                log("✓ 导出成功: " + file.getAbsolutePath());
            } catch (IOException e) {
                log("✗ 导出失败: " + e.getMessage());
            }
        }
    }

    void onExportWebP() {
        TextureImage current = imagePreview.getCurrentTexture();
        if (current == null) {
            log("⚠ 没有可导出的纹理");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出 WebP");
        String webpName = current.getName().replaceFirst("\\.[^.]+$", "") + ".webp";
        chooser.setInitialFileName(webpName);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("WebP 图片", "*.webp"));
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            float quality = (float) pipelinePanel.getWebpQualitySlider().getValue();
            CompletableFuture.supplyAsync(() -> {
                try {
                    FormatConverter.toWebP(current.getImage(), file.toPath(), quality);
                    return file.getAbsolutePath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(path -> Platform.runLater(() ->
                    log("✓ WebP 导出成功 (q=" + String.format("%.0f", quality) + "): " + path)))
              .exceptionally(ex -> {
                  Platform.runLater(() -> log("✗ WebP 导出失败: " + ex.getMessage()));
                  return null;
              });
        }
    }

    void onExportDDS() {
        TextureImage current = imagePreview.getCurrentTexture();
        if (current == null) {
            log("⚠ 没有可导出的纹理");
            return;
        }
        BufferedImage image = current.getImage();
        if (image.getWidth() % 4 != 0 || image.getHeight() % 4 != 0) {
            log("⚠ BC1 压缩要求宽高为 4 的倍数，当前: "
                    + image.getWidth() + "x" + image.getHeight());
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出 DDS (BC1)");
        String ddsName = current.getName().replaceFirst("\\.[^.]+$", "") + ".dds";
        chooser.setInitialFileName(ddsName);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("DDS 纹理", "*.dds"));
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            int origSize = image.getWidth() * image.getHeight() * 4;
            int compSize = TextureCompressor.compressedSize(
                    image.getWidth(), image.getHeight());
            float ratio = (float) compSize / origSize * 100;
            String sizeInfo = String.format("(%.0f%%, %d→%d bytes)", ratio, origSize, compSize);

            CompletableFuture.supplyAsync(() -> {
                try {
                    TextureCompressor.compressToDDS(image, file.toPath());
                    return file.getAbsolutePath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(path -> Platform.runLater(() ->
                    log("✓ DDS(BC1) 导出成功 " + sizeInfo + ": " + path)))
              .exceptionally(ex -> {
                  Platform.runLater(() -> log("✗ DDS 导出失败: " + ex.getMessage()));
                  return null;
              });
        }
    }

    void onBatchNormalMaps() {
        if (loadedImages.isEmpty()) {
            log("⚠ 请先导入高度图（可点击\"导入文件夹\"批量加载）");
            return;
        }
        String modeText = pipelinePanel.getEdgeModeCombo().getValue();
        boolean wrap = modeText != null && modeText.startsWith("Wrap");
        float strength = (float) pipelinePanel.getStrengthSlider().getValue();

        int total = loadedImages.size();
        log("⚡ 开始批量生成法线贴图 (strength=" + String.format("%.1f", strength)
                + ", mode=" + (wrap ? "Wrap" : "Clamp") + ", " + total + " 张)...");

        BatchProcessor processor = new BatchProcessor();
        CompletableFuture.supplyAsync(() -> {
            List<TextureImage> results = processor.batchNormalMaps(
                    loadedImages, strength, wrap,
                    (completed, totalCount, name) ->
                            Platform.runLater(() ->
                                    log(String.format("[%d/%d] ✓ %s", completed, totalCount, name))));
            processor.shutdown();
            return results;
        }).thenAccept(results -> Platform.runLater(() -> {
            long successCount = results.stream().filter(r -> r != null).count();
            log("✓ 批量法线贴图完成: " + successCount + "/" + total + " 张");
            if (successCount > 0 && results.get(0) != null) {
                imagePreview.show(results.get(0));
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> log("✗ 批量处理失败: " + ex.getMessage()));
            return null;
        });
    }

    void onBatchMipmaps() {
        if (loadedImages.isEmpty()) {
            log("⚠ 请先导入纹理（可点击\"导入文件夹\"批量加载）");
            return;
        }
        int total = loadedImages.size();
        log("⚡ 开始批量生成 Mipmap (" + total + " 张)...");

        BatchProcessor processor = new BatchProcessor();
        CompletableFuture.supplyAsync(() -> {
            List<List<BufferedImage>> results = processor.batchMipmaps(
                    loadedImages,
                    (completed, totalCount, name) ->
                            Platform.runLater(() ->
                                    log(String.format("[%d/%d] ✓ %s", completed, totalCount, name))));
            processor.shutdown();
            return results;
        }).thenAccept(results -> Platform.runLater(() -> {
            long successCount = results.stream().filter(r -> r != null).count();
            long totalMips = results.stream()
                    .filter(r -> r != null)
                    .mapToLong(List::size)
                    .sum();
            log("✓ 批量 Mipmap 完成: " + successCount + "/" + total
                    + " 张，共 " + totalMips + " 级");
            if (successCount > 0 && !loadedImages.isEmpty()) {
                imagePreview.show(loadedImages.get(0));
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> log("✗ 批量处理失败: " + ex.getMessage()));
            return null;
        });
    }

    void onBatchWebP() {
        if (loadedImages.isEmpty()) {
            log("⚠ 请先导入纹理（可点击\"导入文件夹\"批量加载）");
            return;
        }
        var chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("选择 WebP 输出目录");
        File dir = chooser.showDialog(root.getScene().getWindow());
        if (dir == null) return;

        int total = loadedImages.size();
        float quality = (float) pipelinePanel.getWebpQualitySlider().getValue();
        log("⚡ 开始批量导出 WebP (q=" + String.format("%.0f", quality)
                + ", " + total + " 张)...");

        CompletableFuture.supplyAsync(() -> {
            try {
                return FormatConverter.batchToWebP(loadedImages, dir.toPath(), quality,
                        (completed, totalCount, name) ->
                                Platform.runLater(() ->
                                        log(String.format("[%d/%d] ✓ %s", completed, totalCount, name))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(results -> Platform.runLater(() -> {
            long successCount = results.stream().filter(r -> r != null).count();
            log("✓ 批量 WebP 导出完成: " + successCount + "/" + total
                    + " 张 → " + dir.getAbsolutePath());
        })).exceptionally(ex -> {
            Platform.runLater(() -> log("✗ 批量 WebP 导出失败: " + ex.getMessage()));
            return null;
        });
    }

    private TextureImage findImageByName(String name) {
        if (name == null || name.equals("(无 — 白=1.0)")) return null;
        return loadedImages.stream()
                .filter(t -> t.getName().equals(name))
                .findFirst().orElse(null);
    }

    private String getComboValue(ComboBox<String> combo) {
        return combo.getValue();
    }

    private void clearAll() {
        loadedImages.clear();
        refreshFileList();
        refreshChannelCombos();
        imagePreview.clear();
        logArea.clear();
        log("已清空。");
    }

    void log(String msg) {
        Platform.runLater(() -> {
            logArea.appendText(msg + "\n");
        });
    }

    public BorderPane getRoot() { return root; }
}
