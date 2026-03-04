package com.example.ros2_android_test_app.ui.fragments.details;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ros2_android_test_app.R;
import com.example.ros2_android_test_app.ROSActivity;
import com.example.ros2_android_test_app.model.config.WidgetConfig;
import com.example.ros2_android_test_app.model.config.WidgetConfigStore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.ros2.rcljava.graph.NameAndTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DetailsFragment extends Fragment {

    private static final String TAG = "DetailsFragment";
    private RecyclerView recyclerView;
    private WidgetAdapter adapter;
    private String expandedWidgetId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_details_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.widgets_rv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        FloatingActionButton fab = view.findViewById(R.id.add_widget_fab);
        fab.setOnClickListener(this::showAddMenu);

        View clearBtn = view.findViewById(R.id.clear_all_btn);
        clearBtn.setOnClickListener(v -> {
            WidgetConfigStore.clearAll(requireContext().getApplicationContext());
            expandedWidgetId = null;
            refresh();
            Toast.makeText(getContext(), "Cleared all widgets", Toast.LENGTH_SHORT).show();
        });

        adapter = new WidgetAdapter();
        recyclerView.setAdapter(adapter);
        refresh();
    }

    private void showAddMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenu().add("Joystick");
        popup.getMenu().add("Button");
        popup.getMenu().add("Label");
        popup.getMenu().add("Map");

        popup.setOnMenuItemClickListener(item -> {
            String type = item.getTitle().toString();
            WidgetConfig newCfg = null;
            if ("Joystick".equals(type)) {
                newCfg = WidgetConfigStore.addDefaultJoystick(requireContext().getApplicationContext());
            } else if ("Button".equals(type)) {
                newCfg = WidgetConfigStore.addDefaultButton(requireContext().getApplicationContext());
            } else if ("Label".equals(type)) {
                newCfg = WidgetConfigStore.addDefaultLabel(requireContext().getApplicationContext());
            } else if ("Map".equals(type)) {
                newCfg = WidgetConfigStore.addDefaultMap(requireContext().getApplicationContext());
            }

            if (newCfg != null) {
                expandedWidgetId = newCfg.id;
                refresh();
            }
            return true;
        });
        popup.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (adapter != null) {
            List<WidgetConfig> widgets = WidgetConfigStore.load(requireContext().getApplicationContext());
            adapter.setItems(widgets);
        }
    }

    private class WidgetAdapter extends RecyclerView.Adapter<WidgetViewHolder> {
        private final List<WidgetConfig> items = new ArrayList<>();

        void setItems(List<WidgetConfig> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public WidgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_widget_config, parent, false);
            return new WidgetViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull WidgetViewHolder holder, int position) {
            WidgetConfig cfg = items.get(position);
            holder.typeTxt.setText(cfg.name != null ? cfg.name : cfg.type);
            holder.topicTxt.setText(cfg.topic);

            ROSActivity activity = (getActivity() instanceof ROSActivity) ? (ROSActivity) getActivity() : null;
            boolean isExpanded = cfg.id.equals(expandedWidgetId);
            holder.editorArea.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            if (isExpanded) {
                holder.editWidgetName.setText(cfg.name);
                holder.editTopic.setText(cfg.topic);
                holder.editTopicType.setText(cfg.topicType);
                holder.editMapTopic.setText(cfg.mapTopic);
                holder.editLaserTopic.setText(cfg.laserTopic);
                holder.editPoseTopic.setText(cfg.poseTopic);
                holder.editMapFollowSwitch.setChecked(cfg.mapFollowRobot);
                holder.editPosX.setText(String.valueOf(cfg.posX));
                holder.editPosY.setText(String.valueOf(cfg.posY));
                holder.editWidth.setText(String.valueOf(cfg.width));
                holder.editHeight.setText(String.valueOf(cfg.height));

                holder.topicDropdownBtn.setOnClickListener(btn -> {
                    if (activity == null) return;
                    Collection<NameAndTypes> topics = activity.getTopicNamesAndTypes();
                    PopupMenu popup = new PopupMenu(getContext(), btn);
                    if (topics.isEmpty()) {
                        popup.getMenu().add("未发现话题 (请确认 ROS2 网络)");
                    } else {
                        for (NameAndTypes nt : topics) {
                            String typeStr = nt.types.isEmpty() ? "" : nt.types.iterator().next();
                            popup.getMenu().add(nt.name + " (" + typeStr + ")").setOnMenuItemClickListener(item -> {
                                holder.editTopic.setText(nt.name);
                                holder.editTopicType.setText(typeStr);
                                return true;
                            });
                        }
                    }
                    popup.show();
                });

                holder.typeDropdownBtn.setOnClickListener(btn -> {
                    PopupMenu popup = new PopupMenu(getContext(), btn);
                    String[] supportedTypes;
                    if ("Joystick".equals(cfg.type)) {
                        supportedTypes = new String[]{"geometry_msgs/msg/Twist"};
                    } else if ("Button".equals(cfg.type)) {
                        supportedTypes = new String[]{"std_msgs/msg/String", "std_msgs/msg/Bool", "std_msgs/msg/Int32", "std_msgs/msg/Int64", "std_msgs/msg/Float32", "std_msgs/msg/Float64"};
                    } else if ("Label".equals(cfg.type)) {
                        supportedTypes = new String[]{
                                "std_msgs/msg/String", "std_msgs/msg/Int32", "std_msgs/msg/Int64", "std_msgs/msg/Float32", "std_msgs/msg/Float64",
                                "geometry_msgs/msg/Twist", "nav_msgs/msg/Odometry", "sensor_msgs/msg/Imu"
                        };
                    } else if ("Map".equals(cfg.type)) {
                        supportedTypes = new String[]{"nav_msgs/msg/OccupancyGrid"};
                    } else {
                        supportedTypes = new String[]{cfg.topicType};
                    }

                    for (String type : supportedTypes) {
                        popup.getMenu().add(type).setOnMenuItemClickListener(item -> {
                            holder.editTopicType.setText(type);
                            return true;
                        });
                    }
                    popup.show();
                });

                if ("Joystick".equals(cfg.type)) {
                    holder.joystickFields.setVisibility(View.VISIBLE);
                    holder.buttonFields.setVisibility(View.GONE);
                    holder.mapFields.setVisibility(View.GONE);

                    ArrayAdapter<CharSequence> dirAdapter = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.joystick_twist_dir, android.R.layout.simple_spinner_item);
                    dirAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    ArrayAdapter<CharSequence> axisAdapter = ArrayAdapter.createFromResource(holder.itemView.getContext(), R.array.joystick_twist_axis, android.R.layout.simple_spinner_item);
                    axisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    holder.xDirSpinner.setAdapter(dirAdapter);
                    holder.xAxisSpinner.setAdapter(axisAdapter);
                    holder.yDirSpinner.setAdapter(dirAdapter);
                    holder.yAxisSpinner.setAdapter(axisAdapter);

                    String[] xAxisMapping = (cfg.xAxisMapping != null ? cfg.xAxisMapping : "Angular/Z").split("/");
                    if (xAxisMapping.length == 2) {
                        holder.xDirSpinner.setSelection(dirAdapter.getPosition(xAxisMapping[0]));
                        holder.xAxisSpinner.setSelection(axisAdapter.getPosition(xAxisMapping[1]));
                    }

                    String[] yAxisMapping = (cfg.yAxisMapping != null ? cfg.yAxisMapping : "Linear/X").split("/");
                    if (yAxisMapping.length == 2) {
                        holder.yDirSpinner.setSelection(dirAdapter.getPosition(yAxisMapping[0]));
                        holder.yAxisSpinner.setSelection(axisAdapter.getPosition(yAxisMapping[1]));
                    }

                    holder.xScaleLeft.setText(String.valueOf(cfg.xScaleLeft));
                    holder.xScaleRight.setText(String.valueOf(cfg.xScaleRight));
                    holder.xScaleMiddle.setText(String.valueOf((cfg.xScaleLeft + cfg.xScaleRight) / 2.0f));
                    holder.yScaleLeft.setText(String.valueOf(cfg.yScaleLeft));
                    holder.yScaleRight.setText(String.valueOf(cfg.yScaleRight));
                    holder.yScaleMiddle.setText(String.valueOf((cfg.yScaleLeft + cfg.yScaleRight) / 2.0f));
                } else if ("Button".equals(cfg.type)) {
                    holder.joystickFields.setVisibility(View.GONE);
                    holder.buttonFields.setVisibility(View.VISIBLE);
                    holder.mapFields.setVisibility(View.GONE);
                    holder.editBtnText.setText(cfg.text);
                } else if ("Map".equals(cfg.type)) {
                    holder.joystickFields.setVisibility(View.GONE);
                    holder.buttonFields.setVisibility(View.GONE);
                    holder.mapFields.setVisibility(View.VISIBLE);
                } else {
                    holder.joystickFields.setVisibility(View.GONE);
                    holder.buttonFields.setVisibility(View.GONE);
                    holder.mapFields.setVisibility(View.GONE);
                }
            }

            holder.itemHeader.setOnClickListener(v -> {
                expandedWidgetId = isExpanded ? null : cfg.id;
                notifyDataSetChanged();
            });

            holder.deleteBtn.setOnClickListener(v -> {
                WidgetConfigStore.deleteById(requireContext().getApplicationContext(), cfg.id);
                if (cfg.id.equals(expandedWidgetId)) expandedWidgetId = null;
                refresh();
            });

            holder.deleteBtn.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });

            holder.saveBtn.setOnClickListener(v -> {
                try {
                    cfg.name = holder.editWidgetName.getText().toString();
                    cfg.topic = holder.editTopic.getText().toString();
                    cfg.topicType = holder.editTopicType.getText().toString();
                    cfg.posX = Integer.parseInt(holder.editPosX.getText().toString());
                    cfg.posY = Integer.parseInt(holder.editPosY.getText().toString());
                    cfg.width = Integer.parseInt(holder.editWidth.getText().toString());
                    cfg.height = Integer.parseInt(holder.editHeight.getText().toString());

                    if ("Joystick".equals(cfg.type)) {
                        String type = holder.editTopicType.getText().toString();
                        if (!"geometry_msgs/msg/Twist".equals(type)) {
                            Toast.makeText(getContext(), "Joystick 仅支持 geometry_msgs/msg/Twist", Toast.LENGTH_LONG).show();
                            return;
                        }
                        cfg.xAxisMapping = holder.xDirSpinner.getSelectedItem().toString() + "/" + holder.xAxisSpinner.getSelectedItem().toString();
                        cfg.yAxisMapping = holder.yDirSpinner.getSelectedItem().toString() + "/" + holder.yAxisSpinner.getSelectedItem().toString();
                        cfg.xScaleLeft = Float.parseFloat(holder.xScaleLeft.getText().toString());
                        cfg.xScaleRight = Float.parseFloat(holder.xScaleRight.getText().toString());
                        cfg.yScaleLeft = Float.parseFloat(holder.yScaleLeft.getText().toString());
                        cfg.yScaleRight = Float.parseFloat(holder.yScaleRight.getText().toString());
                    } else if ("Button".equals(cfg.type)) {
                        String type = holder.editTopicType.getText().toString();
                        if (!"std_msgs/msg/Bool".equals(type) && !"std_msgs/msg/String".equals(type)
                                && !"std_msgs/msg/Int32".equals(type) && !"std_msgs/msg/Int64".equals(type)
                                && !"std_msgs/msg/Float32".equals(type) && !"std_msgs/msg/Float64".equals(type)) {
                            Toast.makeText(getContext(), "Button 暂仅支持 Bool/String/Int/Float 类型", Toast.LENGTH_LONG).show();
                            return;
                        }
                        cfg.text = holder.editBtnText.getText().toString();
                    } else if ("Label".equals(cfg.type)) {
                        String type = holder.editTopicType.getText().toString();
                        if (!"std_msgs/msg/String".equals(type) && !"std_msgs/msg/Int32".equals(type)
                                && !"std_msgs/msg/Int64".equals(type) && !"std_msgs/msg/Float32".equals(type)
                                && !"std_msgs/msg/Float64".equals(type)
                                && !"geometry_msgs/msg/Twist".equals(type)
                                && !"nav_msgs/msg/Odometry".equals(type)
                                && !"sensor_msgs/msg/Imu".equals(type)) {
                            Toast.makeText(getContext(), "Label 暂仅支持 String/Int/Float/Twist/Odometry/Imu", Toast.LENGTH_LONG).show();
                            return;
                        }
                    } else if ("Map".equals(cfg.type)) {
                        cfg.topicType = "nav_msgs/msg/OccupancyGrid";
                        cfg.mapTopic = holder.editMapTopic.getText() == null ? "/map" : holder.editMapTopic.getText().toString().trim();
                        cfg.laserTopic = holder.editLaserTopic.getText() == null ? "/scan" : holder.editLaserTopic.getText().toString().trim();
                        cfg.poseTopic = holder.editPoseTopic.getText() == null ? "/amcl_pose" : holder.editPoseTopic.getText().toString().trim();

                        if (cfg.mapTopic.isEmpty()) cfg.mapTopic = "/map";
                        if (cfg.laserTopic.isEmpty()) cfg.laserTopic = "/scan";
                        if (cfg.poseTopic.isEmpty()) cfg.poseTopic = "/amcl_pose";

                        cfg.topic = cfg.mapTopic;
                        cfg.mapFollowRobot = holder.editMapFollowSwitch.isChecked();
                    }

                    WidgetConfigStore.upsert(requireContext().getApplicationContext(), cfg);
                    restartPersistentNodeForWidget(cfg);

                    expandedWidgetId = null;
                    refresh();
                    Toast.makeText(getContext(), "Saved and node restarted", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Invalid input", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private void restartPersistentNodeForWidget(WidgetConfig cfg) {
        if (!(getActivity() instanceof ROSActivity) || cfg == null || cfg.id == null) return;

        ROSActivity activity = (ROSActivity) getActivity();
        activity.removePersistentNode("twist_pub_" + cfg.id);
        activity.removePersistentNode("bool_pub_" + cfg.id);
        activity.removePersistentNode("string_pub_" + cfg.id);
        activity.removePersistentNode("int32_pub_" + cfg.id);
        activity.removePersistentNode("int64_pub_" + cfg.id);
        activity.removePersistentNode("float32_pub_" + cfg.id);
        activity.removePersistentNode("float64_pub_" + cfg.id);
        activity.removePersistentNode("sub_" + cfg.id);
        activity.removePersistentNode("map_sub_map_" + cfg.id);
        activity.removePersistentNode("map_sub_laser_" + cfg.id);
        activity.removePersistentNode("map_sub_pose_" + cfg.id);

        Log.i(TAG, "Node restart requested for widget: " + cfg.id + ", type=" + cfg.type);
    }

    private static class WidgetViewHolder extends RecyclerView.ViewHolder {
        TextView typeTxt, topicTxt;
        ImageButton deleteBtn;
        View itemHeader, editorArea, joystickFields, buttonFields, mapFields, saveBtn;
        TextInputEditText editWidgetName, editTopic, editTopicType, editBtnText;
        TextInputEditText editMapTopic, editLaserTopic, editPoseTopic;
        com.google.android.material.switchmaterial.SwitchMaterial editMapFollowSwitch;
        TextInputEditText editPosX, editPosY, editWidth, editHeight;
        ImageButton topicDropdownBtn, typeDropdownBtn;

        Spinner xDirSpinner, xAxisSpinner, yDirSpinner, yAxisSpinner;
        EditText xScaleLeft, xScaleRight, yScaleLeft, yScaleRight;
        TextView xScaleMiddle, yScaleMiddle;

        WidgetViewHolder(View v) {
            super(v);
            itemHeader = v.findViewById(R.id.item_header);
            typeTxt = v.findViewById(R.id.widget_type_txt);
            topicTxt = v.findViewById(R.id.widget_topic_txt);
            deleteBtn = v.findViewById(R.id.delete_widget_btn);
            editorArea = v.findViewById(R.id.expandable_editor);
            joystickFields = v.findViewById(R.id.joystick_fields);
            buttonFields = v.findViewById(R.id.button_fields);
            mapFields = v.findViewById(R.id.map_fields);
            saveBtn = v.findViewById(R.id.save_widget_btn);
            editWidgetName = v.findViewById(R.id.edit_widget_name);
            editTopic = v.findViewById(R.id.edit_topic);
            editTopicType = v.findViewById(R.id.edit_topic_type);
            topicDropdownBtn = v.findViewById(R.id.topic_dropdown_btn);
            typeDropdownBtn = v.findViewById(R.id.type_dropdown_btn);
            editBtnText = v.findViewById(R.id.edit_btn_text);
            editMapTopic = v.findViewById(R.id.edit_map_topic);
            editLaserTopic = v.findViewById(R.id.edit_laser_topic);
            editPoseTopic = v.findViewById(R.id.edit_pose_topic);
            editMapFollowSwitch = v.findViewById(R.id.edit_map_follow_switch);
            editPosX = v.findViewById(R.id.edit_pos_x);
            editPosY = v.findViewById(R.id.edit_pos_y);
            editWidth = v.findViewById(R.id.edit_width);
            editHeight = v.findViewById(R.id.edit_height);

            xDirSpinner = v.findViewById(R.id.xDirSpinner);
            xAxisSpinner = v.findViewById(R.id.xAxisSpinner);
            xScaleLeft = v.findViewById(R.id.xScaleLeft);
            xScaleRight = v.findViewById(R.id.xScaleRight);
            xScaleMiddle = v.findViewById(R.id.xScaleMiddle);
            yDirSpinner = v.findViewById(R.id.yDirSpinner);
            yAxisSpinner = v.findViewById(R.id.yAxisSpinner);
            yScaleLeft = v.findViewById(R.id.yScaleLeft);
            yScaleRight = v.findViewById(R.id.yScaleRight);
            yScaleMiddle = v.findViewById(R.id.yScaleMiddle);
        }
    }
}
