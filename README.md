# Sudo - sudo read linux.do

<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Sudo Logo" width="128"/>
  <br>
  <p><b>一个属于 linux.do 社区的现代终端窗口</b></p>
</div>

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white)

## 📌 缘起

最初在论坛上看到了大佬采用 Material You / Material 3 设计的新客户端 [Re::Source](https://github.com/huzheng1/Re-Source)，非常喜欢那种简洁、符合现代 Android 风格的设计，于是萌生了为 linux.do 也写一个类似风格客户端的念头。

本项目是完全依赖大模型（Claude 3.5 Sonnet / Gemini）打造的一次 **Vibe Coding** 实验。通过自然语言对话驱动开发，最终糊出了这个基于最新 Android 技术栈构建的原生客户端。

---

## 🎨 关于设计

- **命名含义**：Linux 下的最高权限指令 `sudo`。每一个 Linux 用户看到这个词都会会心一笑。隐含意义是：*你可以完全掌控这个论坛*。
- **Logo 理念**：标准的 Android 圆角矩形应用图标，搭配经典的终端提示符 `>_`，硬核且极简。

---

## ✨ 核心功能

* 🎨 **丝滑浏览**：完全基于 Jetpack Compose 构建，支持深色模式跟随
* 🔐 **完整账号流**：支持平台 OAuth2 登录授权，安全保存会话
* 📖 **沉浸式阅读**：完美适配 Discourse 语法（Alert提示框、嵌套引用、代码高亮、Onebox 链接卡片）
* 🖼️ **优雅看图**：原生图片灯箱，支持手势缩放、双击放大
* 💬 **便捷互动**：支持点赞、收藏，以及随时随地回复和发帖子（带便捷 Markdown 编辑工具栏）
* ⚙️ **个人中心**：查看通知提醒、我的收藏夹
* 🔍 **全局搜索**：支持快速搜索全站帖子内容

---

## 📸 屏幕截图

> 💡 截图待补充

| 首页流 | 帖子详情 |
|:---:|:---:|
| <img src="" width="250"/> | <img src="" width="250"/> |

| 回复编辑器 | 图片浏览 |
|:---:|:---:|
| <img src="" width="250"/> | <img src="" width="250"/> |

---

## 🛠️ 技术栈

本项目采用了现代 Android 开发的标准最佳实践：

* **100% Kotlin**
* **UI**: [Jetpack Compose](https://developer.android.com/jetpack/compose) - 用于构建界面的现代工具包
* **架构**: MVVM (Model-View-ViewModel)
* **依赖注入**: [Hilt](https://dagger.dev/hilt/)
* **网络请求**: [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/)
* **序列化**: [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
* **图片加载**: [Coil Compose](https://coil-kt.github.io/coil/compose/)
* **本地路由**: [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
* **主题**: Material 3 / Material Design

---

## 🚀 部署与编译

```bash
# 1. 克隆项目
git clone https://github.com/xhuohai/sudo.git
cd sudo

# 2. 编译 Debug 包
./gradlew assembleDebug

# 3. 编译 Release 包 (需配置签名)
./gradlew assembleRelease
```

---

## 🤝 致谢与彩蛋

* 感谢 [Re::Source](https://github.com/huzheng1/Re-Source) 带来的设计灵感
* 感谢社区另一款优秀的客户端 [FluxDO](https://linux.do/t/topic/1609212)！真的是太喜欢 FluxDO 的调调了，不仅功能大而全，而且页面设计得又萌又可爱。

本着“不重复造轮子”的原则（主要是舍不得浪费大模型的 Token 😂），Sudo 的高强度开发暂时告一段落，后续会随缘佛系维护。

**开源不易，如果你觉得这个“赛博包工头”经历和本项目还算有趣，欢迎点亮 ⭐️ Star！**
