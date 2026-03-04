package com.example.ros2_android_test_app.ros2;

import android.util.Log;
import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;

public class StringSubscriberNode extends BaseComposableNode {
    private static final String TAG = "StringSubscriberNode";
    private final String topic;
    private Subscription<std_msgs.msg.String> subscription;
    private final List<MessageCallback> callbacks = new ArrayList<>();

    public interface MessageCallback {
        void onNewMessage(String text);
    }

    public StringSubscriberNode(String name, String topic) {
        super(name);
        this.topic = topic;
    }

    public void addCallback(MessageCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
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

    public String getTopic() {
        return topic;
    }

    public void start() {
        if (subscription == null) {
            subscription = node.createSubscription(
                std_msgs.msg.String.class,
                topic,
                msg -> {
                    synchronized (callbacks) {
                        for (MessageCallback cb : callbacks) {
                            if (cb != null) {
                                cb.onNewMessage(msg.getData());
                            }
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
