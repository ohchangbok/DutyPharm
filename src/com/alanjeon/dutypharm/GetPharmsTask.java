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

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 12. 6. 20. Time: 오전 10:30 To
 * change this template use File | Settings | File Templates.
 */
public class GetPharmsTask
    extends AsyncTask<String, String, List<Pharm>> {

    private static final String TAG = "GetPharmsTask";

    private Pharm114Helper mPharm114Helper;

    //private NMapReverseGeocoder mNMapGeocoder;
    private Geocoder mGeocoder;

    private boolean mRetry = false;

    public String mAddress;

    private WeakReference<OnPharmsSearchListener> mCallbackRef;

    private Location mCurrentPosition;

    public GetPharmsTask(Context ctx, Location currentPosition,
        OnPharmsSearchListener callback) {
        mPharm114Helper = new Pharm114Helper(ctx);

        mGeocoder = new Geocoder(ctx, Locale.KOREA);
        //mNMapGeocoder = new NMapReverseGeocoder(ctx, Constants.NMAP_KEY);
        mCurrentPosition = currentPosition;

        mCallbackRef = new WeakReference<OnPharmsSearchListener>(callback);
    }

    public void updateListener(OnPharmsSearchListener callback) {
        mCallbackRef = new WeakReference<OnPharmsSearchListener>(callback);
    }

    @Override
    protected List<Pharm> doInBackground(String... addrs) {
        mAddress = addrs[0] + " " + addrs[1] + " " + addrs[2];
        Log.d(TAG, "Try to search with " + addrs[0] + " " + addrs[1] + " "
            + addrs[2]);

        publishProgress(mAddress);

        List<Pharm> pharms = mPharm114Helper
            .getPharmList(addrs[0], addrs[1], addrs[2]);

        // 현재 동에 영업 중인 약국이 없는 경우 구 단위로 다시 한번 검색 시도
        if (pharms != null && pharms.size() == 0) {
            mAddress = addrs[0] + " " + addrs[1];
            mRetry = true;

            publishProgress(mAddress);

            Log.d(TAG, "Try to search again with " + addrs[0] + " "
                + addrs[1]);
            pharms = mPharm114Helper
                .getPharmList(addrs[0], addrs[1], "");
        }

        if (pharms == null) {
            return null;
        }

        for (Pharm pharm : pharms) {
            List<Address> address = null;
            try {
                // XX읍XX면
                address = mGeocoder
                    .getFromLocationName(pharm.mAddress, 5);
            } catch (IOException e) {
                continue;
            }
            float[] distance = new float[2];
            if (address != null && address.size() >= 1) {
                Address addr = address.get(0);
                pharm.mLat = addr.getLatitude();
                pharm.mLon = addr.getLongitude();
                String doName = Pharm114Helper.getDoName(addr.getAddressLine(0));
                pharm.mTel = Pharm114Helper
                    .reviseTel(doName, pharm.mTel);
                Location.distanceBetween(
                    mCurrentPosition.getLatitude(),
                    mCurrentPosition.getLongitude(),
                    pharm.mLat, pharm.mLon, distance);
                pharm.mDistance = distance[0];
            }

            Log.d(TAG, pharm.toString());
        }

        return pharms;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (mCallbackRef.get() != null) {
            mCallbackRef.get().onTargetAddressChanged(values[0], mRetry);
        }

        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(List<Pharm> pharms) {
        if (mCallbackRef.get() != null) {
            mCallbackRef.get().onFoundPharms(pharms, true);
        }
    }
}
