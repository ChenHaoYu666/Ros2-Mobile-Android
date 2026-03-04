package com.example.ros2_android_test_app.model.repositories.rosRepo.message;

import java.util.Objects;

public class Topic {
    public String name;
    public String type;

    public Topic() {}

    public Topic(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Topic topic = (Topic) o;
        return Objects.equals(name, topic.name) && Objects.equals(type, topic.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return name + " [" + type + "]";
    }
}
