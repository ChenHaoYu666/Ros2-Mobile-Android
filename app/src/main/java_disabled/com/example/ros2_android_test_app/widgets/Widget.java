package com.example.ros2_android_test_app.widgets;

import android.view.View;

import org.ros2.rcljava.executors.Executor;

public interface Widget {
    void bindView(View rootView);

    void onRos2Attached(Executor executor);

    void onRos2Detached(Executor executor);
}

