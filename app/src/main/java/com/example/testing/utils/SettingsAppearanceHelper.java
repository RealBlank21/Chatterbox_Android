package com.example.testing.utils;

import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;

import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;

import com.github.antonpopoff.colorwheel.ColorWheel;
import com.github.antonpopoff.colorwheel.gradientseekbar.GradientSeekBar;
import com.example.testing.R;

public class SettingsAppearanceHelper {

    private final ColorWheel colorWheel;
    private final GradientSeekBar gradientSeekBar;
    private final CardView previewPrimary, previewSecondary;
    private final LinearLayout containerPrimary, containerSecondary;

    private int currentColorPrimary = Color.BLACK;
    private int currentColorSecondary = Color.BLACK;

    // We store the "Base Color" (Fully Bright) separately.
    // This allows us to drag the slider to Black and back without losing the Hue.
    private int currentBasePrimary = Color.WHITE;
    private int currentBaseSecondary = Color.WHITE;

    private boolean isEditingPrimary = true;
    private boolean isSyncing = false;

    public SettingsAppearanceHelper(ColorWheel colorWheel, GradientSeekBar gradientSeekBar,
                                    CardView[] previews, LinearLayout[] containers) {
        this.colorWheel = colorWheel;
        this.gradientSeekBar = gradientSeekBar;
        this.previewPrimary = previews[0];
        this.previewSecondary = previews[1];
        this.containerPrimary = containers[0];
        this.containerSecondary = containers[1];

        setupListeners();
    }

    private void setupListeners() {
        View.OnClickListener selectPrimary = v -> {
            if (!isEditingPrimary) {
                isEditingPrimary = true;
                syncUiToCurrentState();
                updateSelectionUi();
            }
        };

        View.OnClickListener selectSecondary = v -> {
            if (isEditingPrimary) {
                isEditingPrimary = false;
                syncUiToCurrentState();
                updateSelectionUi();
            }
        };

        containerPrimary.setOnClickListener(selectPrimary);
        previewPrimary.setOnClickListener(selectPrimary);
        containerSecondary.setOnClickListener(selectSecondary);
        previewSecondary.setOnClickListener(selectSecondary);

        // 1. Color Wheel Listener (Updates Hue/Saturation)
        colorWheel.setColorChangeListener(rgb -> {
            if (isSyncing) return null;

            // Update the "Base" color (the fully bright version)
            if (isEditingPrimary) currentBasePrimary = rgb;
            else currentBaseSecondary = rgb;

            // Update the slider gradient immediately
            gradientSeekBar.setEndColor(rgb);

            // Calculate the actual color based on the CURRENT slider offset
            float offset = gradientSeekBar.getOffset();
            int finalColor = ColorUtils.blendARGB(Color.BLACK, rgb, offset);

            updateCurrentColor(finalColor);
            return null;
        });

        // 2. Gradient Slider Listener (Updates Brightness)
        gradientSeekBar.setColorChangeListener((offset, argb) -> {
            if (isSyncing) return null;

            // FIX: Ignore the 'argb' passed by the library.
            // Calculate it manually to ensure consistency with the Wheel listener.
            int baseColor = isEditingPrimary ? currentBasePrimary : currentBaseSecondary;
            int finalColor = ColorUtils.blendARGB(Color.BLACK, baseColor, offset);

            updateCurrentColor(finalColor);
            return null;
        });
    }

    private void updateCurrentColor(int color) {
        if (isEditingPrimary) {
            currentColorPrimary = color;
            previewPrimary.setCardBackgroundColor(color);
        } else {
            currentColorSecondary = color;
            previewSecondary.setCardBackgroundColor(color);
        }
    }

    private void syncUiToCurrentState() {
        isSyncing = true;
        try {
            // 1. Get the colors we are working with
            int targetColor = isEditingPrimary ? currentColorPrimary : currentColorSecondary;
            int baseColor = isEditingPrimary ? currentBasePrimary : currentBaseSecondary;

            // 2. Calculate Brightness (Value) manually
            // We use the Alpha blending logic reversely: Value = max(R,G,B) / 255.
            float[] hsv = new float[3];
            Color.colorToHSV(targetColor, hsv);
            float brightness = hsv[2];

            // 3. Special Case: If the color is very dark or black, we rely on the saved 'BaseColor'.
            // If the user loaded a Dark Gray (5,5,5) but BaseColor was Red, we need to correct BaseColor to White.
            // Check if target is Grayscale (R=G=B)
            if (Color.red(targetColor) == Color.green(targetColor) && Color.green(targetColor) == Color.blue(targetColor)) {
                // If it is Grayscale (and not transparent), the Base Color must be White.
                if (targetColor != Color.TRANSPARENT) {
                    baseColor = Color.WHITE;
                    // Update the saved base so dragging up reveals Gray/White, not Red.
                    if (isEditingPrimary) currentBasePrimary = baseColor;
                    else currentBaseSecondary = baseColor;
                }
            }

            // 4. Update UI
            gradientSeekBar.setStartColor(Color.BLACK);
            gradientSeekBar.setEndColor(baseColor);
            colorWheel.setRgb(baseColor);
            gradientSeekBar.setOffset(brightness);

        } finally {
            isSyncing = false;
        }
    }

    public void setColors(int primary, int secondary) {
        currentColorPrimary = primary;
        currentColorSecondary = secondary;

        // Initialize "Base Colors" (Fully Saturated versions)
        // This ensures the slider gradient looks correct immediately on load.
        currentBasePrimary = getSaturatedColor(primary);
        currentBaseSecondary = getSaturatedColor(secondary);

        previewPrimary.setCardBackgroundColor(primary);
        previewSecondary.setCardBackgroundColor(secondary);

        isEditingPrimary = true;
        syncUiToCurrentState();
        updateSelectionUi();
    }

    // Helper to extract the fully saturated color (Value = 1.0) from a given color
    private int getSaturatedColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = 1.0f; // Force brightness to max
        return Color.HSVToColor(hsv);
    }

    private void updateSelectionUi() {
        containerPrimary.setBackgroundResource(isEditingPrimary ? R.drawable.bg_character_message : 0);
        containerSecondary.setBackgroundResource(!isEditingPrimary ? R.drawable.bg_character_message : 0);
    }

    public int getPrimaryColor() { return currentColorPrimary; }
    public int getSecondaryColor() { return currentColorSecondary; }
}