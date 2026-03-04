package com.example.ros2_android_test_app;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import org.ros2.rcljava.executors.Executor;

public class VizActivity extends ROSActivity implements LaserScanNode.LaserScanListener {

    private static final String TAG = "VizActivity";

    private VizMapView mapView;
    private JoystickView joystickView;
    private LaserScanNode laserScanNode;
    private CmdVelPublisherNode cmdVelNode;

    private static final double MAX_LINEAR = 0.5;  // m/s
    private static final double MAX_ANGULAR = 1.0; // rad/s

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viz);

        mapView = findViewById(R.id.vizMapView);
        joystickView = findViewById(R.id.vizJoystickView);
        //Button backBtn = findViewById(R.id.backBtn);
//        if (backBtn != null) {
//            backBtn.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    finish();
//                }
//            });
//        }

        Executor executor = getExecutor();

        // 创建并添加 LaserScanNode
        try {
            String laserNodeName = generateUniqueNodeName("android_viz_laserscan");
            laserScanNode = new LaserScanNode(laserNodeName, "/scan", this);
            if (executor != null && laserScanNode != null) {
                executor.addNode(laserScanNode);
                Log.i(TAG, "LaserScanNode added to executor with name=" + laserNodeName);
            } else {
                Log.w(TAG, "Executor or LaserScanNode is null, cannot add LaserScanNode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating LaserScanNode", e);
        }

        // 创建并添加 CmdVelPublisherNode（用于 Joystick 控制）
        try {
            String cmdVelName = generateUniqueNodeName("android_viz_cmdvel");
            cmdVelNode = new CmdVelPublisherNode(cmdVelName, "/cmd_vel");
            if (executor != null && cmdVelNode != null) {
                executor.addNode(cmdVelNode);
                Log.i(TAG, "CmdVelPublisherNode added to executor with name=" + cmdVelName);
            } else {
                Log.w(TAG, "Executor or CmdVelPublisherNode is null, cannot add cmd_vel node");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating CmdVelPublisherNode", e);
        }

        // 设置摇杆监听：上=前进（linear.x>0），左=左转（angular.z>0）
        if (joystickView != null) {
            joystickView.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
                @Override
                public void onValueChanged(float x, float y) {
                    float mappedX = -x;
                    float mappedY = y; // 上为正、下为负

                    double linear = MAX_LINEAR * mappedY;
                    double angular = MAX_ANGULAR * mappedX;

                    if (cmdVelNode != null) {
                        cmdVelNode.publishCmd(linear, angular);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Executor executor = getExecutor();

        if (laserScanNode != null) {
            try {
                if (executor != null) {
                    executor.removeNode(laserScanNode);
                }
                laserScanNode.cleanup();
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up LaserScanNode", e);
            }
        }

        if (cmdVelNode != null) {
            try {
                if (executor != null) {
                    executor.removeNode(cmdVelNode);
                }
                cmdVelNode.cleanup();
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up CmdVelPublisherNode", e);
            }
        }
    }

    @Override
    public void onLaserScan(float[] ranges, float angleMin, float angleIncrement) {
        if (mapView != null) {
            mapView.updateLaserScan(ranges, angleMin, angleIncrement);
        }
    }

    /**
     * 生成唯一的节点名称，使用设备ID或时间戳作为后缀
     */
    private String generateUniqueNodeName(String prefix) {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = String.valueOf(System.currentTimeMillis());
        }
        String suffix = deviceId.substring(0, Math.min(deviceId.length(), 8));
        return prefix + "_" + suffix;
    }
}
