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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import hu.sztupy.sowatchface.R;
import hu.sztupy.sowatchface.config.AnalogComplicationConfigRecyclerViewAdapter;
import hu.sztupy.sowatchface.model.AnalogComplicationConfigData;


/**
 * Simple {@link BroadcastReceiver} subclass for asynchronously incrementing an integer for any
 * complication id triggered via TapAction on complication. Also, provides static method to create a
 * {@link PendingIntent} that triggers this receiver.
 */
public class StackOverflowReputationDataReceiver extends BroadcastReceiver {

    private static final String TAG = "SONetwork";

    private static final String EXTRA_PROVIDER_COMPONENT =
            "hu.sztupy.sowatchface.provider.action.PROVIDER_COMPONENT";
    private static final String EXTRA_COMPLICATION_ID =
            "hu.sztupy.sowatchface.provider.action.COMPLICATION_ID";

    static final int MAX_REPUTATION = 100000;
    static final String COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY =
            "hu.sztupy.sowatchface.watchface.COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY";

    static final String SO_ACCESS_KEY = "DHARdYfewh89v8Af)V194A((";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        final ComponentName provider = extras.getParcelable(EXTRA_PROVIDER_COMPONENT);
        final int complicationId = extras.getInt(EXTRA_COMPLICATION_ID);

        SharedPreferences applicationPreferences =
                        context.getSharedPreferences(
                                context.getString(R.string.analog_complication_preference_file_key),
                                Context.MODE_PRIVATE);

        int userId = applicationPreferences.getInt(context.getString(R.string.saved_user_id_pref), AnalogComplicationConfigRecyclerViewAdapter.JON_SKEET_ID);

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://api.stackexchange.com/2.2/users/"+userId+"?site=stackoverflow&key=" + SO_ACCESS_KEY;

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.

                        int result = 0;

                        try {
                            result = response.getJSONArray("items").getJSONObject(0).getInt("reputation");
                        } catch (JSONException e) {
                        }

                        Log.d(TAG, "Response is: "+ result);

                        String preferenceKey = getPreferenceKey(provider, complicationId);
                        SharedPreferences sharedPreferences =
                                context.getSharedPreferences(COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY, 0);

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt(preferenceKey, result);
                        editor.apply();

                        // Request an update for the complication that has just been toggled.
                        ProviderUpdateRequester requester = new ProviderUpdateRequester(context, provider);
                        requester.requestUpdate(complicationId);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Access to StackOverflow API has failed", error);
                Log.i(TAG, error.toString());
            }
        });

        queue.add(jsonRequest);
    }

    /**
     * Returns a pending intent, suitable for use as a tap intent, that causes a complication to be
     * toggled and updated.
     */
    static PendingIntent getToggleIntent(
            Context context, ComponentName provider, int complicationId) {
        Intent intent = new Intent(context, StackOverflowReputationDataReceiver.class);
        intent.putExtra(EXTRA_PROVIDER_COMPONENT, provider);
        intent.putExtra(EXTRA_COMPLICATION_ID, complicationId);

        // Pass complicationId as the requestCode to ensure that different complications get
        // different intents.
        return PendingIntent.getBroadcast(
                context, complicationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Returns the key for the shared preference used to hold the current state of a given
     * complication.
     */
    static String getPreferenceKey(ComponentName provider, int complicationId) {
        return provider.getClassName() + complicationId;
    }
}
