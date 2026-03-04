package com.example.ros2_android_test_app.ui.fragments.viz;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.example.ros2_android_test_app.model.config.WidgetConfig;

public class WidgetViewGroup extends ViewGroup {
    public static final String TAG = "WidgetViewGroup";
    public static final int TILES_X = 20;
    private int tilesX = TILES_X;
    private int tilesY;
    private float tileWidth;
    
    private Paint gridPaint;
    private Paint borderPaint;
    private Paint handlePaint;
    private Paint axisPaint;
    private Paint textPaint;
    
    private boolean isEditMode = false;
    
    // 触摸操作状态
    private enum ActionMode { NONE, MOVE, RESIZE_RIGHT, RESIZE_BOTTOM, RESIZE_CORNER }
    private ActionMode currentMode = ActionMode.NONE;
    private View targetView = null;
    private float lastX, lastY;
    private int initialPosX, initialPosY, initialWidth, initialHeight;
    private float dragAccumX = 0f;
    private float dragAccumY = 0f;
    
    private static final int EDGE_THRESHOLD = 70; // 边框感应范围（像素）

    public WidgetViewGroup(Context context) {
        super(context);
        init();
    }

    public WidgetViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0x4000BCD4); 
        gridPaint.setStrokeWidth(2);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(0xFF00BCD4);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6);
        borderPaint.setPathEffect(new DashPathEffect(new float[]{20, 15}, 0));

        handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handlePaint.setColor(0xFF00BCD4);
        handlePaint.setStyle(Paint.Style.FILL);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setStrokeWidth(4);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(30);

        setWillNotDraw(false);
        
        // 移除旧的系统拖拽监听，改用自定义 onTouchEvent 实现
        setOnDragListener(null);
    }

    public void setEditMode(boolean enabled) {
        this.isEditMode = enabled;
        invalidate();
    }

    public interface OnWidgetMovedListener {
        void onMoved(WidgetConfig config);
    }

    private OnWidgetMovedListener onWidgetMovedListener;

    public void setOnWidgetMovedListener(OnWidgetMovedListener listener) {
        this.onWidgetMovedListener = listener;
    }

    private void calculateTiles(int availableWidth, int availableHeight) {
        if (availableWidth <= 0 || availableHeight <= 0) return;
        tileWidth = (float) availableWidth / TILES_X;
        tilesY = (int) (availableHeight / tileWidth);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEditMode) return false;

        // 编辑模式下：只要按下位置命中任意子控件，就由父容器接管
        // 目标交互：
        // - 中心区域拖拽：移动
        // - 虚线边框区域拖拽：缩放
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            final float downX = ev.getX();
            final float downY = ev.getY();

            for (int i = getChildCount() - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (child.getVisibility() != VISIBLE) continue;

                Rect rect = new Rect();
                child.getHitRect(rect);
                
                // 扩大一点命中范围，便于点到边框
                Rect touchRect = new Rect(rect);
                touchRect.inset(-20, -20);

                if (!touchRect.contains((int) downX, (int) downY)) continue;

                    targetView = child;
                    WidgetConfig cfg = (WidgetConfig) child.getTag();
                if (cfg == null) {
                    targetView = null;
                    continue;
                }

                    initialPosX = cfg.posX;
                    initialPosY = cfg.posY;
                    initialWidth = cfg.width;
                    initialHeight = cfg.height;
                lastX = downX;
                lastY = downY;
                dragAccumX = 0f;
                dragAccumY = 0f;

                // 边框区域：距离四条边在阈值内（优先）；否则为中心移动
                boolean nearLeft = Math.abs(downX - rect.left) < EDGE_THRESHOLD;
                boolean nearRight = Math.abs(downX - rect.right) < EDGE_THRESHOLD;
                boolean nearTop = Math.abs(downY - rect.top) < EDGE_THRESHOLD;
                boolean nearBottom = Math.abs(downY - rect.bottom) < EDGE_THRESHOLD;

                if ((nearRight && nearBottom) || (nearLeft && nearBottom) || (nearRight && nearTop) || (nearLeft && nearTop)) {
                        currentMode = ActionMode.RESIZE_CORNER;
                } else if (nearRight || nearLeft) {
                        currentMode = ActionMode.RESIZE_RIGHT;
                } else if (nearBottom || nearTop) {
                        currentMode = ActionMode.RESIZE_BOTTOM;
                    } else {
                        currentMode = ActionMode.MOVE;
                    }
                    
                // 强制父容器接管后续事件，避免子控件（摇杆）继续收到触摸
                requestDisallowInterceptTouchEvent(true);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEditMode || targetView == null) return super.onTouchEvent(event);

        WidgetConfig cfg = (WidgetConfig) targetView.getTag();
        if (cfg == null) return false;

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - lastX;
                float dy = y - lastY;

                if (currentMode == ActionMode.MOVE) {
                    // 中心拖拽逻辑：即时计算网格位移
                    int gridDx = Math.round(dx / tileWidth);
                    int gridDy = -Math.round(dy / tileWidth); // 坐标系 Y 轴向上为正
                    
                    cfg.posX = Math.max(0, Math.min(initialPosX + gridDx, TILES_X - cfg.width));
                    cfg.posY = Math.max(0, initialPosY + gridDy);
                    
                } else {
                    // 边框缩放逻辑：即时调整网格宽度/高度
                    int gridDw = Math.round(dx / tileWidth);
                    int gridDh = -Math.round(dy / tileWidth);

                    if (currentMode == ActionMode.RESIZE_RIGHT || currentMode == ActionMode.RESIZE_CORNER) {
                        cfg.width = Math.max(1, Math.min(initialWidth + gridDw, TILES_X - cfg.posX));
                    }
                    if (currentMode == ActionMode.RESIZE_BOTTOM || currentMode == ActionMode.RESIZE_CORNER) {
                        cfg.height = Math.max(1, initialHeight + gridDh);
                    }
                }
                requestLayout();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 停止操作，持久化保存
                if (onWidgetMovedListener != null) {
                    onWidgetMovedListener.onMoved(cfg);
                }
                currentMode = ActionMode.NONE;
                targetView = null;
                break;
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        calculateTiles(width - getPaddingLeft() - getPaddingRight(), height - getPaddingTop() - getPaddingBottom());

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            Object tag = child.getTag();
            if (tag instanceof WidgetConfig) {
                WidgetConfig cfg = (WidgetConfig) tag;
                int childWidth = Math.max(1, (int) (cfg.width * tileWidth));
                int childHeight = Math.max(1, (int) (cfg.height * tileWidth));
                child.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                             MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            Object tag = child.getTag();
            if (tag instanceof WidgetConfig) {
                WidgetConfig cfg = (WidgetConfig) tag;
                int left = (int) (getPaddingLeft() + cfg.posX * tileWidth);
                int top = (int) (getPaddingTop() + (tilesY - (cfg.posY + cfg.height)) * tileWidth);
                child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // 画背景网格
        if (isEditMode && tileWidth > 0) {
            for (float x = getPaddingLeft(); x <= getWidth(); x += tileWidth)
                canvas.drawLine(x, 0, x, getHeight(), gridPaint);
            for (float y = getPaddingTop(); y <= getHeight(); y += tileWidth)
                canvas.drawLine(0, y, getWidth(), y, gridPaint);

            // 坐标轴：以左下角为 (0,0)
            float originX = getPaddingLeft();
            float originY = getHeight() - getPaddingBottom();

            // X 轴（红色）
            axisPaint.setColor(0xFFFF5252);
            canvas.drawLine(originX, originY, getWidth(), originY, axisPaint);

            // Y 轴（绿色）
            axisPaint.setColor(0xFF69F0AE);
            canvas.drawLine(originX, originY, originX, 0, axisPaint);

            // 绘制箭头
            float arrowSize = 20;
            // X 轴箭头
            android.graphics.Path xPath = new android.graphics.Path();
            xPath.moveTo(getWidth(), originY);
            xPath.lineTo(getWidth() - arrowSize, originY - arrowSize / 2);
            xPath.lineTo(getWidth() - arrowSize, originY + arrowSize / 2);
            xPath.close();
            axisPaint.setColor(0xFFFF5252);
            axisPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(xPath, axisPaint);
            canvas.drawText("X", getWidth() - 30, originY - 25, textPaint);

            // Y 轴箭头
            android.graphics.Path yPath = new android.graphics.Path();
            yPath.moveTo(originX, 0);
            yPath.lineTo(originX - arrowSize / 2, arrowSize);
            yPath.lineTo(originX + arrowSize / 2, arrowSize);
            yPath.close();
            axisPaint.setColor(0xFF69F0AE);
            canvas.drawPath(yPath, axisPaint);
            canvas.drawText("Y", originX + 25, 30, textPaint);

            // 还原 axisPaint 样式
            axisPaint.setStyle(Paint.Style.STROKE);

            // 原点标注
            canvas.drawText("(0,0)", originX + 8, originY - 8, textPaint);

            // 刻度标注（每 1 格标一次）
            int maxX = TILES_X;
            for (int i = 1; i < maxX; i++) {
                float xPos = originX + i * tileWidth;
                canvas.drawText(String.valueOf(i), xPos + 4, originY - 8, textPaint);
            }

            int maxY = tilesY;
            for (int j = 1; j < maxY; j++) {
                float yPos = originY - j * tileWidth;
                canvas.drawText(String.valueOf(j), originX + 8, yPos - 4, textPaint);
            }
        }
        
        super.dispatchDraw(canvas);

        if (isEditMode) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() != VISIBLE) continue;

                // 绘制虚线框作为缩放感应区的视觉引导
                canvas.drawRect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom(), borderPaint);

                // 绘制右下角视觉提示
                int s = 30;
                canvas.drawRect(child.getRight() - s, child.getBottom() - s, child.getRight(), child.getBottom(), handlePaint);

                // 在编辑模式下绘制控件名称
                WidgetConfig cfg = (WidgetConfig) child.getTag();
                if (cfg != null && cfg.name != null) {
                    String nameToDraw = cfg.name;
                    // 在控件上方绘制名称，背景半透明黑色，文字白色
                    float textWidth = textPaint.measureText(nameToDraw);
                    float textHeight = textPaint.getTextSize();
                    float padding = 8;
                    float rectLeft = child.getLeft();
                    float rectTop = child.getTop() - textHeight - padding * 2;
                    
                    // 防止画出边界
                    if (rectTop < 0) {
                        rectTop = child.getTop() + padding;
                    }

                    Paint bgPaint = new Paint();
                    bgPaint.setColor(0xAA000000); // 半透明黑
                    canvas.drawRect(rectLeft, rectTop, rectLeft + textWidth + padding * 2, rectTop + textHeight + padding * 2, bgPaint);
                    canvas.drawText(nameToDraw, rectLeft + padding, rectTop + textHeight + padding, textPaint);
                }
            }
        }
    }
}
