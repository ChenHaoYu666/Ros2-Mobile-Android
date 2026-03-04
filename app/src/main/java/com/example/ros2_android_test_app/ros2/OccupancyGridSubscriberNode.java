package com.example.ros2_android_test_app.ros2;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.qos.QoSProfile;
import org.ros2.rcljava.qos.policies.Durability;
import org.ros2.rcljava.qos.policies.Reliability;
import org.ros2.rcljava.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;

public class OccupancyGridSubscriberNode extends BaseComposableNode {
    private static final String TAG = "OccupancyGridSubNode";

    public interface MessageCallback {
        void onNewMessage(nav_msgs.msg.OccupancyGrid msg);
    }

    private final String topic;
    private Subscription<nav_msgs.msg.OccupancyGrid> subscription;
    private final List<MessageCallback> callbacks = new ArrayList<>();
    private volatile nav_msgs.msg.OccupancyGrid lastMessage;

    public OccupancyGridSubscriberNode(String name, String topic) {
        super(name);
        this.topic = topic;
    }

    public void addCallback(MessageCallback callback) {
        nav_msgs.msg.OccupancyGrid snapshot = null;
        synchronized (callbacks) {
            if (!callbacks.contains(callback)) callbacks.add(callback);
            snapshot = lastMessage;
        }
        if (callback != null && snapshot != null) {
            callback.onNewMessage(snapshot);
        }
    }

    public void clearCallbacks() {
        synchronized (callbacks) {
            callbacks.clear();
        }
    }

    public void start() {
        if (subscription != null) return;

        QoSProfile qos = QoSProfile.keepLast(1)
                .setReliability(Reliability.RELIABLE)
                .setDurability(Durability.TRANSIENT_LOCAL);

        subscription = node.createSubscription(
                nav_msgs.msg.OccupancyGrid.class,
                topic,
                msg -> {
                    synchronized (callbacks) {
                        lastMessage = msg;
                        for (MessageCallback cb : callbacks) {
                            if (cb != null) cb.onNewMessage(msg);
                        }
                    }
                },
                qos
        );
        Log.i(TAG, "Started subscription on topic: " + topic + " with QoS RELIABLE+TRANSIENT_LOCAL");
    }

    public void disposeNode() {
        try {
            if (subscription != null) node.dispose();
        } catch (Exception e) {
            Log.w(TAG, "dispose() failed", e);
        }
    }
}
