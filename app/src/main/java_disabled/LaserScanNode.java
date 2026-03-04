package com.example.ros2_android_test_app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.subscription.Subscription;

import sensor_msgs.msg.LaserScan;

public class LaserScanNode extends BaseComposableNode {
    public interface LaserScanListener {
        void onLaserScan(float[] ranges, float angleMin, float angleIncrement);
    }

    private static final String TAG = "LaserScanNode";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final String topicName;
    private final LaserScanListener listener;

    private Subscription<LaserScan> subscription;

    public LaserScanNode(String nodeName, String topicName, LaserScanListener listener) {
        super(nodeName);
        this.topicName = topicName;
        this.listener = listener;
        Log.i(TAG, "Creating LaserScanNode with name=" + nodeName + ", topic=" + topicName);
        // 延迟创建 subscription，和 ListenerNode/TalkerNode 风格一致
        start();
    }

    public void start() {
        Log.d(TAG, "LaserScanNode::start()");
        if (this.subscription == null) {
            try {
                this.subscription = this.node.<LaserScan>createSubscription(
                        LaserScan.class,
                        this.topicName,
                        msg -> {
                            if (listener == null || msg == null) {
                                return;
                            }
                            int size = msg.getRanges().size();
                            final float[] rangesCopy = new float[size];
                            for (int i = 0; i < size; i++) {
                                rangesCopy[i] = msg.getRanges().get(i);
                            }
                            final float angleMin = msg.getAngleMin();
                            final float angleIncrement = msg.getAngleIncrement();

                            mainHandler.post(() -> listener.onLaserScan(rangesCopy, angleMin, angleIncrement));
                        });
                Log.i(TAG, "Subscription created on topic=" + this.topicName);
            } catch (Exception e) {
                Log.e(TAG, "Failed to create LaserScan subscription", e);
            }
        }
    }

    public void stop() {
        Log.d(TAG, "LaserScanNode::stop()");
        // 暂不显式取消 subscription，保持与 ListenerNode 一致的简单清理策略
    }

    public void cleanup() {
        try {
            if (subscription != null) {
                Log.i(TAG, "Disposing subscription for topic=" + topicName);
                subscription = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up LaserScanNode", e);
        }
    }

    public Node getNode() {
        return node;
    }
}
