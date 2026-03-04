package com.example.ros2_android_test_app;

import java.util.concurrent.TimeUnit;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.publisher.Publisher;
import org.ros2.rcljava.timer.WallTimer;

public class TalkerNode extends BaseComposableNode {

    private static String logtag = TalkerNode.class.getName();

    private final String topic;

    public Publisher<std_msgs.msg.String> publisher;

    private int count;

    private WallTimer timer;

    public TalkerNode(final String name, final String topic) {
        super(name);
        this.topic = topic;
        Log.i(logtag, "Creating TalkerNode with name=" + name + ", topic=" + topic);
        // 延迟创建publisher，直到start()被调用
    }

    public void start() {
        Log.d(logtag, "TalkerNode::start()");
        if (this.timer != null) {
            this.timer.cancel();
        }
        // 只有在start时才创建publisher
        if (this.publisher == null) {
            this.publisher = this.node.<std_msgs.msg.String>createPublisher(
                    std_msgs.msg.String.class, this.topic);
            Log.i(logtag, "Publisher created on topic=" + this.topic);
        }
        this.count = 0;
        this.timer = node.createWallTimer(500, TimeUnit.MILLISECONDS, this ::onTimer);
        Log.i(logtag, "TalkerNode timer started with period=500ms");
    }

    private void onTimer() {
        std_msgs.msg.String msg = new std_msgs.msg.String();
        msg.setData("Hello!! my ROS2! galactic! " + this.count);
        this.count++;
        Log.d(logtag, "Publishing message: " + msg.getData());
        try {
            if (this.publisher != null) {
                this.publisher.publish(msg);
            }
        } catch (Exception e) {
            Log.e(logtag, "Error publishing message", e);
        }
    }

    public void stop() {
        Log.d(logtag, "TalkerNode::stop()");
        if (this.timer != null) {
            this.timer.cancel();
            Log.i(logtag, "TalkerNode timer cancelled");
        }
    }

    public void cleanup() {
        try {
            stop();
            if (publisher != null) {
                Log.i(logtag, "Disposing publisher for topic=" + topic);
                publisher = null;
            }
        } catch (Exception e) {
            Log.e(logtag, "Error cleaning up TalkerNode", e);
        }
    }

    public Node getNode() {
        return node;
    }
}
