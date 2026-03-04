package com.example.ros2_android_test_app.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatButton;

public class ButtonView extends AppCompatButton {

    public interface OnButtonStateListener {
        void onStateChanged(boolean pressed);
    }

    private OnButtonStateListener listener;

    public ButtonView(Context context) {
        super(context);
    }

    public ButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnButtonStateListener(OnButtonStateListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (listener != null) listener.onStateChanged(true);
                setPressed(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (listener != null) listener.onStateChanged(false);
                setPressed(false);
                break;
        }
        return true;
    }
}
