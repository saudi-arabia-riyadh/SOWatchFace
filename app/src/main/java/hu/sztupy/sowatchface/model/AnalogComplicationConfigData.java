package hu.sztupy.sowatchface.model;

import android.content.Context;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.RecyclerView;

import hu.sztupy.sowatchface.R;
import hu.sztupy.sowatchface.config.AnalogComplicationConfigRecyclerViewAdapter;
import hu.sztupy.sowatchface.config.AnalogComplicationConfigActivity;
import hu.sztupy.sowatchface.watchface.SOWatchFace;

import java.util.ArrayList;

/**
 * Data represents different views for configuring the
 * {@link SOWatchFace} watch face's appearance and complications
 * via {@link AnalogComplicationConfigActivity}.
 */
public class AnalogComplicationConfigData {


    /**
     * Interface all ConfigItems must implement so the {@link RecyclerView}'s Adapter associated
     * with the configuration activity knows what type of ViewHolder to inflate.
     */
    public interface ConfigItemType {
        int getConfigType();
    }

    /**
     * Returns Watch Face Service class associated with configuration Activity.
     */
    public static Class getWatchFaceServiceClass() {
        return SOWatchFace.class;
    }

    /**
     * Includes all data to populate each of the 5 different custom
     * {@link ViewHolder} types in {@link AnalogComplicationConfigRecyclerViewAdapter}.
     */
    public static ArrayList<ConfigItemType> getDataToPopulateAdapter(Context context) {

        ArrayList<ConfigItemType> settingsConfigData = new ArrayList<>();

        // Data for watch face preview and complications UX in settings Activity.
        ConfigItemType complicationConfigItem =
                new PreviewAndComplicationsConfigItem(R.drawable.add_complication);
        settingsConfigData.add(complicationConfigItem);

        // Data for 'Unread Notifications' UX (toggle) in settings Activity.
        ConfigItemType unreadNotificationsConfigItem =
                new SwitchConfigItem(
                        context.getString(R.string.config_unread_notifications_label),
                        R.drawable.ic_notifications_white_24dp,
                        R.drawable.ic_notifications_off_white_24dp,
                        R.string.saved_unread_notifications_pref);
        settingsConfigData.add(unreadNotificationsConfigItem);

        ConfigItemType utcDisplayConfigItem =
                new SwitchConfigItem(
                        context.getString(R.string.config_utc_display_label),
                        R.drawable.baseline_alarm_white_24,
                        R.drawable.baseline_alarm_off_white_24,
                        R.string.saved_utc_notch_pref);
        settingsConfigData.add(utcDisplayConfigItem);

        ConfigItemType designPreferenceConfigItem =
                new SwitchConfigItem(
                        context.getString(R.string.config_design_label),
                        R.drawable.baseline_stars_white_24,
                        R.drawable.baseline_star_rate_white_24,
                        R.string.saved_design_pref);
        settingsConfigData.add(designPreferenceConfigItem);

        ConfigItemType siteNameConfigItem =
                new SelectBoxConfigItem(
                        context.getString(R.string.config_se_site_label),
                        R.drawable.baseline_subject_white_24,
                        R.string.saved_site_name_pref);
        settingsConfigData.add(siteNameConfigItem);

        ConfigItemType userIdSelectConfigItem =
                new InputConfigItem(
                        context.getString(R.string.config_user_id_label),
                        R.drawable.baseline_trending_up_white_24,
                        R.string.saved_user_id_pref);
        settingsConfigData.add(userIdSelectConfigItem);

        return settingsConfigData;
    }

    /**
     * Data for Watch Face Preview with Complications Preview item in RecyclerView.
     */
    public static class PreviewAndComplicationsConfigItem implements ConfigItemType {

        private int defaultComplicationResourceId;

        PreviewAndComplicationsConfigItem(int defaultComplicationResourceId) {
            this.defaultComplicationResourceId = defaultComplicationResourceId;
        }

        public int getDefaultComplicationResourceId() {
            return defaultComplicationResourceId;
        }

        @Override
        public int getConfigType() {
            return AnalogComplicationConfigRecyclerViewAdapter.TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG;
        }
    }

    /**
     * Data for Unread Notification preference picker item in RecyclerView.
     */
    public static class SwitchConfigItem  implements ConfigItemType {

        private String name;
        private int iconEnabledResourceId;
        private int iconDisabledResourceId;
        private int sharedPrefId;

        SwitchConfigItem(
                String name,
                int iconEnabledResourceId,
                int iconDisabledResourceId,
                int sharedPrefId) {
            this.name = name;
            this.iconEnabledResourceId = iconEnabledResourceId;
            this.iconDisabledResourceId = iconDisabledResourceId;
            this.sharedPrefId = sharedPrefId;
        }

        public String getName() {
            return name;
        }

        public int getIconEnabledResourceId() {
            return iconEnabledResourceId;
        }

        public int getIconDisabledResourceId() {
            return iconDisabledResourceId;
        }

        public int getSharedPrefId() {
            return sharedPrefId;
        }

        @Override
        public int getConfigType() {
            return AnalogComplicationConfigRecyclerViewAdapter.TYPE_SWITCH_CONFIG;
        }
    }

    public static class InputConfigItem  implements ConfigItemType {

        private String name;
        private int iconResourceId;
        private int sharedPrefId;

        InputConfigItem(
                String name,
                int iconResourceId,
                int sharedPrefId) {
            this.name = name;
            this.iconResourceId = iconResourceId;
            this.sharedPrefId = sharedPrefId;
        }

        public String getName() {
            return name;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        public int getSharedPrefId() {
            return sharedPrefId;
        }

        @Override
        public int getConfigType() {
            return AnalogComplicationConfigRecyclerViewAdapter.TYPE_INPUT_CONFIG;
        }
    }

    public static class SelectBoxConfigItem implements ConfigItemType {

        private String name;
        private int iconResourceId;
        private int sharedPrefId;

        SelectBoxConfigItem(
                String name,
                int iconResourceId,
                int sharedPrefId) {
            this.name = name;
            this.iconResourceId = iconResourceId;
            this.sharedPrefId = sharedPrefId;
        }

        public String getName() {
            return name;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        public int getSharedPrefId() {
            return sharedPrefId;
        }

        @Override
        public int getConfigType() {
            return AnalogComplicationConfigRecyclerViewAdapter.TYPE_SELECTBOX_CONFIG;
        }
    }
}