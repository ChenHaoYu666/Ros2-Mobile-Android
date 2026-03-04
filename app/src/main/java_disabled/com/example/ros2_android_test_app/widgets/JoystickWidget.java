package com.example.ros2_android_test_app.widgets;

import android.util.Log;
import android.view.View;

import com.example.ros2_android_test_app.CmdVelPublisherNode;
import com.example.ros2_android_test_app.JoystickView;

import org.ros2.rcljava.executors.Executor;

public class JoystickWidget implements Widget {

    private static final String TAG = "JoystickWidget";

    private final JoystickWidgetEntity entity;
    private JoystickView joystickView;
    private CmdVelPublisherNode cmdVelNode;

    public JoystickWidget(JoystickWidgetEntity entity) {
        this.entity = entity;
    }

    @Override
    public void bindView(View rootView) {
        if (rootView == null) return;
        View v = rootView.findViewById(com.example.ros2_android_test_app.R.id.vizJoystickView);
        if (v instanceof JoystickView) {
            joystickView = (JoystickView) v;
        } else {
            Log.w(TAG, "bindView: vizJoystickView not found or not JoystickView");
        }
    }

    @Override
    public void onRos2Attached(Executor executor) {
        if (executor == null) {
            Log.w(TAG, "onRos2Attached: executor is null");
            return;
        }
        try {
            String nodeName = "ros2_joy_widget_cmdvel";
            cmdVelNode = new CmdVelPublisherNode(nodeName, entity.topicName);
            executor.addNode(cmdVelNode);
            Log.i(TAG, "CmdVelPublisherNode added to executor with name=" + nodeName);
        } catch (Exception e) {
            Log.e(TAG, "Error creating or adding CmdVelPublisherNode", e);
        }

        if (joystickView != null) {
            joystickView.setOnJoystickMoveListener((x, y) -> {
                float mappedX = -x;
                float mappedY = y; // 上为正、下为负

                double linear = entity.maxLinear * mappedY;
                double angular = entity.maxAngular * mappedX;

                if (cmdVelNode != null) {
                    cmdVelNode.publishCmd(linear, angular);
                }
            });
        }
    }

    @Override
    public void onRos2Detached(Executor executor) {
        if (executor != null && cmdVelNode != null) {
            try {
                executor.removeNode(cmdVelNode);
            } catch (Exception e) {
                Log.e(TAG, "Error removing CmdVelPublisherNode from executor", e);
            }
        }
        if (cmdVelNode != null) {
            cmdVelNode.cleanup();
            cmdVelNode = null;
        }

        if (joystickView != null) {
            joystickView.setOnJoystickMoveListener(null);
        }
    }
}

