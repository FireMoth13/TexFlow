# Texture Pipeline Tool

PBR 纹理处理工具 — 游戏开发者的纹理流水线工具箱。支持通道打包（MRAO）、Mipmap 生成、高度图烘焙法线贴图。

## 功能

| 功能 | 说明 | 输入 | 输出 |
|------|------|------|------|
| **通道打包** | 将 Metallic / Roughness / AO 三张灰度图合并为一张 RGB 纹理 | 1~3 张灰度图 | 合并后的 PNG（R=M, G=R, B=AO） |
| **Mipmap 生成** | 自动生成 1/2→1/4→...→1px 的 Mipmap 链 | 单张纹理 | 多级缩小的 Mipmap 序列 |
| **法线贴图烘焙** | Sobel 3×3 算子从高度图生成切线空间法线贴图，可调节强度 | 灰度高度图 | 法线贴图（淡紫色） |
| **多线程批处理** | 导入文件夹，对全部纹理并行批量生成法线贴图 / Mipmap | 整个文件夹 | 批量处理结果 + 进度日志 |
| **格式转换** | PNG ↔ WebP 互转，可调节压缩质量 (0~100) | PNG / WebP | WebP / PNG |
| **纹理压缩** | 纯 Java BC1 (DXT1) GPU 块压缩，导出 .dds 文件，8:1 压缩比 | RGBA 图像 (4×对齐) | DDS 文件 |

## 技术栈

- **语言**：Java 17
- **UI**：JavaFX 21（Catppuccin Mocha 暗色主题）
- **图像处理**：Java 2D `BufferedImage`（零额外依赖）
- **WebP 编码**：`webp-imageio` (usefulness fork, 原生库内置)
- **构建**：Maven + `javafx-maven-plugin`

## 快速开始

```bash
# 构建
mvn compile

# 运行测试
mvn test

# 运行
mvn javafx:run
```

## 项目结构

```
texture-pipeline-tool/
├── pom.xml
├── src/main/java/com/texturepipeline/
│   ├── App.java                      ← 入口
│   ├── model/
│   │   └── TextureImage.java         ← 纹理数据对象
│   ├── engine/                       ← 图像处理算法（纯函数）
│   │   ├── ChannelPacker.java        ← 通道合并
│   │   ├── MipmapGenerator.java      ← Mipmap 生成
│   │   ├── NormalMapGenerator.java   ← 法线贴图烘焙
│   │   ├── PipelineEngine.java       ← 异步流水线编排
│   │   ├── BatchProcessor.java       ← 多线程批量处理
│   │   ├── FormatConverter.java      ← PNG↔WebP 格式转换
│   │   └── TextureCompressor.java    ← BC1/DXT1 GPU 块压缩
│   └── ui/                           ← JavaFX 界面
│       ├── MainWindow.java           ← 主窗口 + 事件路由
│       ├── ImagePreview.java         ← Canvas 像素预览
│       └── PipelinePanel.java        ← 控制面板
└── src/test/java/com/texturepipeline/
    └── engine/                       ← 单元测试 (JUnit 5)
        ├── ChannelPackerTest.java
        ├── MipmapGeneratorTest.java
        └── NormalMapGeneratorTest.java
```

## 使用指南

### 通道打包

1. 点击 **📁 导入图片**（或拖拽图片到左侧面板），加载 Metallic、Roughness、AO 三张灰度图
2. 在右侧面板分别选择 R（Metallic）、G（Roughness）、B（AO）通道对应的图片
3. 点击 **▶ 执行打包**
4. 预览区显示合并结果，点击 **📥 导出** 保存

### Mipmap 生成

1. 导入一张纹理，点击左侧列表选中
2. 点击 **▶ 生成 Mipmap**
3. 预览区显示原图，日志输出 Mipmap 总内存占用

### 法线贴图烘焙

1. 导入一张灰度高度图（或拖拽到左侧面板）
2. 调整 **凹凸强度 (Strength)** 滑块（0.5~10.0，值越大凹凸感越强）
3. 选择边界模式（Clamp 边缘复制 / Wrap 循环平铺）
4. 点击 **▶ 生成法线贴图**
5. 导出淡紫色法线贴图

### 多线程批处理

1. 点击 **📂 导入文件夹** 选择包含多张纹理的文件夹
2. 调节法线贴图参数（Strength 滑块 + 边界模式）
3. 点击 **🔄 批量生成法线贴图** 或 **🔄 批量生成 Mipmap**
4. 日志区显示每张图的处理进度 `[N/总数]`，所有图片并行处理

### 格式转换 (PNG → WebP)

1. 导入纹理（单张或文件夹）
2. 调节 **WebP 压缩质量** 滑块（0~100，默认80）
3. 点击 **📥 导出当前为 WebP** 导出单张，或点击 **🔄 批量导出 WebP** 选择输出目录批量转换

### 纹理压缩 (BC1 / DXT1)

1. 导入 RGBA 纹理（宽高须为 4 的倍数）
2. 点击 **📥 导出当前为 DDS (BC1)** 执行 GPU 块压缩
3. 日志输出压缩比（固定 8:1）和文件大小信息

## 算法原理

### 通道打包

PBR 材质标准工作流：将 Metallic（金属度）、Roughness（粗糙度）、Ambient Occlusion（环境光遮蔽）分别编码到 RGB 三个通道，一张纹理存三种信息，节省显存。

### Mipmap 生成

每级 Mipmap 长宽各缩小一半，使用双线性插值。从原图一直缩到 1px。避免远处物体采样时的摩尔纹。

### 法线贴图烘焙（Sobel 3×3）

```
取 3×3 邻域 8 个像素的灰度值：

    tl   t   tr
     l  中心  r
    bl   b   br

水平梯度: dX = (tr + 2×r + br) - (tl + 2×l + bl)
垂直梯度: dY = (bl + 2×b + br) - (tl + 2×t + tr)
深度分量: dZ = 1 / strength   （strength 越大 → dZ 越小 → 法线越倾斜 → 凹凸感越强）

法线归一化: (nx, ny, nz) = normalize(dX, dY, dZ)
RGB 编码:    (R, G, B) = ((n+1)/2 × 255) 各分量
```

### BC1 块压缩 (DXT1)

```
图像按 4×4 块分割，每块独立压缩为 64-bit：

[0-15]  c0 RGB565 (min 颜色)
[16-31] c1 RGB565 (max 颜色)
[32-63] 16 × 2-bit 索引 → 四色调色板:
        {c0, c1, 2/3*c0+1/3*c1, 1/3*c0+2/3*c1}

每像素从调色板中选最近色，索引打包为 32 bits。
固定压缩比 8:1（RGBA 64B→8B），所有桌面 GPU 原生支持。
```

## Vibe Coding 说明

本项目包含一个 OpenCode skill 文件（`.opencode/skills/texture-pipeline/SKILL.md`），定义了项目规范：

- 引擎类放在 `engine/` 包下，纯静态方法
- UI 类放在 `ui/` 包下
- 新功能先在 engine 实现算法，再在 ui 加按钮
- 像素操作用 BufferedImage.TYPE_INT_ARGB 格式

重启 OpenCode 后 skill 自动生效。

## 路线图

- [x] 通道打包（MRAO）
- [x] Mipmap 生成
- [x] 法线贴图烘焙
- [x] 拖拽导入支持
- [x] Strength 参数滑块（法线贴图，值越大凹凸感越强）
- [x] 单元测试（JUnit 5, 21 用例）
- [x] 多线程批处理
- [x] 纹理格式转换（PNG→WebP）
- [x] 纹理压缩（BC1/DXT1，纯 Java 实现）

## License

MIT
