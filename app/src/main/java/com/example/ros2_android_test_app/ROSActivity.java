package com.example.ros2_android_test_app;

/* Copyright 2017 Esteve Fernandez <esteve@apache.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ros2_android_test_app.ros2.BoolPublisherNode;
import com.example.ros2_android_test_app.ros2.Float32PublisherNode;
import com.example.ros2_android_test_app.ros2.Float32SubscriberNode;
import com.example.ros2_android_test_app.ros2.Float64PublisherNode;
import com.example.ros2_android_test_app.ros2.Float64SubscriberNode;
import com.example.ros2_android_test_app.ros2.Int32PublisherNode;
import com.example.ros2_android_test_app.ros2.Int32SubscriberNode;
import com.example.ros2_android_test_app.ros2.Int64PublisherNode;
import com.example.ros2_android_test_app.ros2.Int64SubscriberNode;
import com.example.ros2_android_test_app.ros2.LaserScanSubscriberNode;
import com.example.ros2_android_test_app.ros2.OccupancyGridSubscriberNode;
import com.example.ros2_android_test_app.ros2.PoseSubscriberNode;
import com.example.ros2_android_test_app.ros2.StringPublisherNode;
import com.example.ros2_android_test_app.ros2.StringSubscriberNode;
import com.example.ros2_android_test_app.ros2.TwistPublisherNode;

import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.executors.Executor;
import org.ros2.rcljava.executors.SingleThreadedExecutor;
import org.ros2.rcljava.graph.NameAndTypes;
import org.ros2.rcljava.node.BaseComposableNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ROSActivity extends AppCompatActivity {
    private Executor rosExecutor;
    private Thread spinThread;
    private Handler handler;
    private WifiManager.MulticastLock multicastLock;
    private GraphNode graphNode;
    private final Map<String, BaseComposableNode> persistentNodes = new HashMap<>();
    
    // 线程安全锁：保护 rosExecutor 的所有操作（addNode, removeNode, spin）
    private final Object executorLock = new Object();
    
    // Master 多话题监视状态
    private final List<String> activeMonitorTopics = new ArrayList<>();
    private final Map<String, java.util.ArrayDeque<String>> multiTopicHistoryMap = new HashMap<>();
    private final Map<String, String> multiTopicTypeMap = new HashMap<>();
    
    private List<NameAndTypes> lastMasterTopicList = null;
    private final Map<String, java.util.ArrayDeque<String>> labelHistoryMap = new HashMap<>();

    private static String logtag = ROSActivity.class.getName();
    private volatile int currentEffectiveDomainId = 0;

    public int getCurrentEffectiveDomainId() {
        return currentEffectiveDomainId;
    }

    public void setLastMasterTopicList(Collection<NameAndTypes> list) {
        this.lastMasterTopicList = (list == null) ? null : new ArrayList<>(list);
    }

    public List<NameAndTypes> getLastMasterTopicList() {
        return this.lastMasterTopicList;
    }

    // --- Master 多话题监视 API ---

    public List<String> getActiveMonitorTopics() {
        synchronized (activeMonitorTopics) {
            return new ArrayList<>(activeMonitorTopics);
        }
    }

    public void addMasterMonitorTopic(String topic, String type) {
        synchronized (activeMonitorTopics) {
            if (!activeMonitorTopics.contains(topic)) {
                activeMonitorTopics.add(topic);
                multiTopicTypeMap.put(topic, type);
                if (!multiTopicHistoryMap.containsKey(topic)) {
                    multiTopicHistoryMap.put(topic, new java.util.ArrayDeque<>());
                }
            }
        }
    }

    public void removeMasterMonitorTopic(String topic) {
        synchronized (activeMonitorTopics) {
            activeMonitorTopics.remove(topic);
            multiTopicTypeMap.remove(topic);
            multiTopicHistoryMap.remove(topic);
        }
        removePersistentNode("master_sub_" + topic);
    }

    public void addMasterMonitorMessage(String topic, String data) {
        synchronized (activeMonitorTopics) {
            java.util.ArrayDeque<String> history = multiTopicHistoryMap.get(topic);
            if (history != null && data != null) {
                history.addLast(data);
                while (history.size() > 5) {
                    history.removeFirst();
                }
            }
        }
    }

    public String getMasterMonitorData(String topic) {
        synchronized (activeMonitorTopics) {
            java.util.ArrayDeque<String> history = multiTopicHistoryMap.get(topic);
            if (history == null || history.isEmpty()) return "等待消息...";
            StringBuilder sb = new StringBuilder();
            for (String s : history) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(s);
            }
            return sb.toString();
        }
    }

    // --- Label 历史 API ---

    public void addLabelHistory(String widgetId, String data) {
        if (widgetId == null || data == null) return;
        synchronized (labelHistoryMap) {
            java.util.ArrayDeque<String> history = labelHistoryMap.get(widgetId);
            if (history == null) {
                history = new java.util.ArrayDeque<>();
                labelHistoryMap.put(widgetId, history);
            }
            history.addLast(data);
            while (history.size() > 5) {
                history.removeFirst();
            }
        }
    }

    public String getLabelHistoryText(String widgetId) {
        if (widgetId == null) return null;
        synchronized (labelHistoryMap) {
            java.util.ArrayDeque<String> history = labelHistoryMap.get(widgetId);
            if (history == null || history.isEmpty()) return null;
            StringBuilder sb = new StringBuilder();
            for (String s : history) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(s);
            }
            return sb.toString();
        }
    }

    // --- 节点管理 (线程安全加固) ---

    public void removePersistentNode(String id) {
        synchronized (executorLock) {
            BaseComposableNode node = persistentNodes.remove(id);
            if (node != null && rosExecutor != null) {
                try {
                    rosExecutor.removeNode(node);
                    disposeNodeExplicitly(node);
                } catch (Exception e) {
                    Log.w(logtag, "Error removing persistent node: " + id, e);
                }
            }
        }
    }

    public void addPersistentNode(String id, BaseComposableNode node) {
        synchronized (executorLock) {
            if (!persistentNodes.containsKey(id)) {
                persistentNodes.put(id, node);
                if (rosExecutor != null) {
                    rosExecutor.addNode(node);
                }
            }
        }
    }

    public BaseComposableNode getPersistentNode(String id) {
        synchronized (executorLock) {
            return persistentNodes.get(id);
        }
    }

    public void clearPersistentNodes() {
        synchronized (executorLock) {
            if (rosExecutor != null) {
                for (BaseComposableNode node : persistentNodes.values()) {
                    try {
                        rosExecutor.removeNode(node);
                        disposeNodeExplicitly(node);
                    } catch (Exception e) {
                        Log.w(logtag, "Error clearing persistent node", e);
                    }
                }
            }
            persistentNodes.clear();
            synchronized (labelHistoryMap) { labelHistoryMap.clear(); }
            synchronized (activeMonitorTopics) {
                multiTopicHistoryMap.clear();
                multiTopicTypeMap.clear();
                activeMonitorTopics.clear();
            }
        }
    }

    private void disposeNodeExplicitly(BaseComposableNode node) {
        if (node instanceof TwistPublisherNode) ((TwistPublisherNode) node).disposeNode();
        else if (node instanceof BoolPublisherNode) ((BoolPublisherNode) node).disposeNode();
        else if (node instanceof StringSubscriberNode) ((StringSubscriberNode) node).disposeNode();
        else if (node instanceof StringPublisherNode) ((StringPublisherNode) node).disposeNode();
        else if (node instanceof Int32SubscriberNode) ((Int32SubscriberNode) node).disposeNode();
        else if (node instanceof Int64SubscriberNode) ((Int64SubscriberNode) node).disposeNode();
        else if (node instanceof Float32SubscriberNode) ((Float32SubscriberNode) node).disposeNode();
        else if (node instanceof Float64SubscriberNode) ((Float64SubscriberNode) node).disposeNode();
        else if (node instanceof Int32PublisherNode) ((Int32PublisherNode) node).disposeNode();
        else if (node instanceof Int64PublisherNode) ((Int64PublisherNode) node).disposeNode();
        else if (node instanceof Float32PublisherNode) ((Float32PublisherNode) node).disposeNode();
        else if (node instanceof Float64PublisherNode) ((Float64PublisherNode) node).disposeNode();
        else if (node instanceof OccupancyGridSubscriberNode) ((OccupancyGridSubscriberNode) node).disposeNode();
        else if (node instanceof LaserScanSubscriberNode) ((LaserScanSubscriberNode) node).disposeNode();
        else if (node instanceof PoseSubscriberNode) ((PoseSubscriberNode) node).disposeNode();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.handler = new Handler(getMainLooper());

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("ros2_multicast_lock");
            multicastLock.setReferenceCounted(true);
            acquireMulticastLock();
        }

        initializeRosRuntime();
    }

    private void initializeRosRuntime() {
        int domainIdInt = Ros2ConfigManager.getDomainId(this);
        currentEffectiveDomainId = domainIdInt;
        applyDomainIdEnv(domainIdInt);

        try {
            RCLJava.rclJavaInit();
        } catch (Exception e) {
            Log.e(logtag, "RCLJava init failed", e);
        }

        synchronized (executorLock) {
        this.rosExecutor = new SingleThreadedExecutor();
        this.graphNode = new GraphNode("android_global_graph_node");
        this.rosExecutor.addNode(this.graphNode);
        }

        startRosSpin();
        Log.i(logtag, "ROS runtime initialized with domain id=" + domainIdInt);
    }

    private void shutdownRosRuntime() {
        clearPersistentNodes();

        if (spinThread != null) {
            spinThread.interrupt();
            spinThread = null;
        }

        synchronized (executorLock) {
            if (rosExecutor != null) {
                try {
                    if (graphNode != null) {
                        rosExecutor.removeNode(graphNode);
                    }
                } catch (Exception ignored) {
                }
                graphNode = null;
                rosExecutor = null;
            }
        }

        try {
            RCLJava.shutdown();
        } catch (Exception e) {
            Log.w(logtag, "RCLJava shutdown warning", e);
        }
    }

    public void restartRosRuntime() {
        shutdownRosRuntime();
        initializeRosRuntime();
    }

    private void applyDomainIdEnv(int domainIdInt) {
        String value = String.valueOf(domainIdInt);
        System.setProperty("ROS_DOMAIN_ID", value);
        try {
            Os.setenv("ROS_DOMAIN_ID", value, true);
            Log.i(logtag, "Set ROS_DOMAIN_ID via setenv: " + value);
        } catch (ErrnoException e) {
            Log.w(logtag, "setenv ROS_DOMAIN_ID failed, fallback to System property only", e);
        }
    }

    private void startRosSpin() {
        synchronized (executorLock) {
            if (spinThread != null && spinThread.isAlive()) return;
            
            spinThread = new Thread(() -> {
                Log.i(logtag, "Dedicated ROS spin thread started");
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        synchronized (executorLock) {
                            if (rosExecutor != null) {
                                rosExecutor.spinOnce(100);
                            } else {
                                break;
                            }
                        }
                        Thread.sleep(10); // 避免 CPU 空转
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.e(logtag, "ROS spin error", e);
                    }
                }
                Log.i(logtag, "Dedicated ROS spin thread stopped");
            }, "ROS2-Spin-Thread");
            spinThread.start();
        }
    }

    public Collection<NameAndTypes> getTopicNamesAndTypes() {
        if (graphNode != null && graphNode.getNode() != null) {
            return graphNode.getNode().getTopicNamesAndTypes();
        }
        return Collections.emptyList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        acquireMulticastLock();
        startRosSpin();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (multicastLock != null && multicastLock.isHeld()) {
            try {
                multicastLock.release();
            } catch (Exception ignored) {
            }
        }
    }

    private void acquireMulticastLock() {
        if (multicastLock != null && !multicastLock.isHeld()) {
            try {
                multicastLock.acquire();
            } catch (Exception e) {
                Log.e(logtag, "MulticastLock fail", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutdownRosRuntime();
    }

    public Executor getExecutor() {
        return this.rosExecutor;
    }
}
