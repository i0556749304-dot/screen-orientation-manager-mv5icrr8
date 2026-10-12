package com.generated.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OrientationReceiver extends BroadcastReceiver {
    public static final String ACTION_CHANGE_ORIENTATION = "com.generated.app.ACTION_CHANGE_ORIENTATION";
    public static final String ACTION_STOP_SERVICE = "com.generated.app.ACTION_STOP_SERVICE";
    public static final String EXTRA_ORIENTATION = "extra_orientation";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (ACTION_CHANGE_ORIENTATION.equals(action)) {
            int requested = intent.getIntExtra(EXTRA_ORIENTATION, OrientationService.ORIENTATION_SENSOR);
            OrientationService.startServiceWithOrientation(context, requested);
        } else if (ACTION_STOP_SERVICE.equals(action)) {
            OrientationService.stopService(context);
        }
    }
}
