# 桌游工具箱 · TabletopKit

[English](README.en.md) · [中文](README.md)

一款极简的桌游辅助工具 App，用 Jetpack Compose 打造。聚会开局时，帮你快速完成三件「开局前的琐事」：决定谁先走、给玩家排序、把人分好队。

> 灵感来自微信小程序「左游助手」，交互核心理念：**手机递出去，每个人点一下屏幕就登记，停手几秒自动结束**，全程不用盯着操作。

## 功能

| 工具 | 用途 |
| --- | --- |
| **起始玩家 (First Player)** | 随机选出一名先手玩家，配圆环扩散 + 擦除动画 |
| **排序玩家 (Player Order)** | 给所有玩家随机排一个顺序，圆点带座位号数字 |
| **队伍分组 (Team Splitter)** | 按指定队数把玩家均分，队伍尽量平衡 |

三个工具共享同一套「报名内核」：玩家依次点屏幕注册 → 空闲超时（默认 3 秒，可在设置中调整）自动结束登记 → 系统执行随机逻辑并播放动画。

## 技术栈

- **语言 / UI**：Kotlin + Jetpack Compose（Material 3）
- **架构**：单 Activity + ViewModel + StateFlow，`ui` / `vm` / `data` / `domain` / `core` 分层
- **持久化**：Jetpack DataStore Preferences（保存设置，如超时秒数、圆点大小、主题模式）
- **主题**：支持跟随系统 / 浅色 / 深色，可选动态取色（Android 12+）

| 项 | 值 |
| --- | --- |
| 包名 | `com.ankopala.tabletopkit` |
| minSdk | 24 (Android 7.0) |
| targetSdk | 37 |
| versionName | 1.0 |
| Kotlin | 2.2.x（由 AGP 内置） |
| AGP | 9.4.x |

## 构建

```bash
# 需先指向 Android Studio 自带的 JDK
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Debug 构建
./gradlew assembleDebug

# Release 构建（需先配置签名，见下）
./gradlew assembleRelease
```

### Release 签名

签名文件不提交仓库（见 `.gitignore`）。本地放一份 `keystore`，在 `app/build.gradle.kts` 的 `release` 中配置签名后即可产出可安装的 release APK。R8 压缩仅作用于 `androidx.**` / `kotlin.**` / `kotlinx.**`，业务代码不混淆。

## 项目结构

```
app/src/main/java/com/ankopala/tabletopkit/
├── core/          # 跨页面常量（如玩家颜色板 Palette）
├── data/          # 数据模型 + DataStore 设置仓库
├── domain/        # 纯逻辑：洗牌、颜色分配、队伍均衡
├── vm/            # 主 ViewModel
└── ui/
    ├── firstplayer/   # 起始玩家
    ├── playerorder/   # 排序玩家
    ├── teamsplitter/  # 队伍分组
    ├── registration/  # 共享报名内核
    ├── settings/      # 设置页
    ├── home/          # 首页
    └── theme/         # 主题与配色
```

## 设计说明

- **动画模型**：结果页采用「同心圆环带 + 擦除/挖洞遮罩」，扩散时同色擦除其他玩家圆点，回收时用背景色挖出轮廓，配合全程呼吸效果。
- **座位号朝向**：数字头部统一朝向屏幕中心，随角度旋转。
- **可调参数**：圆点大小（40–56 dp）、报名空闲超时秒数，均可在设置页调整。

## License

个人学习项目。
