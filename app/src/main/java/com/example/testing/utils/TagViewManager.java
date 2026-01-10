package com.example.testing.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;

import com.google.android.material.chip.Chip;

public class TagViewManager {

    // Existing method for "Edit Mode" (Add/Remove tags)
    public static Chip createTagChip(Context context, String tag, View.OnClickListener onCloseListener) {
        Chip chip = new Chip(context);
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(onCloseListener);
        styleChip(context, chip, tag);
        return chip;
    }

    // New method for "Filter Mode" (Select/Deselect tags)
    public static Chip createFilterChip(Context context, String tag, boolean isSelected, CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        Chip chip = new Chip(context);
        chip.setText(tag);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setChecked(isSelected);

        styleChip(context, chip, tag);

        // Visual adjustment for selected state
        if (isSelected) {
            int color = getTagColor(tag);
            chip.setChipBackgroundColor(ColorStateList.valueOf(color));
            chip.setTextColor(Color.WHITE);
        }

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Re-apply style on state change to handle background colors
            if (isChecked) {
                int color = getTagColor(tag);
                chip.setChipBackgroundColor(ColorStateList.valueOf(color));
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT));
                chip.setTextColor(getTagColor(tag));
            }
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
            }
        });

        return chip;
    }

    private static void styleChip(Context context, Chip chip, String tag) {
        int color = getTagColor(tag);
        chip.setTextColor(color);
        chip.setChipStrokeColor(ColorStateList.valueOf(color));
        chip.setChipStrokeWidth(dpToPx(context, 1));
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.TRANSPARENT));
        chip.setRippleColor(ColorStateList.valueOf(Color.parseColor("#20" + Integer.toHexString(color).substring(2))));
        chip.setCloseIconTint(ColorStateList.valueOf(color));
    }

    private static int getTagColor(String tag) {
        int hash = tag.hashCode();
        float[] hsv = new float[3];
        hsv[0] = Math.abs(hash) % 360;
        hsv[1] = 0.6f + (Math.abs(hash * 7) % 40) / 100f; // Saturation
        hsv[2] = 0.85f; // Value (Brightness) - slightly higher for readability
        return Color.HSVToColor(hsv);
    }

    private static float dpToPx(Context context, int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}