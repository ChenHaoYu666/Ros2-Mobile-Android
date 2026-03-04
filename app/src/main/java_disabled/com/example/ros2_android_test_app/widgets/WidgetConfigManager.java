package com.example.ros2_android_test_app.widgets;

import android.content.Context;
import android.content.SharedPreferences;

public class WidgetConfigManager {

    private static final String PREF_NAME = "widget_config";

    // Joystick
    private static final String KEY_JOY_TOPIC = "joy_topic";
    private static final String KEY_JOY_MAX_LINEAR = "joy_max_linear";
    private static final String KEY_JOY_MAX_ANGULAR = "joy_max_angular";

    // Button
    private static final String KEY_BTN_TOPIC = "btn_topic";
    private static final String KEY_BTN_MESSAGE = "btn_message";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Joystick
    public static JoystickWidgetEntity loadJoystickConfig(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String topic = prefs.getString(KEY_JOY_TOPIC, "/cmd_vel");
        double maxLinear = Double.longBitsToDouble(prefs.getLong(KEY_JOY_MAX_LINEAR,
                Double.doubleToLongBits(0.5)));
        double maxAngular = Double.longBitsToDouble(prefs.getLong(KEY_JOY_MAX_ANGULAR,
                Double.doubleToLongBits(1.0)));
        return new JoystickWidgetEntity(topic, maxLinear, maxAngular);
    }

    public static void saveJoystickConfig(Context context, JoystickWidgetEntity entity) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .putString(KEY_JOY_TOPIC, entity.topicName)
                .putLong(KEY_JOY_MAX_LINEAR, Double.doubleToLongBits(entity.maxLinear))
                .putLong(KEY_JOY_MAX_ANGULAR, Double.doubleToLongBits(entity.maxAngular))
                .apply();
    }

    // Button
    public static ButtonWidgetEntity loadButtonConfig(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String topic = prefs.getString(KEY_BTN_TOPIC, "/button_cmd");
        String msg = prefs.getString(KEY_BTN_MESSAGE, "button_pressed");
        return new ButtonWidgetEntity(topic, msg);
    }

    public static void saveButtonConfig(Context context, ButtonWidgetEntity entity) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .putString(KEY_BTN_TOPIC, entity.topicName)
                .putString(KEY_BTN_MESSAGE, entity.messageText)
                .apply();
    }
}
