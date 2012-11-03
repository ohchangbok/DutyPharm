package com.alanjeon.dutypharm.gmap;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 12. 6. 21. Time: 오후 1:32 To
 * change this template use File | Settings | File Templates.
 */
public class GestureDetectorOverlay extends Overlay
    implements GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener {

    private GestureDetector gestureDetector;

    private GestureDetector.OnGestureListener onGestureListener;

    private GestureDetector.OnDoubleTapListener onDoubleTapListener;


    public GestureDetectorOverlay(Context ctx) {
        gestureDetector = new GestureDetector(ctx, this);
        setOnGestureListener(this);
    }

    public GestureDetectorOverlay(Context ctx,
        GestureDetector.OnGestureListener onGestureListener,
        GestureDetector.OnDoubleTapListener onDoubleTapListener
    ) {
        this(ctx);
        setOnGestureListener(onGestureListener);
        this.onDoubleTapListener = onDoubleTapListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event, MapView mapView) {
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (onGestureListener != null) {
            return onGestureListener.onDown(e);
        }
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
        float velocityY) {
        if (onGestureListener != null) {
            return onGestureListener.onFling(e1, e2, velocityX, velocityY);
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (onGestureListener != null) {
            onGestureListener.onLongPress(e);
        }
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
        float distanceY) {
        if (onGestureListener != null) {
            onGestureListener.onScroll(e1, e2, distanceX, distanceY);
        }
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        if (onGestureListener != null) {
            onGestureListener.onShowPress(e);
        }
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (onGestureListener != null) {
            onGestureListener.onSingleTapUp(e);
        }
        return false;
    }

    public boolean isLongpressEnabled() {
        return gestureDetector.isLongpressEnabled();
    }

    public void setIsLongpressEnabled(boolean isLongpressEnabled) {
        gestureDetector.setIsLongpressEnabled(isLongpressEnabled);
    }

    public GestureDetector.OnGestureListener getOnGestureListener() {
        return onGestureListener;
    }

    public void setOnGestureListener(
        GestureDetector.OnGestureListener onGestureListener) {
        this.onGestureListener = onGestureListener;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (onDoubleTapListener != null) {
            onDoubleTapListener.onSingleTapConfirmed(e);
        }
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (onDoubleTapListener != null) {
            onDoubleTapListener.onDoubleTap(e);
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        if (onDoubleTapListener != null) {
            onDoubleTapListener.onDoubleTap(e);
        }
        return false;
    }
}
