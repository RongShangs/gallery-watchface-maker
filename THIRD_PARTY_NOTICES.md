# 第三方说明

## 表盘格式参考

BIN 格式的解析与编码实现参考了 [Mi Create](https://github.com/ooflet/Mi-Create) 的表盘格式、图像压缩及资源组织。本项目代码采用 GPL-3.0-or-later 发布，完整许可见 [LICENSE](LICENSE)。感谢 Mi Create 的作者和贡献者。

## 表盘与示例图像

`app/src/main/assets/template.bin` 中的表盘样式、字形和若叶睦示例图像来自用户提供的表盘和图片，经排版与裁切后作为内置示例。其原有权利归各自权利人所有；本项目的代码许可证不代表对这些图像、角色或既有表盘素材的重新授权。

`app/src/test/resources/default_art.png` 是用于图像导出回归验证的同一示例图像，适用上述说明。用户自己选择的图片保存在手机本地，不会上传到本项目或 GitHub。

## 构建工具与测试依赖

- Gradle Wrapper：Apache-2.0，来自 Gradle。
- Android 构建工具：来自 Google / Android 开源项目。
- JUnit、Robolectric：仅用于测试，不包含在发布 APK 的运行时依赖中。
