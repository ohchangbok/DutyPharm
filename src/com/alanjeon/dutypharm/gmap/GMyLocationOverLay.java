package com.alanjeon.dutypharm.gmap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;

import com.alanjeon.dutypharm.OnLocationChanged;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 12. 6. 21. Time: 오후 5:56 To
 * change this template use File | Settings | File Templates.
 */
public class GMyLocationOverLay extends MyLocationOverlay {

    final OnLocationChanged mListener;

    final Drawable mTargetDrawable;

    private GeoPoint mTargetPoint;

    private Point mTmpPoint = new Point();

    public GMyLocationOverLay(Context context, MapView mapView,
        Drawable targetDrawable, OnLocationChanged callback) {
        super(context, mapView);
        mListener = callback;
        mTargetDrawable = boundCenter(targetDrawable);
    }

    public static Drawable boundCenter(Drawable d) {
        d.setBounds(d.getIntrinsicWidth() / -2, d.getIntrinsicHeight() / -2,
            d.getIntrinsicWidth() / 2, d.getIntrinsicHeight() / 2);
        return d;
    }

    @Override
    public synchronized void onLocationChanged(Location location) {
        super.onLocationChanged(location);

        if (mListener != null) {
            mListener.onLocationChanged(location);
        }
    }

    @Override
    public synchronized boolean draw(Canvas canvas, MapView mapView, boolean b,
        long l) {
        if (!isMyLocationEnabled()) {
            if (mTargetPoint != null) {
                mapView.getProjection().toPixels(mTargetPoint, mTmpPoint);
                canvas.save();
                canvas.translate(mTmpPoint.x, mTmpPoint.y);
                mTargetDrawable.draw(canvas);
                canvas.restore();
            }

            return true;
        } else {
            return super.draw(canvas, mapView, b, l);
        }
    }

    public void setTargetPoint(GeoPoint point) {
        mTargetPoint = point;
    }
}
