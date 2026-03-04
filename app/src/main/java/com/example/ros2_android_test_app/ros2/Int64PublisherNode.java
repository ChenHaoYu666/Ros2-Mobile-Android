package com.example.ros2_android_test_app.ros2;

import android.util.Log;
import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.publisher.Publisher;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Int64PublisherNode extends BaseComposableNode {
    private static final String TAG = "Int64PublisherNode";
    private final String topic;
    private Publisher<std_msgs.msg.Int64> publisher;
    private ScheduledExecutorService scheduler;
    private long currentValue = 0L;

    public Int64PublisherNode(String name, String topic) {
        super(name);
        this.topic = topic;
    }

    public void start() {
        if (publisher == null) {
            publisher = node.createPublisher(std_msgs.msg.Int64.class, topic);
            Log.i(TAG, "Started Int64 publisher on topic: " + topic);
        }
    }

    public void publishOnce(long value) {
        if (publisher != null) {
            std_msgs.msg.Int64 msg = new std_msgs.msg.Int64();
            msg.setData(value);
            publisher.publish(msg);
        }
    }

    public void startPeriodicPublish(long value, int hz) {
        stopPeriodicPublish();
        this.currentValue = value;
        int periodMs = 1000 / Math.max(1, hz);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (publisher != null) {
                    std_msgs.msg.Int64 msg = new std_msgs.msg.Int64();
                    msg.setData(currentValue);
                    publisher.publish(msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in periodic publish", e);
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    public void updateValue(long newValue) {
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
