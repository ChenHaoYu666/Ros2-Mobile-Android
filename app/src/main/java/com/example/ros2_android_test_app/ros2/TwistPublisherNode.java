package com.example.ros2_android_test_app.ros2;

import android.util.Log;
import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.publisher.Publisher;
import com.example.ros2_android_test_app.model.config.WidgetConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TwistPublisherNode extends BaseComposableNode {
    private static final String TAG = "TwistPublisherNode";
    private static final float INPUT_EPS = 1e-4f;
    private static final int STOP_BURST_COUNT = 5;

    private final WidgetConfig config;
    private Publisher<geometry_msgs.msg.Twist> publisher;
    private ScheduledExecutorService scheduler;

    private float rawX = 0.0f;
    private float rawY = 0.0f;

    private float lastSentRawX = Float.NaN;
    private float lastSentRawY = Float.NaN;

    private int pendingStopBursts = 0;

    private long lastLogTime = 0;
    private int msgCount = 0;

    public TwistPublisherNode(String name, WidgetConfig config) {
        super(name);
        this.config = config;
    }

    public void updateRawValues(float linearPercent, float angularPercent) {
        this.rawY = linearPercent;
        this.rawX = angularPercent;

        // 从非零进入零：触发停止 burst（只发有限次 0 速度）
        boolean wasNonZero = !isZero(lastSentRawX) || !isZero(lastSentRawY);
        boolean nowZero = isZero(this.rawX) && isZero(this.rawY);
        if (wasNonZero && nowZero) {
            pendingStopBursts = STOP_BURST_COUNT;
        }
    }

    private boolean isZero(float v) {
        return Float.isNaN(v) || Math.abs(v) < INPUT_EPS;
    }

    public void start() {
        if (publisher == null) {
            publisher = node.createPublisher(geometry_msgs.msg.Twist.class, config.topic);
        }
        stop(); // 确保之前的定时器已关闭
        
        int hz = Math.max(1, config.rateHz);
        long periodMs = 1000 / hz;
        
        Log.i(TAG, "Starting Java native timer: topic=" + config.topic + ", hz=" + hz + ", periodMs=" + periodMs);
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::onTimerTick, 0, periodMs, TimeUnit.MILLISECONDS);
        
        lastLogTime = System.currentTimeMillis();
        msgCount = 0;
    }

    private void onTimerTick() {
        try {
            // 发送策略：
            // - 有输入变化时发送
            // - 松手回到 0 时，仅发送 STOP_BURST_COUNT 次 0 速度
            // - 静止且无 pending stop 时不发送

            boolean nowZero = isZero(rawX) && isZero(rawY);
            boolean inputChanged = Float.isNaN(lastSentRawX) || Float.isNaN(lastSentRawY)
                    || Math.abs(rawX - lastSentRawX) >= INPUT_EPS
                    || Math.abs(rawY - lastSentRawY) >= INPUT_EPS;

            boolean shouldSend;
            if (!nowZero) {
                // 只要摇杆未回中，就持续按频率发布，保证机器人持续运动
                shouldSend = true;
            } else {
                if (pendingStopBursts > 0) {
                    shouldSend = true;
                    pendingStopBursts--;
                } else {
                    shouldSend = false;
                }
            }

            if (!shouldSend) {
                return;
            }

            long now = System.currentTimeMillis();
            msgCount++;
            if (now - lastLogTime >= 1000) {
                Log.d(TAG, "Native Java frequency for " + config.topic + ": " + msgCount + " Hz");
                msgCount = 0;
                lastLogTime = now;
            }

            geometry_msgs.msg.Twist msg = new geometry_msgs.msg.Twist();
            float linearValue = (rawY > 0) ? rawY * config.yScaleLeft : Math.abs(rawY) * config.yScaleRight;
            float angularValue = (rawX > 0) ? rawX * config.xScaleLeft : Math.abs(rawX) * config.xScaleRight;

            msg.getLinear().setX(linearValue);
            msg.getAngular().setZ(angularValue);

            if (publisher != null) {
                publisher.publish(msg);
            }

            lastSentRawX = rawX;
            lastSentRawY = rawY;
        } catch (Exception e) {
            Log.e(TAG, "Error in onTimerTick", e);
        }
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void disposeNode() {
        stop();
        try {
            node.dispose();
        } catch (Exception e) {
            Log.w(TAG, "dispose() failed", e);
        }
    }
}
