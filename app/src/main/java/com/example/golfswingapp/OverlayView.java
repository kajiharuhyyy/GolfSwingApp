package com.example.golfswingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gradPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path curvePath = new Path();
    private final List<PointF> points = new ArrayList<>();
    private OnPointAddedListener listener;
    private boolean inputEnabled = true;
    private int impactIndex = -1;
    private final Paint impactPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint impactRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int transitionIndex = -1;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setStrokeWidth(12f);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);

        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        linePaint.setAlpha(180);

        impactPaint.setStyle(Paint.Style.FILL);
        impactPaint.setStrokeWidth(12f);
        impactPaint.setColor(0xFFFF3B30);

        impactRingPaint.setStyle(Paint.Style.STROKE);
        impactRingPaint.setStrokeWidth(8f);
        impactRingPaint.setColor(0xFFFFFFFF);

        gradPaint.setStyle(Paint.Style.STROKE);
        gradPaint.setStrokeWidth(8f);
        gradPaint.setStrokeCap(Paint.Cap.ROUND);
        gradPaint.setStrokeJoin(Paint.Join.ROUND);

    }
    public void setInputEnabled(boolean enabled) {
        inputEnabled = enabled;
        setClickable(enabled);
        setFocusable(enabled);
    }

    public void markImpactAsLast() {
        if (points.isEmpty()) {
            impactIndex = -1;
        } else {
            impactIndex = points.size() - 1;
        }
        invalidate();
    }
    private int lerpColor(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (android.graphics.Color.alpha(c1) + (android.graphics.Color.alpha(c2) - android.graphics.Color.alpha(c1)) * t);
        int r = (int) (android.graphics.Color.red(c1)   + (android.graphics.Color.red(c2)   - android.graphics.Color.red(c1))   * t);
        int g = (int) (android.graphics.Color.green(c1) + (android.graphics.Color.green(c2) - android.graphics.Color.green(c1)) * t);
        int b = (int) (android.graphics.Color.blue(c1)  + (android.graphics.Color.blue(c2)  - android.graphics.Color.blue(c1))  * t);
        return android.graphics.Color.argb(a, r, g, b);
    }

    public void markTransitionAsLast() {
        transitionIndex = points.isEmpty() ? -1 : points.size() - 1;
        invalidate();
    }

    public int getTransitionIndex() { return transitionIndex; }

    public void undo() {
        if (!points.isEmpty()) {
            points.remove(points.size() - 1);

            if (impactIndex >= points.size()) {
                impactIndex = points.isEmpty() ? -1 : points.size() - 1;
            }
            invalidate();
        }
    }

    public void clear() {
        points.clear();
        invalidate();
    }
    public interface OnPointAddedListener {
        void onPointAdded(float x, float y);
    }

    public void setOnPointAddedListener(OnPointAddedListener l) {
        this.listener = l;
    }

    private Path buildSplinePath(List<PointF> pts) {
        Path path = new Path();
        if (pts.size() < 2) return path;

        PointF p0 = pts.get(0);
        path.moveTo(p0.x, p0.y);

        if (pts.size() == 2) {
            PointF p1 = pts.get(1);
            path.lineTo(p1.x, p1.y);
            return path;
        }

        for (int i = 0; i < pts.size() - 1; i++) {
            PointF p_1 = (i - 1 >= 0) ? pts.get(i - 1) : pts.get(i);
            PointF p1  = pts.get(i);
            PointF p2  = pts.get(i + 1);
            PointF p3  = (i + 2 < pts.size()) ? pts.get(i + 2) : pts.get(i + 1);

            float c1x = p1.x + (p2.x - p_1.x) / 6f;
            float c1y = p1.y + (p2.y - p_1.y) / 6f;
            float c2x = p2.x - (p3.x - p1.x) / 6f;
            float c2y = p2.y - (p3.y - p1.y) / 6f;

            path.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y);
        }
        return path;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (points.size() >= 2) {
            if (transitionIndex <= 0 || transitionIndex >= points.size()-1) {
                // 切り返し未設定 or 端すぎる → 1本
                gradPaint.setColor(0xFF2F80ED); // 好きな色
                canvas.drawPath(buildSplinePath(points), gradPaint);
            } else {
                // 2本に分割
                List<PointF> pre  = points.subList(0, transitionIndex + 1);
                List<PointF> post = points.subList(transitionIndex, points.size()); // ★つながりのため transitionIndex から

                gradPaint.setColor(0xFF2F80ED); // pre: 青
                canvas.drawPath(buildSplinePath(pre), gradPaint);

                gradPaint.setColor(0xFFFF3B30); // post: 赤
                canvas.drawPath(buildSplinePath(post), gradPaint);
            }
        }



        // ここから下は今まで通り：点、インパクト表示、（必要ならスプラインも）
        for (PointF p : points) {
            canvas.drawCircle(p.x, p.y, 10f, pointPaint);
        }

        if (impactIndex >= 0 && impactIndex < points.size()) {
            PointF p = points.get(impactIndex);

            Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(8f);
            ring.setColor(0xFFFFFFFF);

            canvas.drawCircle(p.x, p.y, 22f, ring);
            canvas.drawCircle(p.x, p.y, 14f, impactPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!inputEnabled) return false;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            points.add(new PointF(x, y));
            invalidate();

            if (listener != null) {
                listener.onPointAdded(x, y);
            }
            return true;
        }

        return super.onTouchEvent(event);
    }
    public static class PointF {
        public final float x;
        public final float y;
        public PointF(float x, float y) { this.x = x; this.y = y; }
    }
}
