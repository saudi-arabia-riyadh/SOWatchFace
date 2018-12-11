package hu.sztupy.sowatchface.config;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.text.Html;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;

import hu.sztupy.sowatchface.R;

import static android.text.Html.FROM_HTML_MODE_COMPACT;


/**
 * Allows user to select color for something on the watch face (background, highlight,etc.) and
 * saves it to {@link android.content.SharedPreferences} in
 * {@link android.support.v7.widget.RecyclerView.Adapter}.
 */
public class SiteSelectorActivity extends Activity {

    private static final String TAG = SiteSelectorActivity.class.getSimpleName();

    static final String EXTRA_SHARED_PREF =
            "hu.sztupy.sowatchface.watchface.config.extra.EXTRA_SHARED_PREF";

    private WearableRecyclerView mConfigAppearanceWearableRecyclerView;

    private SiteSelectorRecyclerViewAdapter mSiteSelectorRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_selection_config);

        // Assigns SharedPreference String used to save color selected.
        String sharedPrefString = null;
        int sharedPrefId = getIntent().getIntExtra(EXTRA_SHARED_PREF, 0);
        if (sharedPrefId != 0) {
           sharedPrefString = getApplicationContext().getResources().getString(sharedPrefId);
        }

        ArrayList<SiteSelectorRecyclerViewAdapter.SiteConfigItem> dataSet = new ArrayList<>();

        try {
            InputStream sitesStream = getResources().openRawResource(R.raw.sites);
            byte[] b = new byte[sitesStream.available()];
            sitesStream.read(b);

            JSONObject jsonObject = new JSONObject(new String(b, "UTF-8"));

            JSONArray array = jsonObject.getJSONArray("items");

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                String code = item.getString("api_site_parameter");
                String name = Html.fromHtml(item.getString("name"), FROM_HTML_MODE_COMPACT).toString();

                dataSet.add(new SiteSelectorRecyclerViewAdapter.SiteConfigItem(code, name));
            }
        } catch (Exception e) {

        }

        dataSet.sort(new Comparator<SiteSelectorRecyclerViewAdapter.SiteConfigItem>() {
            @Override
            public int compare(SiteSelectorRecyclerViewAdapter.SiteConfigItem o1, SiteSelectorRecyclerViewAdapter.SiteConfigItem o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        mSiteSelectorRecyclerViewAdapter = new SiteSelectorRecyclerViewAdapter(
                sharedPrefString,
                dataSet);

        mConfigAppearanceWearableRecyclerView =
                (WearableRecyclerView) findViewById(R.id.wearable_recycler_view);

        // Aligns the first and last items on the list vertically centered on the screen.
        mConfigAppearanceWearableRecyclerView.setEdgeItemsCenteringEnabled(true);

        mConfigAppearanceWearableRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mConfigAppearanceWearableRecyclerView.setHasFixedSize(true);

        mConfigAppearanceWearableRecyclerView.setAdapter(mSiteSelectorRecyclerViewAdapter);
    }


}