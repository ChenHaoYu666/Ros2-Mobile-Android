package com.example.ros2_android_test_app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {
    private Paint basePaint;
    private Paint handlePaint;
    private float centerX, centerY;
    private float baseRadius, handleRadius;
    private float touchX, touchY;

    public interface OnJoystickChangeListener {
        void onValueChanged(float linearPercent, float angularPercent);
    }

    private OnJoystickChangeListener listener;

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setOnJoystickChangeListener(OnJoystickChangeListener listener) {
        this.listener = listener;
    }

    private void init() {
        basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        basePaint.setColor(Color.DKGRAY);
        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setAlpha(150);

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(Color.LTGRAY);
        handlePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        baseRadius = Math.min(w, h) / 3f;
        handleRadius = baseRadius / 2.5f;
        touchX = centerX;
        touchY = centerY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint);
        canvas.drawCircle(touchX, touchY, handleRadius, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float dx = event.getX() - centerX;
        float dy = event.getY() - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (event.getAction() == MotionEvent.ACTION_UP) {
            touchX = centerX;
            touchY = centerY;
        } else {
            if (dist > baseRadius) {
                touchX = centerX + (dx / dist) * baseRadius;
                touchY = centerY + (dy / dist) * baseRadius;
            } else {
                touchX = event.getX();
                touchY = event.getY();
            }
        }

        invalidate();

        if (listener != null) {
            // ROS 约定：前向为正 (Y负方向)，左转为正 (X负方向)
            float linearPercent = -(touchY - centerY) / baseRadius;
            float angularPercent = -(touchX - centerX) / baseRadius;
            listener.onValueChanged(linearPercent, angularPercent);
        }
        return true;
    }
}
