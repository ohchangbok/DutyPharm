package com.alanjeon.dutypharm;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockMapActivity;
import com.actionbarsherlock.view.MenuItem;
import com.alanjeon.dutypharm.gmap.GMyLocationOverLay;
import com.alanjeon.dutypharm.gmap.GestureDetectorOverlay;
import com.alanjeon.dutypharm.gmap.GetAddrTask;
import com.androidquery.service.MarketService;
import com.androidquery.util.AQUtility;
import com.bugsense.trace.BugSenseHandler;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class DutyPharmActivity extends SherlockMapActivity implements
    OnClickListener, OnPharmsSearchListener {
    class NonConfigurationInstance {

        GetPharmsTask mGetPharmsTask;

        GetNearPharmsTask mGetNearPharmsTask;
    }

    class PharmOveralyItem extends OverlayItem {

        Pharm mPharm;

        public PharmOveralyItem(GeoPoint geoPoint, String name, String snippet,
            Pharm info) {
            super(geoPoint, name, snippet);
            mPharm = info;
        }
    }

    class PharmsOverlay extends ItemizedOverlay<PharmOveralyItem> {
        private Context mContext;

        private ArrayList<PharmOveralyItem> mOverlaysItems = new ArrayList<PharmOveralyItem>();
        private BalloonLayout balloonView;
        private View mClickRegion;
        private int mBalloonViewOffset;
        private MapView mMapView;
        private Drawable mOfflineMarker;

        public PharmsOverlay(Drawable defaultMarker, Drawable offlineMarker,
            Context context, MapView mapView) {
            super(boundCenterBottom(defaultMarker));
            mContext = context;
            mMapView = mapView;
            mBalloonViewOffset = defaultMarker.getBounds().height();
            mOfflineMarker = offlineMarker;

            setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChanged(ItemizedOverlay itemizedOverlay,
                    OverlayItem overlayItem) {
                    if (overlayItem == null) {
                        balloonView.setVisibility(View.GONE);
                    }
                }
            });
        }

        public void addOverlayItem(PharmOveralyItem overlay) {
            if (overlay.mPharm.mType == Pharm.TYPE_GOOGLE) {
                overlay.setMarker(boundCenterBottom(mOfflineMarker));
            }
            mOverlaysItems.add(overlay);
            setLastFocusedIndex(-1);
            populate();
        }

        public void clear() {
            mOverlaysItems.clear();
            setLastFocusedIndex(-1);
            populate();
        }

        @Override
        protected PharmOveralyItem createItem(int i) {
            return mOverlaysItems.get(i);
        }

        public int find(Pharm pharm) {
            for (int idx = 0; idx < mOverlaysItems.size(); idx++) {
                PharmOveralyItem itemPharm = mOverlaysItems.get(idx);
                if (pharm.mName.equals(itemPharm.mPharm.mName)
                    && pharm.mAddress.equals(itemPharm.mPharm.mAddress)) {
                    return idx;
                }
            }

            return -1;
        }

        @Override
        protected boolean onTap(int i) {
            PharmOveralyItem item = mOverlaysItems.get(i);

            final GeoPoint point = item.getPoint();

            boolean isRecycled = true;
            if (balloonView == null) {
                balloonView = new BalloonLayout(mContext, mBalloonViewOffset);
                mClickRegion = balloonView.findViewById(R.id.btn);
                isRecycled = false;
            }

            balloonView.setVisibility(View.GONE);
            balloonView
                .setText(item.mPharm.mName
                    + "("
                    + (Utils.isEmpty(item.mPharm.mTime) ? getString(R.string.not_checked_on_duty)
                    : item.mPharm.mTime) + ")");

            MapView.LayoutParams params = new MapView.LayoutParams(
                MapView.LayoutParams.WRAP_CONTENT,
                MapView.LayoutParams.WRAP_CONTENT, point,
                MapView.LayoutParams.BOTTOM_CENTER);
            params.mode = MapView.LayoutParams.MODE_MAP;
            balloonView.findViewById(R.id.btn).setTag(item.mPharm);

            mClickRegion.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent ev) {
                    if (ev.getAction() == MotionEvent.ACTION_UP) {
                        Pharm pharm = (Pharm) v.getTag();
                        Intent call = new Intent(Intent.ACTION_DIAL);
                        call.setData(Uri.parse("tel://" + pharm.mTel));
                        startActivity(call);
                        return true;
                    }
                    return true;
                }

            });

            balloonView.setVisibility(View.VISIBLE);
            if (isRecycled) {
                balloonView.setLayoutParams(params);
            } else {
                mMapView.addView(balloonView, params);
            }
            mMapView.getController().animateTo(point);

            return true;
        }

        public void setFocus(int i) {
            PharmOveralyItem item = mOverlaysItems.get(i);
            if (item != null) {
                setFocus(item);
            }
        }

        @Override
        public int size() {
            return mOverlaysItems.size();
        }
    }

    private static final String TAG = "DutyPhamActivity";
    private MapView mMapView;
    private MapController mMapController;

    private GMyLocationOverLay mMyLocationOverlay;
    private PharmsOverlay mPharmsOverlay;

    private List<Overlay> mMapOverlays;
    private final static int FINDPLACE_AND_UPDATE_PHARMS = 0;
    private final static int FINDPLACE_AND_DONE = 1;
    private int mFindPlacemarkType = 0;

    private TextView mMapCenterAddressTextView;

    String mLocationName;
    public Location mCurrentLocation;
    private TreeMap<String, Pharm> mPharmsMap = new TreeMap<String, Pharm>();

    private ArrayList<Pharm> mPharms = new ArrayList<Pharm>();
    private PharmsListAdapter mAdapter;

    private ListView mListView;
    private GetPharmsTask mGetPharmsTask;
    private GetNearPharmsTask mGetNearPharmsTask;
    private static final String CONST_MAP_LOCATON = "map.location";
    private static final String CONST_MAP_ZOOM_LEVEL = "map.zoom.level";
    private static final String CONST_PHARMS = "pharms.on.map";
    private static final String CONST_LOCATION_ADDR1 = "location.addr1";
    private static final String CONST_LOCATION_ADDR2 = "location.addr2";
    private static final String CONST_LOCATION_ADDR3 = "location.addr3";

    private static final String CONST_LOCATION_NAME = "location.name";

    private static final String CONST_LOCATION_LAT = "location.lat";

    private static final String CONST_LOCATION_LON = "location.lon";

    private com.actionbarsherlock.view.Menu mOptionsMenu;

    private TextView mNotificationText;

    GestureDetector.SimpleOnGestureListener mMapGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mMapController.zoomIn();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

            stopMyLocation();

            GeoPoint point = mMapView.getProjection().fromPixels(
                (int) e.getX(), (int) e.getY());

            findPlacemarkAtLocationType(FINDPLACE_AND_UPDATE_PHARMS,
                point.getLongitudeE6(), point.getLatitudeE6());

            setCurrentPoint(point, false);

            SharedPreferencesCompat.apply(getPreferences(Context.MODE_PRIVATE)
                .edit()
                .putString(CONST_LOCATION_LAT,
                    Integer.toString(point.getLatitudeE6()))
                .putString(CONST_LOCATION_LON,
                    Integer.toString(point.getLongitudeE6()))
                .putInt(CONST_MAP_ZOOM_LEVEL, mMapView.getZoomLevel()));
        }
    };

    private OnAddrFound mOnAddrFound = new OnAddrFound() {
        public void onAddrFound(Address addr) {
            if (addr == null) {

                Log.e(TAG, "Failed to findPlacemarkAtLocation");

                showNotification(R.string.couldnt_find_address);
                return;
            }

            String addr1 = "", addr2 = "", addr3 = "";
            // addr1 = addr.getAdminArea();
            // if (addr1 == null) {
            // addr1 = addr.getLocality();
            // addr2 = addr.getSubLocality();
            // addr3 = addr.getThoroughfare();
            // } else {
            // addr2 = addr.getLocality() + " " + addr.getSubLocality();
            // addr3 = addr.getThoroughfare();
            // }
            String addrLine = addr.getAddressLine(0);
            String arrayAddr[] = addrLine.split(" ");
            if (arrayAddr.length > 4) {
                addr1 = arrayAddr[1];
                addr2 = arrayAddr[2];
                addr3 = arrayAddr[3];
            } else if (arrayAddr.length > 3) {
                addr1 = arrayAddr[1];
                addr2 = arrayAddr[2];
            }

            String addrFormat = String.format("%s %s %s", addr1, addr2, addr3);

            SharedPreferencesCompat.apply(getPreferences(Context.MODE_PRIVATE)
                .edit().putString(CONST_LOCATION_ADDR1, addr1)
                .putString(CONST_LOCATION_ADDR2, addr2)
                .putString(CONST_LOCATION_ADDR3, addr3));

            mMapCenterAddressTextView.setText(addrFormat);

            showNotification(getString(R.string.searching, addrFormat));
            Log.d(TAG, "onAddrFound + " + addrFormat);

            if (mFindPlacemarkType == FINDPLACE_AND_UPDATE_PHARMS) {

                mListView.setVisibility(View.GONE);

                startGetPharmTaskIfNeeded(addr1, addr2, addr3);

            } else if (mFindPlacemarkType == FINDPLACE_AND_DONE) {
                // do nothing
            }

        }
    };

    OnLocationChanged mLocationChangedListener = new OnLocationChanged() {

        @Override
        public void onLocationChanged(Location loc) {

            stopMyLocation();

            GeoPoint point = new GeoPoint((int) (loc.getLatitude() * 1E6),
                (int) (loc.getLongitude() * 1E6));

            mMapController.setCenter(point);
            findPlacemarkAtLocationType(FINDPLACE_AND_UPDATE_PHARMS,
                point.getLongitudeE6(), point.getLatitudeE6());

            setCurrentPoint(point, true);

            SharedPreferencesCompat.apply(getPreferences(Context.MODE_PRIVATE)
                .edit()
                .putString(CONST_LOCATION_LAT,
                    Integer.toString(point.getLatitudeE6()))
                .putString(CONST_LOCATION_LON,
                    Integer.toString(point.getLongitudeE6()))
                .putInt(CONST_MAP_ZOOM_LEVEL, mMapView.getZoomLevel()));
        }
    };

    private AdapterView.OnItemClickListener mOnItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i,
            long l) {
            Pharm pharm = (Pharm) adapterView.getItemAtPosition(i);
            if (pharm != null) {
                // mPharmsOverlay.s.selectPOIitemBy(pharm, true);
                int idx = mPharmsOverlay.find(pharm);
                if (idx != -1) {
                    mPharmsOverlay.onTap(idx);
                    mPharmsOverlay.setFocus(idx);
                }

            }
        }
    };

    private Object mLock = new Object();

    private final static Comparator<Pharm> COMPARATOR = new Comparator<Pharm>() {
        @Override
        public int compare(Pharm object1, Pharm object2) {
            // If no dispance info exist, set priority low.
            if (object1.mDistance <= 0f && object2.mDistance > 0f) {
                return (int) (Integer.MAX_VALUE - object2.mDistance);
            }

            if (object1.mDistance > 0f && object2.mDistance <= 0f) {
                return (int) (object1.mDistance - Integer.MIN_VALUE);
            }

            if (object1.mType == Pharm.TYPE_PHARM114
                && object2.mType == Pharm.TYPE_GOOGLE) {
                return -1;
            } else if (object1.mType == Pharm.TYPE_GOOGLE
                && object2.mType == Pharm.TYPE_PHARM114) {
                return 1;
            }

            return (int) (object1.mDistance - object2.mDistance);
        }
    };

    // Transient properties
    private List<String> mPendingNotification = new ArrayList<String>();

    Runnable notificationRunable = new Runnable() {
        @Override
        public void run() {
            if (mPendingNotification.size() > 0) {
                animate(mNotificationText).alpha(1);
                mNotificationText.setText(mPendingNotification.remove(0));
                mNotificationText.setVisibility(View.VISIBLE);
                mNotificationText.postDelayed(this, 2000);
            } else {
                animate(mNotificationText).alpha(0).setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mNotificationText.setVisibility(View.GONE);
                        }
                    });
            }
        }
    };

    private void addPharms(List<Pharm> pharms) {
        updateProgress();

        if (pharms == null || pharms.size() == 0) {
            return;
        }

        synchronized (mLock) {
            for (Pharm pharm : pharms) {
                // set POI data

                Pharm prevPharm = mPharmsMap.get(pharm.mName);
                if (prevPharm != null) {
                    if (prevPharm.mType == Pharm.TYPE_GOOGLE
                        && Utils.getDistance(prevPharm, pharm) < 30f) {
                        Log.d(TAG, "Pharmacy " + pharm.mName + " merged");
                        mPharmsMap.remove(pharm.mName);
                        mPharmsMap.put(pharm.mName, pharm);
                    }
                } else {
                    mPharmsMap.put(pharm.mName, pharm);
                }

                mPharms.add(pharm);
            }
        }

        updateMap();
        updateList();

    }

    private void clearPharamsOnMap() {
        mAdapter.clear();
        mPharmsMap.clear();
        mPharms.clear();
        mMapOverlays.remove(mPharmsOverlay);

        mListView.setVisibility(View.GONE);
    }

    private void findPlacemarkAtLocationType(int type, int lonE6, int latE6) {
        mFindPlacemarkType = type;
        // findPlacemarkAtLocation(lon, lat);

        new GetAddrTask(this, mOnAddrFound).execute(latE6 / 1E6, lonE6 / 1E6);
    }

    @Override
    protected boolean isRouteDisplayed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onBackPressed() {
        if (mListView.getVisibility() == View.VISIBLE) {
            mListView.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.status:
                if (mAdapter.getCount() > 0) {
                    if (mListView.getVisibility() == View.VISIBLE) {
                        mListView.setVisibility(View.GONE);
                    } else {
                        mListView.setVisibility(View.VISIBLE);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseHandler.initAndStartSession(this, Constants.BUGSENSE_KEY);

        requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_ACTION_BAR_OVERLAY);
        //getSupportActionBar().setBackgroundDrawable(
        //getResources().getDrawable(R.drawable.bg_actionbar));

        setContentView(R.layout.main);

        AQUtility.setDebug(false);
        MarketService ms = new MarketService(this);
        ms.level(MarketService.REVISION).checkVersion();

        mMapCenterAddressTextView = (TextView) findViewById(R.id.current_address);

        ViewGroup mapViewHolder = (ViewGroup) findViewById(R.id.mapViewHolder);

        MapView mapView = new MapView(this,
            Utils.isDebug(this) ? Constants.GMAP_DEBUG_KEY
                : Constants.GMAP_RELEASE_KEY);
        mapView.setClickable(true);

        mapViewHolder.addView(mapView, 0);

        mMapView = mapView;

        List<Overlay> mapOverlays = mapView.getOverlays();
        Drawable online = this.getResources().getDrawable(R.drawable.ic_pin_01);
        Drawable offline = this.getResources().getDrawable(
            R.drawable.ic_pin_01_offline);
        Drawable target = this.getResources().getDrawable(R.drawable.ic_target);

        mPharmsOverlay = new PharmsOverlay(online, offline, this, mapView);

        mMyLocationOverlay = new GMyLocationOverLay(this, mapView, target,
            mLocationChangedListener);

        GestureDetectorOverlay gestureDetectorOverlay = new GestureDetectorOverlay(
            this, mMapGestureListener, mMapGestureListener);
        gestureDetectorOverlay.setIsLongpressEnabled(true);

        mapOverlays.add(mMyLocationOverlay);
        mapOverlays.add(gestureDetectorOverlay);

        mMapOverlays = mapOverlays;

        // use map controller to zoom in/out, pan and set map center, zoom level
        // etc.
        mMapController = mMapView.getController();

        mAdapter = new PharmsListAdapter(this);
        mListView = (ListView) findViewById(R.id.lists);
        mListView.setAdapter(mAdapter);
        mListView.setCacheColorHint(0xDDFFFFFF);

        mListView.setOnItemClickListener(mOnItemClick);

        TextView status = (TextView) findViewById(R.id.status);
        status.setOnClickListener(this);

        mNotificationText = (TextView) findViewById(R.id.notification);
        mNotificationText.setVisibility(View.GONE);
        mNotificationText.setOnTouchListener(new SwipeDismissTouchListener(
            mNotificationText, null,
            new SwipeDismissTouchListener.OnDismissCallback() {
                @Override
                public void onDismiss(View view, Object token) {
                    mNotificationText.setVisibility(View.GONE);
                }
            }));

        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        String lastLatE6 = pref.getString(CONST_LOCATION_LAT, "");
        String lastLonE6 = pref.getString(CONST_LOCATION_LON, "");

        if (!TextUtils.isEmpty(lastLatE6) && !TextUtils.isEmpty(lastLonE6)) {
            mMapController.setCenter(new GeoPoint(Integer.parseInt(lastLatE6),
                Integer.parseInt(lastLonE6)));
        }

        int zoomLevel = pref.getInt(CONST_MAP_ZOOM_LEVEL, 17);
        mMapController.setZoom(zoomLevel);

        if (savedInstanceState == null) {
            startMyLocation();
        }

        NonConfigurationInstance nc = (NonConfigurationInstance) getLastNonConfigurationInstance();
        if (nc != null) {
            mGetNearPharmsTask = nc.mGetNearPharmsTask;
            mGetPharmsTask = nc.mGetPharmsTask;

            if (mGetNearPharmsTask != null) {
                mGetNearPharmsTask.updateListener(this);
            }
            if (mGetPharmsTask != null) {
                mGetPharmsTask.updateListener(this);
            }

            Log.d(TAG, "AsyncTask restored!");
        } else {
            Log.d(TAG, "AsyncTask not restored!");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        getSupportMenuInflater().inflate(R.menu.refresh_menu_items, menu);
        mOptionsMenu = menu;
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        if (mGetPharmsTask != null
            && mGetPharmsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mGetPharmsTask.cancel(true);
            mGetPharmsTask = null;
        }

        if (mGetNearPharmsTask != null
            && mGetNearPharmsTask.getStatus() != AsyncTask.Status.FINISHED) {
            mGetNearPharmsTask.cancel(true);
            mGetNearPharmsTask = null;
        }

        super.onDestroy();
    }

    @Override
    public void onError(String reason) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
    }

    @Override
    public void onFoundPharms(List<Pharm> pharms, boolean showMsg) {

        addPharms(pharms);

        if (showMsg) {
            if (pharms == null || pharms.size() == 0) {
                showNotification(R.string.couldnt_find_pharms);
                return;
            }

            showNotification(getString(R.string.pharmcy_found, pharms.size()));

        }

        updateProgress();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            // case R.id.menu_setting:
            //
            // return true;
            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                // fallback
        }

        return super.onMenuItemSelected(featureId, item); // To change body of
        // overridden
        // methods use File
        // |
        // Settings | File Templates.
    }

    @Override
    public boolean onOptionsItemSelected(
        com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                return true;
            case R.id.menu_refresh:
                clearPharamsOnMap();
                startMyLocation();
                return true;
            case R.id.menu_feedback:
                Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName()));
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                    | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(marketIntent);
                return true;
            default:
                // fallback
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        stopMyLocation();
        super.onPause();
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);

        mLocationName = bundle.getString(CONST_LOCATION_NAME);
        mMapCenterAddressTextView.setText(mLocationName);

        Location loc = bundle.getParcelable(CONST_MAP_LOCATON);
        String latE6 = bundle.getString(CONST_LOCATION_LAT);
        String lonE6 = bundle.getString(CONST_LOCATION_LON);

        if (!TextUtils.isEmpty(latE6) && !TextUtils.isEmpty(lonE6)) {
            GeoPoint point = new GeoPoint(Integer.parseInt(latE6),
                Integer.parseInt(lonE6));
            mMapController.setCenter(point);
            mMyLocationOverlay.setTargetPoint(point);
        }

        mMapController.setZoom(bundle.getInt(CONST_MAP_ZOOM_LEVEL));
        ArrayList<Pharm> pharms = bundle.getParcelableArrayList(CONST_PHARMS);

        Log.d(TAG, "onRestoreInstanceState size = " + pharms.size());

        addPharms(pharms);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        NonConfigurationInstance nc = null;

        if ((mGetNearPharmsTask != null && mGetNearPharmsTask.getStatus() != AsyncTask.Status.FINISHED)
            || (mGetPharmsTask != null && mGetPharmsTask.getStatus() != AsyncTask.Status.FINISHED)) {

            Log.d(TAG, "onRetainNonConfigurationInstance AsynTask stored!");

            nc = new NonConfigurationInstance();
            nc.mGetNearPharmsTask = mGetNearPharmsTask;
            nc.mGetPharmsTask = mGetPharmsTask;
        }

        return nc;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);

        bundle.putString(CONST_LOCATION_NAME, mLocationName);

        GeoPoint point = mMapView.getMapCenter();

        bundle.putString(CONST_LOCATION_LAT,
            Integer.toString(point.getLatitudeE6()));
        bundle.putString(CONST_LOCATION_LON,
            Integer.toString(point.getLongitudeE6()));

        bundle.putInt(CONST_MAP_ZOOM_LEVEL, mMapView.getZoomLevel());
        bundle.putParcelableArrayList(CONST_PHARMS, mPharms);
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public void onTargetAddressChanged(String address, boolean retry) {
        String msg;
        if (retry) {
            msg = getString(R.string.re_searching, address);
        } else {
            msg = getString(R.string.searching, address);
        }

        showNotification(msg);

        mLocationName = address;
        mMapCenterAddressTextView.setText(address);
    }

    private void setCurrentPoint(GeoPoint point, boolean sensor) {
        clearPharamsOnMap();

        Location loc = new Location("");
        loc.setLatitude(point.getLatitudeE6() / 1E6);
        loc.setLongitude(point.getLongitudeE6() / 1E6);
        mCurrentLocation = loc;

        mMyLocationOverlay.setTargetPoint(point);

        /*
         * if (mGetPharmsTask != null && mGetPharmsTask.getStatus() !=
         * AsyncTask.Status.FINISHED) { mGetNearPharmsTask.cancel(true); }
         * Projection projection = mMapView.getProjection(); float pixels =
         * projection.metersToEquatorPixels(UNIT); int meters = (int)
         * (mMapView.getWidth() / pixels); mGetNearPharmsTask =
         * (GetNearPharmsTask) new GetNearPharmsTask(this, Math.min(meters *
         * UNIT, 1000), sensor, mCurrentLocation, this)
         * .execute(point.getLatitudeE6() / 1E6, point.getLongitudeE6() / 1E6);
         */
        setRefreshActionButtonCompatState(true);
    }

    public void setRefreshActionButtonCompatState(boolean refreshing) {
        // On Honeycomb, we can set the state of the refresh button by giving it
        // a custom
        // action view.
        if (mOptionsMenu == null) {
            return;
        }

        final com.actionbarsherlock.view.MenuItem refreshItem = mOptionsMenu
            .findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem
                    .setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    private void showNotification(int msgId) {
        showNotification(getString(msgId));
    }

    private void showNotification(String msg) {

        // mPendingNotification.add(msg);
        // if (mPendingNotification.size() == 1) {
        // mNotificationText.setVisibility(View.VISIBLE);
        // mNotificationText.post(notificationRunable);
        // }
        Toast.makeText(DutyPharmActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    // gps timeout시 호출
    private void startFallbackSearch() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        String addr1 = pref.getString(CONST_LOCATION_ADDR1, "");
        String addr2 = pref.getString(CONST_LOCATION_ADDR2, "");
        String addr3 = pref.getString(CONST_LOCATION_ADDR3, "");
        if (!Utils.isEmpty(addr1) && !Utils.isEmpty(addr2)) {
            Log.d(TAG, "Try to find : " + addr1 + " " + addr2 + " " + addr3);

            stopMyLocation();
            startGetPharmTaskIfNeeded(addr1, addr2, addr3);
        } else {
            stopMyLocation();
            // 지도 줌심 위치로 검색

            GeoPoint point = mMapView.getMapCenter();

            Log.d(TAG, "Try to find center of the map : " + point);

            findPlacemarkAtLocationType(FINDPLACE_AND_UPDATE_PHARMS,
                point.getLongitudeE6(), point.getLatitudeE6());

            setCurrentPoint(point, false);
        }
    }

    private void startGetPharmTaskIfNeeded(String addr1, String addr2,
        String addr3) {
        String addrs = addr1 + " " + addr2 + " " + addr3;
        if (Utils.isEmpty(addrs)) {
            return;
        }

        if (mGetPharmsTask != null
            && mGetPharmsTask.getStatus() != AsyncTask.Status.FINISHED
            && !addrs.equals(mGetPharmsTask.mAddress)) {
            mGetPharmsTask.cancel(true);
        }

        if (mGetPharmsTask == null
            || mGetPharmsTask.getStatus() == AsyncTask.Status.FINISHED) {
            mGetPharmsTask = (GetPharmsTask) new GetPharmsTask(this,
                mCurrentLocation, this).execute(addr1, addr2,
                addr3 != null ? addr3 : "");

            setRefreshActionButtonCompatState(true);

        } else {
            Log.e(TAG,
                String.format("GetPharmsTask is not finished :"
                    + mGetPharmsTask.mAddress));
        }
    }

    private void startMyLocation() {
        if (mMyLocationOverlay != null) {
            if (!mMapOverlays.contains(mMyLocationOverlay)) {
                mMapOverlays.add(mMyLocationOverlay);
            }

            if (mMyLocationOverlay.isMyLocationEnabled()) {
                setRefreshActionButtonCompatState(true);
                mMapView.postInvalidate();
            } else {
                boolean isMyLocationEnabled = mMyLocationOverlay
                    .enableMyLocation();
                if (!isMyLocationEnabled) {
                    Toast.makeText(DutyPharmActivity.this,
                        R.string.enable_location_provider,
                        Toast.LENGTH_LONG).show();

                    Intent goToSettings = new Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(goToSettings);

                    return;
                } else {
                    setRefreshActionButtonCompatState(true);
                }
            }
        }
    }

    private void stopMyLocation() {
        if (mMyLocationOverlay != null) {
            mMyLocationOverlay.disableMyLocation();
        }
    }

    private void updateList() {

        ArrayList<Pharm> list = new ArrayList<Pharm>();
        for (String name : mPharmsMap.keySet()) {
            Pharm pharm = mPharmsMap.get(name);
            list.add(pharm);
        }
        Log.d(TAG, "list size = " + list.size());

        for (Pharm param : list) {
            Log.d(TAG, "before pharm = " + param.toString());
        }

        synchronized (mLock) {
            Collections.sort(list, COMPARATOR);
        }

        for (Pharm param : list) {
            Log.d(TAG, "after pharm = " + param.toString());
        }

        mAdapter.addAllCompat(list.toArray());
        mAdapter.notifyDataSetChanged();

        mListView.setVisibility(View.VISIBLE);
    }

    private void updateMap() {
        mMapOverlays.remove(mPharmsOverlay);
        mPharmsOverlay.clear();

        for (String name : mPharmsMap.keySet()) {
            Pharm pharm = mPharmsMap.get(name);

            // set POI data
            if (pharm.mLat > 0 && pharm.mLon > 0) {
                PharmOveralyItem item = new PharmOveralyItem(new GeoPoint(
                    (int) (pharm.mLat * 1E6), (int) (pharm.mLon * 1E6)),
                    pharm.mName, pharm.mAddress, pharm);

                mPharmsOverlay.addOverlayItem(item);

                // NMapPOIitem item = mPoiData.addPOIitem(pharm.mLon,
                // pharm.mLat,
                // pharm.mName
                // + "("
                // + (Utils.isEmpty(pharm.mTime) ? getString(
                // R.string.not_checked_on_duty) : pharm.mTime)
                // + ")",
                // markerId,
                // pharm);
                // item.setRightButton(true);
            }
        }

        mMapOverlays.add(mPharmsOverlay);
        // mPoiDataOverlay.setOnStateChangeListener(onPOIdataStateChangeListener);

        // show all POI data
        // mPoiDataOverlay.showAllPOIdata(0);
        mMapView.postInvalidate();
    }

    private void updateProgress() {
        // if ((mGetPharmsTask == null || (mGetPharmsTask != null &&
        // mGetPharmsTask.getStatus() == AsyncTask.Status.FINISHED))
        // && (mGetNearPharmsTask == null || (mGetNearPharmsTask != null &&
        // mGetNearPharmsTask.getStatus() == AsyncTask.Status.FINISHED))) {
        // setRefreshActionButtonCompatState(false);
        // }

        setRefreshActionButtonCompatState(false);
    }

}
