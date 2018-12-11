package hu.sztupy.sowatchface.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import hu.sztupy.sowatchface.R;
import hu.sztupy.sowatchface.config.AnalogComplicationConfigRecyclerViewAdapter;
import hu.sztupy.sowatchface.utils.LogoDownloadService;
import hu.sztupy.sowatchface.utils.SiteListService;

import static hu.sztupy.sowatchface.config.AnalogComplicationConfigRecyclerViewAdapter.JON_SKEET_ID;
import static hu.sztupy.sowatchface.config.AnalogComplicationConfigRecyclerViewAdapter.JON_SKEET_SE_ID;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class SOWatchFace extends CanvasWatchFaceService {
    private static final String TAG = "WatchFace";
    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    private static final int LEFT_COMPLICATION_ID = 100;
    private static final int RIGHT_COMPLICATION_ID = 101;

    private static final int GRAY = Color.rgb(187, 187, 187);
    private static final int ORANGE = Color.rgb(244, 128, 36);

    private static final int[] COMPLICATION_IDS = {
            LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID
    };

    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            }
    };

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to check if complication location
    // is supported in settings config activity.
    public static int getComplicationId(
            AnalogComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case RIGHT:
                return RIGHT_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to retrieve all complication ids.
    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link AnalogComplicationConfigRecyclerViewAdapter} to see which complication types
    // are supported in the settings config activity.
    public static int[] getSupportedComplicationTypes(
            AnalogComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case RIGHT:
                return COMPLICATION_SUPPORTED_TYPES[1];
            default:
                return new int[] {};
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SOWatchFace.Engine> mWeakReference;

        public EngineHandler(SOWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SOWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;
        private Paint mBackgroundPaint;
        private Paint mForeGroundPaint;

        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;
        private Bitmap mMainTickBitmap;
        private Bitmap mGrayMainTickBitmap;
        private Bitmap mHourBitmap;
        private Bitmap mGrayHourBitmap;
        private Bitmap mMinuteBitmap;
        private Bitmap mGrayMinuteBitmap;
        private Bitmap mSecondBitmap;

        // Used to pull user's preferences for background color, highlight color, and visual
        // indicating there are unread notifications.
        SharedPreferences mSharedPref;

        // User's preference for if they want visual shown to indicate unread notifications.
        private boolean mUnreadNotificationsPreference;
        private boolean mUTCNotchPreference;
        private boolean mDesignPreference;
        private String mSiteName = "";
        private int mUserId;
        private int mSEUserId;
        private int mNumberOfUnreadNotifications = 0;

        /* Maps active complication ids to the data for that complication. Note: Data will only be
         * present if the user has chosen a provider via the settings activity for the watch face.
         */
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

        /* Maps complication ids to corresponding ComplicationDrawable that renders the
         * the complication data on the watch face.
         */
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        private LogoDownloadService mLogoService;
        private SiteListService mSiteListService;

        private int mScreenWidth = -1;
        private int mScreenHeight = -1;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SOWatchFace.this)
                    .setAcceptsTapEvents(true)
                    .setHideNotificationIndicator(true)
                    .build());


            Context context = getApplicationContext();
            mSharedPref =
                    context.getSharedPreferences(
                            getString(R.string.analog_complication_preference_file_key),
                            Context.MODE_PRIVATE);

            mCalendar = Calendar.getInstance();
            mLogoService = new LogoDownloadService(context);
            mSiteListService = new SiteListService(context);

            initializeLogoDownload();
            loadSavedPreferences();
            initializeComplications();
            initializeBackground();
        }

        private void initializeLogoDownload() {
            if (!mLogoService.logoExists(mLogoService.getCurrentSiteCodeName())) {
                Runnable reloadBackgrounds = new Runnable() {
                    @Override
                    public void run() {
                        regenerateScreenData();
                    }
                };
                mLogoService.downloadSiteIcon(mLogoService.getCurrentSiteCodeName(), reloadBackgrounds);
            }
        }

        // Pulls all user's preferences for watch face appearance.
        private void loadSavedPreferences() {

            String unreadNotificationPreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_unread_notifications_pref);

            String utcNotchPreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_utc_notch_pref);

            String designPreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_design_pref);

            String userIdPreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_user_id_pref);

            String seUserIdPreferenceResourceName =
                    getApplicationContext().getString(R.string.saved_se_user_id_pref);

            boolean oldDesignPreference = mDesignPreference;
            String oldSiteName = mSiteName;
            int oldUserId = mUserId;
            int oldSEUserId = mSEUserId;

            mUnreadNotificationsPreference =
                    mSharedPref.getBoolean(unreadNotificationPreferenceResourceName, true);

            mUTCNotchPreference =
                    mSharedPref.getBoolean(utcNotchPreferenceResourceName, true);

            mDesignPreference =
                    mSharedPref.getBoolean(designPreferenceResourceName, true);

            mUserId =
                    mSharedPref.getInt(userIdPreferenceResourceName, JON_SKEET_ID);

            mSEUserId =
                    mSharedPref.getInt(seUserIdPreferenceResourceName, JON_SKEET_SE_ID);

            mSiteName = mLogoService.getCurrentSiteCodeName();

            if (mUserId != oldUserId || mSEUserId != oldSEUserId) {
                setActiveComplications();
                setActiveComplications(COMPLICATION_IDS);
            }

            if (oldDesignPreference != mDesignPreference) {
                setActiveComplications();
                regenerateScreenData();
                setActiveComplications(COMPLICATION_IDS);
            }

            if (!mSiteName.equals(oldSiteName)) {
                setActiveComplications();
                initializeLogoDownload();
                regenerateScreenData();
                setActiveComplications(COMPLICATION_IDS);
            }
        }

        private void initializeComplications() {
            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face. In this watch face, we create one for left, right,
            // and background, but you could add many more.
            ComplicationDrawable leftComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            ComplicationDrawable rightComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            // Adds new complications to a SparseArray to simplify setting styles and ambient
            // properties for all complications, i.e., iterate over them all.
            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(RIGHT_COMPLICATION_ID, rightComplicationDrawable);

            setComplicationsActiveAndAmbientColors();
            setActiveComplications(COMPLICATION_IDS);
        }

        /* Sets active/ambient mode colors for all complications.
         *
         * Note: With the rest of the watch face, we update the paint colors based on
         * ambient/active mode callbacks, but because the ComplicationDrawable handles
         * the active/ambient colors, we only set the colors twice. Once at initialization and
         * again if the user changes the highlight color via AnalogComplicationConfigActivity.
         */
        private void setComplicationsActiveAndAmbientColors() {
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                // Active mode colors.
                complicationDrawable.setBorderColorActive(ORANGE);
                complicationDrawable.setRangedValuePrimaryColorActive(GRAY);

                // Ambient mode colors.
                complicationDrawable.setBorderColorAmbient(GRAY);
                complicationDrawable.setRangedValuePrimaryColorAmbient(GRAY);
            }
        }

        private void initializeBackground() {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);

            mForeGroundPaint = new Paint();
            mForeGroundPaint.setColor(GRAY);

            if (mDesignPreference) {
                mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_2);
            } else {
                mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg_1);
            }

            mMainTickBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.main_tick);
            mHourBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.hour);
            mMinuteBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.minute);
            mSecondBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.second);

            addIconsToBackground();
        }

        private void addIconsToBackground() {
            if (mLogoService.logoExists(mLogoService.getCurrentSiteCodeName())){
                Bitmap icon = BitmapFactory.decodeFile(mLogoService.getIconFile(mLogoService.getCurrentSiteCodeName()).getAbsolutePath());

                mBackgroundBitmap = mBackgroundBitmap.copy(Bitmap.Config.ARGB_8888, true);

                if (mDesignPreference) {
                    // for the SWAG watch we draw a small logo, and the title text nearby
                    Bitmap smallLogo = LogoDownloadService.resizeLogo(icon, 36, 36);

                    Canvas canvas = new Canvas(mBackgroundBitmap);
                    canvas.drawBitmap(smallLogo, 144 - smallLogo.getWidth() / 2, 113 - smallLogo.getHeight() / 2, null);

                    Paint font = new Paint();
                    font.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
                    font.setColor(ORANGE);
                    font.setTextAlign(Paint.Align.LEFT);
                    font.setTextSize(24);

                    String shortName = mSiteListService.getShortName(mLogoService.getCurrentSiteCodeName());
                    canvas.drawText(shortName, 167, 123, font);
                } else {
                    // for the simple watch we just draw the logo dimmed
                    Paint alphaPaint = new Paint();
                    alphaPaint.setAlpha(110);

                    Bitmap largeLogo = LogoDownloadService.resizeLogo(icon, 150, 150);
                    Canvas canvas = new Canvas(mBackgroundBitmap);
                    canvas.drawBitmap(largeLogo, 160 - largeLogo.getWidth() / 2, 160 - largeLogo.getHeight() / 2, alphaPaint);
                }

            }
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            // Updates complications to properly render in ambient mode based on the
            // screen's capabilities.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);

                complicationDrawable.setLowBitAmbient(mLowBitAmbient);
                complicationDrawable.setBurnInProtection(mBurnInProtection);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mAmbient = inAmbientMode;

            // Update drawable complications' ambient state.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);
                complicationDrawable.setInAmbientMode(mAmbient);
            }

            // Check and trigger whether or not timer should be running (only in active mode).
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            mScreenHeight = height;
            mScreenWidth = width;

            mCenterX = width / 2f;
            mCenterY = height / 2f;

            regenerateScreenData();
        }

        private void regenerateScreenData() {
            Log.d(TAG, "Regenerating Screen Data");

            initializeBackground();

            if (mScreenWidth == -1 || mScreenHeight == -1)
                return;

            /* Scale loaded background image (more efficient) if surface dimensions change. */
            float scale = ((float) mScreenWidth) / (float) mBackgroundBitmap.getWidth();

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * scale),
                    (int) (mBackgroundBitmap.getHeight() * scale), true);
            mMainTickBitmap = Bitmap.createScaledBitmap(mMainTickBitmap,
                    (int) (mMainTickBitmap.getWidth() * scale),
                    (int) (mMainTickBitmap.getHeight() * scale), true);
            mHourBitmap = Bitmap.createScaledBitmap(mHourBitmap,
                    (int) (mHourBitmap.getWidth() * scale),
                    (int) (mHourBitmap.getHeight() * scale), true);
            mMinuteBitmap = Bitmap.createScaledBitmap(mMinuteBitmap,
                    (int) (mMinuteBitmap.getWidth() * scale),
                    (int) (mMinuteBitmap.getHeight() * scale), true);
            mSecondBitmap = Bitmap.createScaledBitmap(mSecondBitmap,
                    (int) (mSecondBitmap.getWidth() * scale),
                    (int) (mSecondBitmap.getHeight() * scale), true);
            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don't want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren't
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            if (!mBurnInProtection) {
                initGrayBackgroundBitmap();
            }

            int sizeOfComplication = mScreenWidth / 5;
            int midpointOfScreen = mScreenWidth / 2;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            horizontalOffset,
                            verticalOffset,
                            (horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            leftComplicationDrawable.setBounds(leftBounds);

            Rect rightBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (midpointOfScreen + horizontalOffset),
                            verticalOffset,
                            (midpointOfScreen + horizontalOffset + sizeOfComplication),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable rightComplicationDrawable =
                    mComplicationDrawableSparseArray.get(RIGHT_COMPLICATION_ID);
            rightComplicationDrawable.setBounds(rightBounds);
        }

        private void initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(
                    mBackgroundBitmap.getWidth(),
                    mBackgroundBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            mGrayHourBitmap = Bitmap.createBitmap(
                    mHourBitmap.getWidth(),
                    mHourBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            mGrayMainTickBitmap = Bitmap.createBitmap(
                    mMainTickBitmap.getWidth(),
                    mMainTickBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            mGrayMinuteBitmap = Bitmap.createBitmap(
                    mMinuteBitmap.getWidth(),
                    mMinuteBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);

            Paint grayPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            grayPaint.setColorFilter(filter);

            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
            canvas = new Canvas(mGrayHourBitmap);
            canvas.drawBitmap(mHourBitmap, 0, 0, grayPaint);
            canvas = new Canvas(mGrayMainTickBitmap);
            canvas.drawBitmap(mMainTickBitmap, 0, 0, grayPaint);
            canvas = new Canvas(mGrayMinuteBitmap);
            canvas.drawBitmap(mMinuteBitmap, 0, 0, grayPaint);
        }

        /*
         * Called when there is updated data for a complication id.
         */
        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TAP:

                    // If your background complication is the first item in your array, you need
                    // to walk backward through the array to make sure the tap isn't for a
                    // complication above the background complication.
                    for (int i = COMPLICATION_IDS.length - 1; i >= 0; i--) {
                        int complicationId = COMPLICATION_IDS[i];
                        ComplicationDrawable complicationDrawable =
                                mComplicationDrawableSparseArray.get(complicationId);

                        boolean successfulTap = complicationDrawable.onTap(x, y);

                        if (successfulTap) {
                            return;
                        }
                    }
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawUnreadNotificationIcon(canvas);
            drawComplications(canvas, now);
            drawWatchFace(canvas);
        }

        private void drawBackground(Canvas canvas) {
            if (mAmbient && (mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
            }
        }

        private void drawUnreadNotificationIcon(Canvas canvas) {

            if (mUnreadNotificationsPreference && (mNumberOfUnreadNotifications > 0)) {

                int width = canvas.getWidth();
                int height = canvas.getHeight();

                canvas.drawCircle(width / 2, height - 50, 10, mForeGroundPaint);

                /*
                 * Ensure center highlight circle is only drawn in interactive mode. This ensures
                 * we don't burn the screen with a solid circle in ambient mode.
                 */
                if (!mAmbient) {
                    canvas.drawCircle(width / 2, height - 50, 4, mForeGroundPaint);
                }
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        private void drawWatchFace(Canvas canvas) {
            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds = mCalendar.get(Calendar.SECOND);
            final float secondsRotation = seconds * 6f;

            final float minutesHandOffset = seconds / 10f;
            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f + minutesHandOffset;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Draw the UTC offset which will show the time when SO resets itself
             */
            final TimeZone tz = TimeZone.getDefault();
            final Date now = new Date();
            final float offsetFromUtc = tz.getOffset(now.getTime()) / 1000;

            if (mUTCNotchPreference) {
                final float mainTickRotation = offsetFromUtc / 120;
                canvas.save();
                canvas.rotate(mainTickRotation, mCenterX, mCenterY);
                canvas.drawBitmap(mAmbient ? mGrayMainTickBitmap : mMainTickBitmap, 0, 0, null);
                canvas.restore();
            }

            canvas.save();
            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawBitmap(mAmbient ? mGrayHourBitmap : mHourBitmap, 0, 0, null);
            canvas.restore();

            canvas.save();
            canvas.rotate(minutesRotation, mCenterX, mCenterY);
            canvas.drawBitmap(mAmbient ? mGrayMinuteBitmap : mMinuteBitmap, 0, 0, null);
            canvas.restore();

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAmbient) {
                canvas.save();
                canvas.rotate(secondsRotation, mCenterX, mCenterY);
                canvas.drawBitmap(mSecondBitmap, 0, 0, null);
                canvas.restore();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                loadSavedPreferences();
                setComplicationsActiveAndAmbientColors();

                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onUnreadCountChanged(int count) {
            if (mUnreadNotificationsPreference) {
                if (mNumberOfUnreadNotifications != count) {
                    mNumberOfUnreadNotifications = count;
                    invalidate();
                }
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SOWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SOWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
