package hu.sztupy.sowatchface.config;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.support.wear.widget.WearableRecyclerView;
import android.util.Log;

import hu.sztupy.sowatchface.R;
import hu.sztupy.sowatchface.model.AnalogComplicationConfigData;
import hu.sztupy.sowatchface.watchface.SOWatchFace;

/**
 * The watch-side config activity for {@link SOWatchFace}, which
 * allows for setting the left and right complications of watch face along with the second's marker
 * color, background color, unread notifications toggle, and background complication image.
 */
public class AnalogComplicationConfigActivity extends Activity {

    private static final String TAG = AnalogComplicationConfigActivity.class.getSimpleName();

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;
    static final int SITE_SETTINGS_CONFIG_REQUEST_CODE = 1002;

    private WearableRecyclerView mWearableRecyclerView;
    private AnalogComplicationConfigRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_analog_complication_config);

        mAdapter = new AnalogComplicationConfigRecyclerViewAdapter(
                getApplicationContext(),
                AnalogComplicationConfigData.getWatchFaceServiceClass(),
                AnalogComplicationConfigData.getDataToPopulateAdapter(this));

        mWearableRecyclerView =
                (WearableRecyclerView) findViewById(R.id.wearable_recycler_view);

        // Aligns the first and last items on the list vertically centered on the screen.
        mWearableRecyclerView.setEdgeItemsCenteringEnabled(true);

        mWearableRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mWearableRecyclerView.setHasFixedSize(true);

        mWearableRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE
                && resultCode == RESULT_OK) {

            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
            Log.d(TAG, "Provider: " + complicationProviderInfo);

            // Updates preview with new complication information for selected complication id.
            // Note: complication id is saved and tracked in the adapter class.
            mAdapter.updateSelectedComplication(complicationProviderInfo);

        }

        if (requestCode == SITE_SETTINGS_CONFIG_REQUEST_CODE && resultCode == RESULT_OK) {
            mAdapter.updateSiteName();
        }
    }
}