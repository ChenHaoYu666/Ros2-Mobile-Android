package com.example.ros2_android_test_app.widgets.touchgoal;

import com.example.ros2_android_test_app.model.entities.widgets.PublisherLayerEntity;
import com.example.ros2_android_test_app.model.repositories.rosRepo.message.Topic;

import geometry_msgs.PoseStamped;


/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 26.05.2021
 */
public class TouchGoalEntity extends PublisherLayerEntity {

    public TouchGoalEntity() {
        this.topic = new Topic("/goal", PoseStamped._TYPE);
        this.immediatePublish = true;
    }
}
