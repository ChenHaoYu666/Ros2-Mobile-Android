package com.example.ros2_android_test_app.ros2;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TwistSubscriberNode extends BaseComposableNode {
    private static final String TAG = "TwistSubscriberNode";
    private final String topic;
    private Subscription<geometry_msgs.msg.Twist> subscription;
    private final List<MessageCallback> callbacks = new ArrayList<>();

    public interface MessageCallback {
        void onNewMessage(String text);
    }

    public TwistSubscriberNode(String name, String topic) {
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

    public void clearCallbacks() {
        synchronized (callbacks) {
            callbacks.clear();
        }
    }

    public void start() {
        if (subscription == null) {
            subscription = node.createSubscription(
                    geometry_msgs.msg.Twist.class,
                    topic,
                    msg -> {
                        String text = String.format(
                                Locale.US,
                                "linear[x=%.3f,y=%.3f,z=%.3f], angular[x=%.3f,y=%.3f,z=%.3f]",
                                msg.getLinear().getX(), msg.getLinear().getY(), msg.getLinear().getZ(),
                                msg.getAngular().getX(), msg.getAngular().getY(), msg.getAngular().getZ()
                        );
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

