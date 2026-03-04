package com.example.ros2_android_test_app.widgets;

import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.ros2_android_test_app.ButtonPublisherNode;
import com.example.ros2_android_test_app.R;

import org.ros2.rcljava.executors.Executor;

public class ButtonWidget implements Widget {

    private static final String TAG = "ButtonWidget";

    private final ButtonWidgetEntity entity;
    private Button buttonView;
    private ButtonPublisherNode publisherNode;

    public ButtonWidget(ButtonWidgetEntity entity) {
        this.entity = entity;
    }

    @Override
    public void bindView(View rootView) {
        if (rootView == null) return;
        View v = rootView.findViewById(R.id.vizButtonWidget);
        if (v instanceof Button) {
            buttonView = (Button) v;
        } else {
            Log.w(TAG, "bindView: vizButtonWidget not found or not Button");
        }
    }

    @Override
    public void onRos2Attached(Executor executor) {
        if (executor == null) {
            Log.w(TAG, "onRos2Attached: executor is null");
            return;
        }
        try {
            String nodeName = "ros2_button_widget";
            publisherNode = new ButtonPublisherNode(nodeName, entity.topicName);
            executor.addNode(publisherNode);
            Log.i(TAG, "ButtonPublisherNode added to executor with name=" + nodeName);
        } catch (Exception e) {
            Log.e(TAG, "Error creating or adding ButtonPublisherNode", e);
        }

        if (buttonView != null) {
            buttonView.setOnClickListener(v -> {
                if (publisherNode != null) {
                    publisherNode.publishOnce(entity.messageText);
                }
            });
        }
    }

    @Override
    public void onRos2Detached(Executor executor) {
        if (executor != null && publisherNode != null) {
            try {
                executor.removeNode(publisherNode);
            } catch (Exception e) {
                Log.e(TAG, "Error removing ButtonPublisherNode from executor", e);
            }
        }
        if (publisherNode != null) {
            publisherNode.cleanup();
            publisherNode = null;
        }

        if (buttonView != null) {
            buttonView.setOnClickListener(null);
        }
    }
}

