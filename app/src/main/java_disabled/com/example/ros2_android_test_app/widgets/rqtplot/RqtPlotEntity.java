package com.example.ros2_android_test_app.widgets.rqtplot;

import com.example.ros2_android_test_app.model.entities.widgets.SubscriberWidgetEntity;
import com.example.ros2_android_test_app.model.repositories.rosRepo.message.Topic;

import org.ros.node.topic.Subscriber;


/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 29.05.21
 */
public class RqtPlotEntity extends SubscriberWidgetEntity {

    public String fieldPath;

    public RqtPlotEntity() {
        this.width = 8;
        this.height = 6;
        this.topic = new Topic("/plot", Subscriber.TOPIC_MESSAGE_TYPE_WILDCARD);
        this.fieldPath = "/pos/xy";
    }
}
