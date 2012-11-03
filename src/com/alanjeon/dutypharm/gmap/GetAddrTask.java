package com.alanjeon.dutypharm.gmap;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

import com.alanjeon.dutypharm.OnAddrFound;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 12. 6. 21. Time: 오후 2:57 To
 * change this template use File | Settings | File Templates.
 */
public class GetAddrTask extends AsyncTask<Double, String, List<Address>> {

    Geocoder mGeocoder;

    OnAddrFound mListener;

    public GetAddrTask(Context ctx, OnAddrFound listener) {
        mGeocoder = new Geocoder(ctx, Locale.KOREA);
        mListener = listener;
    }

    @Override
    protected List<Address> doInBackground(Double... params) {
        List<Address> results = null;
        try {
            results = mGeocoder.getFromLocation(params[0], params[1], 5);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    protected void onPostExecute(List<Address> addresses) {
        super.onPostExecute(addresses);

        if (addresses != null && addresses.size() > 0) {
            Address addr = addresses.get(0);
            mListener.onAddrFound(addr);
        } else {
            mListener.onAddrFound(null);
        }
    }
}
