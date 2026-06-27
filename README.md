# 济你太美

同济一系统 Android 客户端，基于 Material3 + Jetpack Compose 构建。

## 功能

| 模块 | 说明 |
|---|---|
| **课程表** | 日历视图展示本周/月课程，支持日历导出 |
| **考试安排** | 展示已安排和未安排的考试，支持导出到系统日历 |
| **课程成绩** | 手风琴式学期列表，当前学期默认展开，展示 GPA、学分、绩点 |
| **卓越星活动** | 校园活动列表，支持模块与状态筛选 |
| **通知公告** | 教学通知列表，WebView 渲染 HTML 详情 |
| **校园卡** | 一卡通余额查询（通过 all.tongji.edu.cn） |
| **图书馆座位** | 图书馆座位区域与实时占用查询 |

## 技术栈

- **UI**: Jetpack Compose + Material3
- **网络**: Retrofit + OkHttp + Gson
- **本地存储**: Room + DataStore + EncryptedSharedPreferences
- **日历**: CalendarView（haibin 开源库，子模块）
- **日历导出**: CalendarContract (Android 系统日历)
- **认证**: WebView 登录 + Cookie 同步（CookieManager → OkHttp CookieJar）

## 构建

```bash
./gradlew assembleDebug
```

构建产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 登录

应用内置 WebView 登录同济统一身份认证（1.tongji.edu.cn）。登录成功后：

1. 从 URL 参数或 localStorage 提取 uid
2. 同步 `1.tongji.edu.cn` Cookie 到 OkHttp CookieJar
3. 自动导航到 `all.tongji.edu.cn` 完成 CAS SSO，同步该域 Cookie
4. 各业务 API 通过共享 CookieJar 自动携带认证 Cookie

## 项目结构

```
app/src/main/java/com/example/tongji/
├── auth/                    # 认证相关（CookieJar、CredentialStore、各模块 AuthCoordinator）
├── data/
│   ├── local/               # Room 数据库（Entity + DAO）
│   ├── remote/              # Retrofit API + NetworkModule + AuthInterceptor
│   └── repository/          # 各模块数据仓库
├── state/                   # 全局响应式状态（TermInfo）
├── ui/
│   ├── components/          # 可复用组件
│   ├── navigation/          # 导航路由
│   ├── screens/             # 各页面 Compose Screen
│   └── theme/               # Material3 主题
└── util/                    # 工具类
```

## 参考

本项目参考 [Jinitaimei](https://github.com/okatu-loli/Jinitaimei)（Swift iOS 版同济一系统客户端）实现，核心 API 调用逻辑保持一致。

## License

MIT
