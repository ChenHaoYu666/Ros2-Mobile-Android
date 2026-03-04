package com.example.ros2_android_test_app.ros2;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.subscription.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OdometrySubscriberNode extends BaseComposableNode {
    private static final String TAG = "OdometrySubscriberNode";
    private final String topic;
    private Subscription<nav_msgs.msg.Odometry> subscription;
    private final List<MessageCallback> callbacks = new ArrayList<>();

    public interface MessageCallback {
        void onNewMessage(String text);
    }

    public OdometrySubscriberNode(String name, String topic) {
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
                    nav_msgs.msg.Odometry.class,
                    topic,
                    msg -> {
                        String text = String.format(
                                Locale.US,
                                "pos[x=%.3f,y=%.3f,z=%.3f], yaw? q[z=%.3f,w=%.3f], vel[vx=%.3f,wz=%.3f]",
                                msg.getPose().getPose().getPosition().getX(),
                                msg.getPose().getPose().getPosition().getY(),
                                msg.getPose().getPose().getPosition().getZ(),
                                msg.getPose().getPose().getOrientation().getZ(),
                                msg.getPose().getPose().getOrientation().getW(),
                                msg.getTwist().getTwist().getLinear().getX(),
                                msg.getTwist().getTwist().getAngular().getZ()
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

