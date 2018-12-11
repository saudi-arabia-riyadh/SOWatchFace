package hu.sztupy.sowatchface.config;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.text.Html;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;

import hu.sztupy.sowatchface.R;
import hu.sztupy.sowatchface.utils.LogoDownloadService;
import hu.sztupy.sowatchface.utils.SiteListService;

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

    private LinearLayoutManager mLinearLayoutManager;

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

        final SiteListService siteListService = new SiteListService(getApplicationContext());
        final LogoDownloadService logoDownloadService = new LogoDownloadService(getApplicationContext());

        mSiteSelectorRecyclerViewAdapter = new SiteSelectorRecyclerViewAdapter(
                sharedPrefString,
                siteListService.getSiteList());

        mConfigAppearanceWearableRecyclerView =
                (WearableRecyclerView) findViewById(R.id.wearable_recycler_view);

        // Aligns the first and last items on the list vertically centered on the screen.
        mConfigAppearanceWearableRecyclerView.setEdgeItemsCenteringEnabled(true);

        mLinearLayoutManager = new LinearLayoutManager(this);

        mConfigAppearanceWearableRecyclerView.setLayoutManager(mLinearLayoutManager);

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mConfigAppearanceWearableRecyclerView.setHasFixedSize(true);

        mConfigAppearanceWearableRecyclerView.setAdapter(mSiteSelectorRecyclerViewAdapter);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                int position = siteListService.getPosition(logoDownloadService.getCurrentSiteCodeName());
                if (position >= 0) {
                    mConfigAppearanceWearableRecyclerView.scrollToPosition(position);
                }
            }
        }, 250);
    }
}