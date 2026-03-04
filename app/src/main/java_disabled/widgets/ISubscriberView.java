package com.example.ros2_android_test_app.ui.views.widgets;

import org.ros.internal.message.Message;


/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 10.03.21
 */
public interface ISubscriberView {

     void onNewMessage(Message message);
}
