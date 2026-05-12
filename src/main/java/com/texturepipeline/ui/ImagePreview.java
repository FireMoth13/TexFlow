package com.texturepipeline.ui;

import com.texturepipeline.model.TextureImage;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;

import java.awt.image.BufferedImage;

/**
 * 图像预览组件 — 用 ImageView 显示 BufferedImage 像素。
 *
 * <p>替代原先的 Canvas 方案：Canvas 在尺寸绑定时会触发 JavaFX 内部
 * RTTexture 空指针异常（JDK-8089273），ImageView 走不同的渲染路径，
 * 不受此 bug 影响。</p>
 *
 * <p>支持鼠标滚轮缩放（以鼠标位置为中心）和拖拽平移。</p>
 */
public class ImagePreview {

    private final BorderPane root;
    private final ImageView imageView;
    private final Label infoLabel;
    private final Label placeholderLabel;
    private final StackPane center;
    private final ObjectProperty<TextureImage> currentTextureProperty = new SimpleObjectProperty<>();

    /** 缩放范围限制 */
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 10.0;
    /** 每次滚轮事件的缩放倍率 */
    private static final double ZOOM_FACTOR = 1.1;

    private double currentScale = 1.0;
    private double currentTranslateX = 0.0;
    private double currentTranslateY = 0.0;

    // 拖拽状态
    private double dragStartX;
    private double dragStartY;
    private double dragStartTranslateX;
    private double dragStartTranslateY;

    public ImagePreview() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #11111b;");
        root.setPadding(new Insets(8));

        // 使用 ImageView 替代 Canvas，避免 RTTexture NPE
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(600);
        imageView.setFitHeight(600);

        // 占位提示
        placeholderLabel = new Label("暂无预览 — 点击左侧纹理查看");
        placeholderLabel.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 14px;");

        // 用 StackPane 叠加图片和提示文字
        center = new StackPane();
        center.setStyle("-fx-background-color: #11111b;");
        center.getChildren().addAll(imageView, placeholderLabel);

        // 图片可见时隐藏提示
        imageView.imageProperty().addListener((obs, oldImg, newImg) -> {
            placeholderLabel.setVisible(newImg == null);
            imageView.setVisible(newImg != null);
        });
        imageView.setVisible(false);
        placeholderLabel.setVisible(true);

        root.setCenter(center);

        // 底部信息栏
        infoLabel = new Label("就绪");
        infoLabel.setStyle("-fx-text-fill: #a6adc8;");
        HBox.setHgrow(infoLabel, Priority.ALWAYS);

        Button resetBtn = new Button("重置视图");
        resetBtn.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4; "
                + "-fx-border-color: #45475a; -fx-border-radius: 4; -fx-background-radius: 4; "
                + "-fx-padding: 4 12;");
        resetBtn.setOnAction(e -> resetView());

        HBox bottomBar = new HBox(8, infoLabel, resetBtn);
        bottomBar.setStyle("-fx-alignment: center-left; -fx-padding: 4 0 0 0;");
        root.setBottom(bottomBar);

        // 监听纹理变化，自动更新图片
        currentTextureProperty.addListener((obs, oldTex, newTex) -> {
            if (newTex != null) {
                Image fxImage = bufferedToFx(newTex.getImage());
                imageView.setImage(fxImage);
                updateInfoLabel(newTex);
                resetView();
            } else {
                imageView.setImage(null);
                infoLabel.setText("就绪");
            }
        });

        // === 滚轮缩放（以鼠标位置为中心） ===
        center.setOnScroll(event -> {
            if (imageView.getImage() == null) return;
            event.consume();

            double delta = event.getDeltaY();
            if (delta == 0) return;

            double oldScale = currentScale;
            double factor = delta > 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
            double newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, currentScale * factor));
            if (newScale == oldScale) return;

            // 鼠标在 StackPane 中的坐标
            double mouseX = event.getX();
            double mouseY = event.getY();

            // 图像中心在 StackPane 中的坐标
            double imgCenterX = center.getWidth() / 2.0 + currentTranslateX;
            double imgCenterY = center.getHeight() / 2.0 + currentTranslateY;

            // 鼠标相对于图像中心的偏移
            double dx = mouseX - imgCenterX;
            double dy = mouseY - imgCenterY;

            // 缩放后调整平移，使鼠标下的点保持不变
            double scaleRatio = newScale / oldScale;
            currentTranslateX = mouseX - center.getWidth() / 2.0 - dx * scaleRatio;
            currentTranslateY = mouseY - center.getHeight() / 2.0 - dy * scaleRatio;
            currentScale = newScale;

            applyTransform();
            updateZoomInfo();
        });

        // === 拖拽平移 ===
        center.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown() && imageView.getImage() != null) {
                dragStartX = event.getX();
                dragStartY = event.getY();
                dragStartTranslateX = currentTranslateX;
                dragStartTranslateY = currentTranslateY;
                center.setCursor(Cursor.CLOSED_HAND);
            }
        });

        center.setOnMouseDragged(event -> {
            if (imageView.getImage() == null) return;
            if (!event.isPrimaryButtonDown()) return;

            double dx = event.getX() - dragStartX;
            double dy = event.getY() - dragStartY;
            currentTranslateX = dragStartTranslateX + dx;
            currentTranslateY = dragStartTranslateY + dy;

            applyTransform();
        });

        center.setOnMouseReleased(event -> {
            center.setCursor(Cursor.DEFAULT);
        });

        // 鼠标进入预览区时显示十字光标（提示可交互）
        center.setOnMouseEntered(event -> {
            if (imageView.getImage() != null) {
                center.setCursor(Cursor.OPEN_HAND);
            }
        });

        center.setOnMouseExited(event -> {
            center.setCursor(Cursor.DEFAULT);
        });
    }

    /** 应用缩放和平移变换到 ImageView */
    private void applyTransform() {
        imageView.setScaleX(currentScale);
        imageView.setScaleY(currentScale);
        imageView.setTranslateX(currentTranslateX);
        imageView.setTranslateY(currentTranslateY);
    }

    /** 重置视图到原始大小和位置 */
    private void resetView() {
        currentScale = 1.0;
        currentTranslateX = 0.0;
        currentTranslateY = 0.0;
        applyTransform();
        updateZoomInfo();
    }

    /** 更新信息栏中的缩放比例 */
    private void updateZoomInfo() {
        TextureImage tex = currentTextureProperty.get();
        if (tex != null) {
            updateInfoLabel(tex);
        } else {
            infoLabel.setText("就绪");
        }
    }

    /** 更新信息栏文本（含缩放比例） */
    private void updateInfoLabel(TextureImage tex) {
        String zoom = String.format("%.0f%%", currentScale * 100);
        infoLabel.setText(String.format("%s | %dx%d | 通道: ARGB | 缩放: %s",
                tex.getName(), tex.getWidth(), tex.getHeight(), zoom));
    }

    /** 显示纹理 */
    public void show(TextureImage texture) {
        if (Platform.isFxApplicationThread()) {
            currentTextureProperty.set(texture);
        } else {
            Platform.runLater(() -> currentTextureProperty.set(texture));
        }
    }

    /** 清空预览 */
    public void clear() {
        if (Platform.isFxApplicationThread()) {
            currentTextureProperty.set(null);
        } else {
            Platform.runLater(() -> currentTextureProperty.set(null));
        }
    }

    /** 获取当前预览的纹理 */
    public TextureImage getCurrentTexture() {
        return currentTextureProperty.get();
    }

    /** BufferedImage → JavaFX Image（像素级转换） */
    private Image bufferedToFx(BufferedImage bi) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        int[] argb = new int[w * h];
        bi.getRGB(0, 0, w, h, argb, 0, w);

        javafx.scene.image.WritableImage fx = new javafx.scene.image.WritableImage(w, h);
        javafx.scene.image.PixelWriter pw = fx.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, argb[y * w + x]);
            }
        }
        return fx;
    }

    public BorderPane getRoot() { return root; }
}