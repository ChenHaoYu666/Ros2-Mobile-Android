package com.example.ros2_android_test_app.ros2;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.qos.QoSProfile;
import org.ros2.rcljava.qos.policies.Durability;
import org.ros2.rcljava.qos.policies.Reliability;
import org.ros2.rcljava.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;

public class PoseSubscriberNode extends BaseComposableNode {
    private static final String TAG = "PoseSubNode";

    public interface MessageCallback {
        void onNewMessage(geometry_msgs.msg.PoseWithCovarianceStamped msg);
    }

    private final String topic;
    private Subscription<geometry_msgs.msg.PoseWithCovarianceStamped> subscription;
    private final List<MessageCallback> callbacks = new ArrayList<>();

    public PoseSubscriberNode(String name, String topic) {
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

        QoSProfile qos = QoSProfile.keepLast(10)
                .setReliability(Reliability.RELIABLE)
                .setDurability(Durability.VOLATILE);

        subscription = node.createSubscription(
                geometry_msgs.msg.PoseWithCovarianceStamped.class,
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
        Log.i(TAG, "Started subscription on topic: " + topic + " with QoS RELIABLE+VOLATILE");
    }

    public void disposeNode() {
        try {
            if (subscription != null) node.dispose();
        } catch (Exception e) {
            Log.w(TAG, "dispose() failed", e);
        }
    }
}
