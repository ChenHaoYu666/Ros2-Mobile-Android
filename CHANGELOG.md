# CHANGELOG

## 2026-02-10 – 初次引入 ROS2 配置与 VIZ 基础

### 新增

- **Ros2ConfigManager** (`app/src/main/java/com/example/ros2_android_test_app/Ros2ConfigManager.java`)
  - 使用 `SharedPreferences` 管理 ROS2 相关配置，当前支持：
    - `ros_domain_id`（int，默认 0）。
  - 提供：
    - `int getDomainId(Context context)`
    - `void setDomainId(Context context, int domainId)`

- **VizMapView** (`app/src/main/java/com/example/ros2_android_test_app/VizMapView.java`)
  - 自定义 `View`，基于 `Canvas` 绘制 2D 点云和坐标轴：
    - 背景为深色矩形。
    - 中心绘制简单坐标轴表示机器人位置。
    - 按 1m≈50px 比例将激光点云绘制为绿色点集。
  - 提供 `updateLaserScan(float[] ranges, float angleMin, float angleIncrement)` 接口：
    - 将 `LaserScan` 极坐标(r, angle) 转换为平面坐标 (x, y)，保存在内部点云列表中并触发重绘。

- **LaserScanNode** (`app/src/main/java/com/example/ros2_android_test_app/LaserScanNode.java`)
  - 基于 `BaseComposableNode` 的 ROS2 订阅节点，订阅 `sensor_msgs/msg/LaserScan`：
    - 内部持有 `Subscription<LaserScan>`，根据当前 rcljava API 使用 `this.node.createSubscription(...)` 创建订阅。
    - 构造时指定节点名和话题名（例如 `/scan`），并在 `start()` 中延迟创建订阅。
    - 回调中拷贝 `ranges`，提取 `angle_min` 与 `angle_increment`，通过 `Handler(Looper.getMainLooper())` 将数据回调到主线程。
  - 提供 `LaserScanListener` 接口：
    - `void onLaserScan(float[] ranges, float angleMin, float angleIncrement)`。
  - 提供 `cleanup()` 方法清理订阅引用。
  - 提供 `Node getNode()`（当前 Executor 直接使用 `LaserScanNode` 实例进行 add/remove，`getNode()` 主要供调试或扩展使用）。

- **CmdVelPublisherNode** (`app/src/main/java/com/example/ros2_android_test_app/CmdVelPublisherNode.java`)
  - 基于 `BaseComposableNode` 的简单 `/cmd_vel` 发布节点：
    - 延迟创建 `Publisher<geometry_msgs.msg.Twist>`，在第一次调用 `publishCmd` 时实例化。
    - `publishCmd(double linearX, double angularZ)` 用于发送线速度与角速度指令。

- **VizActivity / VizFragment**
  - 早期版本使用 `VizActivity`，后迁移为 `VizFragment` (`app/src/main/java/com/example/ros2_android_test_app/VizFragment.java`)，承载 ROS 可视化界面：
    - 在 `onCreateView` / `onViewCreated` 中：
      - 加载布局 `fragment_viz.xml`。
      - 绑定 `VizMapView` 与右下角 `JoystickView`。
      - 创建 `LaserScanNode`（节点名 `android_viz_laserscan_*`，默认订阅 `/scan`），并通过 `Executor` 加入执行器。
    - 在 `onDestroyView` 中：
      - 从 `Executor` 中移除 `LaserScanNode`，调用 `cleanup()` 释放资源。
    - 在 `onLaserScan` 中：
      - 将获得的激光数据传递给 `VizMapView.updateLaserScan(...)` 进行绘制。

- **activity_viz.xml / fragment_viz.xml**
  - 早期版本为 `activity_viz.xml`，后迁移为 `fragment_viz.xml` 以适配 Tab + ViewPager 结构：
    - 顶部工具条 `vizToolbar` 模拟 ROS-Mobile 的可视化标题栏：
      - 左侧 `btnBack` 按钮（目前调用 `onBackPressed()`）。
      - 中间 `vizTitle` 文本显示 "VIZ"。
      - 右侧 `btnEditWidgets` 按钮，预留 Widget 编辑入口（当前点击弹出 Toast "Widget 编辑功能开发中"）。
    - 中间 `VizMapView` 填充可视化区域，用于显示激光点云/未来地图。
    - 右下角使用 `CardView` 包裹的 `JoystickView` (`vizJoystickView`)，作为悬浮的控制 Widget：
      - 圆角卡片、半透明背景，更接近 ROS-Mobile 中 Widget 卡片的外观。

### 修改

- **AndroidManifest.xml** (`app/src/main/AndroidManifest.xml`)
  - 新增 VIZ Activity 声明（早期版本）：
    ```xml
    <activity
        android:name=".VizActivity"
        android:exported="false" />
    ```
  - 保持 `MainActivity` 为启动入口的配置不变。

- **ROSActivity** (`app/src/main/java/com/example/ros2_android_test_app/ROSActivity.java`)
  - 在调用 `RCLJava.rclJavaInit()` 之前，统一从 `Ros2ConfigManager` 读取并设置 ROS Domain ID：
    ```java
    int domainIdInt = Ros2ConfigManager.getDomainId(this);
    String domainId = String.valueOf(domainIdInt);
    System.setProperty("ROS_DOMAIN_ID", domainId);
    ```
  - 保持原有 WiFi 状态检测、多播锁 (`MulticastLock`) 管理和网络状态监听逻辑不变。
  - 日志中输出当前使用的 `ROS_DOMAIN_ID` 和环境变量 `RMW_IMPLEMENTATION`。

- **activity_main.xml** (`app/src/main/res/layout/activity_main.xml`)
  - 重构为顶部 TabLayout + 下方 ViewPager2 结构：
    - 使用 `CoordinatorLayout` + `AppBarLayout` + `TabLayout` 作为顶部标签栏容器。
    - 使用 `ViewPager2` 作为内容区域，承载多个 Fragment：VIZ / SETTINGS / DETAILS。

- **MainActivity / MainPagerAdapter**
  - `MainActivity` (`app/src/main/java/com/example/ros2_android_test_app/MainActivity.java`) 继承 `ROSActivity`，作为单一主窗口：
    - 在 `onCreate` 中：
      - 绑定 `TabLayout` 和 `ViewPager2`。
      - 使用 `MainPagerAdapter` 提供 3 个 Fragment：
        - `position 0` → `VizFragment`（VIZ 页面）。
        - `position 1` → `SettingsFragment`（Domain ID 设置页）。
        - `position 2` → `DetailsFragment`（Widget 详情配置页）。
      - 调用 `viewPager.setUserInputEnabled(false)` 禁用左右滑动切换，只允许通过点击 Tab 标签切换，符合你“点击上方按钮切换窗口”的需求。
      - 使用 `TabLayoutMediator` 为三个位置分别设置标题："VIZ"、"SETTINGS"、"DETAILS"。

  - `MainPagerAdapter` (`app/src/main/java/com/example/ros2_android_test_app/MainPagerAdapter.java`)：
    - `createFragment(position)` 返回对应 Fragment 实例。
    - `getItemCount()` 返回 3。

- **主题调整** (`app/src/main/res/values/themes.xml`)
  - 将应用主题从带 ActionBar 的样式改为无 ActionBar，以隐藏系统默认标题栏 `ros2-android-test-app`：
    ```xml
    <style name="Theme.Ros2androidtestapp" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        ...
    </style>
    ```
  - 顶部 UI 完全由自定义的 TabLayout 和 Fragment 内 toolbar 控制，更接近 ROS-Mobile 的外观。

- **SettingsFragment / fragment_settings.xml**
  - `SettingsFragment` (`app/src/main/java/com/example/ros2_android_test_app/SettingsFragment.java`)：
    - 负责 Domain ID 的设置界面：
      - 从 `Ros2ConfigManager` 读取当前 Domain ID，显示在 `EditText` 中。
      - 点击 `Save Domain` 按钮时，解析输入并保存到 SharedPreferences，提示用户重启 App 后生效。
  - `fragment_settings.xml`：
    - 顶部蓝色标题条 `SETTINGS`。
    - 下方为 `Domain ID` 标签、输入框和保存按钮。

## 2026-02-10 – 引入 Widget 抽象与 JoystickWidget（ROS-Mobile 控件逻辑适配）

### 新增

- **JoystickWidgetEntity** (`app/src/main/java/com/example/ros2_android_test_app/widgets/JoystickWidgetEntity.java`)
  - 对标 ROS-Mobile 中的 `JoystickEntity`，但做了简化，当前仅保存与 ROS2 控制相关的核心参数：
    - `topicName`：摇杆控制发布的 topic 名，默认 `/cmd_vel`。
    - `maxLinear`：最大线速度（m/s），默认 `0.5`。
    - `maxAngular`：最大角速度（rad/s），默认 `1.0`。

- **Widget 接口** (`app/src/main/java/com/example/ros2_android_test_app/widgets/Widget.java`)
  - 抽象 Widget 的生命周期与职责：
    - `void bindView(View rootView)`：从 Fragment 的布局中查找并绑定实际的 View 控件（例如 `vizJoystickView`）。
    - `void onRos2Attached(Executor executor)`：在 ROS2 Executor 准备好时，创建并注册需要的节点以及绑定控件行为。
    - `void onRos2Detached(Executor executor)`：在 Fragment 销毁时，从 Executor 中移除节点并清理回调，防止资源泄露。

- **JoystickWidget** (`app/src/main/java/com/example/ros2_android_test_app/widgets/JoystickWidget.java`)
  - 封装 Joystick 控件和 ROS2 通信逻辑的 Widget：
    - 持有一个 `JoystickWidgetEntity`（topicName、maxLinear、maxAngular）。
    - 持有界面中的 `JoystickView`（通过 `bindView` 查找 `R.id.vizJoystickView`）。
    - 持有一个内部的 `CmdVelPublisherNode`：
      - 在 `onRos2Attached` 中：
        ```java
        String nodeName = "ros2_joy_widget_cmdvel";
        cmdVelNode = new CmdVelPublisherNode(nodeName, entity.topicName);
        executor.addNode(cmdVelNode);
        ```
      - 将 `JoystickView` 的坐标回调映射到速度指令：
        ```java
        joystickView.setOnJoystickMoveListener((x, y) -> {
            float mappedX = -x;
            float mappedY = y; // 上为正，下为负

            double linear = entity.maxLinear * mappedY;
            double angular = entity.maxAngular * mappedX;

            if (cmdVelNode != null) {
                cmdVelNode.publishCmd(linear, angular);
            }
        });
        ```
      - 在 `onRos2Detached` 中：
        - 从 Executor 移除 `cmdVelNode`；
        - 调用 `cmdVelNode.cleanup()` 并将引用清空；
        - 将 `JoystickView` 的监听置空，避免引用泄露。
  - 这样就将原本直接写在 Fragment 中的摇杆控制逻辑抽象为一个可管理的 Widget，更接近 ROS-Mobile 的控件体系。

- **WidgetConfigManager** (`app/src/main/java/com/example/ros2_android_test_app/widgets/WidgetConfigManager.java`)
  - 使用 SharedPreferences 管理 Widget 的配置，当前只实现 Joystick 的配置持久化：
    - 存储键：
      - `joy_topic`：字符串，Joystick 控制的 topic 名。
      - `joy_max_linear`：double 的 long bits。
      - `joy_max_angular`：double 的 long bits。
  - 提供：
    - `JoystickWidgetEntity loadJoystickConfig(Context context)`：
      - 读取上述键，构造并返回 `JoystickWidgetEntity`，默认值为 `/cmd_vel`、`0.5`、`1.0`。
    - `void saveJoystickConfig(Context context, JoystickWidgetEntity entity)`：
      - 将当前 Entity 的配置写入 SharedPreferences。

- **DetailsFragment** (`app/src/main/java/com/example/ros2_android_test_app/DetailsFragment.java`)
  - 对标 ROS-Mobile 的 `DETAILS` 窗口，暂时只负责 Joystick Widget 的配置编辑：
    - 在 `onCreateView` 加载 `fragment_details.xml`。
    - 在 `onViewCreated` 中：
      - 调用 `WidgetConfigManager.loadJoystickConfig(requireContext())` 获取当前配置。
      - 初始化三个输入框：
        - `joyTopicEdit`：显示 `topicName`。
        - `joyMaxLinearEdit`：显示 `maxLinear`。
        - `joyMaxAngularEdit`：显示 `maxAngular`。
      - 点击 `saveJoyBtn` 时：
        - 从输入框读取 topic 名、线速度、角速度并做合法性检查；
        - 构造新的 `JoystickWidgetEntity`；
        - 调用 `WidgetConfigManager.saveJoystickConfig(...)` 保存；
        - 通过 Toast 提示 `Saved`。

- **fragment_details.xml** (`app/src/main/res/layout/fragment_details.xml`)
  - 布局结构：
    - 顶部蓝色标题条 `DETAILS`。
    - 下方依次为：
      - `Joystick Topic` + 编辑框 `joyTopicEdit`。
      - `Max Linear (m/s)` + 编辑框 `joyMaxLinearEdit`（`inputType=numberDecimal`）。
      - `Max Angular (rad/s)` + 编辑框 `joyMaxAngularEdit`（`inputType=numberDecimal`）。
      - `Save` 按钮 `saveJoyBtn`。
  - 当前仅服务于单一 Joystick Widget 的配置，后续可以在该界面继续增加其它 Widget 的配置选项。

### 修改

- **VizFragment** (`app/src/main/java/com/example/ros2_android_test_app/VizFragment.java`)
  - 原有逻辑中直接在 Fragment 内创建 `CmdVelPublisherNode` 并绑定 `JoystickView` 的代码已经移除，改为使用 `JoystickWidget`：
    ```java
    ROSActivity rosActivity = (ROSActivity) requireActivity();
    Executor executor = rosActivity.getExecutor();

    // 创建并添加 LaserScanNode（保持不变）
    ...

    // 使用 JoystickWidget 适配摇杆控件和 /cmd_vel 发布
    JoystickWidgetEntity entity = WidgetConfigManager.loadJoystickConfig(requireContext());
    joystickWidget = new JoystickWidget(entity);
    joystickWidget.bindView(view);
    joystickWidget.onRos2Attached(executor);
    ```

  - 在 `onDestroyView` 中，除了清理 `LaserScanNode`，还会调用：
    ```java
    if (joystickWidget != null) {
        joystickWidget.onRos2Detached(executor);
    }
    ```

  - 这样 VIZ 页的摇杆完全通过 Widget 抽象来驱动，配置由 `JoystickWidgetEntity` + `WidgetConfigManager` 提供，结构上更接近 ROS-Mobile 的控件逻辑。

- **MainPagerAdapter / MainActivity**
  - `MainPagerAdapter` 更新为 3 个页面：
    - `position 0`: `VizFragment`。
    - `position 1`: `SettingsFragment`。
    - `position 2`: `DetailsFragment`。
  - `MainActivity` 中 Tab 文本更新为：
    - `VIZ` / `SETTINGS` / `DETAILS`，并继续禁用 ViewPager 的左右滑动，只通过 Tab 点击切换页面。

### 已知行为与使用说明

- **VIZ 页**：
  - 通过 `VizFragment` 显示：
    - 顶部工具条（Back / VIZ / Edit）。
    - 中间 `VizMapView` 显示 `/scan` 点云。
    - 右下角 `CardView` 包裹的 `JoystickView` 作为摇杆控件。
  - `JoystickWidget` 使用 `JoystickWidgetEntity` 中的 topic 和速度参数，通过内部的 `CmdVelPublisherNode` 发布 `/cmd_vel`。

- **DETAILS 页**：
  - 通过 `DetailsFragment` 编辑 Joystick 的配置：
    - Topic 名称（缺省 `/cmd_vel`）。
    - 最大线速度与角速度。
  - 点击 Save 后配置会存入 SharedPreferences，并在下一次进入 VIZ 时生效（VIZ 页面通过 `WidgetConfigManager.loadJoystickConfig` 加载配置）。

- **SETTINGS 页**：
  - 继续负责 Domain ID 配置，与 Widget 系统独立。
