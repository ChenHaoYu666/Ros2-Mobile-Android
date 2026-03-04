package com.example.ros2_android_test_app.ui.fragments.master;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ros2_android_test_app.GraphNode;
import com.example.ros2_android_test_app.R;
import com.example.ros2_android_test_app.ROSActivity;
import com.example.ros2_android_test_app.Ros2ConfigManager;
import com.example.ros2_android_test_app.ros2.Float32PublisherNode;
import com.example.ros2_android_test_app.ros2.Float32SubscriberNode;
import com.example.ros2_android_test_app.ros2.Float64PublisherNode;
import com.example.ros2_android_test_app.ros2.Float64SubscriberNode;
import com.example.ros2_android_test_app.ros2.Int32PublisherNode;
import com.example.ros2_android_test_app.ros2.Int32SubscriberNode;
import com.example.ros2_android_test_app.ros2.ImuSubscriberNode;
import com.example.ros2_android_test_app.ros2.Int64PublisherNode;
import com.example.ros2_android_test_app.ros2.Int64SubscriberNode;
import com.example.ros2_android_test_app.ros2.OdometrySubscriberNode;
import com.example.ros2_android_test_app.ros2.StringPublisherNode;
import com.example.ros2_android_test_app.ros2.StringSubscriberNode;
import com.example.ros2_android_test_app.ros2.TwistSubscriberNode;
import com.google.android.material.textfield.TextInputEditText;

import org.ros2.rcljava.graph.NameAndTypes;
import org.ros2.rcljava.node.BaseComposableNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MasterFragment extends Fragment {

    private static final String TAG = "MasterFragment";
    private TextInputEditText domainIdEditText;
    private Button saveButton;
    private Button topicListButton;
    private TextView topicListDisplay;

    private RecyclerView topicRecyclerView;
    private TopicAdapter topicAdapter;

    private RecyclerView monitorRecyclerView;
    private MonitorAdapter monitorAdapter;

    private GraphNode graphNode = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_master, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        domainIdEditText = view.findViewById(R.id.domain_id_editText);
        saveButton = view.findViewById(R.id.save_domain_button);
        topicListButton = view.findViewById(R.id.topic_list_button);
        topicListDisplay = view.findViewById(R.id.topic_list_display);

        monitorRecyclerView = view.findViewById(R.id.monitor_recycler_view);
        monitorRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        monitorAdapter = new MonitorAdapter();
        monitorRecyclerView.setAdapter(monitorAdapter);

        topicRecyclerView = view.findViewById(R.id.topic_recycler_view);
        topicRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        topicAdapter = new TopicAdapter();
        topicRecyclerView.setAdapter(topicAdapter);

        initGraphNode();

        if (getActivity() instanceof ROSActivity) {
            ROSActivity activity = (ROSActivity) getActivity();
            int currentDomainId = Ros2ConfigManager.getDomainId(getContext());
            domainIdEditText.setText(String.valueOf(currentDomainId));

            // 1. 恢复话题列表数据
            List<NameAndTypes> cachedTopics = activity.getLastMasterTopicList();
            if (cachedTopics != null && !cachedTopics.isEmpty()) {
                topicListDisplay.setVisibility(View.GONE);
                topicRecyclerView.setVisibility(View.VISIBLE);
                topicAdapter.setItems(cachedTopics);
            }

            // 2. 恢复 monitor 列表并重绑回调
            List<String> activeTopics = activity.getActiveMonitorTopics();
            if (activeTopics != null && !activeTopics.isEmpty()) {
                monitorAdapter.setTopics(activeTopics);
                for (String topic : activeTopics) {
                    rebindSubscription(topic);
                }
            }
        }

        saveButton.setOnClickListener(v -> {
            String domainIdStr = domainIdEditText.getText() == null ? "" : domainIdEditText.getText().toString();
            try {
                int newDomainId = Integer.parseInt(domainIdStr);
                Ros2ConfigManager.setDomainId(getContext(), newDomainId);
                Toast.makeText(getContext(), "Domain ID 已保存，正在重启...", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "请输入有效的数字", Toast.LENGTH_SHORT).show();
            }
        });

        topicListButton.setOnClickListener(v -> fetchTopicList());
    }

    private void initGraphNode() {
        if (graphNode == null) {
            graphNode = new GraphNode("android_graph_node");
            if (getActivity() instanceof ROSActivity) {
                ROSActivity rosActivity = (ROSActivity) getActivity();
                if (rosActivity.getExecutor() != null) {
                    rosActivity.getExecutor().addNode(graphNode);
                    Log.i(TAG, "GraphNode added to executor");
                }
            }
        }
    }

    private void fetchTopicList() {
        if (topicListDisplay.getVisibility() == View.VISIBLE) {
            topicListDisplay.setText("正在获取话题列表...");
        }

        try {
            if (graphNode == null || graphNode.getNode() == null) {
                Toast.makeText(getContext(), "错误: 节点未初始化", Toast.LENGTH_SHORT).show();
                return;
            }

            Collection<NameAndTypes> topics = graphNode.getNode().getTopicNamesAndTypes();
            if (topics == null || topics.isEmpty()) {
                topicListDisplay.setVisibility(View.VISIBLE);
                topicRecyclerView.setVisibility(View.GONE);
                topicListDisplay.setText("未发现任何话题。\n提示：如果是刚启动，请等待几秒。");
                if (getActivity() instanceof ROSActivity) {
                    ((ROSActivity) getActivity()).setLastMasterTopicList(null);
                }
            } else {
                topicListDisplay.setVisibility(View.GONE);
                topicRecyclerView.setVisibility(View.VISIBLE);
                List<NameAndTypes> list = new ArrayList<>(topics);
                topicAdapter.setItems(list);
                if (getActivity() instanceof ROSActivity) {
                    ((ROSActivity) getActivity()).setLastMasterTopicList(list);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "获取话题列表失败", e);
            Toast.makeText(getContext(), "获取失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startMonitoring(String topic, String type) {
        if (!(getActivity() instanceof ROSActivity)) return;
        ROSActivity activity = (ROSActivity) getActivity();

        activity.addMasterMonitorTopic(topic, type);
        monitorAdapter.setTopics(activity.getActiveMonitorTopics());

        String subId = "master_sub_" + topic;
        BaseComposableNode existing = activity.getPersistentNode(subId);

        // 关键修复：每次点“订阅”都强制移除旧节点并重建，避免 DDS / rcljava 层残留导致重新订阅不出数据
        activity.removePersistentNode(subId);

        BaseComposableNode node;
        if (type.contains("std_msgs/msg/String")) {
            StringSubscriberNode s = new StringSubscriberNode("m_sub_str_" + System.currentTimeMillis(), topic);
            s.clearCallbacks();
            s.addCallback(msg -> onMonitorMessage(topic, msg));
            node = s;
            activity.addPersistentNode(subId, node);
            s.start();
        } else if (type.contains("std_msgs/msg/Int32")) {
            Int32SubscriberNode s = new Int32SubscriberNode("m_sub_i32_" + System.currentTimeMillis(), topic);
            s.clearCallbacks();
            s.addCallback(msg -> onMonitorMessage(topic, msg));
            node = s;
            activity.addPersistentNode(subId, node);
            s.start();
        } else if (type.contains("std_msgs/msg/Int64")) {
            Int64SubscriberNode s = new Int64SubscriberNode("m_sub_i64_" + System.currentTimeMillis(), topic);
            s.clearCallbacks();
            s.addCallback(msg -> onMonitorMessage(topic, msg));
            node = s;
            activity.addPersistentNode(subId, node);
            s.start();
        } else if (type.contains("std_msgs/msg/Float32")) {
            Float32SubscriberNode s = new Float32SubscriberNode("m_sub_f32_" + System.currentTimeMillis(), topic);
            s.clearCallbacks();
            s.addCallback(msg -> onMonitorMessage(topic, msg));
            node = s;
            activity.addPersistentNode(subId, node);
            s.start();
        } else if (type.contains("std_msgs/msg/Float64")) {
            Float64SubscriberNode s = new Float64SubscriberNode("m_sub_f64_" + System.currentTimeMillis(), topic);
            s.clearCallbacks();
            s.addCallback(msg -> onMonitorMessage(topic, msg));
            node = s;
            activity.addPersistentNode(subId, node);
            s.start();
        } else if (type.contains("geometry_msgs/msg/Twist")) {
            TwistSubscriberNode s = new TwistSubscriberNode("m_sub_twist_" + System.currentTimeMillis(), topic);
            s.clearCallbacks();
            s.addCallback(msg -> onMonitorMessage(topic, msg));
            node = s;
            activity.addPersistentNode(subId, node);
            s.start();
        } else if (type.contains("nav_msgs/msg/Odometry")) {
            OdometrySubscriberNode s = new OdometrySubscriberNode("m_sub_odom_" + System.currentTimeMillis(), topic);
            s.clearCallbacks();
            s.addCallback(msg -> onMonitorMessage(topic, msg));
            node = s;
            activity.addPersistentNode(subId, node);
            s.start();
        } else if (type.contains("sensor_msgs/msg/Imu")) {
            ImuSubscriberNode s = new ImuSubscriberNode("m_sub_imu_" + System.currentTimeMillis(), topic);
            s.clearCallbacks();
            s.addCallback(msg -> onMonitorMessage(topic, msg));
            node = s;
            activity.addPersistentNode(subId, node);
            s.start();
        } else {
            Toast.makeText(getContext(), "暂不支持该类型监听: " + type, Toast.LENGTH_SHORT).show();
        }
    }

    private void rebindSubscription(String topic) {
        if (!(getActivity() instanceof ROSActivity)) return;
        ROSActivity activity = (ROSActivity) getActivity();
        String subId = "master_sub_" + topic;
        BaseComposableNode node = activity.getPersistentNode(subId);
        if (node == null) return;

        if (node instanceof StringSubscriberNode) {
            ((StringSubscriberNode) node).clearCallbacks();
            ((StringSubscriberNode) node).addCallback(msg -> onMonitorMessage(topic, msg));
        } else if (node instanceof Int32SubscriberNode) {
            ((Int32SubscriberNode) node).clearCallbacks();
            ((Int32SubscriberNode) node).addCallback(msg -> onMonitorMessage(topic, msg));
        } else if (node instanceof Int64SubscriberNode) {
            ((Int64SubscriberNode) node).clearCallbacks();
            ((Int64SubscriberNode) node).addCallback(msg -> onMonitorMessage(topic, msg));
        } else if (node instanceof Float32SubscriberNode) {
            ((Float32SubscriberNode) node).clearCallbacks();
            ((Float32SubscriberNode) node).addCallback(msg -> onMonitorMessage(topic, msg));
        } else if (node instanceof Float64SubscriberNode) {
            ((Float64SubscriberNode) node).clearCallbacks();
            ((Float64SubscriberNode) node).addCallback(msg -> onMonitorMessage(topic, msg));
        } else if (node instanceof TwistSubscriberNode) {
            ((TwistSubscriberNode) node).clearCallbacks();
            ((TwistSubscriberNode) node).addCallback(msg -> onMonitorMessage(topic, msg));
        } else if (node instanceof OdometrySubscriberNode) {
            ((OdometrySubscriberNode) node).clearCallbacks();
            ((OdometrySubscriberNode) node).addCallback(msg -> onMonitorMessage(topic, msg));
        } else if (node instanceof ImuSubscriberNode) {
            ((ImuSubscriberNode) node).clearCallbacks();
            ((ImuSubscriberNode) node).addCallback(msg -> onMonitorMessage(topic, msg));
        }
    }

    private void onMonitorMessage(String topic, String msg) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (getActivity() instanceof ROSActivity) {
                ROSActivity activity = (ROSActivity) getActivity();
                // 关键检查：如果话题已经被关闭了，就不再更新 UI
                if (!activity.getActiveMonitorTopics().contains(topic)) return;
                
                activity.addMasterMonitorMessage(topic, msg);
                int pos = activity.getActiveMonitorTopics().indexOf(topic);
                if (pos != -1 && monitorAdapter != null) {
                    monitorAdapter.notifyItemChanged(pos);
                }
            }
        });
    }

    private void stopMonitoring(String topic) {
        if (!(getActivity() instanceof ROSActivity)) return;
        ROSActivity activity = (ROSActivity) getActivity();
        
        getActivity().runOnUiThread(() -> {
            activity.removeMasterMonitorTopic(topic);
            if (monitorAdapter != null) {
                monitorAdapter.setTopics(activity.getActiveMonitorTopics());
            }
        });
    }

    private void showPublishDialog(String topic, String type) {
        if (!(getActivity() instanceof ROSActivity)) return;
        ROSActivity activity = (ROSActivity) getActivity();

        String nodeId = "master_pub_" + topic;
        BaseComposableNode node = activity.getPersistentNode(nodeId);

        if (node == null) {
            if (type.contains("String")) node = new StringPublisherNode("mp_str_" + System.currentTimeMillis(), topic);
            else if (type.contains("Int32")) node = new Int32PublisherNode("mp_i32_" + System.currentTimeMillis(), topic);
            else if (type.contains("Int64")) node = new Int64PublisherNode("mp_i64_" + System.currentTimeMillis(), topic);
            else if (type.contains("Float32")) node = new Float32PublisherNode("mp_f32_" + System.currentTimeMillis(), topic);
            else if (type.contains("Float64")) node = new Float64PublisherNode("mp_f64_" + System.currentTimeMillis(), topic);

            if (node != null) {
                activity.addPersistentNode(nodeId, node);
                if (node instanceof StringPublisherNode) ((StringPublisherNode) node).start();
                else if (node instanceof Int32PublisherNode) ((Int32PublisherNode) node).start();
                else if (node instanceof Int64PublisherNode) ((Int64PublisherNode) node).start();
                else if (node instanceof Float32PublisherNode) ((Float32PublisherNode) node).start();
                else if (node instanceof Float64PublisherNode) ((Float64PublisherNode) node).start();
            }
        }

        final BaseComposableNode pubNode = node;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("发布 (" + type + ")\n" + topic);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText msgInput = new EditText(getContext());
        msgInput.setHint("输入数值内容");
        if (type.contains("Int")) {
            msgInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        } else if (type.contains("Float")) {
            msgInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        }
        layout.addView(msgInput);

        LinearLayout hzRow = new LinearLayout(getContext());
        hzRow.setOrientation(LinearLayout.HORIZONTAL);
        final EditText hzInput = new EditText(getContext());
        hzInput.setHint("周期频率");
        hzInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        hzInput.setText("1");
        hzInput.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        final TextView hzUnit = new TextView(getContext());
        hzUnit.setText("Hz");
        hzUnit.setPadding(16, 0, 0, 0);
        hzRow.addView(hzInput);
        hzRow.addView(hzUnit);
        layout.addView(hzRow);

        builder.setView(layout);
        builder.setPositiveButton("单次发布", null);
        builder.setNeutralButton("开始周期发布", null);
        builder.setNegativeButton("停止并关闭", (dialog, which) -> stopNodePeriodic(pubNode));

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String val = msgInput.getText().toString();
            try {
                publishOnce(pubNode, val);
                Toast.makeText(getContext(), "已发送一次", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "格式错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            String val = msgInput.getText().toString();
            int hz = 1;
            try { hz = Integer.parseInt(hzInput.getText().toString()); } catch (Exception ignored) {}
            try {
                startPeriodic(pubNode, val, hz);
                Toast.makeText(getContext(), "开始周期发布", Toast.LENGTH_SHORT).show();
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText("更新数值");
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v2 -> {
                    updateNodeValue(pubNode, msgInput.getText().toString());
                    Toast.makeText(getContext(), "数值已更新", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Toast.makeText(getContext(), "格式错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setOnDismissListener(d -> stopNodePeriodic(pubNode));
    }

    private void publishOnce(BaseComposableNode node, String val) {
        if (node instanceof StringPublisherNode) ((StringPublisherNode) node).publishOnce(val);
        else if (node instanceof Int32PublisherNode) ((Int32PublisherNode) node).publishOnce(Integer.parseInt(val));
        else if (node instanceof Int64PublisherNode) ((Int64PublisherNode) node).publishOnce(Long.parseLong(val));
        else if (node instanceof Float32PublisherNode) ((Float32PublisherNode) node).publishOnce(Float.parseFloat(val));
        else if (node instanceof Float64PublisherNode) ((Float64PublisherNode) node).publishOnce(Double.parseDouble(val));
    }

    private void startPeriodic(BaseComposableNode node, String val, int hz) {
        if (node instanceof StringPublisherNode) ((StringPublisherNode) node).startPeriodicPublish(val, hz);
        else if (node instanceof Int32PublisherNode) ((Int32PublisherNode) node).startPeriodicPublish(Integer.parseInt(val), hz);
        else if (node instanceof Int64PublisherNode) ((Int64PublisherNode) node).startPeriodicPublish(Long.parseLong(val), hz);
        else if (node instanceof Float32PublisherNode) ((Float32PublisherNode) node).startPeriodicPublish(Float.parseFloat(val), hz);
        else if (node instanceof Float64PublisherNode) ((Float64PublisherNode) node).startPeriodicPublish(Double.parseDouble(val), hz);
    }

    private void updateNodeValue(BaseComposableNode node, String val) {
        if (node instanceof StringPublisherNode) ((StringPublisherNode) node).updateMessage(val);
        else if (node instanceof Int32PublisherNode) ((Int32PublisherNode) node).updateValue(Integer.parseInt(val));
        else if (node instanceof Int64PublisherNode) ((Int64PublisherNode) node).updateValue(Long.parseLong(val));
        else if (node instanceof Float32PublisherNode) ((Float32PublisherNode) node).updateValue(Float.parseFloat(val));
        else if (node instanceof Float64PublisherNode) ((Float64PublisherNode) node).updateValue(Double.parseDouble(val));
    }

    private void stopNodePeriodic(BaseComposableNode node) {
        if (node instanceof StringPublisherNode) ((StringPublisherNode) node).stopPeriodicPublish();
        else if (node instanceof Int32PublisherNode) ((Int32PublisherNode) node).stopPeriodicPublish();
        else if (node instanceof Int64PublisherNode) ((Int64PublisherNode) node).stopPeriodicPublish();
        else if (node instanceof Float32PublisherNode) ((Float32PublisherNode) node).stopPeriodicPublish();
        else if (node instanceof Float64PublisherNode) ((Float64PublisherNode) node).stopPeriodicPublish();
    }

    private class TopicAdapter extends RecyclerView.Adapter<TopicViewHolder> {
        private final List<NameAndTypes> items = new ArrayList<>();

        void setItems(List<NameAndTypes> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new TopicViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
            NameAndTypes nt = items.get(position);
            holder.text1.setText(nt.name);
            holder.text1.setTextColor(getResources().getColor(R.color.whiteHigh));
            String typeStr = nt.types.toString();
            holder.text2.setText(typeStr);
            holder.text2.setTextColor(getResources().getColor(R.color.colorAccent));

            holder.itemView.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(getContext(), v);
                popup.getMenu().add("订阅");
                popup.getMenu().add("发布");
                popup.setOnMenuItemClickListener(item -> {
                    String title = item.getTitle().toString();
                    if ("订阅".equals(title)) {
                        startMonitoring(nt.name, typeStr);
                    } else if ("发布".equals(title)) {
                        showPublishDialog(nt.name, typeStr);
                    }
                    return true;
                });
                popup.show();
            });
        }

        @Override
        public int getItemCount() { return items.size(); }
    }

    private class MonitorAdapter extends RecyclerView.Adapter<MonitorViewHolder> {
        private final List<String> topics = new ArrayList<>();

        void setTopics(List<String> t) {
            topics.clear();
            if (t != null) topics.addAll(t);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MonitorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_monitor_topic, parent, false);
            return new MonitorViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MonitorViewHolder holder, int position) {
            String topic = topics.get(position);
            holder.topicName.setText(topic);
            if (getActivity() instanceof ROSActivity) {
                ROSActivity act = (ROSActivity) getActivity();
                holder.dataDisplay.setText(act.getMasterMonitorData(topic));
            }
            holder.closeBtn.setOnClickListener(v -> stopMonitoring(topic));
        }

        @Override
        public int getItemCount() {
            return topics.size();
        }
    }

    private static class MonitorViewHolder extends RecyclerView.ViewHolder {
        TextView topicName;
        TextView dataDisplay;
        ImageButton closeBtn;

        MonitorViewHolder(View v) {
            super(v);
            topicName = v.findViewById(R.id.monitor_topic_name);
            dataDisplay = v.findViewById(R.id.monitor_data_display);
            closeBtn = v.findViewById(R.id.close_monitor_btn);
        }
    }

    private static class TopicViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;
        TopicViewHolder(View v) {
            super(v);
            text1 = v.findViewById(android.R.id.text1);
            text2 = v.findViewById(android.R.id.text2);
        }
    }

    @Override
    public void onDestroy() {
        // 关键修复：不要在这里 removeNode(graphNode)，让它随 Activity 存活，
        // 避免 Tab 切换销毁 Fragment 导致话题发现功能中断。
        super.onDestroy();
    }
}
