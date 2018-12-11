package hu.sztupy.sowatchface.provider;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import hu.sztupy.sowatchface.R;
import hu.sztupy.sowatchface.config.AnalogComplicationConfigRecyclerViewAdapter;
import hu.sztupy.sowatchface.utils.LogoDownloadService;

/**
 * Example Watch Face Complication data provider provides a number that can be incremented on tap.
 */
public class StackOverflowReputationProviderService extends ComplicationProviderService {

    private static final String TAG = "SORepProviderService";
    public static final String SO_ACCESS_KEY = "DHARdYfewh89v8Af)V194A((";
    public static final String REPUTATION_KEY = "reputation";

    private static final int[] REPUTATION_MILESTONES = {
            1,
            5,
            10,
            15,
            20,
            50,
            75,
            100,
            125,
            200,
            250,
            500,
            1000,
            1500,
            2000,
            2500,
            3000,
            5000,
            10000,
            15000,
            20000,
            25000, // last real milestone, after this they are just numbers
            100000,
            250000,
            500000,
            1000000,
            2500000,
            5000000
    };

    @Override
    public void onComplicationActivated(
            int complicationId, int dataType, ComplicationManager complicationManager) {
        Log.d(TAG, "onComplicationActivated(): " + complicationId);
    }

    @Override
    public void onComplicationUpdate(
            final int complicationId, final int dataType, final ComplicationManager complicationManager) {
        Log.d(TAG, "onComplicationUpdate() id: " + complicationId);

        final ComponentName thisProvider = new ComponentName(this, getClass());
        final PendingIntent complicationTogglePendingIntent =
                ComplicationUpdateReceiver.getToggleIntent(this, thisProvider, complicationId);

        final LogoDownloadService logoService = new LogoDownloadService(getApplicationContext());

        final Runnable updateComplications = new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferences = logoService.getComplicationPreferences();
                String preferenceKey = logoService.getComplicationDataKey(complicationId, REPUTATION_KEY);
                int number = preferences.getInt(preferenceKey, 0);
                String numberText = String.format(Locale.getDefault(), "%d", number);

                int nextReputationMilestone = 0;
                for (int milestone : REPUTATION_MILESTONES) {
                    if (milestone > number) {
                        nextReputationMilestone = milestone;
                        break;
                    }
                }

                ComplicationData complicationData = null;

                Icon icon = null;
                if (logoService.logoExists(logoService.getCurrentSiteCodeName())){
                    Bitmap bmp = BitmapFactory.decodeFile(logoService.getIconFile(logoService.getCurrentSiteCodeName()).getAbsolutePath());
                    icon = Icon.createWithBitmap(LogoDownloadService.resizeLogo(bmp, 64, 64));
                }

                switch (dataType) {
                    case ComplicationData.TYPE_RANGED_VALUE:
                        complicationData =
                                new ComplicationData.Builder(ComplicationData.TYPE_RANGED_VALUE)
                                        .setValue(number)
                                        .setMinValue(0)
                                        .setMaxValue(nextReputationMilestone)
                                        .setShortText(ComplicationText.plainText(numberText))
                                        .setTapAction(complicationTogglePendingIntent)
                                        .setIcon(icon)
                                        .build();
                        break;
                    case ComplicationData.TYPE_SHORT_TEXT:
                        complicationData =
                                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                        .setShortText(ComplicationText.plainText(numberText))
                                        .setTapAction(complicationTogglePendingIntent)
                                        .setIcon(icon)
                                        .build();
                        break;
                    case ComplicationData.TYPE_LONG_TEXT:
                        complicationData =
                                new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                        .setLongText(ComplicationText.plainText("Reputation: " + numberText))
                                        .setTapAction(complicationTogglePendingIntent)
                                        .setIcon(icon)
                                        .build();
                        break;
                    case ComplicationData.TYPE_ICON:
                        if (icon != null) {
                            complicationData =
                                    new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                                            .setTapAction(complicationTogglePendingIntent)
                                            .setIcon(icon)
                                            .build();
                        }
                        break;
                    default:
                        if (Log.isLoggable(TAG, Log.WARN)) {
                            Log.w(TAG, "Unexpected complication type " + dataType);
                        }
                }

                if (complicationData != null) {
                    complicationManager.updateComplicationData(complicationId, complicationData);

                } else {
                    // If no data is sent, we still need to inform the ComplicationManager, so the update
                    // job can finish and the wake lock isn't held any longer than necessary.
                    complicationManager.noUpdateRequired(complicationId);
                }
            }
        };


        logoService.downloadSiteIcon(logoService.getCurrentSiteCodeName(), new Runnable() {
            @Override
            public void run() {
                getLatestData(getApplicationContext(), complicationId, updateComplications);
            }
        });
    }

    public void getLatestData(final Context context, final int complicationId, final Runnable callback) {
        SharedPreferences applicationPreferences =
                context.getSharedPreferences(
                        context.getString(R.string.analog_complication_preference_file_key),
                        Context.MODE_PRIVATE);

        int userId = applicationPreferences.getInt(context.getString(R.string.saved_user_id_pref), AnalogComplicationConfigRecyclerViewAdapter.JON_SKEET_ID);
        String siteName = applicationPreferences.getString(context.getString(R.string.saved_site_name_pref), AnalogComplicationConfigRecyclerViewAdapter.STACKOVERFLOW_NAME);

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://api.stackexchange.com/2.2/users/" + userId + "?site=" + siteName + "&key=" + SO_ACCESS_KEY;

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        int result = 0;

                        try {
                            result = response.getJSONArray("items").getJSONObject(0).getInt("reputation");
                        } catch (JSONException e) {
                        }

                        Log.d(TAG, "Response is: " + result);

                        LogoDownloadService logoService = new LogoDownloadService(context);

                        String preferenceKey = logoService.getComplicationDataKey(complicationId, REPUTATION_KEY);
                        SharedPreferences sharedPreferences = logoService.getComplicationPreferences();

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(preferenceKey, result);
                        editor.apply();

                        callback.run();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Access to StackOverflow API has failed", error);
                Log.i(TAG, error.toString());
                callback.run();
            }
        });

        queue.add(jsonRequest);
    }

    /*
     * Called when the complication has been deactivated.
     */
    @Override
    public void onComplicationDeactivated(int complicationId) {
        Log.d(TAG, "onComplicationDeactivated(): " + complicationId);
    }
}
