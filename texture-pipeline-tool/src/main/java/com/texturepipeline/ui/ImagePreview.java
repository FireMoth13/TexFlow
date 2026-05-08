package com.texturepipeline.ui;

import com.texturepipeline.model.TextureImage;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;

/**
 * 图像预览组件 — 用 Canvas 直接绘制 BufferedImage 像素。
 *
 * <p>为什么不用 ImageView：Canvas 可以直接操作像素数据，方便后续加
 * 标注（如通道标识、Mipmap 级别水印）。性能更好——跳过 JavaFX Image 编码/解码。</p>
 */
public class ImagePreview {

    private final BorderPane root;
    private final Canvas canvas;
    private final Label infoLabel;
    private TextureImage currentTexture;

    public ImagePreview() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #11111b;");
        root.setPadding(new Insets(8));

        canvas = new Canvas(600, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.rgb(17, 17, 27));
        gc.fillRect(0, 0, 600, 600);

        // 居中显示提示
        gc.setFill(Color.rgb(108, 112, 134));
        gc.fillText("暂无预览 — 点击左侧纹理查看", 200, 300);

        root.setCenter(canvas);

        // 底部信息栏
        infoLabel = new Label("就绪");
        infoLabel.setStyle("-fx-text-fill: #a6adc8; -fx-padding: 4 0 0 0;");
        root.setBottom(infoLabel);

        // Canvas 自适应大小
        canvas.widthProperty().bind(root.widthProperty().subtract(16));
        canvas.heightProperty().bind(root.heightProperty().subtract(30));
        canvas.widthProperty().addListener(e -> redraw());
        canvas.heightProperty().addListener(e -> redraw());
    }

    /** 显示纹理 */
    public void show(TextureImage texture) {
        this.currentTexture = texture;
        redraw();
        infoLabel.setText(String.format("%s | %dx%d | 通道: ARGB",
                texture.getName(), texture.getWidth(), texture.getHeight()));
    }

    /** 清空预览 */
    public void clear() {
        this.currentTexture = null;
        redraw();
        infoLabel.setText("就绪");
    }

    /** 获取当前预览的纹理 */
    public TextureImage getCurrentTexture() {
        return currentTexture;
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // 清除背景
        gc.setFill(Color.rgb(17, 17, 27));
        gc.fillRect(0, 0, w, h);

        if (currentTexture == null) {
            gc.setFill(Color.rgb(108, 112, 134));
            gc.fillText("暂无预览", w / 2 - 30, h / 2);
            return;
        }

        BufferedImage bi = currentTexture.getImage();
        WritableImage fxImage = bufferedToFx(bi);

        // 等比例缩放居中显示
        double scale = Math.min(w / bi.getWidth(), h / bi.getHeight());
        double imgW = bi.getWidth() * scale;
        double imgH = bi.getHeight() * scale;
        double x = (w - imgW) / 2;
        double y = (h - imgH) / 2;

        gc.drawImage(fxImage, x, y, imgW, imgH);

        // 画边框
        gc.setStroke(Color.rgb(69, 71, 90));
        gc.setLineWidth(1);
        gc.strokeRect(x, y, imgW, imgH);
    }

    /** BufferedImage → JavaFX WritableImage（像素级转换） */
    private WritableImage bufferedToFx(BufferedImage bi) {
        WritableImage fx = new WritableImage(bi.getWidth(), bi.getHeight());
        PixelWriter pw = fx.getPixelWriter();
        int[] argb = new int[bi.getWidth() * bi.getHeight()];
        bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), argb, 0, bi.getWidth());

        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                int pixel = argb[y * bi.getWidth() + x];
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                pw.setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return fx;
    }

    public BorderPane getRoot() { return root; }
}
