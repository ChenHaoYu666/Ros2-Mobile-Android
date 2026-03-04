package com.example.ros2_android_test_app.model.repositories.rosRepo.node;

import com.example.ros2_android_test_app.model.repositories.rosRepo.message.Topic;
import com.example.ros2_android_test_app.model.entities.widgets.BaseEntity;

import org.ros.internal.message.Message;
import org.ros.node.topic.Publisher;


public abstract class BaseData {

    protected Topic topic;


    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Topic getTopic() {
        return this.topic;
    }

    public Message toRosMessage(Publisher<Message> publisher, BaseEntity widget){
        return null;
    }
}
