package com.example.ros2_android_test_app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.subscription.Subscription;

public class ListenerNode extends BaseComposableNode {
    private static String logtag = ListenerNode.class.getName();

    private final String topic;

    private final TextView listenerView;
    private final Handler handler;

    private Subscription<std_msgs.msg.String> subscriber;

    public ListenerNode(final String name, final String topic,
                        final TextView listenerView) {
        super(name);
        this.topic = topic;
        this.listenerView = listenerView;
        this.handler = new Handler(Looper.getMainLooper());
        Log.i(logtag, "Creating ListenerNode with name=" + name + ", topic=" + topic);
        // 延迟创建subscriber，直到start()被调用
    }

    public void start() {
        Log.d(logtag, "ListenerNode::start()");
        // 只有在start时才创建subscriber
        if (this.subscriber == null) {
            this.subscriber = this.node.<std_msgs.msg.String>createSubscription(
                    std_msgs.msg.String.class, this.topic, msg
                            -> {
                        String data = msg.getData();
                        Log.d(logtag, "Received message on topic " + this.topic + ": " + data);
                        // 使用Handler在主线程中更新UI
                        handler.post(() -> {
                            if (listenerView != null) {
                                listenerView.setText("Hello ROS2 from Android: " + data +
                                        "\r\n" + listenerView.getText());
                            }
                        });

                    });
            Log.i(logtag, "Subscription created on topic=" + this.topic);
        }
    }

    public void stop() {
        Log.d(logtag, "ListenerNode::stop()");
        // 可以在这里添加停止逻辑
    }

    public void cleanup() {
        try {
            if (subscriber != null) {
                Log.i(logtag, "Disposing subscription for topic=" + topic);
                subscriber = null;
            }
        } catch (Exception e) {
            Log.e(logtag, "Error cleaning up ListenerNode", e);
        }
    }
}
