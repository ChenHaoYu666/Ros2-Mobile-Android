package com.example.ros2_android_test_app.widgets.viz2d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.example.ros2_android_test_app.R;
import com.example.ros2_android_test_app.model.entities.widgets.BaseEntity;
import com.example.ros2_android_test_app.model.repositories.rosRepo.message.RosData;
import com.example.ros2_android_test_app.model.repositories.rosRepo.node.BaseData;
import com.example.ros2_android_test_app.ui.general.DataListener;
import com.example.ros2_android_test_app.ui.opengl.visualisation.VisualizationView;
import com.example.ros2_android_test_app.ui.views.widgets.LayerView;
import com.example.ros2_android_test_app.ui.views.widgets.PublisherLayerView;
import com.example.ros2_android_test_app.ui.views.widgets.WidgetGroupView;


/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.0.0
 * @created on 08.03.21
 */
public class Viz2DView extends WidgetGroupView {

    public static final String TAG = Viz2DView.class.getSimpleName();

    private DataListener dataListener;
    private Paint borderPaint;
    private Paint paintBackground;
    private VisualizationView layerView;
    private int border = 4;

    public Viz2DView(Context context) {
        super(context);
        init();
    }

    public Viz2DView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        layerView.layout(border, border, getWidth()-border, getHeight()-border);
    }


    private void init() {
        // Border color painted as Background
        int borderColor = getContext().getResources().getColor(R.color.borderColor);
        paintBackground = new Paint();
        paintBackground.setColor(borderColor);
        paintBackground.setStyle(Paint.Style.FILL);

        layerView = new VisualizationView(getContext());
        this.addView(layerView);
    }

    @Override
    public void setWidgetEntity(BaseEntity widgetEntity) {
        super.setWidgetEntity(widgetEntity);

        layerView.getCamera().jumpToFrame(((Viz2DEntity)widgetEntity).frame);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return layerView.onTouchEvent(event);
    }


    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawPaint(paintBackground);
        super.onDraw(canvas);
    }


    @Override
    public void onNewData(RosData data) {
        layerView.onNewData(data);
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

    @Override
    public void addLayer(LayerView layer) {
        if (layer instanceof PublisherLayerView) {
            ((PublisherLayerView)layer).setDataListener(data -> {
                if (dataListener != null) dataListener.onNewWidgetData(data);
            });
        }

        layerView.addLayer(layer);
    }
}