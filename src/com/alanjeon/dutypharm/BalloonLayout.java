package com.alanjeon.dutypharm;

/**
 * Created with IntelliJ IDEA.
 * User: skyisle
 * Date: 12. 6. 21.
 * Time: 오후 1:18
 * To change this template use File | Settings | File Templates.
 */

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class BalloonLayout extends FrameLayout {

    private final ViewGroup layout;

    private final TextView title;
    //private final TextView message;

    public BalloonLayout(Context context, int balloonBottomOffset) {
        super(context);

        setPadding(10, 0, 10, balloonBottomOffset);

        // FIXME reuse the view
        final LayoutInflater inflater = (LayoutInflater) context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        layout = (ViewGroup) inflater.inflate(R.layout.balloon, null, false);
        title = (TextView) layout.findViewById(R.id.title);
        title.setTextColor(Color.WHITE);

        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.NO_GRAVITY;
        addView(layout, params);
    }

    /**
     * Sets the text shown into the balloon.
     */
    public void setText(String text) {
        layout.setVisibility(VISIBLE);
        if (text != null) {
            title.setVisibility(VISIBLE);
            title.setText(text);

        } else {
            title.setVisibility(GONE);
        }
    }
}
