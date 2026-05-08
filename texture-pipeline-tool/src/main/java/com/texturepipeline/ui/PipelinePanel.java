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

        root.getChildren().addAll(
                packTitle, packDesc, rChannelCombo, gChannelCombo, bChannelCombo, packBtn,
                sep1, mipTitle, mipDesc, mipBtn,
                sep2, normalTitle, normalDesc, edgeModeCombo, normalBtn,
                sep3, exportTitle, exportBtn
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

    public VBox getRoot() { return root; }
}
