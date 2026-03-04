package com.example.ros2_android_test_app.ros2;

import android.util.Log;
import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.publisher.Publisher;

public class BoolPublisherNode extends BaseComposableNode {
    private static final String TAG = "BoolPublisherNode";
    private final String topic;
    private Publisher<std_msgs.msg.Bool> publisher;

    public BoolPublisherNode(String name, String topic) {
        super(name);
        this.topic = topic;
    }

    public void start() {
        if (publisher == null) {
            publisher = node.createPublisher(std_msgs.msg.Bool.class, topic);
            Log.i(TAG, "Started publisher on topic: " + topic);
        }
    }

    public void publishValue(boolean value) {
        if (publisher != null) {
            std_msgs.msg.Bool msg = new std_msgs.msg.Bool();
            msg.setData(value);
            publisher.publish(msg);
        }
    }

    public void disposeNode() {
        try {
            if (publisher != null) {
                node.dispose();
            }
        } catch (Exception e) {
            Log.w(TAG, "dispose() failed", e);
        }
    }
}
