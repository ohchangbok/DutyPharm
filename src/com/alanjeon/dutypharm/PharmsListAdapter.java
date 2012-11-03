package com.alanjeon.dutypharm;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 12. 6. 13. Time: 오전 11:21 To
 * change this template use File | Settings | File Templates.
 */
class PharmsListAdapter extends ArrayAdapter<Pharm> {

    private static final Method ADD_ALL = findAddAll();
    LayoutInflater mLayoutInflater;
    int mResId;
    View.OnClickListener mCallClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String tel = (String) view.getTag();
            Intent call = new Intent(Intent.ACTION_DIAL);
            call.setData(Uri.parse("tel://" + tel));
            getContext().startActivity(call);
        }
    };

    public PharmsListAdapter(Context ctx) {
        super(ctx, R.layout.pharm_list_item);

        mLayoutInflater = LayoutInflater.from(ctx);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Method findAddAll() {
        try {
            Class cls = ArrayAdapter.class;
            Class[] params = new Class[] { Pharm[].class };
            return cls.getMethod("addAll", params);
        } catch (NoSuchMethodException unused) {
            // fall through
        }
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            View v = mLayoutInflater.inflate(R.layout.pharm_list_item, null);
            holder = new ViewHolder();

            holder.mName = (TextView) v.findViewById(R.id.name);
            holder.mAddress = (TextView) v.findViewById(R.id.address);
            holder.mTel = (TextView) v.findViewById(R.id.tel);
            holder.mTime = (TextView) v.findViewById(R.id.time);
            holder.mDistance = (TextView) v.findViewById(R.id.distance);
            //holder.mStatus = (TextView) v.findViewById(R.id.status);
            holder.mCall = (Button) v.findViewById(R.id.call);

            v.setTag(holder);
            convertView = v;
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Pharm pharm = getItem(position);
        holder.mName.setText(pharm.mName);
        holder.mAddress.setText(pharm.mAddress);
        holder.mTel.setText(pharm.mTel);
        holder.mTime.setText(Utils.isEmpty(pharm.mTime) ? getContext()
            .getString(R.string.not_checked_on_duty) : pharm.mTime);
        if (pharm.mDistance <= 0f) {
            holder.mDistance.setText(R.string.incorrect_address);
        } else {
            holder.mDistance.setText(String.valueOf((int) pharm.mDistance)
                + " m");
        }

        if (holder.mStatus != null) {
            if (pharm.mType == Pharm.TYPE_PHARM114) {
                Spannable span = (Spannable) new SpannableString(getContext()
                    .getString(R.string.on_duty_checked));
                span.setSpan(new ForegroundColorSpan(0xFF0B9E17), 0, span.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.mStatus.setText(span);
            } else {
                Spannable span = (Spannable) new SpannableString(getContext()
                    .getString(R.string.on_duty_not_checked));
                span.setSpan(new ForegroundColorSpan(0xFFFF1212), 0, span.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.mStatus.setText(span);
            }
        }

        holder.mCall.setFocusable(false);
        holder.mCall.setTag(pharm.mTel);
        holder.mCall.setOnClickListener(mCallClick);
        return convertView;
    }

    public void addAllCompat(Object[] items) {
        if (ADD_ALL != null) {
            try {
                ADD_ALL.invoke(this, new Object[] { items });
                return;
            } catch (InvocationTargetException unused) {
                // fall through
            } catch (IllegalAccessException unused) {
                // fall through
            }
        } else {
            for (int i = 0; i < items.length; i++) {
                add((Pharm) items[i]);
            }
        }
    }

    class ViewHolder {
        TextView mName;
        TextView mAddress;
        TextView mTel;
        TextView mTime;
        TextView mDistance;
        TextView mStatus;
        Button mCall;
    }
}
