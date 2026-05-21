package com.texturepipeline.ui;

import com.texturepipeline.engine.BatchProcessor;
import com.texturepipeline.engine.FormatConverter;
import com.texturepipeline.engine.PipelineEngine;
import com.texturepipeline.engine.TextureCompressor;
import com.texturepipeline.engine.TextureCompressor.Format;
import com.texturepipeline.model.TextureImage;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

public class MainWindow {

    public static final String NONE_OPTION = "(无)";

    private final BorderPane root = new BorderPane();
    private final VBox imageListBox = new VBox(6);
    private final ScrollPane imageListScroll = new ScrollPane(imageListBox);
    private final ImagePreview imagePreview;
    private final PipelinePanel pipelinePanel;
    private final TextArea logArea = new TextArea();
    private final List<TextureImage> loadedImages = new ArrayList<>();
    private final HistoryManager history = new HistoryManager();
    private final Map<TextureImage, javafx.scene.image.Image> thumbnailCache = new WeakHashMap<>();
    private final ComboBox<String> rChannelCombo;
    private final ComboBox<String> gChannelCombo;
    private final ComboBox<String> bChannelCombo;
    private Button undoBtn;
    private Button redoBtn;

    public MainWindow() {
        root.setStyle("-fx-background-color: #1e1e2e;");
        root.setTop(createToolbar());

        VBox leftPanel = createImageListPanel();
        imagePreview = new ImagePreview();
        pipelinePanel = new PipelinePanel(this);
        rChannelCombo = pipelinePanel.getRChannelCombo();
        gChannelCombo = pipelinePanel.getGChannelCombo();
        bChannelCombo = pipelinePanel.getBChannelCombo();

        logArea.setEditable(false);
        logArea.setPrefRowCount(3);
        logArea.setMinHeight(60);
        logArea.setMaxHeight(160);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-control-inner-background: #181825; -fx-text-fill: #cdd6f4;");
        BorderPane.setMargin(logArea, new Insets(4, 0, 0, 0));

        SplitPane centerSplit = new SplitPane(leftPanel, imagePreview.getRoot(), pipelinePanel.getRoot());
        centerSplit.setDividerPositions(0.22, 0.66);
        centerSplit.setMinHeight(400);
        root.setCenter(centerSplit);
        root.setBottom(logArea);

        log("TexFlow 已就绪。");
    }

    private ToolBar createToolbar() {
        Button importBtn = new Button("导入图片");
        importBtn.setOnAction(e -> importImages());

        Button importFolderBtn = new Button("导入文件夹");
        importFolderBtn.setOnAction(e -> onImportFolder());

        Button clearBtn = new Button("清空");
        clearBtn.setOnAction(e -> clearAll());

        undoBtn = new Button("撤销");
        undoBtn.setDisable(true);
        undoBtn.setOnAction(e -> onUndo());

        redoBtn = new Button("重做");
        redoBtn.setDisable(true);
        redoBtn.setOnAction(e -> onRedo());

        ToolBar toolbar = new ToolBar(importBtn, importFolderBtn, clearBtn, undoBtn, redoBtn);
        toolbar.setStyle("-fx-background-color: #313244;");
        return toolbar;
    }

    private VBox createImageListPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(8));
        panel.setStyle("-fx-background-color: #181825;");

        Label title = new Label("图片列表");
        title.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold;");

        Label hint = new Label("把图片拖到列表里即可导入");
        hint.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");

        imageListBox.setPadding(new Insets(8));
        imageListBox.setFillWidth(true);
        imageListScroll.setFitToWidth(true);
        imageListScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        imageListScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        imageListScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        imageListScroll.setPrefViewportHeight(180);

        installDropTarget(imageListScroll);
        installDropTarget(imageListBox);
        refreshFileList();

        panel.getChildren().addAll(title, hint, imageListScroll);
        VBox.setVgrow(imageListScroll, Priority.ALWAYS);
        return panel;
    }

    private void installDropTarget(Node target) {
        target.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        target.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                importFiles(db.getFiles());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void importImages() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入图片");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg", "*.tga", "*.bmp"));
        List<File> files = chooser.showOpenMultipleDialog(root.getScene().getWindow());
        if (files != null) {
            importFiles(files);
        }
    }

    void onImportFolder() {
        var chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("导入文件夹");
        File dir = chooser.showDialog(root.getScene().getWindow());
        if (dir == null) return;

        File[] files = dir.listFiles((f, name) -> isSupportedImage(name));
        if (files == null || files.length == 0) {
            log("文件夹里没有找到可导入的图片。");
            return;
        }

        importFiles(List.of(files));
    }

    private void importFiles(List<File> files) {
        for (File file : files) {
            if (file != null && isSupportedImage(file.getName())) {
                loadImage(file.toPath());
            }
        }
    }

    private boolean isSupportedImage(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".tga")
                || lower.endsWith(".bmp");
    }

    private void loadImage(Path path) {
        try {
            TextureImage tex = new TextureImage(path);
            loadedImages.add(tex);
            refreshFileList();
            refreshChannelCombos();
            log("已导入: " + tex.getName());
            if (loadedImages.size() == 1) {
                imagePreview.show(tex);
            }
        } catch (Exception e) {
            log("导入失败: " + path.getFileName() + " - " + e.getMessage());
        }
    }

    private void refreshFileList() {
        imageListBox.getChildren().clear();
        if (loadedImages.isEmpty()) {
            Label empty = new Label("把图片拖到这里，或用上方按钮导入");
            empty.setStyle("-fx-text-fill: #6c7086;");
            empty.setPadding(new Insets(8));
            imageListBox.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < loadedImages.size(); i++) {
            TextureImage tex = loadedImages.get(i);
            imageListBox.getChildren().add(createImageListItem(tex, i));
        }
    }

    private HBox createImageListItem(TextureImage tex, int index) {
        ImageView thumb = new ImageView(createThumbnail(tex));
        thumb.setFitWidth(44);
        thumb.setFitHeight(44);
        thumb.setPreserveRatio(true);
        thumb.setSmooth(true);

        Label name = new Label(tex.getName());
        name.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold;");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);

        Label meta = new Label(tex.getWidth() + "x" + tex.getHeight());
        meta.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");

        VBox textBox = new VBox(2, name, meta);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox item = new HBox(10, thumb, textBox);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8));
        item.setMaxWidth(Double.MAX_VALUE);
        item.setStyle("-fx-background-color: #1e1e2e; -fx-background-radius: 6; -fx-cursor: hand;");
        item.setOnMouseEntered(e ->
                item.setStyle("-fx-background-color: #26263a; -fx-background-radius: 6; -fx-cursor: hand;"));
        item.setOnMouseExited(e ->
                item.setStyle("-fx-background-color: #1e1e2e; -fx-background-radius: 6; -fx-cursor: hand;"));
        item.setOnMouseClicked(e -> {
            imagePreview.show(loadedImages.get(index));
            log("选中: " + loadedImages.get(index).getName());
        });
        return item;
    }

    private javafx.scene.image.Image createThumbnail(TextureImage tex) {
        return thumbnailCache.computeIfAbsent(tex, t -> {
            BufferedImage src = t.getImage();
            int maxSize = 48;
            double scale = Math.min((double) maxSize / src.getWidth(), (double) maxSize / src.getHeight());
            int width = Math.max(1, (int) Math.round(src.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(src.getHeight() * scale));
            BufferedImage thumb = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = thumb.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(src, 0, 0, width, height, null);
            } finally {
                g.dispose();
            }
            return SwingFXUtils.toFXImage(thumb, null);
        });
    }

    private void refreshChannelCombos() {
        String currentR = rChannelCombo.getValue();
        String currentG = gChannelCombo.getValue();
        String currentB = bChannelCombo.getValue();

        List<String> names = new ArrayList<>();
        names.add(NONE_OPTION);
        for (TextureImage t : loadedImages) {
            names.add(t.getName());
        }

        rChannelCombo.getItems().setAll(names);
        gChannelCombo.getItems().setAll(names);
        bChannelCombo.getItems().setAll(names);

        rChannelCombo.setValue(names.contains(currentR) ? currentR : NONE_OPTION);
        gChannelCombo.setValue(names.contains(currentG) ? currentG : NONE_OPTION);
        bChannelCombo.setValue(names.contains(currentB) ? currentB : NONE_OPTION);
    }

    void onPackChannels() {
        if (loadedImages.isEmpty()) {
            log("请先导入图片。");
            return;
        }

        TextureImage r = findImageByName(getComboValue(rChannelCombo));
        TextureImage g = findImageByName(getComboValue(gChannelCombo));
        TextureImage b = findImageByName(getComboValue(bChannelCombo));

        if (r == null && g == null && b == null) {
            log("没有选择任何通道。");
            return;
        }

        TextureImage currentPreview = imagePreview.getCurrentTexture();
        if (currentPreview != null) {
            history.pushState(currentPreview);
        }

        log("正在合成通道...");
        PipelineEngine.packChannelsAsync(r, g, b)
                .thenAccept(result -> Platform.runLater(() -> {
                    log(result.message);
                    if (!result.outputs.isEmpty()) {
                        imagePreview.show(result.outputs.get(0));
                    }
                    updateUndoRedoButtons();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> log("通道合成失败: " + ex.getMessage()));
                    return null;
                });
    }

    void onGenerateMipmaps() {
        TextureImage selected = imagePreview.getCurrentTexture();
        if (selected == null && !loadedImages.isEmpty()) {
            selected = loadedImages.get(0);
        }
        if (selected == null) {
            log("请先导入图片。");
            return;
        }

        TextureImage currentPreview = imagePreview.getCurrentTexture();
        if (currentPreview != null) {
            history.pushState(currentPreview);
        }

        log("正在生成 Mipmap: " + selected.getName());
        PipelineEngine.generateMipmapsAsync(selected)
                .thenAccept(result -> Platform.runLater(() -> {
                    log(result.message);
                    for (TextureImage mip : result.outputs) {
                        loadedImages.add(mip);
                    }
                    refreshFileList();
                    if (result.outputs.size() > 1) {
                        imagePreview.show(result.outputs.get(1));
                    } else if (!result.outputs.isEmpty()) {
                        imagePreview.show(result.outputs.get(0));
                    }
                    updateUndoRedoButtons();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> log("Mipmap 生成失败: " + ex.getMessage()));
                    return null;
                });
    }

    void onGenerateNormalMap() {
        TextureImage selected = imagePreview.getCurrentTexture();
        if (selected == null && !loadedImages.isEmpty()) {
            selected = loadedImages.get(0);
        }
        if (selected == null) {
            log("请先导入图片。");
            return;
        }

        TextureImage currentPreview = imagePreview.getCurrentTexture();
        if (currentPreview != null) {
            history.pushState(currentPreview);
        }

        String modeText = pipelinePanel.getEdgeModeCombo().getValue();
        boolean wrap = modeText != null && modeText.startsWith("环绕");
        float strength = (float) pipelinePanel.getStrengthSlider().getValue();

        log("正在生成法线图: " + selected.getName());
        PipelineEngine.generateNormalMapAsync(selected, strength, wrap)
                .thenAccept(result -> Platform.runLater(() -> {
                    log(result.message);
                    if (!result.outputs.isEmpty()) {
                        imagePreview.show(result.outputs.get(0));
                    }
                    updateUndoRedoButtons();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> log("法线图生成失败: " + ex.getMessage()));
                    return null;
                });
    }

    void onExport() {
        TextureImage current = imagePreview.getCurrentTexture();
        if (current == null) {
            log("没有可导出的图片。");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出 PNG");
        chooser.setInitialFileName(current.getName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG 文件", "*.png"));
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try {
                current.saveTo(file.toPath(), "png");
                log("已导出 PNG: " + file.getAbsolutePath());
            } catch (IOException e) {
                log("PNG 导出失败: " + e.getMessage());
            }
        }
    }

    void onExportWebP() {
        TextureImage current = imagePreview.getCurrentTexture();
        if (current == null) {
            log("没有可导出的图片。");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出 WebP");
        chooser.setInitialFileName(current.getName().replaceFirst("\\.[^.]+$", "") + ".webp");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("WebP 文件", "*.webp"));
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
                    log("已导出 WebP: " + path)))
              .exceptionally(ex -> {
                  Platform.runLater(() -> log("WebP 导出失败: " + ex.getMessage()));
                  return null;
              });
        }
    }

    void onExportDDS() {
        exportDDS(Format.BC1_DXT1, "BC1");
    }

    void onExportDDS3() {
        exportDDS(Format.BC3_DXT5, "BC3/DXT5");
    }

    void onExportBC7() {
        exportDDS(Format.BC7, "BC7");
    }

    private void exportDDS(Format format, String label) {
        TextureImage current = imagePreview.getCurrentTexture();
        if (current == null) {
            log("没有可导出的图片。");
            return;
        }

        BufferedImage image = current.getImage();
        if (image.getWidth() % 4 != 0 || image.getHeight() % 4 != 0) {
            log("无法导出 " + label + "，尺寸必须是 4 的倍数。");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出 DDS");
        chooser.setInitialFileName(current.getName().replaceFirst("\\.[^.]+$", "") + ".dds");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DDS 文件", "*.dds"));
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            int origSize = image.getWidth() * image.getHeight() * 4;
            int compSize = TextureCompressor.compressedSize(image.getWidth(), image.getHeight(), format);
            float ratio = (float) compSize / origSize * 100;
            String sizeInfo = String.format("(%.1f%%, %d -> %d bytes)", ratio, origSize, compSize);

            CompletableFuture.supplyAsync(() -> {
                try {
                    TextureCompressor.compress(image, file.toPath(), format);
                    return file.getAbsolutePath();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(path -> Platform.runLater(() ->
                    log("已导出 DDS(" + label + ") " + sizeInfo + ": " + path)))
              .exceptionally(ex -> {
                  Platform.runLater(() -> log("DDS 导出失败: " + ex.getMessage()));
                  return null;
              });
        }
    }

    void onBatchNormalMaps() {
        if (loadedImages.isEmpty()) {
            log("请先导入图片。");
            return;
        }

        String modeText = pipelinePanel.getEdgeModeCombo().getValue();
        boolean wrap = modeText != null && modeText.startsWith("环绕");
        float strength = (float) pipelinePanel.getStrengthSlider().getValue();
        int total = loadedImages.size();

        log("批量生成法线图: " + total + " 张");

        BatchProcessor processor = new BatchProcessor();
        CompletableFuture.supplyAsync(() -> {
            List<TextureImage> results = processor.batchNormalMaps(
                    loadedImages, strength, wrap,
                    (completed, totalCount, name) ->
                            Platform.runLater(() -> log(String.format("[%d/%d] %s", completed, totalCount, name))));
            processor.shutdown();
            return results;
        }).thenAccept(results -> Platform.runLater(() -> {
            long successCount = results.stream().filter(t -> t != null).count();
            log("批量法线图完成: " + successCount + "/" + total);
            results.stream().filter(t -> t != null).findFirst().ifPresent(imagePreview::show);
        })).exceptionally(ex -> {
            Platform.runLater(() -> log("批量法线图失败: " + ex.getMessage()));
            return null;
        });
    }

    void onBatchMipmaps() {
        if (loadedImages.isEmpty()) {
            log("请先导入图片。");
            return;
        }

        int total = loadedImages.size();
        log("批量生成 Mipmap: " + total + " 张");

        BatchProcessor processor = new BatchProcessor();
        CompletableFuture.supplyAsync(() -> {
            List<List<BufferedImage>> results = processor.batchMipmaps(
                    loadedImages,
                    (completed, totalCount, name) ->
                            Platform.runLater(() -> log(String.format("[%d/%d] %s", completed, totalCount, name))));
            processor.shutdown();
            return results;
        }).thenAccept(results -> Platform.runLater(() -> {
            long successCount = results.stream().filter(t -> t != null).count();
            long totalMips = results.stream().filter(t -> t != null).mapToLong(List::size).sum();
            log("批量 Mipmap 完成: " + successCount + "/" + total + "，生成 " + totalMips + " 张");
            if (!loadedImages.isEmpty()) {
                imagePreview.show(loadedImages.get(0));
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> log("批量 Mipmap 失败: " + ex.getMessage()));
            return null;
        });
    }

    void onBatchWebP() {
        if (loadedImages.isEmpty()) {
            log("请先导入图片。");
            return;
        }

        var chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("批量导出 WebP");
        File dir = chooser.showDialog(root.getScene().getWindow());
        if (dir == null) return;

        int total = loadedImages.size();
        float quality = (float) pipelinePanel.getWebpQualitySlider().getValue();
        log("批量导出 WebP: " + total + " 张");

        CompletableFuture.supplyAsync(() -> {
            try {
                return FormatConverter.batchToWebP(loadedImages, dir.toPath(), quality,
                        (completed, totalCount, name) ->
                                Platform.runLater(() -> log(String.format("[%d/%d] %s", completed, totalCount, name))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(results -> Platform.runLater(() -> {
            long successCount = results.stream().filter(t -> t != null).count();
            log("批量 WebP 完成: " + successCount + "/" + total + "，目录 " + dir.getAbsolutePath());
        })).exceptionally(ex -> {
            Platform.runLater(() -> log("批量 WebP 失败: " + ex.getMessage()));
            return null;
        });
    }

    private TextureImage findImageByName(String name) {
        if (name == null || NONE_OPTION.equals(name)) return null;
        return loadedImages.stream().filter(t -> t.getName().equals(name)).findFirst().orElse(null);
    }

    private String getComboValue(ComboBox<String> combo) {
        return combo.getValue();
    }

    private void clearAll() {
        loadedImages.clear();
        thumbnailCache.clear();
        refreshFileList();
        refreshChannelCombos();
        imagePreview.clear();
        history.clear();
        logArea.clear();
        updateUndoRedoButtons();
        log("已清空。");
    }

    void onUndo() {
        TextureImage current = imagePreview.getCurrentTexture();
        if (current == null) return;
        TextureImage prevState = history.undo(current);
        if (prevState != null) {
            imagePreview.show(prevState);
            log("撤销");
            updateUndoRedoButtons();
        }
    }

    void onRedo() {
        TextureImage current = imagePreview.getCurrentTexture();
        if (current == null) return;
        TextureImage nextState = history.redo(current);
        if (nextState != null) {
            imagePreview.show(nextState);
            log("重做");
            updateUndoRedoButtons();
        }
    }

    private void updateUndoRedoButtons() {
        if (undoBtn != null) {
            undoBtn.setDisable(!history.canUndo());
            undoBtn.setStyle(history.canUndo()
                    ? "-fx-background-color: #45475a; -fx-text-fill: #cdd6f4;"
                    : "-fx-background-color: #313244; -fx-text-fill: #6c7086;");
        }
        if (redoBtn != null) {
            redoBtn.setDisable(!history.canRedo());
            redoBtn.setStyle(history.canRedo()
                    ? "-fx-background-color: #45475a; -fx-text-fill: #cdd6f4;"
                    : "-fx-background-color: #313244; -fx-text-fill: #6c7086;");
        }
    }

    void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    public BorderPane getRoot() {
        return root;
    }
}
