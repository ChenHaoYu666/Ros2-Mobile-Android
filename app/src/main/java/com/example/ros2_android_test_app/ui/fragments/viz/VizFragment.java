package com.example.ros2_android_test_app.ui.fragments.viz;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ros2_android_test_app.R;
import com.example.ros2_android_test_app.ROSActivity;
import com.example.ros2_android_test_app.VizMapView;
import com.example.ros2_android_test_app.model.config.WidgetConfig;
import com.example.ros2_android_test_app.model.config.WidgetConfigStore;
import com.example.ros2_android_test_app.ros2.BoolPublisherNode;
import com.example.ros2_android_test_app.ros2.Float32SubscriberNode;
import com.example.ros2_android_test_app.ros2.Float64SubscriberNode;
import com.example.ros2_android_test_app.ros2.Int32SubscriberNode;
import com.example.ros2_android_test_app.ros2.Int64SubscriberNode;
import com.example.ros2_android_test_app.ros2.ImuSubscriberNode;
import com.example.ros2_android_test_app.ros2.LaserScanSubscriberNode;
import com.example.ros2_android_test_app.ros2.OdometrySubscriberNode;
import com.example.ros2_android_test_app.ros2.OccupancyGridSubscriberNode;
import com.example.ros2_android_test_app.ros2.PoseSubscriberNode;
import com.example.ros2_android_test_app.ros2.StringSubscriberNode;
import com.example.ros2_android_test_app.ros2.TwistPublisherNode;
import com.example.ros2_android_test_app.ros2.TwistSubscriberNode;
import com.example.ros2_android_test_app.ui.views.ButtonView;
import com.example.ros2_android_test_app.ui.views.JoystickView;

import org.ros2.rcljava.node.BaseComposableNode;

import java.util.List;

public class VizFragment extends Fragment {

    private static final String TAG = "VizFragment";
    private WidgetViewGroup widgetGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_viz, container, false);
        widgetGroup = root.findViewById(R.id.widget_groupview);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        com.google.android.material.switchmaterial.SwitchMaterial editSwitch = view.findViewById(R.id.edit_viz_switch);
        if (editSwitch != null) {
            editSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "Edit mode toggled: " + isChecked);
                setEditMode(isChecked);
            });
        }

        if (widgetGroup != null) {
            widgetGroup.setOnWidgetMovedListener(config -> {
                WidgetConfigStore.upsert(requireContext().getApplicationContext(), config);
                widgetGroup.requestLayout();
            });
        }

        refreshWidgets();
    }

    private void setEditMode(boolean enabled) {
        if (widgetGroup == null) return;
        widgetGroup.setEditMode(enabled);
        for (int i = 0; i < widgetGroup.getChildCount(); i++) {
            View child = widgetGroup.getChildAt(i);
            child.setOnLongClickListener(null);
            child.setLongClickable(false);
        }
    }


    private void refreshWidgets() {
        if (widgetGroup == null) return;
        widgetGroup.removeAllViews();

        List<WidgetConfig> configs = WidgetConfigStore.load(getContext());
        Log.d(TAG, "Loading widgets for VIZ grid, count: " + configs.size());

        for (WidgetConfig cfg : configs) {
            View widgetView = null;
            if ("Joystick".equals(cfg.type)) {
                widgetView = addJoystickWidget(cfg);
            } else if ("Button".equals(cfg.type)) {
                widgetView = addButtonWidget(cfg);
            } else if ("Label".equals(cfg.type)) {
                widgetView = addLabelWidget(cfg);
            } else if ("Map".equals(cfg.type)) {
                widgetView = addMapWidget(cfg);
            }

            if (widgetView != null) {
                widgetView.setTag(cfg);
                widgetGroup.addView(widgetView);
            }
        }
    }

    private View addJoystickWidget(WidgetConfig cfg) {
        JoystickView joystickView = new JoystickView(requireContext(), null);
        TwistPublisherNode node = createTwistPublisher(cfg);
        if (node != null) {
            joystickView.setOnJoystickChangeListener(node::updateRawValues);
        }
        return joystickView;
    }

    private View addButtonWidget(WidgetConfig cfg) {
        ButtonView buttonView = new ButtonView(requireContext());
        buttonView.setText(cfg.text);
        BoolPublisherNode node = createBoolPublisher(cfg);
        if (node != null) {
            buttonView.setOnButtonStateListener(node::publishValue);
        }
        return buttonView;
    }

    private View addLabelWidget(WidgetConfig cfg) {
        TextView textView = new TextView(requireContext());
        textView.setText(cfg.labelText);
        textView.setTextSize(cfg.textSize);
        textView.setTextColor(getResources().getColor(R.color.whiteHigh));
        textView.setBackgroundColor(getResources().getColor(R.color.black02dp));
        textView.setPadding(16, 8, 16, 8);

        if (getActivity() instanceof ROSActivity) {
            String history = ((ROSActivity) getActivity()).getLabelHistoryText(cfg.id);
            if (history != null) {
                textView.setText(history);
            }
        }

        createUniversalSubscriber(cfg, textView);
        return textView;
    }

    private View addMapWidget(WidgetConfig cfg) {
        VizMapView mapView = new VizMapView(requireContext());
        mapView.setFollowRobot(cfg.mapFollowRobot);
        createMapSubscribers(cfg, mapView);
        return mapView;
    }

    private void createMapSubscribers(WidgetConfig cfg, VizMapView mapView) {
        ROSActivity activity = getRosActivity();
        if (activity == null || cfg == null) return;

        String mapId = "map_sub_map_" + cfg.id;
        String laserId = "map_sub_laser_" + cfg.id;
        String poseId = "map_sub_pose_" + cfg.id;

        String mapTopic = (cfg.mapTopic == null || cfg.mapTopic.isEmpty()) ? "/map" : cfg.mapTopic;
        String laserTopic = (cfg.laserTopic == null || cfg.laserTopic.isEmpty()) ? "/scan" : cfg.laserTopic;
        String poseTopic = (cfg.poseTopic == null || cfg.poseTopic.isEmpty()) ? "/amcl_pose" : cfg.poseTopic;

        BaseComposableNode mapNodeBase = activity.getPersistentNode(mapId);
        OccupancyGridSubscriberNode mapNode;
        if (mapNodeBase instanceof OccupancyGridSubscriberNode) {
            mapNode = (OccupancyGridSubscriberNode) mapNodeBase;
        } else {
            mapNode = new OccupancyGridSubscriberNode("map_sub_" + cfg.id.substring(0, 8), mapTopic);
            activity.addPersistentNode(mapId, mapNode);
            mapNode.start();
        }
        mapNode.clearCallbacks();
        mapNode.addCallback(mapView::updateOccupancyGrid);

        BaseComposableNode laserNodeBase = activity.getPersistentNode(laserId);
        LaserScanSubscriberNode laserNode;
        if (laserNodeBase instanceof LaserScanSubscriberNode) {
            laserNode = (LaserScanSubscriberNode) laserNodeBase;
        } else {
            laserNode = new LaserScanSubscriberNode("laser_sub_" + cfg.id.substring(0, 8), laserTopic);
            activity.addPersistentNode(laserId, laserNode);
            laserNode.start();
        }
        laserNode.addCallback(mapView::updateLaserScan);

        BaseComposableNode poseNodeBase = activity.getPersistentNode(poseId);
        PoseSubscriberNode poseNode;
        if (poseNodeBase instanceof PoseSubscriberNode) {
            poseNode = (PoseSubscriberNode) poseNodeBase;
        } else {
            poseNode = new PoseSubscriberNode("pose_sub_" + cfg.id.substring(0, 8), poseTopic);
            activity.addPersistentNode(poseId, poseNode);
            poseNode.start();
        }
        poseNode.addCallback(mapView::updateRobotPose);
    }

    private void createUniversalSubscriber(WidgetConfig cfg, TextView textView) {
        ROSActivity activity = getRosActivity();
        if (activity == null) return;

        String id = "sub_" + cfg.id;
        String type = cfg.topicType != null ? cfg.topicType : "std_msgs/msg/String";
        BaseComposableNode node = activity.getPersistentNode(id);

        if ("geometry_msgs/msg/Twist".equals(type)) {
            TwistSubscriberNode subNode;
            if (node instanceof TwistSubscriberNode) {
                subNode = (TwistSubscriberNode) node;
            } else {
                subNode = new TwistSubscriberNode("twist_sub_" + cfg.id.substring(0, 8), cfg.topic);
                activity.addPersistentNode(id, subNode);
                subNode.start();
            }
            subNode.addCallback(text -> updateLabelUI(activity, cfg.id, text, textView));
        } else if ("nav_msgs/msg/Odometry".equals(type)) {
            OdometrySubscriberNode subNode;
            if (node instanceof OdometrySubscriberNode) {
                subNode = (OdometrySubscriberNode) node;
            } else {
                subNode = new OdometrySubscriberNode("odom_sub_" + cfg.id.substring(0, 8), cfg.topic);
                activity.addPersistentNode(id, subNode);
                subNode.start();
            }
            subNode.addCallback(text -> updateLabelUI(activity, cfg.id, text, textView));
        } else if ("sensor_msgs/msg/Imu".equals(type)) {
            ImuSubscriberNode subNode;
            if (node instanceof ImuSubscriberNode) {
                subNode = (ImuSubscriberNode) node;
            } else {
                subNode = new ImuSubscriberNode("imu_sub_" + cfg.id.substring(0, 8), cfg.topic);
                activity.addPersistentNode(id, subNode);
                subNode.start();
            }
            subNode.addCallback(text -> updateLabelUI(activity, cfg.id, text, textView));
        } else if ("std_msgs/msg/Int32".equals(type)) {
            Int32SubscriberNode subNode;
            if (node instanceof Int32SubscriberNode) {
                subNode = (Int32SubscriberNode) node;
            } else {
                subNode = new Int32SubscriberNode("int32_sub_" + cfg.id.substring(0, 8), cfg.topic);
                activity.addPersistentNode(id, subNode);
                subNode.start();
            }
            subNode.addCallback(text -> updateLabelUI(activity, cfg.id, text, textView));
        } else if ("std_msgs/msg/Int64".equals(type)) {
            Int64SubscriberNode subNode;
            if (node instanceof Int64SubscriberNode) {
                subNode = (Int64SubscriberNode) node;
            } else {
                subNode = new Int64SubscriberNode("int64_sub_" + cfg.id.substring(0, 8), cfg.topic);
                activity.addPersistentNode(id, subNode);
                subNode.start();
            }
            subNode.addCallback(text -> updateLabelUI(activity, cfg.id, text, textView));
        } else if ("std_msgs/msg/Float32".equals(type)) {
            Float32SubscriberNode subNode;
            if (node instanceof Float32SubscriberNode) {
                subNode = (Float32SubscriberNode) node;
            } else {
                subNode = new Float32SubscriberNode("f32_sub_" + cfg.id.substring(0, 8), cfg.topic);
                activity.addPersistentNode(id, subNode);
                subNode.start();
            }
            subNode.addCallback(text -> updateLabelUI(activity, cfg.id, text, textView));
        } else if ("std_msgs/msg/Float64".equals(type)) {
            Float64SubscriberNode subNode;
            if (node instanceof Float64SubscriberNode) {
                subNode = (Float64SubscriberNode) node;
            } else {
                subNode = new Float64SubscriberNode("f64_sub_" + cfg.id.substring(0, 8), cfg.topic);
                activity.addPersistentNode(id, subNode);
                subNode.start();
            }
            subNode.addCallback(text -> updateLabelUI(activity, cfg.id, text, textView));
        } else {
            StringSubscriberNode subNode;
            if (node instanceof StringSubscriberNode) {
                subNode = (StringSubscriberNode) node;
            } else {
                subNode = new StringSubscriberNode("str_sub_" + cfg.id.substring(0, 8), cfg.topic);
                activity.addPersistentNode(id, subNode);
                subNode.start();
            }
            subNode.addCallback(text -> updateLabelUI(activity, cfg.id, text, textView));
        }
    }

    private void updateLabelUI(ROSActivity activity, String widgetId, String text, TextView textView) {
        if (getActivity() != null && textView != null) {
            getActivity().runOnUiThread(() -> {
                activity.addLabelHistory(widgetId, text);
                textView.setText(activity.getLabelHistoryText(widgetId));
            });
        }
    }

    private TwistPublisherNode createTwistPublisher(WidgetConfig cfg) {
        ROSActivity activity = getRosActivity();
        if (activity == null) return null;

        String id = "twist_pub_" + cfg.id;
        BaseComposableNode existing = activity.getPersistentNode(id);
        if (existing instanceof TwistPublisherNode) {
            return (TwistPublisherNode) existing;
        }

        String name = "twist_pub_" + cfg.id.substring(0, 8);
        TwistPublisherNode node = new TwistPublisherNode(name, cfg);
        activity.addPersistentNode(id, node);
        node.start();
        return node;
    }

    private BoolPublisherNode createBoolPublisher(WidgetConfig cfg) {
        ROSActivity activity = getRosActivity();
        if (activity == null) return null;

        String id = "bool_pub_" + cfg.id;
        BaseComposableNode existing = activity.getPersistentNode(id);
        if (existing instanceof BoolPublisherNode) {
            return (BoolPublisherNode) existing;
        }

        String name = "bool_pub_" + cfg.id.substring(0, 8);
        BoolPublisherNode node = new BoolPublisherNode(name, cfg.topic);
        activity.addPersistentNode(id, node);
        node.start();
        return node;
    }

    private ROSActivity getRosActivity() {
        if (getActivity() instanceof ROSActivity) {
            return (ROSActivity) getActivity();
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
