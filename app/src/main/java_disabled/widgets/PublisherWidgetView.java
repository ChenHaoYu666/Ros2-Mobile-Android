package com.example.ros2_android_test_app.ui.views.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.ros2_android_test_app.model.repositories.rosRepo.node.BaseData;
import com.example.ros2_android_test_app.ui.general.DataListener;
import com.example.ros2_android_test_app.widgets.button.ButtonData;


/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 10.03.21
 */
public abstract class PublisherWidgetView extends WidgetView implements IPublisherView{

    public static String TAG = PublisherWidgetView.class.getSimpleName();

    private DataListener dataListener;


    public PublisherWidgetView(Context context) {
        super(context);
    }

    public PublisherWidgetView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public void publishViewData(BaseData data) {
        if(dataListener == null) return;

        data.setTopic(widgetEntity.topic);
        dataListener.onNewWidgetData(data);
    }

    @Override
    public void setDataListener(DataListener listener) {
        this.dataListener = listener;
    }

}
