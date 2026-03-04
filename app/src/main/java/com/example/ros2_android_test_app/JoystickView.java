package com.example.ros2_android_test_app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class JoystickView extends View {

    public interface OnJoystickMoveListener {
        void onValueChanged(float x, float y);
    }

    private Paint bgPaint;
    private Paint handlePaint;

    private float centerX;
    private float centerY;
    private float baseRadius;
    private float handleRadius;

    private float handleX;
    private float handleY;

    private OnJoystickMoveListener listener;

    public JoystickView(Context context) {
        super(context);
        init();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.LTGRAY);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAlpha(150);

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(Color.DKGRAY);
        handlePaint.setStyle(Paint.Style.FILL);

        setClickable(true);
    }

    public void setOnJoystickMoveListener(OnJoystickMoveListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        baseRadius = Math.min(w, h) / 2f * 0.9f;
        handleRadius = baseRadius * 0.4f;
        resetHandlePosition();
    }

    private void resetHandlePosition() {
        handleX = centerX;
        handleY = centerY;
        invalidate();
        if (listener != null) {
            listener.onValueChanged(0f, 0f);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 背景圆
        canvas.drawCircle(centerX, centerY, baseRadius, bgPaint);
        // 摇杆手柄
        canvas.drawCircle(handleX, handleY, handleRadius, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        float dx = x - centerX;
        float dy = y - centerY;

        double distance = Math.sqrt(dx * dx + dy * dy);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (distance > baseRadius) {
                    // 限制在圆内
                    dx = (float) (dx * baseRadius / distance);
                    dy = (float) (dy * baseRadius / distance);
                }
                handleX = centerX + dx;
                handleY = centerY + dy;
                invalidate();

                if (listener != null) {
                    // 归一化到 [-1, 1]
                    float normX = dx / baseRadius; // 左为正或右为正，这里按屏幕坐标：右为正
                    float normY = -dy / baseRadius; // 上为正（前进），下为负（后退）
                    listener.onValueChanged(normX, normY);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetHandlePosition();
                return true;
        }
        return super.onTouchEvent(event);
    }
}

