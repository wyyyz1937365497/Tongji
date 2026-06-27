# 项目代理指令

## 构建与安装约定

- 运行 `./gradlew assembleDebug` 验证构建是否通过。
- **构建成功后不要执行 `adb install` 或 `./gradlew installDebug` 上传到设备。** 设备端安装由用户手动完成。
- 如需验证 APK，可提示用户自行安装 `app/build/outputs/apk/debug/app-debug.apk`。
