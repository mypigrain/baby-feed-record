# 宝宝喝奶记录 🍼

一款极简的 Android 宝宝喂养记录应用。**一键记录，无需繁琐操作。**

## 设计理念

记录宝宝的每一次喝奶，操作要足够简单——因为麻烦就不想记了。

打开应用，点击中间的大按钮，喝奶记录即刻保存。喂养量和类型都是可选的，不填也能记录。

## 功能

- **记录** — 大圆形按钮，一键记录当前时间。可选快速选量（30/60/90/120ml）和喂养类型（母乳/配方奶/混合）
- **历史** — 按日期分组展示全部记录，支持删除
- **统计** — 本周喂养总量和次数、每日柱状图、喂养类型比例、历史周趋势

## 技术栈

| 层 | 选型 |
|---|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 数据库 | Room (SQLite) |
| 架构 | MVVM (ViewModel + StateFlow) |
| 导航 | Navigation Compose |
| 最低版本 | Android 8.0 (API 26) |

## 构建

需要 Android Studio 或命令行构建环境。

```bash
# 设置 Android SDK 路径
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# 构建 debug APK
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 项目结构

```
app/src/main/java/com/example/baby/
├── BabyApp.kt              # Application
├── MainActivity.kt         # 唯一 Activity
├── data/
│   ├── FeedingRecord.kt    # Room 实体
│   ├── FeedingDao.kt       # 数据访问
│   └── AppDatabase.kt      # 数据库
├── ui/
│   ├── home/               # 记录页面
│   ├── history/            # 历史页面
│   ├── stats/              # 统计页面
│   ├── navigation/         # 底部导航
│   └── theme/              # Material 3 主题
└── util/
    └── DateUtils.kt        # 日期工具
```
