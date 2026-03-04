package com.example.ros2_android_test_app.ros2;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;

public class Int32SubscriberNode extends BaseComposableNode {
    private static final String TAG = "Int32SubscriberNode";
    private final String topic;
    private Subscription<std_msgs.msg.Int32> subscription;
    private final List<MessageCallback> callbacks = new ArrayList<>();

    public interface MessageCallback {
        void onNewMessage(String text);
    }

    public Int32SubscriberNode(String name, String topic) {
        super(name);
        this.topic = topic;
    }

    public void addCallback(MessageCallback callback) {
        synchronized (callbacks) {
            if (!callbacks.contains(callback)) {
                callbacks.add(callback);
            }
        }
    }

    public void removeCallback(MessageCallback callback) {
        synchronized (callbacks) {
            callbacks.remove(callback);
        }
    }

    public void clearCallbacks() {
        synchronized (callbacks) {
            callbacks.clear();
        }
    }

    public void start() {
        if (subscription == null) {
            subscription = node.createSubscription(
                    std_msgs.msg.Int32.class,
                    topic,
                    msg -> {
                        String text = String.valueOf(msg.getData());
                        synchronized (callbacks) {
                            for (MessageCallback cb : callbacks) {
                                if (cb != null) cb.onNewMessage(text);
                            }
                        }
                    }
            );
            Log.i(TAG, "Started subscription on topic: " + topic);
        }
    }

    public void disposeNode() {
        try {
            if (subscription != null) {
                node.dispose();
            }
        } catch (Exception e) {
            Log.w(TAG, "dispose() failed", e);
        }
    }
}
