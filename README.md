# ROS2-Mobile-Android

`ROS2-Mobile-Android` 是一个基于 Android 的 ROS2 移动端控制与可视化应用，面向移动机器人场景，支持在手机端进行话题查看、发布/订阅交互与可视化控件操作。帮助机器人工程师更好地方便地调试机器人。

本项目**参考并继承自** [ROS-Mobile-Android-master](https://github.com/ROS-Mobile/ROS-Mobile-Android)（ROS1 版本）的整体设计与交互思路，并将核心通信能力迁移到 ROS2 生态。基于 ROS2 rcljava 与 FastDDS 中间件进行局域网内原生通信，不依赖 WebSocket 网关，不用配置IP。

## 项目特性

- Android 端 ROS2 通信（基于 `rcljava`）
- 主界面多标签页：`Master / Details / SSH / Viz`
- 支持动态添加与编辑可视化控件（Widget）
- 当前已支持的典型控件类型：
  - Joystick（`geometry_msgs/msg/Twist`）
  - Button（`std_msgs` 多基础类型）
  - Label（多基础类型订阅显示）
  - map (地图、机器人位姿、激光雷达数据显示)
- Master 页支持话题发现与监控
  
 <img src="https://github.com/ChenHaoYu666/Ros2-Mobile-Android/blob/main/images/123.jpg" width="210px"> <img src="https://github.com/ChenHaoYu666/Ros2-Mobile-Android/blob/main/images/viz.jpg" width="210px"> <img src="https://github.com/ChenHaoYu666/Ros2-Mobile-Android/blob/main/images/details.jpg" width="210px"> <img src="https://github.com/ChenHaoYu666/Ros2-Mobile-Android/blob/main/images/ssh.jpg" width="210px">


## 环境要求

- Android Studio（建议较新稳定版本）
- Android 设备（建议 Android 6.0+）
- ROS2 运行环境（与设备网络互通）

## 版本兼容性说明

- 当前仓库内提供的 `.so` 与 `.jar` 依赖以 **ROS2 Humble** 构建链为基准。
- 建议优先与 ROS2 Humble 环境配套使用。
- 其他 ROS2 版本（如 Iron/Jazzy）在部分 Topic 场景可能可互通，但不保证完全兼容；如需稳定支持，建议使用对应版本重新编译一套 Android 依赖。

## 快速开始

### 方式一：直接下载 Release（推荐）

1. 打开本仓库的 `Releases` 页面。
2. 下载与你设备架构匹配的 APK（通常为 `arm64-v8a`）。
3. 将 APK 安装到 Android 设备并启动。
4. 在应用中配置 ROS2 Domain ID 与网络环境，进入 Master 页面查看话题。

### 方式二：从源码构建

1. 使用 Android Studio 打开本项目。
2. 准备并放置 ROS2 Android 二进制依赖到以下目录：
   - `app/libs`
   - `app/src/main/jniLibs`
3. 连接 Android 真机并构建运行。
4. 在应用中配置 ROS2 Domain ID 与网络环境，进入 Master 页面查看话题。
## 构建前准备（重要）

在运行源码构建之前，请先准备 ROS2 Android 相关二进制依赖（`.so` / `.jar`），并放置到项目对应目录：

- `app/libs`
- `app/src/main/jniLibs`

可参考：

- https://github.com/YasuChiba/ros2-android-build
## 致谢

- ROS-Mobile-Android（ROS1）项目贡献者
- ROS2 与 rcljava 社区

## 参考来源

本项目参考了以下开源项目（ROS1）：

- [ROS-Mobile-Android-master](https://github.com/ROS-Mobile/ROS-Mobile-Android)

感谢原项目作者在架构、UI 组织与移动端 ROS 交互方面的工作。
## License

本项目采用 **Apache License 2.0**，详见仓库根目录 `LICENSE` 文件。

- License 文本：`http://www.apache.org/licenses/LICENSE-2.0`
- 项目中涉及的第三方依赖与参考项目（包括 `ROS-Mobile-Android-master`）仍分别遵循其原始许可证声明。
- 若仓库内个别源文件头部包含独立许可证/版权声明，以对应文件声明为准。
