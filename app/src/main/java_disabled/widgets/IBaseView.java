package com.example.ros2_android_test_app.ui.views.widgets;

import com.example.ros2_android_test_app.model.entities.widgets.BaseEntity;

/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 10.03.21
 */
public interface IBaseView {

    void setWidgetEntity(BaseEntity entity);
    BaseEntity getWidgetEntity();

    boolean sameWidgetEntity(BaseEntity other);
}
