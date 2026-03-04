package com.example.ros2_android_test_app.ros2;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ImuSubscriberNode extends BaseComposableNode {
    private static final String TAG = "ImuSubscriberNode";
    private final String topic;
    private Subscription<sensor_msgs.msg.Imu> subscription;
    private final List<MessageCallback> callbacks = new ArrayList<>();

    public interface MessageCallback {
        void onNewMessage(String text);
    }

    public ImuSubscriberNode(String name, String topic) {
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
                    sensor_msgs.msg.Imu.class,
                    topic,
                    msg -> {
                        String text = String.format(
                                Locale.US,
                                "angVel[x=%.3f,y=%.3f,z=%.3f], linAcc[x=%.3f,y=%.3f,z=%.3f]",
                                msg.getAngularVelocity().getX(),
                                msg.getAngularVelocity().getY(),
                                msg.getAngularVelocity().getZ(),
                                msg.getLinearAcceleration().getX(),
                                msg.getLinearAcceleration().getY(),
                                msg.getLinearAcceleration().getZ()
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

