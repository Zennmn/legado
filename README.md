# 阅读手表版

基于 [legado](https://github.com/gedoor/legado) 深度裁剪的离线 TXT 阅读器，专为 OPPO Watch X 圆屏手表使用。

## 功能

- 扫描 Download 文件夹下的 `.txt` 文件并自动导入书架
- 保留阅读进度
- 手表适配的书架、目录、阅读菜单和阅读设置
- 圆屏安全区布局，内容不溢出表盘边缘
- 左边缘右滑退出应用（首页）/ 左边缘点击翻上一页（阅读页）
- 极简黑底开屏，无在线功能

## 不包含

无书源、无 RSS、无 Web 服务、无云同步、无在线下载、无网络权限。完全离线运行。

## 构建

```bash
# 构建 debug APK
./gradlew assembleAppDebug

# 运行单元测试
./gradlew testAppDebugUnitTest
```

输出路径：`app/build/outputs/apk/app/debug/`

## 目标设备

- OPPO Watch X（466×466 圆屏，Android 11 / API 30）
- 理论兼容其他 Wear OS 圆屏手表

## 技术要点

- `EdgeSwipeBackLayout`：共享边缘手势层，ACTION_DOWN 先交给子页面，空白边缘才回退捕获，ACTION_MOVE 阶段触发返回
- `ReadView`：九宫格点击翻页区域，左/中右映射上一页/下一页
- `WatchReaderDefaults`：手表专属默认配置（夜间模式、圆屏 padding、点击区域映射）
- TXT 章节正则使用 `\p{Punct}` 替代 `\p{P}` 避免 Android 兼容性问题

## 致谢

原始项目：[legado](https://github.com/gedoor/legado) by gedoor
