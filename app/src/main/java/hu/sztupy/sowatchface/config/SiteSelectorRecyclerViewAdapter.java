package hu.sztupy.sowatchface.config;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import hu.sztupy.sowatchface.R;

public class SiteSelectorRecyclerViewAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = SiteSelectorRecyclerViewAdapter.class.getSimpleName();

    private ArrayList<SiteConfigItem> mSiteList;
    private String mSharedPrefString;

    public SiteSelectorRecyclerViewAdapter(
            String sharedPrefString,
            ArrayList<SiteConfigItem> siteList) {

        mSharedPrefString = sharedPrefString;
        mSiteList = siteList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder =
                new SiteConfigViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.site_config_list_item, parent, false));
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        SiteConfigItem site = mSiteList.get(position);
        SiteConfigViewHolder siteConfigViewHolder = (SiteConfigViewHolder) viewHolder;
        siteConfigViewHolder.setName(site.getName());
    }

    @Override
    public int getItemCount() {
        return mSiteList.size();
    }

    static public class SiteConfigItem {
        private final String code;
        private final String name;

        public SiteConfigItem(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "SiteConfigItem{" +
                    "code='" + code + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    /**
     * Displays color options for an item on the watch face and saves value to the
     * SharedPreference associated with it.
     */
    public class SiteConfigViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private TextView mTextView;

        public SiteConfigViewHolder(final View view) {
            super(view);
            mTextView = (TextView) view.findViewById(R.id.site_name);
            view.setOnClickListener(this);
        }

        public void setName(String name) {
            mTextView.setText(name);
        }

        @Override
        public void onClick (View view) {
            int position = getAdapterPosition();
            SiteConfigItem site = mSiteList.get(position);

            Log.d(TAG, "Site: " + site + " onClick() position: " + position);

            Activity activity = (Activity) view.getContext();

            if (mSharedPrefString != null && !mSharedPrefString.isEmpty()) {
                SharedPreferences sharedPref = activity.getSharedPreferences(
                        activity.getString(R.string.analog_complication_preference_file_key),
                        Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(mSharedPrefString, site.getCode());
                editor.apply();

                activity.setResult(Activity.RESULT_OK);
            }
            activity.finish();
        }
    }
}