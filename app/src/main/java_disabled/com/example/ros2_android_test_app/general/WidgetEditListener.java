package com.example.ros2_android_test_app.ui.general;

import com.example.ros2_android_test_app.model.entities.widgets.BaseEntity;


/**
 * TODO: Description
 *
 * @author Sarthak Mittal
 * @version 1.0.1
 * @created on 01.07.21
 */
public interface WidgetEditListener {

    void onWidgetEdited(BaseEntity widgetEntity, boolean updateConfig);
}
