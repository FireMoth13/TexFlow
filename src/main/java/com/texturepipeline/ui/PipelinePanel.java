package com.texturepipeline.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 处理控制面板 — 通道选择、操作按钮、参数调节。
 *
 * <p>设计思路：垂直布局的控制面板，分三个区域：
 * 通道打包区、Mipmap 生成区、导出区。</p>
 */
public class PipelinePanel {

    private final VBox root;
    private final ComboBox<String> rChannelCombo;
    private final ComboBox<String> gChannelCombo;
    private final ComboBox<String> bChannelCombo;
    private final ComboBox<String> edgeModeCombo;
    private final Slider strengthSlider;
    private final Label strengthValueLabel;
    private final Slider webpQualitySlider;

    public PipelinePanel(MainWindow mainWindow) {
        root = new VBox(12);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #181825;");
        root.setPrefWidth(250);

        // === 通道打包区域 ===
        Label packTitle = createSectionTitle("🎨 通道打包 (MRAO)");
        Label packDesc = new Label("R=Metallic  G=Roughness  B=AO\n默认白=1.0（无效果）");
        packDesc.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
        packDesc.setWrapText(true);

        rChannelCombo = createChannelCombo("R 通道 (Metallic):");
        gChannelCombo = createChannelCombo("G 通道 (Roughness):");
        bChannelCombo = createChannelCombo("B 通道 (AO):");

        Button packBtn = new Button("▶ 执行打包");
        packBtn.setStyle("-fx-background-color: #cba6f7; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        packBtn.setMaxWidth(Double.MAX_VALUE);
        packBtn.setOnAction(e -> mainWindow.onPackChannels());

        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background: #45475a;");

        // === Mipmap 生成区域 ===
        Label mipTitle = createSectionTitle("📐 Mipmap 生成");
        Label mipDesc = new Label("为选中纹理生成完整 Mipmap 链\n(1/2 逐级缩小到 1px)");
        mipDesc.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
        mipDesc.setWrapText(true);

        Button mipBtn = new Button("▶ 生成 Mipmap");
        mipBtn.setStyle("-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        mipBtn.setMaxWidth(Double.MAX_VALUE);
        mipBtn.setOnAction(e -> mainWindow.onGenerateMipmaps());

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background: #45475a;");

        // === 法线贴图生成区域 ===
        Label normalTitle = createSectionTitle("🧭 法线贴图生成");
        Label normalDesc = new Label("从高度图烘焙切线空间法线贴图\n(Sobel 3×3 算子)");
        normalDesc.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
        normalDesc.setWrapText(true);

        edgeModeCombo = new ComboBox<>();
        edgeModeCombo.getItems().addAll("Clamp（边缘复制）", "Wrap（循环平铺）");
        edgeModeCombo.setValue("Clamp（边缘复制）");
        edgeModeCombo.setMaxWidth(Double.MAX_VALUE);

        // Strength 滑块
        Label strengthLabel = new Label("凹凸强度 (Strength):");
        strengthLabel.setStyle("-fx-text-fill: #bac2de; -fx-font-size: 12px;");
        strengthValueLabel = new Label("2.0");
        strengthValueLabel.setStyle("-fx-text-fill: #f9e2af; -fx-font-size: 12px;");

        strengthSlider = new Slider(0.5, 10.0, 2.0);
        strengthSlider.setMaxWidth(Double.MAX_VALUE);
        strengthSlider.setShowTickLabels(true);
        strengthSlider.setShowTickMarks(true);
        strengthSlider.setMajorTickUnit(2.0);
        strengthSlider.setMinorTickCount(3);
        strengthSlider.setBlockIncrement(0.5);
        strengthValueLabel.setText(String.format("%.1f", strengthSlider.getValue()));
        strengthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            strengthValueLabel.setText(String.format("%.1f", newVal.doubleValue()));
        });

        Button normalBtn = new Button("▶ 生成法线贴图");
        normalBtn.setStyle("-fx-background-color: #f9e2af; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        normalBtn.setMaxWidth(Double.MAX_VALUE);
        normalBtn.setOnAction(e -> mainWindow.onGenerateNormalMap());

        Separator sep3 = new Separator();
        sep3.setStyle("-fx-background: #45475a;");

        // === 导出区域 ===
        Label exportTitle = createSectionTitle("💾 导出");
        Button exportBtn = new Button("📥 导出当前纹理 (PNG)");
        exportBtn.setStyle("-fx-background-color: #a6e3a1; -fx-text-fill: #1e1e2e;");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> mainWindow.onExport());

        // WebP 导出
        Label webpQualityLabel = new Label("WebP 压缩质量:");
        webpQualityLabel.setStyle("-fx-text-fill: #bac2de; -fx-font-size: 12px;");
        Label webpValueLabel = new Label("80");
        webpValueLabel.setStyle("-fx-text-fill: #a6e3a1; -fx-font-size: 12px;");

        Slider webpQualitySlider = new Slider(0, 100, 80);
        webpQualitySlider.setMaxWidth(Double.MAX_VALUE);
        webpQualitySlider.setShowTickLabels(true);
        webpQualitySlider.setMajorTickUnit(25);
        webpQualitySlider.setBlockIncrement(5);
        webpValueLabel.setText(String.format("%.0f", webpQualitySlider.getValue()));
        webpQualitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            webpValueLabel.setText(String.format("%.0f", newVal.doubleValue()));
        });
        this.webpQualitySlider = webpQualitySlider;

        Button webpExportBtn = new Button("📥 导出当前为 WebP");
        webpExportBtn.setStyle("-fx-background-color: #a6e3a1; -fx-text-fill: #1e1e2e;");
        webpExportBtn.setMaxWidth(Double.MAX_VALUE);
        webpExportBtn.setOnAction(e -> mainWindow.onExportWebP());

        Button ddsExportBtn = new Button("📥 导出当前为 DDS (BC1)");
        ddsExportBtn.setStyle("-fx-background-color: #cba6f7; -fx-text-fill: #1e1e2e;");
        ddsExportBtn.setMaxWidth(Double.MAX_VALUE);
        ddsExportBtn.setOnAction(e -> mainWindow.onExportDDS());

        Button dds3ExportBtn = new Button("📥 导出当前为 DDS (BC3/DXT5)");
        dds3ExportBtn.setStyle("-fx-background-color: #f5c2e7; -fx-text-fill: #1e1e2e;");
        dds3ExportBtn.setMaxWidth(Double.MAX_VALUE);
        dds3ExportBtn.setOnAction(e -> mainWindow.onExportDDS3());

        Button dds7ExportBtn = new Button("📥 导出当前为 DDS (BC7)");
        dds7ExportBtn.setStyle("-fx-background-color: #eba0ac; -fx-text-fill: #1e1e2e;");
        dds7ExportBtn.setMaxWidth(Double.MAX_VALUE);
        dds7ExportBtn.setOnAction(e -> mainWindow.onExportBC7());

        Separator sep4 = new Separator();
        sep4.setStyle("-fx-background: #45475a;");

        // === 批量处理区域 ===
        Label batchTitle = createSectionTitle("⚡ 批量处理");
        Label batchDesc = new Label("对已加载的全部纹理执行同一操作\n使用多线程并行处理");
        batchDesc.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
        batchDesc.setWrapText(true);

        Button importFolderBtn = new Button("📂 导入文件夹");
        importFolderBtn.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4;");
        importFolderBtn.setMaxWidth(Double.MAX_VALUE);
        importFolderBtn.setOnAction(e -> mainWindow.onImportFolder());

        Button batchNormalBtn = new Button("🔄 批量生成法线贴图");
        batchNormalBtn.setStyle("-fx-background-color: #f9e2af; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        batchNormalBtn.setMaxWidth(Double.MAX_VALUE);
        batchNormalBtn.setOnAction(e -> mainWindow.onBatchNormalMaps());

        Button batchMipBtn = new Button("🔄 批量生成 Mipmap");
        batchMipBtn.setStyle("-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        batchMipBtn.setMaxWidth(Double.MAX_VALUE);
        batchMipBtn.setOnAction(e -> mainWindow.onBatchMipmaps());

        Button batchWebpBtn = new Button("🔄 批量导出 WebP");
        batchWebpBtn.setStyle("-fx-background-color: #a6e3a1; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        batchWebpBtn.setMaxWidth(Double.MAX_VALUE);
        batchWebpBtn.setOnAction(e -> mainWindow.onBatchWebP());

        root.getChildren().addAll(
                packTitle, packDesc, rChannelCombo, gChannelCombo, bChannelCombo, packBtn,
                sep1, mipTitle, mipDesc, mipBtn,
                sep2, normalTitle, normalDesc, strengthLabel, strengthValueLabel, strengthSlider,
                edgeModeCombo, normalBtn,
                sep3, exportTitle, exportBtn,
                webpQualityLabel, webpValueLabel, webpQualitySlider, webpExportBtn,
                ddsExportBtn, dds3ExportBtn, dds7ExportBtn,
                sep4, batchTitle, batchDesc, importFolderBtn, batchNormalBtn, batchMipBtn, batchWebpBtn
        );
    }

    private Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold; -fx-font-size: 14px;");
        return label;
    }

    private ComboBox<String> createChannelCombo(String label) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #bac2de; -fx-font-size: 12px;");
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().add("(无 — 白=1.0)");
        combo.setValue("(无 — 白=1.0)");
        combo.setMaxWidth(Double.MAX_VALUE);

        // 把 label 放在 combo 上面
        // 这里用简单的布局处理：label 已在外部 VBox 中
        return combo;
    }

    public ComboBox<String> getRChannelCombo() { return rChannelCombo; }
    public ComboBox<String> getGChannelCombo() { return gChannelCombo; }
    public ComboBox<String> getBChannelCombo() { return bChannelCombo; }
    public ComboBox<String> getEdgeModeCombo() { return edgeModeCombo; }
    public Slider getStrengthSlider() { return strengthSlider; }
    public Slider getWebpQualitySlider() { return webpQualitySlider; }

    public VBox getRoot() { return root; }
}
