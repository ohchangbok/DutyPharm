package com.alanjeon.dutypharm;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 12. 7. 6. Time: 오후 5:34 To
 * change this template use File | Settings | File Templates.
 */
public class AboutActivity extends Activity {

    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        PackageManager pm = getPackageManager();
        String versionName;
        try {
            versionName = pm.getPackageInfo(getPackageName(), 0).versionName;

            TextView content = (TextView) findViewById(R.id.content);
            content.setText(
                Html.fromHtml(getString(R.string.about_body, versionName)));
            content.setMovementMethod(LinkMovementMethod.getInstance());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this); // Add this method.
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this); // Add this method.
    }
}