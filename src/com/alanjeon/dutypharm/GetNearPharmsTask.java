package com.alanjeon.dutypharm;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 12. 6. 20. Time: 오전 10:30 To
 * change this template use File | Settings | File Templates.
 */
public class GetNearPharmsTask extends AsyncTask<Double, String, List<Pharm>> {
    private static final String TAG = "GetNearPharmsTask";

    private GooglePlaceHelper mSearchHelper;
    private Geocoder mGeocoder;

    int mAccuracy;
    boolean mSensor;

    private WeakReference<OnPharmsSearchListener> mCallbackRef;
    private Location mCurrentPosition;

    public GetNearPharmsTask(Context ctx, int accuracy, boolean sensor,
        Location currentPosition, OnPharmsSearchListener callback) {
        mSearchHelper = new GooglePlaceHelper(ctx, Constants.PLACE_KEY);
        mGeocoder = new Geocoder(ctx, Locale.KOREA);

        mAccuracy = accuracy;
        mSensor = sensor;
        mCurrentPosition = currentPosition;

        mCallbackRef = new WeakReference<OnPharmsSearchListener>(callback);
    }

    public void updateListener(OnPharmsSearchListener callback) {
        mCallbackRef = new WeakReference<OnPharmsSearchListener>(callback);
    }

    @Override
    protected List<Pharm> doInBackground(Double... params) {

        List<Pharm> pharms = null;
        try {
            pharms = mSearchHelper.getPharmList(params[0], params[1],
                mAccuracy, mSensor);
        } catch (JSONException ignore) {
            ignore.printStackTrace();
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }

        // 현재 영역에 영업 중인 약국이 없는 경우 구 단위로 다시 한번 검색 시도
        if (pharms.size() == 0) {
            Log.d(TAG, "Try to search again with" + " lat = " + params[0]
                + " lon = " + params[1]);
            try {
                pharms = mSearchHelper.getPharmList(params[0], params[1],
                    mAccuracy + 1000, mSensor);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        for (Pharm pharm : pharms) {
            List<Address> address = null;
            try {
                address = mGeocoder.getFromLocationName(pharm.mAddress, 5);
            } catch (IOException e) {
                continue;
            }

            float[] distance = new float[2];
            if (address != null && address.size() >= 1) {
                Address addr = address.get(0);
                Location.distanceBetween(mCurrentPosition.getLatitude(),
                    mCurrentPosition.getLongitude(), addr.getLatitude(),
                    addr.getLongitude(), distance);
                pharm.mDistance = distance[0];
            }

            Log.d(TAG, pharm.toString());
        }

        return pharms;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(List<Pharm> pharms) {
        if (mCallbackRef.get() != null) {
            mCallbackRef.get().onFoundPharms(pharms, false);
        }
    }
}
