package com.generated.app;

import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class OrientationTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();

        SharedPreferences prefs = getSharedPreferences(OrientationService.PREFS_NAME, MODE_PRIVATE);
        boolean isRunning = prefs.getBoolean(OrientationService.KEY_IS_RUNNING, false);
        int currentOrientation = prefs.getInt(
                OrientationService.KEY_CURRENT_ORIENTATION,
                OrientationService.ORIENTATION_PORTRAIT
        );

        if (!isRunning) {
            OrientationService.startServiceWithOrientation(this, OrientationService.ORIENTATION_PORTRAIT);
        } else {
            // Cycle between Portrait -> Landscape -> Sensor -> Turn off
            if (currentOrientation == OrientationService.ORIENTATION_PORTRAIT) {
                OrientationService.startServiceWithOrientation(this, OrientationService.ORIENTATION_LANDSCAPE);
            } else if (currentOrientation == OrientationService.ORIENTATION_LANDSCAPE) {
                OrientationService.startServiceWithOrientation(this, OrientationService.ORIENTATION_SENSOR);
            } else {
                OrientationService.stopService(this);
            }
        }

        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(OrientationService.PREFS_NAME, MODE_PRIVATE);
        boolean isRunning = prefs.getBoolean(OrientationService.KEY_IS_RUNNING, false);
        int currentOrientation = prefs.getInt(
                OrientationService.KEY_CURRENT_ORIENTATION,
                OrientationService.ORIENTATION_PORTRAIT
        );

        if (isRunning) {
            tile.setState(Tile.STATE_ACTIVE);
            switch (currentOrientation) {
                case OrientationService.ORIENTATION_LANDSCAPE:
                    tile.setLabel("רוחב (לרוחב)");
                    break;
                case OrientationService.ORIENTATION_SENSOR:
                    tile.setLabel("חיישן אוטומטי");
                    break;
                case OrientationService.ORIENTATION_REVERSE_LANDSCAPE:
                    tile.setLabel("רוחב הפוך");
                    break;
                case OrientationService.ORIENTATION_REVERSE_PORTRAIT:
                    tile.setLabel("אורך הפוך");
                    break;
                case OrientationService.ORIENTATION_PORTRAIT:
                default:
                    tile.setLabel("אורך (לאורך)");
                    break;
            }
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("כיוון מסך: כבוי");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_launcher));
        }

        tile.updateTile();
    }
}
