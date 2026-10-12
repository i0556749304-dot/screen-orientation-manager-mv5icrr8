package com.generated.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public class OrientationService extends Service {

    public static final String PREFS_NAME = "OrientationPrefs";
    public static final String KEY_IS_RUNNING = "is_running";
    public static final String KEY_CURRENT_ORIENTATION = "current_orientation";
    public static final String KEY_TILE_MODE = "tile_cycle_mode";

    public static final int TILE_MODE_PORTRAIT_LANDSCAPE = 0;
    public static final int TILE_MODE_ALL = 1;

    public static final int ORIENTATION_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    public static final int ORIENTATION_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    public static final int ORIENTATION_REVERSE_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
    public static final int ORIENTATION_REVERSE_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
    public static final int ORIENTATION_SENSOR = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;

    public static final String ACTION_UPDATE_ORIENTATION = "com.generated.app.UPDATE_ORIENTATION";
    public static final String ACTION_STOP = "com.generated.app.STOP";
    public static final String EXTRA_TARGET_ORIENTATION = "target_orientation";

    private static final String NOTIFICATION_CHANNEL_ID = "orientation_control_channel";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams layoutParams;
    private int currentOrientation = ORIENTATION_PORTRAIT;

    public static void startServiceWithOrientation(Context context, int orientation) {
        Intent intent = new Intent(context, OrientationService.class);
        intent.setAction(ACTION_UPDATE_ORIENTATION);
        intent.putExtra(EXTRA_TARGET_ORIENTATION, orientation);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, OrientationService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            removeOverlay();
            setRunningPref(false);
            notifyTileUpdate();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null && intent.hasExtra(EXTRA_TARGET_ORIENTATION)) {
            currentOrientation = intent.getIntExtra(EXTRA_TARGET_ORIENTATION, ORIENTATION_PORTRAIT);
        } else {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            currentOrientation = prefs.getInt(KEY_CURRENT_ORIENTATION, ORIENTATION_PORTRAIT);
        }

        saveOrientationPref(currentOrientation);
        setRunningPref(true);
        notifyTileUpdate();

        Notification notification = buildNotification(currentOrientation);
        startForeground(NOTIFICATION_ID, notification);

        applyOverlayOrientation(currentOrientation);

        return START_STICKY;
    }

    private void applyOverlayOrientation(int orientation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return;
        }

        if (overlayView == null) {
            overlayView = new View(this);
            int windowType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                windowType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            layoutParams = new WindowManager.LayoutParams(
                    0,
                    0,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            layoutParams.gravity = Gravity.TOP | Gravity.START;
            layoutParams.screenOrientation = orientation;

            try {
                windowManager.addView(overlayView, layoutParams);
            } catch (Exception ignored) {
            }
        } else {
            layoutParams.screenOrientation = orientation;
            try {
                windowManager.updateViewLayout(overlayView, layoutParams);
            } catch (Exception ignored) {
            }
        }
    }

    private void removeOverlay() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {
            }
            overlayView = null;
        }
    }

    private Notification buildNotification(int orientation) {
        String modeName = getOrientationLabel(orientation);

        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pOpenApp = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        PendingIntent pPortrait = createActionIntent(ORIENTATION_PORTRAIT, 1);
        PendingIntent pLandscape = createActionIntent(ORIENTATION_LANDSCAPE, 2);
        PendingIntent pSensor = createActionIntent(ORIENTATION_SENSOR, 3);
        PendingIntent pStop = createStopIntent(4);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText("מצב נוכחי: " + modeName)
                .setContentIntent(pOpenApp)
                .setOngoing(true)
                .addAction(0, getString(R.string.notification_btn_portrait), pPortrait)
                .addAction(0, getString(R.string.notification_btn_landscape), pLandscape)
                .addAction(0, getString(R.string.notification_btn_sensor), pSensor)
                .addAction(0, getString(R.string.notification_btn_close), pStop);

        return builder.build();
    }

    private PendingIntent createActionIntent(int orientation, int requestCode) {
        Intent intent = new Intent(this, OrientationReceiver.class);
        intent.setAction(OrientationReceiver.ACTION_CHANGE_ORIENTATION);
        intent.putExtra(OrientationReceiver.EXTRA_ORIENTATION, orientation);
        return PendingIntent.getBroadcast(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
    }

    private PendingIntent createStopIntent(int requestCode) {
        Intent intent = new Intent(this, OrientationReceiver.class);
        intent.setAction(OrientationReceiver.ACTION_STOP_SERVICE);
        return PendingIntent.getBroadcast(
                this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
    }

    private String getOrientationLabel(int orientation) {
        switch (orientation) {
            case ORIENTATION_PORTRAIT:
                return getString(R.string.mode_portrait);
            case ORIENTATION_LANDSCAPE:
                return getString(R.string.mode_landscape);
            case ORIENTATION_REVERSE_PORTRAIT:
                return getString(R.string.mode_reverse_portrait);
            case ORIENTATION_REVERSE_LANDSCAPE:
                return getString(R.string.mode_reverse_landscape);
            case ORIENTATION_SENSOR:
            default:
                return getString(R.string.mode_sensor);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Orientation Control",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows active screen orientation status and controls");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void saveOrientationPref(int orientation) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(KEY_CURRENT_ORIENTATION, orientation)
                .apply();
    }

    private void setRunningPref(boolean running) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_RUNNING, running)
                .apply();
    }

    private void notifyTileUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                TileService.requestListeningState(
                        this,
                        new ComponentName(this, OrientationTileService.class)
                );
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        setRunningPref(false);
        notifyTileUpdate();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
