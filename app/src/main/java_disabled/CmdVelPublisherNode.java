package com.example.ros2_android_test_app;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.publisher.Publisher;

import geometry_msgs.msg.Twist;

public class CmdVelPublisherNode extends BaseComposableNode {

    private static final String logtag = CmdVelPublisherNode.class.getName();

    private final String topic;

    private Publisher<Twist> publisher;

    public CmdVelPublisherNode(final String name, final String topic) {
        super(name);
        this.topic = topic;
        Log.i(logtag, "Creating CmdVelPublisherNode with name=" + name + ", topic=" + topic);
        // 延迟创建 publisher，直到第一次发布时
    }

    private void ensurePublisher() {
        if (this.publisher == null) {
            this.publisher = this.node.createPublisher(Twist.class, this.topic);
            Log.i(logtag, "CmdVel publisher created on topic=" + this.topic);
        }
    }

    public void publishCmd(double linearX, double angularZ) {
        try {
            ensurePublisher();
            Twist msg = new Twist();
            msg.getLinear().setX(linearX);
            msg.getLinear().setY(0.0);
            msg.getLinear().setZ(0.0);
            msg.getAngular().setX(0.0);
            msg.getAngular().setY(0.0);
            msg.getAngular().setZ(angularZ);
            if (this.publisher != null) {
                this.publisher.publish(msg);
                Log.d(logtag, "Published Twist: linear.x=" + linearX + ", angular.z=" + angularZ);
            }
        } catch (Exception e) {
            Log.e(logtag, "Error publishing cmd_vel", e);
        }
    }

    public void cleanup() {
        try {
            if (publisher != null) {
                Log.i(logtag, "Disposing cmd_vel publisher for topic=" + topic);
                publisher = null;
            }
        } catch (Exception e) {
            Log.e(logtag, "Error cleaning up CmdVelPublisherNode", e);
        }
    }

    public Node getNode() {
        return node;
    }
}

