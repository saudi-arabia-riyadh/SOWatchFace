package hu.sztupy.sowatchface.utils;

import android.content.Context;
import android.text.Html;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hu.sztupy.sowatchface.R;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

public class SiteListService {
    private static List<SiteConfigItem> mSiteList;
    private Context context;

    public SiteListService(Context context) {
        this.context = context;
        if (mSiteList == null) {
            updateSiteList();
        }
    }

    public synchronized void updateSiteList() {
        if (mSiteList == null) {
            mSiteList = new ArrayList<>();

            try {
                InputStream sitesStream = context.getResources().openRawResource(R.raw.sites);
                byte[] b = new byte[sitesStream.available()];
                sitesStream.read(b);

                JSONObject jsonObject = new JSONObject(new String(b, "UTF-8"));

                JSONArray array = jsonObject.getJSONArray("items");

                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);

                    String code = item.getString("api_site_parameter");
                    String name = Html.fromHtml(item.getString("name"), FROM_HTML_MODE_COMPACT).toString();
                    String url = item.getString("site_url");
                    String type = item.getString("site_type");

                    if (type.equals("main_site")) {
                        mSiteList.add(new SiteConfigItem(code, name, url));
                    }
                }
            } catch (Exception e) {

            }

            mSiteList.sort(new Comparator<SiteConfigItem>() {
                @Override
                public int compare(SiteConfigItem o1, SiteConfigItem o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
        }
    }

    public synchronized List<SiteConfigItem> getSiteList() {
        return Collections.unmodifiableList(mSiteList);
    }

    public String getName(String siteCode) {
        for (SiteConfigItem item : getSiteList()) {
            if (item.getCode().equals(siteCode)) {
                return item.getName();
            }
        }
        return null;
    }

    public String getShortName(String siteCode) {
        String siteFullName = getName(siteCode);
        if (siteFullName != null) {
            StringBuilder initials = new StringBuilder();
            for (String s : siteFullName.split("[ .]")) {
                initials.append(s.charAt(0));
            }
            return initials.toString();
        }
        return null;
    }

    public String getUrl(String siteCode) {
        for (SiteConfigItem item : getSiteList()) {
            if (item.getCode().equals(siteCode)) {
                return item.getUrl();
            }
        }
        return null;
    }

    static public class SiteConfigItem {
        private final String code;
        private final String name;
        private final String url;

        public SiteConfigItem(String code, String name, String url) {
            this.code = code;
            this.name = name;
            this.url = url;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "SiteConfigItem{" +
                    "code='" + code + '\'' +
                    ", name='" + name + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}
