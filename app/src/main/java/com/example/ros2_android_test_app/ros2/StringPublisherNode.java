package com.example.ros2_android_test_app.ros2;

import android.util.Log;
import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.publisher.Publisher;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StringPublisherNode extends BaseComposableNode {
    private static final String TAG = "StringPublisherNode";
    private final String topic;
    private Publisher<std_msgs.msg.String> publisher;
    private ScheduledExecutorService scheduler;
    private String currentMessage = "";

    public StringPublisherNode(String name, String topic) {
        super(name);
        this.topic = topic;
    }

    public void start() {
        if (publisher == null) {
            publisher = node.createPublisher(std_msgs.msg.String.class, topic);
            Log.i(TAG, "Started String publisher on topic: " + topic);
        }
    }

    public void publishOnce(String message) {
        if (publisher != null) {
            std_msgs.msg.String msg = new std_msgs.msg.String();
            msg.setData(message);
            publisher.publish(msg);
            Log.d(TAG, "Published once to " + topic + ": " + message);
        }
    }

    public void startPeriodicPublish(String message, int hz) {
        stopPeriodicPublish();
        this.currentMessage = message;
        int periodMs = 1000 / Math.max(1, hz);
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (publisher != null) {
                    std_msgs.msg.String msg = new std_msgs.msg.String();
                    msg.setData(currentMessage);
                    publisher.publish(msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in periodic publish", e);
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
        Log.i(TAG, "Started periodic publish to " + topic + " at " + hz + "Hz");
    }

    public void updateMessage(String newMessage) {
        this.currentMessage = newMessage;
    }

    public void stopPeriodicPublish() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            Log.i(TAG, "Stopped periodic publish to " + topic);
        }
    }

    public void disposeNode() {
        stopPeriodicPublish();
        try {
            if (node != null) {
                node.dispose();
            }
        } catch (Exception e) {
            Log.w(TAG, "dispose() failed", e);
        }
    }
}
