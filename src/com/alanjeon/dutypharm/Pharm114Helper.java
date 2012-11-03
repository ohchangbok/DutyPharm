package com.alanjeon.dutypharm;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created with IntelliJ IDEA. User: skyisle Date: 6/10/12 Time: 9:30 오후 To
 * change this template use File | Settings | File Templates.
 */
public class Pharm114Helper {

    private static final String TAG = "Pharm114Helper";
    // http://goo.gl/eu4Ep 를 바탕으로 전화번호 보정
    static HashMap<String, String> areaCode = new HashMap<String, String>();
    private AQuery aq;
    private AjaxCallback<String> callbackStep1;

    {
        /*
         * 02 서울특별시 031 경기도 032 인천광역시 033 강원도 041 충청남도 042 대전광역시 043 충청북도 044
         * 세종특별자치시 051 부산광역시 052 울산광역시 053 대구광역시 054 경상북도 055 경상남도 061 전라남도 062
         * 광주광역시 063 전라북도 064 제주특별자치도
         */
        areaCode.put("서울", "02");
        areaCode.put("경기", "031");
        areaCode.put("인천", "032");
        areaCode.put("강원", "033");
        areaCode.put("충청남", "041");
        areaCode.put("대전", "042");
        areaCode.put("충청북", "043");
        areaCode.put("세종", "044");
        areaCode.put("부산", "051");
        areaCode.put("울산", "052");
        areaCode.put("대구", "053");
        areaCode.put("경상북", "054");
        areaCode.put("경상남", "055");
        areaCode.put("전라남", "061");
        areaCode.put("광주", "062");
        areaCode.put("전라북", "063");
        areaCode.put("제주", "064");
    }

    public Pharm114Helper(Context ctx) {
        aq = new AQuery(ctx);
    }

    private static String nomalizeCityName(String city) {
        String normCity = city.replace("특별시", "").replace("광역시", "").replace("특별자치도", "");
        if (TextUtils.isEmpty(normCity)) {
            return city;
        }

        String last = normCity.substring(normCity.length() - 1);
        if ("시".equals(last) || "도".equals(last)) {
            normCity = normCity.substring(0, normCity.length() - 1);
        }

        if (normCity.startsWith("경상")) {
            normCity = normCity.replace("경상", "경");
        } else if (normCity.startsWith("충청")) {
            normCity = normCity.replace("충청", "충");
        } else if (normCity.startsWith("전라")) {
            normCity = normCity.replace("전라", "전");
        }

        Log.d(TAG, city + " normalized to " + normCity);
        return normCity;
    }

    public static String getDoName(String addressLine) {
        if (TextUtils.isEmpty(addressLine)) {
            return "";
        }

        String strArr[] = addressLine.split(" ");
        if (strArr.length > 2) {
            return strArr[1];
        }

        return "";
    }

    public static String reviseTel(String doName, String tel) {
        String normDoName = nomalizeCityName(doName);
        String areaNum = areaCode.get(normDoName);
        String revisedTel = tel.replaceAll("[^\\d.]", "");

        if (TextUtils.isEmpty(areaNum)) {
            return revisedTel;
        }

        if (!revisedTel.startsWith(areaNum)) {
            revisedTel = areaNum + revisedTel;
        }

        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber krNumberProto = phoneUtil.parse(revisedTel,
                "KR");
            revisedTel = phoneUtil.format(krNumberProto,
                PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
        } catch (NumberParseException e) {
            System.err.println("NumberParseException was thrown: "
                + e.toString());
        }

        return revisedTel;
    }

    public String getSSKey() {
        callbackStep1 = new AjaxCallback<String>();

        String url1 = "http://www.pharm114.or.kr/";
        callbackStep1.url(url1).type(String.class).fileCache(false);

        aq.sync(callbackStep1);

        String result1 = callbackStep1.getResult();
        if (Utils.isEmpty(result1)) {
            Log.e(TAG, "step1 result is empty");
            return null;
        }

        int idx = result1.indexOf("ss_key");
        if (idx == -1) {
            Log.e(TAG, "cant find ss_key from result");
            return null;
        }

        // 의미있는 부분만 잘라내기
        result1 = result1.substring(idx - 10, idx + 50);
        Pattern p = Pattern.compile("name=\"ss_key\" value=\"(\\d+)\"",
            Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(result1);
        String ssKey = null;
        while (m.find()) {
            ssKey = m.group(1);
        }
        return ssKey;
    }

    public List<Pharm> getPharmList(String... addrs) {

        String ssKey = getSSKey();

        String url2 = "http://www.pharm114.or.kr/search/search_result.asp";
        AjaxCallback<String> step2 = new AjaxCallback<String>();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("search_first", "T");
        params.put("realtime_TF", "T");
        params.put("ss_key", ssKey);
        params.put("addr1", nomalizeCityName(addrs[0]));
        params.put("addr2", addrs[1]);
        params.put("addr3", addrs[2]);
        params.put("image2.x", ((int) Math.random()) % 20);
        params.put("image2.y", ((int) Math.random()) % 20);

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();

        for (Map.Entry<String, Object> e : params.entrySet()) {
            Object value = e.getValue();
            if (value != null) {
                pairs.add(new BasicNameValuePair(e.getKey(), value.toString()));
            }
        }

        UrlEncodedFormEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(pairs, "euc-kr");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        step2.url(url2)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Origin", "http://www.pharm114.or.kr")
            .header("Referer", "http://www.pharm114.or.kr/")
            .header("User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/537.1 (KHTML, "
                    + "like Gecko) Chrome/21.0.1163.0 Safari/537.1")
            .param(AQuery.POST_ENTITY, entity).encoding("windows-949")
            .fileCache(false).type(String.class);

        // Step1 에서 가져온 cookie를 계속 사용해야한다.
        List<Cookie> cookies = callbackStep1.getStatus().getCookies();
        for (Cookie cookie : cookies) {
            step2.cookie(cookie.getName(), cookie.getValue());
        }

        aq.sync(step2);

        String result2 = step2.getResult();
        if (Utils.isEmpty(result2)) {
            Log.e(TAG, "step2 result is empty statusCode = "
                + step2.getStatus().getCode());
            return null;
        }

        return parseResult(result2);
    }

    protected List<Pharm> parseResult(String html) {
        ArrayList<Pharm> list = new ArrayList<Pharm>();

        Document doc = Jsoup.parse(html);
        Elements tables = doc.getElementsByTag("table");

        for (Element table : tables) {
            if ("654".equals(table.attr("width"))
                && "TABLE-LAYOUT: fixed".equals(table.attr("style"))) {
                Elements trs = table.getElementsByTag("tr");
                for (Element tr : trs) {
                    Elements tds = tr.getElementsByTag("td");

                    Pharm pharm = null;
                    for (int i = 0; i < tds.size(); i++) {
                        Element td = tds.get(i);
                        switch (i) {
                            case 1: {
                                if (pharm == null) {
                                    pharm = new Pharm();
                                    pharm.mType = Pharm.TYPE_PHARM114;
                                }
                                pharm.mName = td.text().trim();
                            }
                            break;
                            case 2: {
                                pharm.mAddress = td.text().trim();
                            }
                            break;
                            case 3: {
                                pharm.mTel = td.text().trim();
                            }
                            break;
                            case 4: {
                                pharm.mTime = td.text().trim();
                            }
                            break;
                            case 9: {
                                pharm.mDesc = td.text().trim();
                            }
                            break;
                            default: {

                            }
                        }
                    }

                    if (pharm != null && !Utils.isEmpty(pharm.mName)) {
                        list.add(pharm);
                    }

                }
            }
        }

        return list;
    }
}
