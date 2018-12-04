/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.sztupy.sowatchface.provider;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
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

/**
 * Example Watch Face Complication data provider provides a number that can be incremented on tap.
 */
public class StackOverflowReputationProviderService extends ComplicationProviderService {

    private static final String TAG = "SORepProviderService";
    static final String SO_ACCESS_KEY = "DHARdYfewh89v8Af)V194A((";

    static final String COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY =
            "hu.sztupy.sowatchface.watchface.COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY";

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

    /*
     * Called when a complication has been activated. The method is for any one-time
     * (per complication) set-up.
     *
     * You can continue sending data for the active complicationId until onComplicationDeactivated()
     * is called.
     */
    @Override
    public void onComplicationActivated(
            int complicationId, int dataType, ComplicationManager complicationManager) {
        Log.d(TAG, "onComplicationActivated(): " + complicationId);
    }

    /*
     * Called when the complication needs updated data from your provider. There are four scenarios
     * when this will happen:
     *
     *   1. An active watch face complication is changed to use this provider
     *   2. A complication using this provider becomes active
     *   3. The period of time you specified in the manifest has elapsed (UPDATE_PERIOD_SECONDS)
     *   4. You triggered an update from your own class via the
     *       ProviderUpdateRequester.requestUpdate() method.
     */
    @Override
    public void onComplicationUpdate(
            final int complicationId, final int dataType, final ComplicationManager complicationManager) {
        Log.d(TAG, "onComplicationUpdate() id: " + complicationId);

        final ComponentName thisProvider = new ComponentName(this, getClass());
        final PendingIntent complicationTogglePendingIntent =
                StackOverflowReputationDataReceiver.getToggleIntent(this, thisProvider, complicationId);

        getLatestData(getApplicationContext(), thisProvider, complicationId, new Runnable() {
                    @Override
                    public void run() {

                        SharedPreferences preferences = getSharedPreferences(COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY, 0);
                        int number = preferences.getInt(getPreferenceKey(thisProvider, complicationId), 0);
                        String numberText = String.format(Locale.getDefault(), "%d", number);

                        int nextReputationMilestone = 0;
                        for (int milestone : REPUTATION_MILESTONES) {
                            if (milestone > number) {
                                nextReputationMilestone = milestone;
                                break;
                            }
                        }

                        ComplicationData complicationData = null;

                        Icon icon = Icon.createWithResource(getApplicationContext(), R.drawable.stack_overflow_icon);

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
                                complicationData =
                                        new ComplicationData.Builder(ComplicationData.TYPE_ICON)
                                                .setTapAction(complicationTogglePendingIntent)
                                                .setIcon(icon)
                                                .build();
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
                }
        );
    }

    public void getLatestData(final Context context, final ComponentName provider, final int complicationId, final Runnable callback) {
        SharedPreferences applicationPreferences =
                context.getSharedPreferences(
                        context.getString(R.string.analog_complication_preference_file_key),
                        Context.MODE_PRIVATE);

        int userId = applicationPreferences.getInt(context.getString(R.string.saved_user_id_pref), AnalogComplicationConfigRecyclerViewAdapter.JON_SKEET_ID);

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://api.stackexchange.com/2.2/users/" + userId + "?site=stackoverflow&key=" + SO_ACCESS_KEY;

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

                        String preferenceKey = getPreferenceKey(provider, complicationId);
                        SharedPreferences sharedPreferences =
                                context.getSharedPreferences(COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY, 0);

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


    /**
     * Returns the key for the shared preference used to hold the current state of a given
     * complication.
     */
    static String getPreferenceKey(ComponentName provider, int complicationId) {
        return provider.getClassName() + complicationId;
    }
}
