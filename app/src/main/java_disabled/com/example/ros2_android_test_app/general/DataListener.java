package com.example.ros2_android_test_app.ui.general;


import com.example.ros2_android_test_app.model.repositories.rosRepo.node.BaseData;

/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 15.03.20
 * @updated on 15.03.20
 * @modified by
 */
public interface DataListener {

    void onNewWidgetData(BaseData data);
}
