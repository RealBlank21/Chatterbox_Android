package com.example.testing.ui.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ThemeUtils {

    private static final String PREFS_NAME = "chatterbox_theme_prefs";
    private static final String KEY_PRIMARY = "theme_color_primary";
    private static final String KEY_SECONDARY = "theme_color_secondary";

    // Defaults
    private static final int DEFAULT_PRIMARY = Color.parseColor("#212121");
    private static final int DEFAULT_SECONDARY = Color.parseColor("#FF03DAC5");

    public static void saveColors(Context context, int primary, int secondary) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_PRIMARY, primary)
                .putInt(KEY_SECONDARY, secondary)
                .apply();
    }

    public static int getPrimaryColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_PRIMARY, DEFAULT_PRIMARY);
    }

    public static int getSecondaryColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_SECONDARY, DEFAULT_SECONDARY);
    }

    public static void applyTheme(AppCompatActivity activity) {
        int primary = getPrimaryColor(activity);
        int secondary = getSecondaryColor(activity);

        // 1. Status Bar
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(primary);

        // 2. Action Bar (Toolbar)
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(primary));
        }
    }

    // Helper to tint standard views
    public static void tintFab(FloatingActionButton fab, int color) {
        if (fab != null) {
            fab.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }
}