package com.generated.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY_PERMISSION = 2001;

    private LinearLayout statusBox;
    private TextView tvStatusIcon;
    private TextView tvStatusText;
    private Button btnToggleService;
    private LinearLayout boxPermissionWarning;
    private Button btnGrantPermission;

    private Button btnPortrait;
    private Button btnLandscape;
    private Button btnReversePortrait;
    private Button btnReverseLandscape;
    private Button btnSensor;

    private Button btnTilePortLand;
    private Button btnTileAll;
    private TextView tvTileModeSummary;

    private SharedPreferences prefs;
    private boolean isServiceRunning;
    private int selectedOrientation;
    private int selectedTileMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(OrientationService.PREFS_NAME, MODE_PRIVATE);

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadState();
        checkPermissions();
        updateUI();
    }

    private void initViews() {
        statusBox = (LinearLayout) findViewById(R.id.status_box);
        tvStatusIcon = (TextView) findViewById(R.id.tv_status_icon);
        tvStatusText = (TextView) findViewById(R.id.tv_status_text);
        btnToggleService = (Button) findViewById(R.id.btn_toggle_service);
        boxPermissionWarning = (LinearLayout) findViewById(R.id.box_permission_warning);
        btnGrantPermission = (Button) findViewById(R.id.btn_grant_permission);

        btnPortrait = (Button) findViewById(R.id.btn_mode_portrait);
        btnLandscape = (Button) findViewById(R.id.btn_mode_landscape);
        btnReversePortrait = (Button) findViewById(R.id.btn_mode_reverse_portrait);
        btnReverseLandscape = (Button) findViewById(R.id.btn_mode_reverse_landscape);
        btnSensor = (Button) findViewById(R.id.btn_mode_sensor);

        btnTilePortLand = (Button) findViewById(R.id.btn_tile_port_land);
        btnTileAll = (Button) findViewById(R.id.btn_tile_all);
        tvTileModeSummary = (TextView) findViewById(R.id.tv_tile_mode_summary);
    }

    private void setupListeners() {
        btnToggleService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasOverlayPermission()) {
                    requestOverlayPermission();
                    return;
                }

                if (isServiceRunning) {
                    OrientationService.stopService(MainActivity.this);
                    isServiceRunning = false;
                } else {
                    OrientationService.startServiceWithOrientation(MainActivity.this, selectedOrientation);
                    isServiceRunning = true;
                }
                updateUI();
            }
        });

        btnGrantPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOverlayPermission();
            }
        });

        View.OnClickListener modeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.btn_mode_portrait) {
                    selectedOrientation = OrientationService.ORIENTATION_PORTRAIT;
                } else if (id == R.id.btn_mode_landscape) {
                    selectedOrientation = OrientationService.ORIENTATION_LANDSCAPE;
                } else if (id == R.id.btn_mode_reverse_portrait) {
                    selectedOrientation = OrientationService.ORIENTATION_REVERSE_PORTRAIT;
                } else if (id == R.id.btn_mode_reverse_landscape) {
                    selectedOrientation = OrientationService.ORIENTATION_REVERSE_LANDSCAPE;
                } else if (id == R.id.btn_mode_sensor) {
                    selectedOrientation = OrientationService.ORIENTATION_SENSOR;
                }

                if (isServiceRunning) {
                    if (hasOverlayPermission()) {
                        OrientationService.startServiceWithOrientation(MainActivity.this, selectedOrientation);
                    } else {
                        requestOverlayPermission();
                    }
                } else {
                    prefs.edit().putInt(OrientationService.KEY_CURRENT_ORIENTATION, selectedOrientation).apply();
                }

                updateUI();
            }
        };

        btnPortrait.setOnClickListener(modeClickListener);
        btnLandscape.setOnClickListener(modeClickListener);
        btnReversePortrait.setOnClickListener(modeClickListener);
        btnReverseLandscape.setOnClickListener(modeClickListener);
        btnSensor.setOnClickListener(modeClickListener);

        btnTilePortLand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTileMode = OrientationService.TILE_MODE_PORTRAIT_LANDSCAPE;
                prefs.edit().putInt(OrientationService.KEY_TILE_MODE, selectedTileMode).apply();
                notifyTileUpdate();
                Toast.makeText(MainActivity.this, getString(R.string.tile_mode_changed_toast), Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });

        btnTileAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTileMode = OrientationService.TILE_MODE_ALL;
                prefs.edit().putInt(OrientationService.KEY_TILE_MODE, selectedTileMode).apply();
                notifyTileUpdate();
                Toast.makeText(MainActivity.this, getString(R.string.tile_mode_changed_toast), Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
    }

    private void loadState() {
        isServiceRunning = prefs.getBoolean(OrientationService.KEY_IS_RUNNING, false);
        selectedOrientation = prefs.getInt(
                OrientationService.KEY_CURRENT_ORIENTATION,
                OrientationService.ORIENTATION_PORTRAIT
        );
        selectedTileMode = prefs.getInt(
                OrientationService.KEY_TILE_MODE,
                OrientationService.TILE_MODE_PORTRAIT_LANDSCAPE
        );
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Toast.makeText(this, "אנא אשר הצגה מעל אפליקציות אחרות", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, REQ_OVERLAY_PERMISSION);
        }
    }

    private void checkPermissions() {
        if (!hasOverlayPermission()) {
            boxPermissionWarning.setVisibility(View.VISIBLE);
        } else {
            boxPermissionWarning.setVisibility(View.GONE);
        }
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

    private void updateUI() {
        if (isServiceRunning) {
            statusBox.setBackgroundResource(R.drawable.bg_status_active);
            tvStatusIcon.setText("🟢");
            tvStatusText.setText(getString(R.string.status_active));
            btnToggleService.setText(getString(R.string.disable_service));
            btnToggleService.setBackgroundResource(R.drawable.btn_danger);
        } else {
            statusBox.setBackgroundResource(R.drawable.bg_status_inactive);
            tvStatusIcon.setText("⚪");
            tvStatusText.setText(getString(R.string.status_inactive));
            btnToggleService.setText(getString(R.string.enable_service));
            btnToggleService.setBackgroundResource(R.drawable.btn_primary);
        }

        highlightButton(btnPortrait, selectedOrientation == OrientationService.ORIENTATION_PORTRAIT);
        highlightButton(btnLandscape, selectedOrientation == OrientationService.ORIENTATION_LANDSCAPE);
        highlightButton(btnReversePortrait, selectedOrientation == OrientationService.ORIENTATION_REVERSE_PORTRAIT);
        highlightButton(btnReverseLandscape, selectedOrientation == OrientationService.ORIENTATION_REVERSE_LANDSCAPE);
        highlightButton(btnSensor, selectedOrientation == OrientationService.ORIENTATION_SENSOR);

        // Highlight Quick Settings tile mode selection
        boolean isPortLandMode = (selectedTileMode == OrientationService.TILE_MODE_PORTRAIT_LANDSCAPE);
        highlightButton(btnTilePortLand, isPortLandMode);
        highlightButton(btnTileAll, !isPortLandMode);

        if (isPortLandMode) {
            tvTileModeSummary.setText(getString(R.string.tile_mode_port_land_desc));
        } else {
            tvTileModeSummary.setText(getString(R.string.tile_mode_all_desc));
        }
    }

    private void highlightButton(Button btn, boolean isSelected) {
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.btn_mode_selected);
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundResource(R.drawable.btn_mode_unselected);
            btn.setTextColor(Color.parseColor("#0D1B2A"));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY_PERMISSION) {
            if (hasOverlayPermission()) {
                boxPermissionWarning.setVisibility(View.GONE);
                Toast.makeText(this, "הרשאה אושרה בהצלחה!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
