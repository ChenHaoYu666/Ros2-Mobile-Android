package com.example.ros2_android_test_app;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.publisher.Publisher;

public class ButtonPublisherNode extends BaseComposableNode {

    private static final String logtag = ButtonPublisherNode.class.getName();

    private final String topic;
    private Publisher<std_msgs.msg.String> publisher;

    public ButtonPublisherNode(final String name, final String topic) {
        super(name);
        this.topic = topic;
        Log.i(logtag, "Creating ButtonPublisherNode with name=" + name + ", topic=" + topic);
    }

    private void ensurePublisher() {
        if (publisher == null) {
            publisher = this.node.createPublisher(std_msgs.msg.String.class, topic);
            Log.i(logtag, "Button publisher created on topic=" + topic);
        }
    }

    public void publishOnce(String text) {
        try {
            ensurePublisher();
            std_msgs.msg.String msg = new std_msgs.msg.String();
            msg.setData(text);
            if (publisher != null) {
                publisher.publish(msg);
                Log.d(logtag, "Published String: '" + text + "' to topic=" + topic);
            }
        } catch (Exception e) {
            Log.e(logtag, "Error publishing button message", e);
        }
    }

    public void cleanup() {
        try {
            if (publisher != null) {
                Log.i(logtag, "Disposing button publisher for topic=" + topic);
                publisher = null;
            }
        } catch (Exception e) {
            Log.e(logtag, "Error cleaning up ButtonPublisherNode", e);
        }
    }
}

