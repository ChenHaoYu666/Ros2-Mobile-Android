package com.example.ros2_android_test_app.ros2;

import android.util.Log;
import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.publisher.Publisher;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Float32PublisherNode extends BaseComposableNode {
    private static final String TAG = "Float32PublisherNode";
    private final String topic;
    private Publisher<std_msgs.msg.Float32> publisher;
    private ScheduledExecutorService scheduler;
    private float currentValue = 0.0f;

    public Float32PublisherNode(String name, String topic) {
        super(name);
        this.topic = topic;
    }

    public void start() {
        if (publisher == null) {
            publisher = node.createPublisher(std_msgs.msg.Float32.class, topic);
            Log.i(TAG, "Started Float32 publisher on topic: " + topic);
        }
    }

    public void publishOnce(float value) {
        if (publisher != null) {
            std_msgs.msg.Float32 msg = new std_msgs.msg.Float32();
            msg.setData(value);
            publisher.publish(msg);
        }
    }

    public void startPeriodicPublish(float value, int hz) {
        stopPeriodicPublish();
        this.currentValue = value;
        int periodMs = 1000 / Math.max(1, hz);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (publisher != null) {
                    std_msgs.msg.Float32 msg = new std_msgs.msg.Float32();
                    msg.setData(currentValue);
                    publisher.publish(msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in periodic publish", e);
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    public void updateValue(float newValue) {
        this.currentValue = newValue;
    }

    public void stopPeriodicPublish() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void disposeNode() {
        stopPeriodicPublish();
        try {
            if (node != null) node.dispose();
        } catch (Exception e) {
            Log.w(TAG, "dispose() failed", e);
        }
    }
}
