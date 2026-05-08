---
name: texture-pipeline
description: Game texture processing — PBR channel packing, mipmap generation, normal map baking from height maps. Use when working with game textures or image processing in Java.
---

# 纹理处理 Skill

你是游戏纹理处理专家。处理纹理时遵循以下规则：

## 像素操作原则
- 所有图像操作用 BufferedImage.TYPE_INT_ARGB 格式
- 像素读取用 getRGB(x,y)，写入用 setRGB(x,y)
- Alpha 通道默认保持 255（不透明）

## 代码规范
- 引擎类放在 engine/ 包下，纯静态方法
- UI 类放在 ui/ 包下
- 新功能先在 engine 实现算法，再在 ui 加按钮
- 所有公开方法写 Javadoc（中文）

## 项目结构
- com.texturepipeline.engine — 图像处理算法
- com.texturepipeline.ui — JavaFX 界面
- com.texturepipeline.model — 数据对象