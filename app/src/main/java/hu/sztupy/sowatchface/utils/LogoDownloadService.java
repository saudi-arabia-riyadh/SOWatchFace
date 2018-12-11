package hu.sztupy.sowatchface.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import hu.sztupy.sowatchface.R;
import hu.sztupy.sowatchface.config.AnalogComplicationConfigRecyclerViewAdapter;
import hu.sztupy.sowatchface.provider.StackOverflowReputationProviderService;

public class LogoDownloadService {
    private static String TAG = "LogoDownloadService";

    public static final String COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY =
            "hu.sztupy.sowatchface.watchface.COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY";

    public static final String SITE_DOWNLOADED_KEY = "DOWNLOADED";

    private final Context context;

    public LogoDownloadService(Context context) {
        this.context = context;
    }

    public void downloadSiteIcon(final Runnable callback) {
        downloadSiteIcon(getCurrentSiteCodeName(),callback);
    }

    public void downloadSiteIcon(final String site, final Runnable callback) {
        if (logoExists(site)) {
            callback.run();
            return;
        }

        final RequestQueue queue = Volley.newRequestQueue(context);
        final String url = "https://api.stackexchange.com/2.2/info?site=" + site + "&filter=!9Z(-wtBWT&key=" + StackOverflowReputationProviderService.SO_ACCESS_KEY;

        JsonObjectRequest siteDownloadRequest = new JsonObjectRequest(Request.Method.GET, url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            final String iconUrl = response.getJSONArray("items").getJSONObject(0).getJSONObject("site").getString("high_resolution_icon_url");
                            Log.d(TAG, "Response is: " + iconUrl);

                            InputStreamVolleyRequest logoDownloadRequest = new InputStreamVolleyRequest(
                                    Request.Method.GET,
                                    iconUrl,
                                    new Response.Listener<byte[]>() {
                                        @Override
                                        public void onResponse(byte[] response) {
                                            if (response!=null) {
                                                try {
                                                    InputStream input = new ByteArrayInputStream(response);
                                                    File file = getIconFile(site);

                                                    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
                                                    byte data[] = new byte[1024];

                                                    int count;

                                                    while ((count = input.read(data)) != -1) {
                                                        output.write(data, 0, count);
                                                    }

                                                    output.flush();
                                                    output.close();
                                                    input.close();

                                                    SharedPreferences preferences = getComplicationPreferences();
                                                    SharedPreferences.Editor edit = preferences.edit();
                                                    edit.putBoolean(getSiteDataKey(site, SITE_DOWNLOADED_KEY), true);
                                                    edit.apply();
                                                } catch (IOException error) {
                                                    Log.i(TAG, "Saving the logo failed", error);
                                                    Log.i(TAG, error.toString());
                                                }
                                            }
                                            callback.run();
                                        }
                                    },
                                    new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Log.i(TAG, "Access to StackOverflow Logo Download failed", error);
                                            Log.i(TAG, error.toString());
                                            callback.run();
                                        }
                                    },
                                    null
                            );
                            queue.add(logoDownloadRequest);

                        } catch (JSONException e) {
                            callback.run();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Access to StackOverflow API has failed", error);
                Log.i(TAG, error.toString());
                callback.run();
            }
        });

        queue.add(siteDownloadRequest);
    }

    public boolean logoExists() {
        return logoExists(getCurrentSiteCodeName());
    }

    public boolean logoExists(String site) {
        SharedPreferences preferences = getComplicationPreferences();
        return preferences.getBoolean(getSiteDataKey(site, SITE_DOWNLOADED_KEY), false);
    }

    public File getIconFile(String site) {
        final File path = context.getFilesDir();
        String filename = site + ".png";
        return new File(path, filename);
    }

    public String getCurrentSiteCodeName() {
        return getWatchPreferences().getString(context.getString(R.string.saved_site_name_pref), AnalogComplicationConfigRecyclerViewAdapter.STACKOVERFLOW_NAME);
    }

    public SharedPreferences getWatchPreferences() {
        return context.getSharedPreferences(
                        context.getString(R.string.analog_complication_preference_file_key),
                        Context.MODE_PRIVATE);
    }

    public String getComplicationDataKey(int complicationId, String key) {
        return LogoDownloadService.class.getName() + "." + complicationId + "." + key;
    }

    public String getSiteDataKey(String site, String key) {
        return LogoDownloadService.class.getName() + ".site." + site + "." + key;
    }

    public String getSiteData(String site, String key) {
        return getComplicationPreferences().getString(getSiteDataKey(site, key), "");
    }

    public SharedPreferences getComplicationPreferences() {
        return context.getSharedPreferences(COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY, Context.MODE_PRIVATE);
    }

    public static Bitmap resizeLogo(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }
}
