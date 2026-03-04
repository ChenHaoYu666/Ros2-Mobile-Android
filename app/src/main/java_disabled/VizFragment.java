package com.example.ros2_android_test_app;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ros2_android_test_app.widgets.ButtonWidget;
import com.example.ros2_android_test_app.widgets.ButtonWidgetEntity;
import com.example.ros2_android_test_app.widgets.JoystickWidget;
import com.example.ros2_android_test_app.widgets.JoystickWidgetEntity;
import com.example.ros2_android_test_app.widgets.WidgetConfigManager;

import org.ros2.rcljava.executors.Executor;

public class VizFragment extends Fragment implements LaserScanNode.LaserScanListener {

    private static final String TAG = "VizFragment";

    private VizMapView mapView;
    private LaserScanNode laserScanNode;
    private JoystickWidget joystickWidget;
    private ButtonWidget buttonWidget;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_viz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.vizMapView);

        ROSActivity rosActivity = (ROSActivity) requireActivity();
        Executor executor = rosActivity.getExecutor();

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

        // 使用 JoystickWidget
        JoystickWidgetEntity joyEntity = WidgetConfigManager.loadJoystickConfig(requireContext());
        joystickWidget = new JoystickWidget(joyEntity);
        joystickWidget.bindView(view);
        joystickWidget.onRos2Attached(executor);

        // 使用 ButtonWidget
        ButtonWidgetEntity btnEntity = WidgetConfigManager.loadButtonConfig(requireContext());
        buttonWidget = new ButtonWidget(btnEntity);
        buttonWidget.bindView(view);
        buttonWidget.onRos2Attached(executor);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ROSActivity rosActivity = (ROSActivity) requireActivity();
        Executor executor = rosActivity.getExecutor();

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

        if (joystickWidget != null) {
            joystickWidget.onRos2Detached(executor);
        }

        if (buttonWidget != null) {
            buttonWidget.onRos2Detached(executor);
        }
    }

    @Override
    public void onLaserScan(float[] ranges, float angleMin, float angleIncrement) {
        if (mapView != null) {
            mapView.updateLaserScan(ranges, angleMin, angleIncrement);
        }
    }

    private String generateUniqueNodeName(String prefix) {
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = String.valueOf(System.currentTimeMillis());
        }
        String suffix = deviceId.substring(0, Math.min(deviceId.length(), 8));
        return prefix + "_" + suffix;
    }
}
