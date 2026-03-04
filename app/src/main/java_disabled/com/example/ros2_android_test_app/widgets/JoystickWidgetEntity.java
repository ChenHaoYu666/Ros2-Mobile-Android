package com.example.ros2_android_test_app.widgets;

public class JoystickWidgetEntity {
    public String topicName = "/cmd_vel";
    public double maxLinear = 0.5;   // m/s
    public double maxAngular = 1.0;  // rad/s

    public JoystickWidgetEntity() {
    }

    public JoystickWidgetEntity(String topicName, double maxLinear, double maxAngular) {
        this.topicName = topicName;
        this.maxLinear = maxLinear;
        this.maxAngular = maxAngular;
    }
}

