package com.example.ros2_android_test_app.widgets.logger;

import com.example.ros2_android_test_app.model.entities.widgets.SubscriberWidgetEntity;
import com.example.ros2_android_test_app.model.repositories.rosRepo.message.Topic;


/**
 * TODO: Description
 *
 * @author Dragos Circa
 * @version 1.0.0
 * @created on 02.11.2020
 * @updated on 18.11.2020
 * @modified by Nils Rottmann
 */

public class LoggerEntity extends SubscriberWidgetEntity {

    public String text;
    public int rotation;


    public LoggerEntity() {
        this.width = 3;
        this.height = 1;
        this.topic = new Topic("log", std_msgs.String._TYPE);
        this.text = "A logger";
        this.rotation = 0;
    }

}
