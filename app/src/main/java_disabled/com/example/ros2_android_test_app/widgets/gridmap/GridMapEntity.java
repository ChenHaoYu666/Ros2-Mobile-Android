package com.example.ros2_android_test_app.widgets.gridmap;

import com.example.ros2_android_test_app.model.entities.widgets.SubscriberLayerEntity;
import com.example.ros2_android_test_app.model.repositories.rosRepo.message.Topic;

import nav_msgs.OccupancyGrid;


/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 08.03.21
 */
public class GridMapEntity extends SubscriberLayerEntity {
    
    public GridMapEntity() {
        this.topic = new Topic("/move_base/local_costmap/costmap", OccupancyGrid._TYPE);
    }
    
}
