package com.example.ros2_android_test_app.widgets;

public class ButtonWidgetEntity {
    public String topicName = "/button_cmd";
    public String messageText = "button_pressed";

    public ButtonWidgetEntity() {
    }

    public ButtonWidgetEntity(String topicName, String messageText) {
        this.topicName = topicName;
        this.messageText = messageText;
    }
}

