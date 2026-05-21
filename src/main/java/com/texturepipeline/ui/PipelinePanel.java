package com.texturepipeline.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PipelinePanel {

    private final VBox root;
    private final ScrollPane scrollPane;
    private final ComboBox<String> rChannelCombo;
    private final ComboBox<String> gChannelCombo;
    private final ComboBox<String> bChannelCombo;
    private final ComboBox<String> edgeModeCombo;
    private final Slider strengthSlider;
    private final Slider webpQualitySlider;

    public PipelinePanel(MainWindow mainWindow) {
        root = new VBox(12);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #181825;");
        root.setPrefWidth(240);
        root.setMinWidth(240);

        scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #181825;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        root.getChildren().addAll(
                sectionTitle("通道合成（MRAO）"),
                sectionText("R = 金属度  G = 粗糙度  B = AO"),
                labeledCombo("R 通道（Metallic）", rChannelCombo = new ComboBox<>()),
                labeledCombo("G 通道（Roughness）", gChannelCombo = new ComboBox<>()),
                labeledCombo("B 通道（AO）", bChannelCombo = new ComboBox<>()),
                button("执行合成", "#cba6f7", e -> mainWindow.onPackChannels()),
                new Separator(),
                sectionTitle("Mipmap 生成"),
                sectionText("为选中的图片生成完整 Mipmap 链。"),
                button("生成 Mipmap", "#89b4fa", e -> mainWindow.onGenerateMipmaps()),
                new Separator(),
                sectionTitle("法线图生成"),
                sectionText("从高度图生成法线图。"),
                labeledCombo("边缘模式", edgeModeCombo = new ComboBox<>(), "Clamp（边缘复制）", "Wrap（环绕平铺）"),
                labeledSlider("强度", strengthSlider = new Slider(0.5, 10.0, 2.0), true),
                button("生成法线图", "#f9e2af", e -> mainWindow.onGenerateNormalMap()),
                new Separator(),
                sectionTitle("导出"),
                button("导出 PNG", "#a6e3a1", e -> mainWindow.onExport()),
                labeledSlider("WebP 质量", webpQualitySlider = new Slider(0, 100, 80), false),
                button("导出 WebP", "#a6e3a1", e -> mainWindow.onExportWebP()),
                button("导出 DDS（BC1）", "#cba6f7", e -> mainWindow.onExportDDS()),
                button("导出 DDS（BC3/DXT5）", "#f5c2e7", e -> mainWindow.onExportDDS3()),
                button("导出 DDS（BC7）", "#eba0ac", e -> mainWindow.onExportBC7()),
                new Separator(),
                sectionTitle("批处理"),
                sectionText("对文件夹中的所有图片批量执行处理。"),
                button("批量导入文件夹", "#313244", e -> mainWindow.onImportFolder()),
                button("批量法线图", "#f9e2af", e -> mainWindow.onBatchNormalMaps()),
                button("批量 Mipmap", "#89b4fa", e -> mainWindow.onBatchMipmaps()),
                button("批量 WebP", "#a6e3a1", e -> mainWindow.onBatchWebP())
        );

        rChannelCombo.getItems().add(MainWindow.NONE_OPTION);
        gChannelCombo.getItems().add(MainWindow.NONE_OPTION);
        bChannelCombo.getItems().add(MainWindow.NONE_OPTION);
        rChannelCombo.setValue(MainWindow.NONE_OPTION);
        gChannelCombo.setValue(MainWindow.NONE_OPTION);
        bChannelCombo.setValue(MainWindow.NONE_OPTION);

        edgeModeCombo.getItems().addAll("Clamp（边缘复制）", "Wrap（环绕平铺）");
        edgeModeCombo.setValue("Clamp（边缘复制）");

        strengthSlider.setMaxWidth(Double.MAX_VALUE);
        strengthSlider.setShowTickLabels(true);
        strengthSlider.setShowTickMarks(true);
        strengthSlider.setMajorTickUnit(2.0);
        strengthSlider.setMinorTickCount(3);
        strengthSlider.setBlockIncrement(0.5);

        webpQualitySlider.setMaxWidth(Double.MAX_VALUE);
        webpQualitySlider.setShowTickLabels(true);
        webpQualitySlider.setShowTickMarks(true);
        webpQualitySlider.setMajorTickUnit(25);
        webpQualitySlider.setBlockIncrement(5);
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold; -fx-font-size: 14px;");
        return label;
    }

    private Label sectionText(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
        label.setWrapText(true);
        return label;
    }

    private VBox labeledCombo(String labelText, ComboBox<String> combo, String... items) {
        VBox box = new VBox(4);
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #bac2de; -fx-font-size: 12px;");
        combo.getItems().addAll(items);
        combo.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(label, combo);
        return box;
    }

    private VBox labeledSlider(String labelText, Slider slider, boolean showDecimals) {
        VBox box = new VBox(4);
        HBox row = new HBox(8);
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #bac2de; -fx-font-size: 12px;");
        Label value = new Label();
        value.setStyle("-fx-text-fill: #f9e2af; -fx-font-size: 12px;");
        value.setText(showDecimals ? String.format("%.1f", slider.getValue()) : String.format("%.0f", slider.getValue()));
        slider.valueProperty().addListener((obs, oldVal, newVal) ->
                value.setText(showDecimals ? String.format("%.1f", newVal.doubleValue()) : String.format("%.0f", newVal.doubleValue())));
        row.getChildren().addAll(label, value);
        HBox.setHgrow(label, Priority.ALWAYS);
        box.getChildren().addAll(row, slider);
        return box;
    }

    private Button button(String text, String color, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(handler);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        return button;
    }

    public ComboBox<String> getRChannelCombo() { return rChannelCombo; }
    public ComboBox<String> getGChannelCombo() { return gChannelCombo; }
    public ComboBox<String> getBChannelCombo() { return bChannelCombo; }
    public ComboBox<String> getEdgeModeCombo() { return edgeModeCombo; }
    public Slider getStrengthSlider() { return strengthSlider; }
    public Slider getWebpQualitySlider() { return webpQualitySlider; }

    public ScrollPane getRoot() { return scrollPane; }
}
