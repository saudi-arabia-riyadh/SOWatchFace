package hu.sztupy.sowatchface.provider;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Environment;
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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import hu.sztupy.sowatchface.R;
import hu.sztupy.sowatchface.config.AnalogComplicationConfigRecyclerViewAdapter;
import hu.sztupy.sowatchface.utils.InputStreamVolleyRequest;
import hu.sztupy.sowatchface.utils.LogoDownloadService;
import hu.sztupy.sowatchface.utils.SiteListService;

import static android.support.wearable.complications.ComplicationData.IMAGE_STYLE_ICON;

/**
 * Example Watch Face Complication data provider provides a number that can be incremented on tap.
 */
public class StackOverflowLogoProviderService extends ComplicationProviderService {

    private static final String TAG = "SOLogoProviderService";

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
        final SiteListService siteListService = new SiteListService(getApplicationContext());

        Runnable updateComplications = new Runnable() {
            @Override
            public void run() {
                ComplicationData complicationData = null;

                Icon icon = null;
                Icon image = null;

                if (logoService.logoExists(logoService.getCurrentSiteCodeName())) {
                    Bitmap bmp = BitmapFactory.decodeFile(logoService.getIconFile(logoService.getCurrentSiteCodeName()).getAbsolutePath());
                    image = Icon.createWithBitmap(bmp);
                    icon = Icon.createWithBitmap(LogoDownloadService.resizeLogo(bmp, 64, 64));
                }

                String shortName = siteListService.getShortName(logoService.getCurrentSiteCodeName());
                String longName = siteListService.getName(logoService.getCurrentSiteCodeName());

                switch (dataType) {
                    case ComplicationData.TYPE_SHORT_TEXT:
                        complicationData =
                                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                        .setShortText(ComplicationText.plainText(shortName))
                                        .setTapAction(complicationTogglePendingIntent)
                                        .setIcon(icon)
                                        .build();
                        break;
                    case ComplicationData.TYPE_LONG_TEXT:
                        complicationData =
                                new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                        .setLongText(ComplicationText.plainText(longName))
                                        .setTapAction(complicationTogglePendingIntent)
                                        .setIcon(icon)
                                        .setImageStyle(IMAGE_STYLE_ICON)
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
                    case ComplicationData.TYPE_SMALL_IMAGE:
                        if (icon != null) {
                            complicationData =
                                    new ComplicationData.Builder(ComplicationData.TYPE_SMALL_IMAGE)
                                            .setTapAction(complicationTogglePendingIntent)
                                            .setSmallImage(image)
                                            .setImageStyle(IMAGE_STYLE_ICON)
                                            .build();
                        }
                        break;
                    case ComplicationData.TYPE_LARGE_IMAGE:
                        if (icon != null) {
                            complicationData =
                                    new ComplicationData.Builder(ComplicationData.TYPE_LARGE_IMAGE)
                                            .setTapAction(complicationTogglePendingIntent)
                                            .setLargeImage(image)
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

        logoService.downloadSiteIcon(logoService.getCurrentSiteCodeName(), updateComplications);
    }

    @Override
    public void onComplicationDeactivated(int complicationId) {
        Log.d(TAG, "onComplicationDeactivated(): " + complicationId);
    }
}
