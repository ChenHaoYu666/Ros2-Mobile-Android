package com.example.ros2_android_test_app.ui.views.widgets;

import com.example.ros2_android_test_app.model.repositories.rosRepo.node.BaseData;
import com.example.ros2_android_test_app.ui.general.DataListener;

/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 10.03.21
 */
public interface IPublisherView {

    void publishViewData(BaseData data);
    void setDataListener(DataListener listener);
}
