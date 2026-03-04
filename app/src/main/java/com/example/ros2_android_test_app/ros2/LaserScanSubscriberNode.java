package com.example.ros2_android_test_app.ros2;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.qos.QoSProfile;
import org.ros2.rcljava.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;

public class LaserScanSubscriberNode extends BaseComposableNode {
    private static final String TAG = "LaserScanSubNode";

    public interface MessageCallback {
        void onNewMessage(sensor_msgs.msg.LaserScan msg);
    }

    private final String topic;
    private Subscription<sensor_msgs.msg.LaserScan> subscription;
    private final List<MessageCallback> callbacks = new ArrayList<>();

    public LaserScanSubscriberNode(String name, String topic) {
        super(name);
        this.topic = topic;
    }

    public void addCallback(MessageCallback callback) {
        synchronized (callbacks) {
            if (!callbacks.contains(callback)) callbacks.add(callback);
        }
    }

    public void clearCallbacks() {
        synchronized (callbacks) {
            callbacks.clear();
        }
    }

    public void start() {
        if (subscription != null) return;

        QoSProfile qos = QoSProfile.sensorData(); // BEST_EFFORT + VOLATILE

        subscription = node.createSubscription(
                sensor_msgs.msg.LaserScan.class,
                topic,
                msg -> {
                    synchronized (callbacks) {
                        for (MessageCallback cb : callbacks) {
                            if (cb != null) cb.onNewMessage(msg);
                        }
                    }
                },
                qos
        );
        Log.i(TAG, "Started subscription on topic: " + topic + " with QoS SENSOR_DATA");
    }

    public void disposeNode() {
        try {
            if (subscription != null) node.dispose();
        } catch (Exception e) {
            Log.w(TAG, "dispose() failed", e);
        }
    }
}
