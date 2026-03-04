package com.example.ros2_android_test_app;

import android.util.Log;

import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.node.Node;

public class GraphNode extends BaseComposableNode {

    private static final String TAG = GraphNode.class.getSimpleName();

    public GraphNode(final String name) {
        super(name);
        Log.i(TAG, "Creating GraphNode with name=" + name);
    }

    public Node getNode() {
        return node;
    }
}
