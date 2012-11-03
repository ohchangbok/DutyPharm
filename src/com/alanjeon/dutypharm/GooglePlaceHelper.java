package com.alanjeon.dutypharm;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 6/16/12 Time: 8:51 오후
 */
public class GooglePlaceHelper {

    @SuppressWarnings("unused")
    private static final String TAG = "GooglePlaceHelper";
    private static final String PLACE_API_URL = "https://maps.googleapis.com/maps/api/place/search/json";
    private static final String PLACE_DETAIL_API_URL = "https://maps.googleapis.com/maps/api/place/details/json";
    private static final List<Pharm> EMPTY_LIST = new ArrayList<Pharm>();
    private final String mKey;

    public GooglePlaceHelper(Context ctx, String key) {
        mKey = key;
    }

    private static String readInputStream(InputStream inputStream)
        throws IOException {
        BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(inputStream));
        String responseLine;
        StringBuilder responseBuilder = new StringBuilder();
        while ((responseLine = bufferedReader.readLine()) != null) {
            responseBuilder.append(responseLine);
        }
        return responseBuilder.toString();
    }

    public List<Pharm> getPharmList(double lat, double lon, int accuracy,
        boolean sensor) throws JSONException, IOException {

        String searchUri = null;
        try {
            searchUri = PLACE_API_URL + "?location=" + Double.toString(lat)
                + "," + Double.toString(lon) + "&radius="
                + Integer.toString(accuracy) + "&language=ko" + "&sensor="
                + sensor + "&key=" + mKey + "&name="
                + URLEncoder.encode("약국", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return EMPTY_LIST;
        }

        HttpURLConnection urlConnection = (HttpURLConnection) new URL(searchUri)
            .openConnection();
        urlConnection.connect();
        String json = readInputStream(urlConnection.getInputStream());

        JSONObject result = new JSONObject(json);
        if (!"OK".equals(result.getString("status"))) {
            return EMPTY_LIST;
        }

        JSONArray pharams = result.getJSONArray("results");
        if (pharams.length() <= 0) {
            return EMPTY_LIST;
        }

        ArrayList<Pharm> list = new ArrayList<Pharm>();

        for (int i = 0; i < pharams.length(); i++) {
            JSONObject pharmInfo = pharams.getJSONObject(i);
            Pharm newPham = new Pharm();
            newPham.mType = Pharm.TYPE_GOOGLE;

            /*
             * { "id":"158cdf535f1f5eeeb75ce4469b1d5ea0e97f82ef",
             * "icon":"http:\/\/maps.gstatic.com\/mapfiles\/place_api\/icons
             * \/generic_business -71.png", "vicinity":"서울특별시 강북구 미아동 688-5",
             * "types":["pharmacy","store","health","establishment"],
             * "reference":"CnRrAAAA0mXrUJ9JswxE2iPQ5fYs6q5GBxa8sV8C_Zii7mc5JA
             * X8dV5hBJb4qbAyAsb_K8ZGEnTja7MwCKW0oly3_kD0YXUZxUHoh
             * --vqYpZpumq_8F3OeICgJh9s_9UDYqUoeLAFEqDy4N 8oAxEhcq
             * nRb-4SBIQicpw9fFkYEa2BEIrMsih-hoUa_bwqfIWxqrojoUc4U 9SdnnfAeI",
             * "name":"요나약국",
             * "geometry":{"location":{"lng":127.020714,"lat ":37.619251}} }
             */

            newPham.mRef = pharmInfo.getString("reference");
            newPham.mName = pharmInfo.getString("name");
            newPham.mAddress = pharmInfo.getString("vicinity");
            newPham.mDesc = "";
            newPham.mTel = "";

            JSONObject geometry = pharmInfo.getJSONObject("geometry");
            if (geometry != null) {
                JSONObject location = geometry.getJSONObject("location");
                newPham.mLon = location.getDouble("lng");
                newPham.mLat = location.getDouble("lat");
            }

            updatePharmDetail(newPham, sensor);
            list.add(newPham);
        }

        return list;
    }

    private Pharm updatePharmDetail(Pharm pharm, boolean sensor)
        throws JSONException, IOException {

        String searchUri = PLACE_DETAIL_API_URL + "?reference=" + pharm.mRef
            + "&language=ko" + "&sensor=" + sensor + "&key=" + mKey;

        HttpURLConnection urlConnection = (HttpURLConnection) new URL(searchUri)
            .openConnection();
        urlConnection.connect();
        String json = readInputStream(urlConnection.getInputStream());

        JSONObject result = new JSONObject(json);
        if (!"OK".equals(result.getString("status"))) {
            return null;
        }

        JSONObject pharmDetail = result.getJSONObject("result");
        if (pharmDetail.has("formatted_phone_number")) {
            pharm.mTel = pharmDetail.getString("formatted_phone_number");
        }
        return pharm;
    }
}
