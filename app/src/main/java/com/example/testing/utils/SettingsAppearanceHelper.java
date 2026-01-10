package com.example.testing.utils;

import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;

import androidx.cardview.widget.CardView;

import com.github.antonpopoff.colorwheel.ColorWheel;
import com.github.antonpopoff.colorwheel.gradientseekbar.GradientSeekBar;

public class SettingsAppearanceHelper {

    private final ColorWheel colorWheel;
    private final GradientSeekBar gradientSeekBar;
    private final CardView previewPrimary, previewSecondary;
    private final LinearLayout containerPrimary, containerSecondary;

    private int currentColorPrimary = Color.BLACK;
    private int currentColorSecondary = Color.BLACK;

    private boolean isEditingPrimary = true;

    public SettingsAppearanceHelper(ColorWheel colorWheel, GradientSeekBar gradientSeekBar,
                                    CardView[] previews, LinearLayout[] containers) {
        this.colorWheel = colorWheel;
        this.gradientSeekBar = gradientSeekBar;
        this.previewPrimary = previews[0];
        this.previewSecondary = previews[1];
        this.containerPrimary = containers[0];
        this.containerSecondary = containers[1];

        setupListeners();
        updateSelectionUi();
    }

    private void setupListeners() {
        // Selection Listeners
        View.OnClickListener selectPrimary = v -> {
            isEditingPrimary = true;
            updateSelectionUi();
            syncWheelToCurrentColor();
        };

        View.OnClickListener selectSecondary = v -> {
            isEditingPrimary = false;
            updateSelectionUi();
            syncWheelToCurrentColor();
        };

        containerPrimary.setOnClickListener(selectPrimary);
        previewPrimary.setOnClickListener(selectPrimary);
        containerSecondary.setOnClickListener(selectSecondary);
        previewSecondary.setOnClickListener(selectSecondary);

        // Color Wheel Listener
        colorWheel.setColorChangeListener(rgb -> {
            if (isEditingPrimary) {
                currentColorPrimary = rgb;
                previewPrimary.setCardBackgroundColor(rgb);
            } else {
                currentColorSecondary = rgb;
                previewSecondary.setCardBackgroundColor(rgb);
            }
            // Update Gradient Bar to match new color
            gradientSeekBar.setStartColor(Color.BLACK);
            gradientSeekBar.setEndColor(rgb);
            return null;
        });

        // Gradient (Brightness) Listener
        gradientSeekBar.setColorChangeListener((offset, argb) -> {
            // Update the preview directly as we slide
            if (isEditingPrimary) {
                currentColorPrimary = argb;
                previewPrimary.setCardBackgroundColor(argb);
            } else {
                currentColorSecondary = argb;
                previewSecondary.setCardBackgroundColor(argb);
            }
            return null;
        });
    }

    private void updateSelectionUi() {
        float activeAlpha = 1.0f;
        float inactiveAlpha = 0.3f;

        containerPrimary.setAlpha(isEditingPrimary ? activeAlpha : inactiveAlpha);
        containerSecondary.setAlpha(!isEditingPrimary ? activeAlpha : inactiveAlpha);
    }

    private void syncWheelToCurrentColor() {
        int targetColor = isEditingPrimary ? currentColorPrimary : currentColorSecondary;

        // This sets the RGB on the wheel (hue/saturation)
        colorWheel.setRgb(targetColor);

        // This sets the gradient bar to range from Black -> Target Color
        gradientSeekBar.setStartColor(Color.BLACK);
        gradientSeekBar.setEndColor(targetColor);
    }

    public void setColors(int primary, int secondary) {
        currentColorPrimary = primary;
        currentColorSecondary = secondary;

        previewPrimary.setCardBackgroundColor(primary);
        previewSecondary.setCardBackgroundColor(secondary);

        // Initialize wheel
        syncWheelToCurrentColor();
    }

    public int getPrimaryColor() { return currentColorPrimary; }
    public int getSecondaryColor() { return currentColorSecondary; }
}