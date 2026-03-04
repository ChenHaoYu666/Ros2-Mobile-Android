package com.example.ros2_android_test_app.model.config;

public class WidgetConfig {
    public String id;
    public String type; // "Joystick", "Button", etc.
    public String name; // User-editable display name
    public String topic = "/cmd_vel";
    public String topicType = "geometry_msgs/msg/Twist";
    public int rateHz = 10;
    
    // Common Publisher parameters
    public boolean immediatePublish = true;

    // Joystick specific parameters
    public float maxLinear = 1.0f;
    public float maxAngular = 1.0f;
    public String xAxisMapping = "Angular/Z";
    public String yAxisMapping = "Linear/X";
    public float xScaleLeft = 1.0f;
    public float xScaleRight = -1.0f;
    public float yScaleLeft = 1.0f;
    public float yScaleRight = -1.0f;

    // Button specific parameters
    public String text = "A Button";

    // Label specific parameters
    public String labelText = "No data";
    public int textSize = 18;

    // Map specific parameters
    public String mapTopic = "/map";
    public String laserTopic = "/scan";
    public String poseTopic = "/amcl_pose";
    public boolean mapFollowRobot = true;

    // Position and Size (aligned with ROS-Mobile)
    public int posX = 5;
    public int posY = 15;
    public int width = 10;
    public int height = 10;

    public WidgetConfig() {
        // Ensure defaults even for JSON without these fields
        this.posX = 5;
        this.posY = 15;
        this.width = 10;
        this.height = 10;
    }
    
    public WidgetConfig(String id, String type) {
        this(); // Initialize common defaults (pos/size)
        this.id = id;
        this.type = type;
        if ("Joystick".equals(type)) {
            this.topic = "/cmd_vel";
            this.topicType = "geometry_msgs/msg/Twist";
            this.rateHz = 20;
            this.immediatePublish = false; 
            this.yScaleLeft = 1.0f;
            this.yScaleRight = -1.0f;
        } else if ("Button".equals(type)) {
            this.topic = "/button_cmd";
            this.topicType = "std_msgs/msg/String";
            this.rateHz = 1;
            this.immediatePublish = true; 
            this.text = "Send Trigger";
        } else if ("Label".equals(type)) {
            this.topic = "/chatter";
            this.topicType = "std_msgs/msg/String";
            this.labelText = "Waiting for data...";
        } else if ("Map".equals(type)) {
            this.mapTopic = "/map";
            this.laserTopic = "/scan";
            this.poseTopic = "/amcl_pose";
            this.width = 16;
            this.height = 12;
        }
    }
}
