package com.example.ros2_android_test_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ros2_android_test_app.widgets.ButtonWidgetEntity;
import com.example.ros2_android_test_app.widgets.JoystickWidgetEntity;
import com.example.ros2_android_test_app.widgets.WidgetConfigManager;

public class DetailsFragment extends Fragment {

    private EditText topicEdit;
    private EditText maxLinearEdit;
    private EditText maxAngularEdit;

    private EditText btnTopicEdit;
    private EditText btnMsgEdit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        topicEdit = view.findViewById(R.id.joyTopicEdit);
        maxLinearEdit = view.findViewById(R.id.joyMaxLinearEdit);
        maxAngularEdit = view.findViewById(R.id.joyMaxAngularEdit);
        Button saveBtn = view.findViewById(R.id.saveJoyBtn);

        btnTopicEdit = view.findViewById(R.id.btnTopicEdit);
        btnMsgEdit = view.findViewById(R.id.btnMsgEdit);
        Button saveBtnConfig = view.findViewById(R.id.saveBtnConfig);

        // 加载 Joystick 配置
        JoystickWidgetEntity entity = WidgetConfigManager.loadJoystickConfig(requireContext());
        if (topicEdit != null) {
            topicEdit.setText(entity.topicName);
        }
        if (maxLinearEdit != null) {
            maxLinearEdit.setText(String.valueOf(entity.maxLinear));
        }
        if (maxAngularEdit != null) {
            maxAngularEdit.setText(String.valueOf(entity.maxAngular));
        }

        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> {
                String topic = topicEdit != null ? topicEdit.getText().toString().trim() : "/cmd_vel";
                if (topic.isEmpty()) {
                    topic = "/cmd_vel";
                }

                double maxLinear;
                double maxAngular;
                try {
                    maxLinear = Double.parseDouble(maxLinearEdit != null ? maxLinearEdit.getText().toString().trim() : "0.5");
                    maxAngular = Double.parseDouble(maxAngularEdit != null ? maxAngularEdit.getText().toString().trim() : "1.0");
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Invalid number", Toast.LENGTH_SHORT).show();
                    return;
                }

                JoystickWidgetEntity newEntity = new JoystickWidgetEntity(topic, maxLinear, maxAngular);
                WidgetConfigManager.saveJoystickConfig(requireContext(), newEntity);
                Toast.makeText(requireContext(), "Joystick saved", Toast.LENGTH_SHORT).show();
            });
        }

        // 加载 Button 配置
        ButtonWidgetEntity btnEntity = WidgetConfigManager.loadButtonConfig(requireContext());
        if (btnTopicEdit != null) {
            btnTopicEdit.setText(btnEntity.topicName);
        }
        if (btnMsgEdit != null) {
            btnMsgEdit.setText(btnEntity.messageText);
        }

        if (saveBtnConfig != null) {
            saveBtnConfig.setOnClickListener(v -> {
                String topic = btnTopicEdit != null ? btnTopicEdit.getText().toString().trim() : "/button_cmd";
                if (topic.isEmpty()) {
                    topic = "/button_cmd";
                }
                String msg = btnMsgEdit != null ? btnMsgEdit.getText().toString().trim() : "button_pressed";
                if (msg.isEmpty()) {
                    msg = "button_pressed";
                }

                ButtonWidgetEntity newBtnEntity = new ButtonWidgetEntity(topic, msg);
                WidgetConfigManager.saveButtonConfig(requireContext(), newBtnEntity);
                Toast.makeText(requireContext(), "Button config saved", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
