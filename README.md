# TexFlow

PBR 纹理处理工具 — 游戏开发者的纹理流水线工具箱。

## 功能

| 功能 | 说明 | 输入 | 输出 |
|------|------|------|------|
| **通道打包** | 将 Metallic / Roughness / AO 三张灰度图合并为一张 RGB 纹理 | 1~3 张灰度图 | 合并后的 PNG（R=M, G=R, B=AO） |
| **Mipmap 生成** | 自动生成 1/2→1/4→...→1px 的 Mipmap 链 | 单张纹理 | 多级缩小的 Mipmap 序列 |
| **法线贴图烘焙** | Sobel 3×3 算子从高度图生成切线空间法线贴图，可调节强度 | 灰度高度图 | 法线贴图（淡紫色） |
| **多线程批处理** | 导入文件夹，对全部纹理并行批量生成法线贴图 / Mipmap | 整个文件夹 | 批量处理结果 + 进度日志 |
| **格式转换** | PNG ↔ WebP 互转，可调节压缩质量 (0~100) | PNG / WebP | WebP / PNG |
| **纹理压缩** | 纯 Java BC1/BC3/BC7 GPU 块压缩，导出 .dds 文件 | RGBA 图像 (4×对齐) | DDS 文件 |
| **预览缩放/拖拽** | 鼠标滚轮缩放 + 左键拖拽平移，支持重置视图 | 任意纹理 | 交互式预览 |
| **撤销/重做** | 操作历史管理，最多 50 步，随时回退不满意的结果 | — | — |

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
│   ├── Launcher.java                 ← Fat JAR 入口
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
│       ├── ImagePreview.java         ← 图像预览（支持缩放/拖拽）
│       ├── PipelinePanel.java        ← 控制面板
│       └── HistoryManager.java       ← 撤销/重做历史管理
└── src/test/java/com/texturepipeline/
    └── engine/                       ← 单元测试 (JUnit 5)
        ├── ChannelPackerTest.java
        ├── MipmapGeneratorTest.java
        └── NormalMapGeneratorTest.java
```

## 使用指南

### 导入纹理

- **单张导入**：点击顶部 **📁 导入图片**，或直接将图片拖拽到左侧面板
- **批量导入**：点击右侧面板 **📂 导入文件夹**，选择包含多张图片的文件夹
- 支持格式：PNG、JPG、JPEG、TGA、BMP

### 通道打包

> **是什么？** 把 Metallic（金属度）、Roughness（粗糙度）、AO（环境光遮蔽）三张灰度图合并成一张 RGB 纹理，一张图存三种信息，节省显存。

1. 导入 Metallic、Roughness、AO 三张灰度图
2. 在右侧面板分别选择 R 通道（Metallic）、G 通道（Roughness）、B 通道（AO）对应的图片
3. 点击 **▶ 执行打包**
4. 预览区显示合并结果，点击 **📥 导出当前纹理 (PNG)** 保存

> **提示**：某个通道不选 = 默认白色（值 1.0）。比如只选 R 和 G 通道，B 通道默认白。

### Mipmap 生成

> **是什么？** 预生成同一张图片的多个缩小版本（原图 → 1/2 → 1/4 → ... → 1px），游戏引擎根据物体远近自动选用合适大小的贴图，避免远处物体闪烁。

1. 导入一张纹理，点击左侧列表选中
2. 点击 **▶ 生成 Mipmap**
3. 日志输出 Mipmap 总内存占用

### 法线贴图烘焙

> **是什么？** 用一张灰度高度图生成紫蓝色的法线贴图，让平面模型看起来也有凹凸感，性能比真实高模高 100 倍。

1. 导入一张灰度高度图（白色凸起、黑色凹陷）
2. 调整 **凹凸强度 (Strength)** 滑块（0.5~10.0，值越大凹凸感越强）
3. 选择边界模式：
   - **Clamp**：边缘复制，适合独立物体
   - **Wrap**：循环平铺，适合无缝贴图
4. 点击 **▶ 生成法线贴图**
5. 预览区显示法线贴图，点击 **📥 导出当前纹理 (PNG)** 保存

### 预览区交互

> 预览区支持鼠标缩放和拖拽，方便查看细节。

| 操作 | 效果 |
|------|------|
| **滚轮上滚** | 以鼠标位置为中心放大 |
| **滚轮下滚** | 以鼠标位置为中心缩小 |
| **左键拖拽** | 平移图像 |
| **点击「重置视图」** | 恢复原始大小和位置 |

缩放范围：0.1x ~ 10x，底部信息栏实时显示当前缩放比例。

### 多线程批处理

> **是什么？** 对已加载的全部纹理执行同一个操作，多线程并行处理，日志实时显示进度。

1. 点击 **📂 导入文件夹** 选择包含多张纹理的文件夹
2. 调节法线贴图参数（Strength 滑块 + 边界模式）
3. 点击以下按钮之一：
   - **🔄 批量生成法线贴图**：所有图片并行生成法线贴图
   - **🔄 批量生成 Mipmap**：所有图片并行生成 Mipmap 链
   - **🔄 批量导出 WebP**：所有图片并行转为 WebP 格式
4. 日志区显示每张图的处理进度 `[N/总数]`

### 格式转换 (PNG → WebP)

> **是什么？** WebP 是 Google 推出的图片格式，同等画质下体积比 PNG 小 25%~35%。

1. 导入纹理（单张或文件夹）
2. 调节 **WebP 压缩质量** 滑块（0~100，默认 80，越高画质越好体积越大）
3. 导出方式：
   - **📥 导出当前为 WebP**：导出单张
   - **🔄 批量导出 WebP**：选择输出目录，全部转换

### 纹理压缩 (BC1 / BC3 / BC7)

> **是什么？** GPU 原生支持的块压缩格式，所有桌面/主机显卡都支持。不同格式适合不同场景：

| 格式 | 压缩比 | 适用场景 |
|------|--------|----------|
| **BC1 (DXT1)** | 8:1 | 不透明纹理，无 alpha 通道 |
| **BC3 (DXT5)** | 4:1 | 带透明度的纹理，alpha 独立压缩 |
| **BC7** | 4:1 | 高质量纹理，自动检测 alpha，画质最优 |

1. 导入 RGBA 纹理（宽高须为 4 的倍数）
2. 根据需要选择导出格式：
   - **📥 导出当前为 DDS (BC1)** — 不透明纹理，最小体积
   - **📥 导出当前为 DDS (BC3/DXT5)** — 带透明度，经典格式
   - **📥 导出当前为 DDS (BC7)** — 最高画质
3. 日志输出压缩比和文件大小信息

### 撤销/重做

> **是什么？** 每次执行通道打包、Mipmap 生成、法线贴图生成前，系统自动保存当前预览状态。你可以随时回退到上一步。

| 操作 | 快捷键/按钮 | 效果 |
|------|------------|------|
| **撤销** | 顶部工具栏 ↩ 按钮 | 回退到上一次操作前的状态 |
| **重做** | 顶部工具栏 ↪ 按钮 | 恢复被撤销的操作 |
| **清空** | 🗑 清空列表 | 清除所有历史和已加载纹理 |

- 最多保存 50 步历史
- 新操作会清空重做栈
- 日志区会记录每次撤销/重做操作

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

## 打包为 .exe

```bash
# 1. 打出 fat JAR
mvn clean package

# 2. 用 jpackage 打包成 .exe（需要 JDK 17+ 和 JavaFX SDK）
jpackage --type exe ^
  --input target ^
  --name TexFlow ^
  --main-jar texture-pipeline-tool-0.1.0.jar ^
  --main-class com.texturepipeline.Launcher ^
  --module-path "C:\javafx-sdk-21.0.2\lib" ^
  --add-modules javafx.controls,javafx.graphics,javafx.swing ^
  --java-options "-Dprism.order=sw" ^
  --dest dist ^
  --vendor "YourName"
```

生成 `dist/TexFlow-0.1.0.exe`，双击即可安装。

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
- [x] 预览区鼠标缩放/拖拽交互
- [x] 更多纹理压缩格式（BC3/DXT5、BC7）
- [x] 撤销/重做支持

## License

MIT
