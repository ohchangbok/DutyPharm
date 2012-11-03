package com.alanjeon.dutypharm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 6/10/12 Time: 9:26 오후 To
 * change this template use File | Settings | File Templates.
 */
public class Pharm implements Parcelable {
    @SuppressWarnings("unused")
    private static final String TAG = "Pharm";

    public final static int TYPE_PHARM114 = 0;
    public final static int TYPE_GOOGLE = 1;

    public int mType;

    // google place specific
    public String mRef;
    public String mName;
    public String mAddress;
    public String mTel;
    public String mTime;
    public String mDesc;
    public double mLat;
    public double mLon;
    public float mDistance;

    public static final Parcelable.Creator<Pharm> CREATOR = new Parcelable.Creator<Pharm>() {
        public Pharm createFromParcel(Parcel in) {
            return new Pharm(in);
        }

        public Pharm[] newArray(int size) {
            return new Pharm[size];
        }
    };

    public Pharm() {
    }

    public Pharm(Parcel in) {
        mType = in.readInt();
        mRef = in.readString();
        mName = in.readString();
        mAddress = in.readString();
        mTel = in.readString();
        mTime = in.readString();
        mDesc = in.readString();
        mLat = in.readDouble();
        mLon = in.readDouble();
        mDistance = in.readFloat();
    }

    public int describeContents() {
        return 0;
    }

    private String notNullStr(String str) {
        if (str == null) {
            return "";
        } else {
            return str;
        }
    }

    @Override
    public String toString() {
        return "Pharm mType : "
            + mType
            // + " mRef : " + mRef
            + " mName : " + mName + " mAddress : " + mAddress + " mTel : "
            + mTel + " mTime : " + mTime + " mDesc : " + mDesc + " mLat : "
            + mLat + " mLon : " + mLon + " mDistance : " + mDistance;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mType);
        parcel.writeString(notNullStr(mRef));
        parcel.writeString(notNullStr(mName));
        parcel.writeString(notNullStr(mAddress));
        parcel.writeString(notNullStr(mTel));
        parcel.writeString(notNullStr(mTime));
        parcel.writeString(notNullStr(mDesc));
        parcel.writeDouble(mLat);
        parcel.writeDouble(mLon);
        parcel.writeFloat(mDistance);
    }

}
