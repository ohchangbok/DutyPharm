package com.alanjeon.dutypharm;

import android.app.Application;
import android.util.Log;
import android.view.ViewConfiguration;

import com.bugsense.trace.BugSenseHandler;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 6/13/12 Time: 10:54 오후 To
 * change this template use File | Settings | File Templates.
 */
public class DutyPharmApplication extends Application {

    private static final String TAG = "DutyPharmApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        final long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
        final File httpCacheDir = new File(getCacheDir(), "http");
        try {
            Class.forName("android.net.http.HttpResponseCache")
                .getMethod("install", File.class, long.class)
                .invoke(null, httpCacheDir, httpCacheSize);
        } catch (Exception httpResponseCacheNotAvailable) {
            Log.d(TAG,
                "android.net.http.HttpResponseCache not available, probably because we're running on a pre-ICS "
                    + "version of Android. Using com.integralblue.httpresponsecache.HttpHttpResponseCache.",
                httpResponseCacheNotAvailable);
            try {
                com.integralblue.httpresponsecache.HttpResponseCache
                    .install(httpCacheDir, httpCacheSize);
            } catch (Exception e) {
                Log.e(TAG,
                    "Failed to set up com.integralblue.httpresponsecache.HttpResponseCache",
                    e);
            }
        }

        try {
            // force to use overflow menu
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class
                .getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }
    }
}
