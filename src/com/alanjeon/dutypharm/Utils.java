package com.alanjeon.dutypharm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 12. 6. 12. Time: 오전 10:26 To
 * change this template use File | Settings | File Templates.
 */
public class Utils {

    public static boolean isEmpty(String str) {
        if (str == null || str.length() == 0) {
            return true;
        }

        return false;
    }

    public static float getDistance(Pharm start, Pharm end) {
        float[] distance = new float[2];
        float result = -1f;

        if (start.mLat >= 0f && start.mLon >= 0f && end.mLat >= 0f
            && end.mLon >= 0f) {

            Location.distanceBetween(start.mLat, start.mLon, end.mLat, end.mLon,
                distance);
            result = distance[0];
        }

        return result;
    }

    public static boolean isDebug(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), 0);
            return ((pi.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return true;
        }
    }

}
