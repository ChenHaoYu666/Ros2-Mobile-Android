package com.example.ros2_android_test_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class VizMapView extends View {
    private final Object lock = new Object();

    private int mapWidth = 0;
    private int mapHeight = 0;
    private float mapResolution = 0.05f;
    private float mapOriginX = 0f;
    private float mapOriginY = 0f;

    private Bitmap mapBitmap = null;
    private final RectF mapDstRect = new RectF();

    private final List<PointF> laserPoints = new ArrayList<>();

    private float robotX = 0f;
    private float robotY = 0f;
    private float robotYaw = 0f;

    private final Paint backgroundPaint = new Paint();
    private final Paint laserPaint = new Paint();
    private final Paint robotPaint = new Paint();
    private final Paint headingPaint = new Paint();
    private final Paint hintPaint = new Paint();

    private float meterToPixel = 30f;
    private float panX = 0f;
    private float panY = 0f;
    private boolean followRobot = true;

    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private boolean dragging = false;

    private final ScaleGestureDetector scaleDetector;
    private long lastInvalidateTs = 0L;

    public VizMapView(Context context) {
        super(context);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        init();
    }

    public VizMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        init();
    }

    public VizMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        init();
    }

    private void init() {
        backgroundPaint.setColor(Color.rgb(20, 20, 20));

        laserPaint.setColor(Color.GREEN);
        laserPaint.setStyle(Paint.Style.FILL);

        robotPaint.setColor(Color.CYAN);
        robotPaint.setStyle(Paint.Style.FILL);

        headingPaint.setColor(Color.RED);
        headingPaint.setStyle(Paint.Style.STROKE);
        headingPaint.setStrokeWidth(4f);

        hintPaint.setColor(Color.WHITE);
        hintPaint.setTextSize(28f);
        hintPaint.setAntiAlias(true);
    }

    public void setFollowRobot(boolean follow) {
        this.followRobot = follow;
        if (follow) {
            panX = 0f;
            panY = 0f;
        }
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        if (followRobot) {
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                dragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (dragging && !scaleDetector.isInProgress()) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    panX += dx;
                    panY += dy;
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    requestRender();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        synchronized (lock) {
            float viewCx = followRobot ? (cx - robotX * meterToPixel) : (cx + panX);
            float viewCy = followRobot ? (cy + robotY * meterToPixel) : (cy + panY);

            drawMap(canvas, viewCx, viewCy);
            drawLaser(canvas, viewCx, viewCy);
            drawRobot(canvas, viewCx, viewCy);

            if (mapBitmap == null) {
                canvas.drawText("等待 /map 数据...", 24f, 40f, hintPaint);
            }
        }
    }

    private void drawMap(Canvas canvas, float cx, float cy) {
        if (mapBitmap == null || mapWidth <= 0 || mapHeight <= 0 || mapResolution <= 0f) return;

        float left = cx + mapOriginX * meterToPixel;
        float top = cy - (mapOriginY + mapHeight * mapResolution) * meterToPixel;
        float right = left + mapWidth * mapResolution * meterToPixel;
        float bottom = top + mapHeight * mapResolution * meterToPixel;

        mapDstRect.set(left, top, right, bottom);
        canvas.drawBitmap(mapBitmap, null, mapDstRect, null);
    }

    private void drawLaser(Canvas canvas, float cx, float cy) {
        int count = laserPoints.size();
        int step = count > 1500 ? (count / 1500) : 1;
        float radius = Math.max(1.5f, meterToPixel * 0.015f);

        for (int i = 0; i < count; i += step) {
            PointF p = laserPoints.get(i);
            float worldX = robotX + p.x;
            float worldY = robotY + p.y;
            float sx = cx + worldX * meterToPixel;
            float sy = cy - worldY * meterToPixel;
            canvas.drawCircle(sx, sy, radius, laserPaint);
            }
        }

    private void drawRobot(Canvas canvas, float cx, float cy) {
        float rx = cx + robotX * meterToPixel;
        float ry = cy - robotY * meterToPixel;

        float robotRadius = Math.max(6f, meterToPixel * 0.12f);
        canvas.drawCircle(rx, ry, robotRadius, robotPaint);

        float headingLen = Math.max(14f, meterToPixel * 0.28f);
        float hx = (float) (rx + headingLen * Math.cos(robotYaw));
        float hy = (float) (ry - headingLen * Math.sin(robotYaw));
        canvas.drawLine(rx, ry, hx, hy, headingPaint);
    }

    public void updateOccupancyGrid(nav_msgs.msg.OccupancyGrid msg) {
        if (msg == null) return;

        synchronized (lock) {
            try {
                mapWidth = (int) msg.getInfo().getWidth();
                mapHeight = (int) msg.getInfo().getHeight();
                mapResolution = msg.getInfo().getResolution();
                mapOriginX = (float) msg.getInfo().getOrigin().getPosition().getX();
                mapOriginY = (float) msg.getInfo().getOrigin().getPosition().getY();

                int[] cells = convertToIntArray(msg.getData(), mapWidth * mapHeight);
                if (cells != null && mapWidth > 0 && mapHeight > 0) {
                    rebuildMapBitmap(cells, mapWidth, mapHeight);
                }
            } catch (Exception ignored) {
            }
        }

        requestRender();
    }

    private void rebuildMapBitmap(int[] cells, int width, int height) {
        if (cells == null || width <= 0 || height <= 0) return;

        int[] pixels = new int[width * height];
        int n = Math.min(pixels.length, cells.length);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int src = y * width + x;
                if (src >= n) continue;

                int value = cells[src];
                int color;
                if (value < 0) color = Color.rgb(80, 80, 80);
                else if (value >= 50) color = Color.rgb(25, 25, 25);
                else color = Color.rgb(235, 235, 235);

                int dstY = (height - 1 - y);
                pixels[dstY * width + x] = color;
            }
        }

        mapBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        mapBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    public void updateLaserScan(sensor_msgs.msg.LaserScan msg) {
        if (msg == null) return;

        float[] ranges = convertToFloatArray(msg.getRanges());
        if (ranges == null || ranges.length == 0) return;

        float angleMin = msg.getAngleMin();
        float angleIncrement = msg.getAngleIncrement();

        synchronized (lock) {
            laserPoints.clear();
            float angle = angleMin + robotYaw;
            for (float r : ranges) {
                if (Float.isNaN(r) || Float.isInfinite(r) || r <= 0f) {
                    angle += angleIncrement;
                    continue;
                }
                laserPoints.add(new PointF((float) (r * Math.cos(angle)), (float) (r * Math.sin(angle))));
                angle += angleIncrement;
            }
        }

        requestRender();
    }

    public void updateRobotPose(geometry_msgs.msg.PoseWithCovarianceStamped msg) {
        if (msg == null) return;

        synchronized (lock) {
            robotX = (float) msg.getPose().getPose().getPosition().getX();
            robotY = (float) msg.getPose().getPose().getPosition().getY();

            double qx = msg.getPose().getPose().getOrientation().getX();
            double qy = msg.getPose().getPose().getOrientation().getY();
            double qz = msg.getPose().getPose().getOrientation().getZ();
            double qw = msg.getPose().getPose().getOrientation().getW();
            robotYaw = (float) Math.atan2(2.0 * (qw * qz + qx * qy), 1.0 - 2.0 * (qy * qy + qz * qz));
        }

        requestRender();
    }

    private void requestRender() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastInvalidateTs >= 33) { // ~30 FPS cap
            lastInvalidateTs = now;
            postInvalidateOnAnimation();
    }
}

    private int[] convertToIntArray(Object data, int expectedSize) {
        if (data == null) return null;

        if (data instanceof byte[]) {
            byte[] arr = (byte[]) data;
            int[] out = new int[arr.length];
            for (int i = 0; i < arr.length; i++) out[i] = arr[i];
            return out;
        }

        if (data instanceof java.nio.ByteBuffer) {
            java.nio.ByteBuffer bb = ((java.nio.ByteBuffer) data).duplicate();
            int[] out = new int[bb.remaining()];
            for (int i = 0; i < out.length; i++) out[i] = bb.get();
            return out;
        }

        if (data instanceof List) {
            List<?> list = (List<?>) data;
            int[] out = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object v = list.get(i);
                out[i] = (v instanceof Number) ? ((Number) v).intValue() : -1;
            }
            return out;
        }

        try {
            Method sizeMethod = data.getClass().getMethod("size");
            Method getMethod = data.getClass().getMethod("get", int.class);
            int size = ((Number) sizeMethod.invoke(data)).intValue();
            int[] out = new int[size];
            for (int i = 0; i < size; i++) {
                Object v = getMethod.invoke(data, i);
                out[i] = (v instanceof Number) ? ((Number) v).intValue() : -1;
            }
            return out;
        } catch (Exception ignored) {
        }

        if (expectedSize > 0) {
            int[] out = new int[expectedSize];
            for (int i = 0; i < expectedSize; i++) out[i] = -1;
            return out;
        }
        return null;
    }

    private float[] convertToFloatArray(Object data) {
        if (data == null) return null;

        if (data instanceof float[]) return (float[]) data;

        if (data instanceof java.nio.FloatBuffer) {
            java.nio.FloatBuffer fb = ((java.nio.FloatBuffer) data).duplicate();
            float[] out = new float[fb.remaining()];
            fb.get(out);
            return out;
        }

        if (data instanceof List) {
            List<?> list = (List<?>) data;
            float[] out = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object v = list.get(i);
                out[i] = (v instanceof Number) ? ((Number) v).floatValue() : Float.NaN;
            }
            return out;
        }

        try {
            Method sizeMethod = data.getClass().getMethod("size");
            Method getMethod = data.getClass().getMethod("get", int.class);
            int size = ((Number) sizeMethod.invoke(data)).intValue();
            float[] out = new float[size];
            for (int i = 0; i < size; i++) {
                Object v = getMethod.invoke(data, i);
                out[i] = (v instanceof Number) ? ((Number) v).floatValue() : Float.NaN;
            }
            return out;
        } catch (Exception ignored) {
        }

        return null;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            meterToPixel *= detector.getScaleFactor();
            meterToPixel = Math.max(5f, Math.min(meterToPixel, 300f));
            requestRender();
            return true;
        }
    }
}
