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
    private final CardView[] previews;
    private final LinearLayout[] containers;

    private final int[] currentColors = new int[4];
    private final int[] baseColors = new int[4];

    private int activeIndex = 0;
    private boolean isSyncing = false;

    public SettingsAppearanceHelper(ColorWheel colorWheel, GradientSeekBar gradientSeekBar,
                                    CardView[] previews, LinearLayout[] containers) {
        this.colorWheel = colorWheel;
        this.gradientSeekBar = gradientSeekBar;
        this.previews = previews;
        this.containers = containers;

        currentColors[0] = Color.BLACK; baseColors[0] = Color.RED;
        currentColors[1] = Color.BLACK; baseColors[1] = Color.CYAN;
        currentColors[2] = Color.GRAY;  baseColors[2] = Color.GRAY;
        currentColors[3] = Color.WHITE; baseColors[3] = Color.WHITE;

        setupListeners();
    }

    private void setupListeners() {
        for (int i = 0; i < containers.length; i++) {
            final int index = i;
            View.OnClickListener listener = v -> {
                if (activeIndex != index) {
                    activeIndex = index;
                    syncUiToCurrentState();
                    updateSelectionUi();
                }
            };
            containers[i].setOnClickListener(listener);
            previews[i].setOnClickListener(listener);
        }

        colorWheel.setColorChangeListener(rgb -> {
            if (isSyncing) return null;

            baseColors[activeIndex] = rgb;
            gradientSeekBar.setEndColor(rgb);

            float offset = gradientSeekBar.getOffset();
            int finalColor = ColorUtils.blendARGB(Color.BLACK, rgb, offset);

            updateCurrentColor(finalColor);
            return null;
        });

        gradientSeekBar.setColorChangeListener((offset, argb) -> {
            if (isSyncing) return null;

            int baseColor = baseColors[activeIndex];
            int finalColor = ColorUtils.blendARGB(Color.BLACK, baseColor, offset);

            updateCurrentColor(finalColor);
            return null;
        });
    }

    private void updateCurrentColor(int color) {
        currentColors[activeIndex] = color;
        previews[activeIndex].setCardBackgroundColor(color);
    }

    private void syncUiToCurrentState() {
        isSyncing = true;
        try {
            int targetColor = currentColors[activeIndex];
            int baseColor = baseColors[activeIndex];

            float[] hsv = new float[3];
            Color.colorToHSV(targetColor, hsv);
            float brightness = hsv[2];

            if (Color.red(targetColor) == Color.green(targetColor) && Color.green(targetColor) == Color.blue(targetColor)) {
                if (targetColor != Color.TRANSPARENT) {
                    baseColor = Color.WHITE;
                    baseColors[activeIndex] = baseColor;
                }
            }

            gradientSeekBar.setStartColor(Color.BLACK);
            gradientSeekBar.setEndColor(baseColor);
            colorWheel.setRgb(baseColor);
            gradientSeekBar.setOffset(brightness);

        } finally {
            isSyncing = false;
        }
    }

    public void setColors(int primary, int secondary, int narrative, int dialogue) {
        currentColors[0] = primary;
        currentColors[1] = secondary;
        currentColors[2] = narrative;
        currentColors[3] = dialogue;

        for (int i = 0; i < 4; i++) {
            baseColors[i] = getSaturatedColor(currentColors[i]);
            previews[i].setCardBackgroundColor(currentColors[i]);
        }

        syncUiToCurrentState();
        updateSelectionUi();
    }

    private int getSaturatedColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = 1.0f;
        return Color.HSVToColor(hsv);
    }

    private void updateSelectionUi() {
        for (int i = 0; i < containers.length; i++) {
            containers[i].setBackgroundResource(i == activeIndex ? R.drawable.bg_character_message : 0);
        }
    }

    public int getPrimaryColor() { return currentColors[0]; }
    public int getSecondaryColor() { return currentColors[1]; }
    public int getNarrativeColor() { return currentColors[2]; }
    public int getDialogueColor() { return currentColors[3]; }
}