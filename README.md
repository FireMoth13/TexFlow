# TexFlow

TexFlow 是一款面向游戏美术和游戏开发的贴图处理工具。它把常见的 PBR 贴图处理功能放到一个桌面软件里，方便你快速做这些事：

- 导入一张或多张图片
- 预览图片，支持缩放和拖拽
- 把多张灰度图打包到一张图的不同通道里
- 生成 Mipmap
- 把灰度高度图烘焙成法线贴图
- 导出 PNG、WebP、DDS
- 对多张图片批量处理

## 这个软件适合做什么

如果你在做游戏资源，TexFlow 可以帮你把“美术图”整理成“引擎更容易用的贴图”。

常见场景：

- 把 `Metallic`、`Roughness`、`AO` 打包成一张图
- 从高度图生成法线贴图
- 为远距离渲染准备 Mipmap
- 批量导出 WebP 或 DDS，减少文件体积或适配引擎

## 新手先看懂这几个词

### 灰度图

灰度图就是黑白图，没有彩色信息。

- 黑色表示数值低
- 白色表示数值高
- 灰色表示中间值

它常常不是拿来“看颜色”的，而是拿来存数据，比如：

- 粗糙度
- 金属度
- AO
- 高度图

### RGB 图

RGB 图就是我们平时最常见的彩色图，由红、绿、蓝三个通道组成。

它常用于：

- 颜色贴图
- 贴图打包后的数据图

### 通道打包

通道打包就是把几张灰度图塞进同一张图的不同通道里。

比如：

- R 通道放 Metallic
- G 通道放 Roughness
- B 通道放 AO

这样引擎只要读一张图，就能拿到三份数据。

### Mipmap

Mipmap 是一张贴图的多级缩小版本。

它的作用是：

- 物体离镜头远时，自动用更小的贴图
- 减少闪烁和摩尔纹
- 让远处画面更稳定

注意：Mipmap 不是把原图“修复回去”的东西，它只是给引擎准备多个尺寸版本。

### 法线贴图烘焙

法线贴图烘焙是把高模表面的细节，转换成低模可用的法线贴图。

它的目的不是“消灭高模”，而是：

- 保留高模细节的视觉效果
- 让游戏里真正运行的模型保持低面数

通常工作流是：

- 高模负责提供细节
- 低模负责运行时显示
- 法线贴图负责“看起来像有凹凸”

高模一般会保留在制作阶段，运行时通常不直接加载。

## 支持的功能

### 1. 导入图片

支持导入：

- `png`
- `jpg`
- `jpeg`
- `tga`
- `bmp`

你可以直接导入多个文件，也可以导入整个文件夹。

### 2. 预览图片

左侧列表会显示所有导入的图片。
中间预览区支持：

- 鼠标滚轮缩放
- 左键拖拽平移
- 一键重置视图

### 3. 通道打包

你可以选择三张图片分别放进：

- R 通道
- G 通道
- B 通道

如果某个通道不选，默认相当于白色 `1.0`。

适合做：

- MRAO
- ORM
- 其他通道遮罩图

### 4. 生成 Mipmap

会为当前选中的图片生成一组逐级缩小的版本。

生成后，这些 mip level 会加入图片列表，方便你继续预览或导出。

### 5. 生成法线贴图

输入通常是灰度高度图。

你可以调整：

- `Strength`：凹凸强度
- `Clamp / Wrap`：边缘采样方式

输出结果是一张法线贴图，通常看起来会偏蓝紫色。

### 6. 导出图片

当前预览图可以导出为：

- `PNG`
- `WebP`
- `DDS`

其中：

- `WebP` 可以设置质量
- `DDS` 支持 `BC1 / BC3(DXT5) / BC7`

### 7. 批量处理

可以对当前载入的所有图片批量执行：

- 批量生成法线贴图
- 批量生成 Mipmap
- 批量导出 WebP

## 基本使用方法

1. 点击 `导入图片` 或 `导入文件夹`
2. 在左侧选择一张图片，在中间区域查看
3. 需要打包通道时，在右侧选择 R/G/B 对应图片
4. 点击对应功能按钮：
   - `通道打包`
   - `生成 Mipmap`
   - `生成法线贴图`
5. 处理完成后，使用 `导出 PNG`、`导出 WebP` 或 `导出 DDS`

## 常见用法举例

### PBR 通道打包

你有三张灰度图：

- Metallic
- Roughness
- AO

可以把它们打包成一张图：

- R = Metallic
- G = Roughness
- B = AO

这样更省贴图数量，也更省显存。

### 法线贴图

你有一张灰度高度图，想让低模看起来有凹凸感：

1. 导入高度图
2. 调整 `Strength`
3. 生成法线贴图
4. 导出并接到引擎材质的 Normal Map 槽位

### Mipmap

你有一张大图，想让远处显示更稳、更少闪烁：

1. 导入图片
2. 生成 Mipmap
3. 导出你需要的那一层，或者继续保留在软件里做后续处理

## 导出限制

- `DDS` 的 `BC1 / BC3 / BC7` 压缩要求图片宽和高都能被 4 整除
- `WebP` 导出依赖项目里的 WebP ImageIO 支持
- `Mipmap` 和法线贴图是生成出来的新图，不会自动替你改动原图

## 开发环境

- Java 17
- JavaFX 21.0.2

## 运行

```bash
mvn javafx:run
```

## 测试

```bash
mvn test
```

## 打包

```bash
mvn clean package
```

## 项目结构

```text
texture-pipeline-tool/
├── pom.xml                           Maven 配置
├── src/main/java/com/texturepipeline/
│   ├── App.java                      JavaFX 启动入口
│   ├── Launcher.java                 打包后的启动入口
│   ├── model/
│   │   └── TextureImage.java         贴图数据对象
│   ├── engine/                       贴图处理核心
│   │   ├── ChannelPacker.java        通道打包
│   │   ├── MipmapGenerator.java      Mipmap 生成
│   │   ├── NormalMapGenerator.java   法线贴图烘焙
│   │   ├── PipelineEngine.java       异步流水线
│   │   ├── BatchProcessor.java       批量处理
│   │   ├── FormatConverter.java      PNG / WebP 导出
│   │   └── TextureCompressor.java    BC1 / BC3 / BC7 DDS 压缩
│   └── ui/                           JavaFX 界面
│       ├── MainWindow.java           主窗口和按钮逻辑
│       ├── ImagePreview.java         图片预览区
│       ├── PipelinePanel.java        功能面板
│       └── HistoryManager.java       撤销 / 重做
└── src/test/java/com/texturepipeline/
    └── engine/
        ├── ChannelPackerTest.java
        ├── MipmapGeneratorTest.java
        └── NormalMapGeneratorTest.java
```

## 说明

这个项目是一个桌面端贴图处理工具，目标不是替代专业 DCC 软件，而是把游戏开发里最常用的贴图整理工作做得更快一点。

## License

MIT
