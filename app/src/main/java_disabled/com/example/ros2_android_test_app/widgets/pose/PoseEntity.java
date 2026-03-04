package com.example.ros2_android_test_app.widgets.pose;

import com.example.ros2_android_test_app.model.entities.widgets.SubscriberLayerEntity;
import com.example.ros2_android_test_app.model.repositories.rosRepo.message.Topic;

import geometry_msgs.PoseWithCovarianceStamped;


/**
 * Pose entity represents a widget which subscribes
 * to a topic with message type "geometry_msgs.PoseStamped".
 * Usable in 2D widgets.
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 10.03.21
 */
public class PoseEntity extends SubscriberLayerEntity {


    public PoseEntity() {
        this.topic = new Topic("/amcl_pose", PoseWithCovarianceStamped._TYPE);
    }
}
