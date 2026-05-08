# Texture Pipeline Tool

PBR 纹理处理工具 — 游戏开发者的纹理流水线工具箱。支持通道打包（MRAO）、Mipmap 生成、高度图烘焙法线贴图。

## 功能

| 功能 | 说明 | 输入 | 输出 |
|------|------|------|------|
| **通道打包** | 将 Metallic / Roughness / AO 三张灰度图合并为一张 RGB 纹理 | 1~3 张灰度图 | 合并后的 PNG（R=M, G=R, B=AO） |
| **Mipmap 生成** | 自动生成 1/2→1/4→...→1px 的 Mipmap 链 | 单张纹理 | 多级缩小的 Mipmap 序列 |
| **法线贴图烘焙** | Sobel 3×3 算子从高度图生成切线空间法线贴图 | 灰度高度图 | 法线贴图（淡紫色） |

## 技术栈

- **语言**：Java 17
- **UI**：JavaFX 21（Catppuccin Mocha 暗色主题）
- **图像处理**：Java 2D `BufferedImage`（零额外依赖）
- **构建**：Maven + `javafx-maven-plugin`

## 快速开始

```bash
# 构建
mvn compile

# 运行
mvn javafx:run
```

## 项目结构

```
texture-pipeline-tool/
├── pom.xml
└── src/main/java/com/texturepipeline/
    ├── App.java                      ← 入口
    ├── model/
    │   └── TextureImage.java         ← 纹理数据对象
    ├── engine/                       ← 图像处理算法（纯函数）
    │   ├── ChannelPacker.java        ← 通道合并
    │   ├── MipmapGenerator.java      ← Mipmap 生成
    │   ├── NormalMapGenerator.java   ← 法线贴图烘焙
    │   └── PipelineEngine.java       ← 异步流水线编排
    └── ui/                           ← JavaFX 界面
        ├── MainWindow.java           ← 主窗口 + 事件路由
        ├── ImagePreview.java         ← Canvas 像素预览
        └── PipelinePanel.java        ← 控制面板
```

## 使用指南

### 通道打包

1. 点击 **📁 导入图片**，加载 Metallic、Roughness、AO 三张灰度图
2. 在右侧面板分别选择 R（Metallic）、G（Roughness）、B（AO）通道对应的图片
3. 点击 **▶ 执行打包**
4. 预览区显示合并结果，点击 **📥 导出** 保存

### Mipmap 生成

1. 导入一张纹理，点击左侧列表选中
2. 点击 **▶ 生成 Mipmap**
3. 预览区显示原图，日志输出 Mipmap 总内存占用

### 法线贴图烘焙

1. 导入一张灰度高度图
2. 选择边界模式（Clamp 边缘复制 / Wrap 循环平铺）
3. 点击 **▶ 生成法线贴图**
4. 导出淡紫色法线贴图

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
深度分量: dZ = 1 / strength

法线归一化: (nx, ny, nz) = normalize(dX, dY, dZ)
RGB 编码:    (R, G, B) = ((n+1)/2 × 255) 各分量
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
- [ ] 拖拽导入支持
- [ ] 多线程批处理
- [ ] 纹理格式转换（PNG→WebP）
- [ ] Strength 参数滑块（法线贴图）
- [ ] 纹理压缩（BC7/ETC2）

## License

MIT
