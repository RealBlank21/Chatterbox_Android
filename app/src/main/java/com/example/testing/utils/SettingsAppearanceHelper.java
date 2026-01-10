package com.example.testing.ui.settings.utils;

import android.graphics.Color;
import android.widget.EditText;
import android.widget.SeekBar;
import androidx.cardview.widget.CardView;

public class SettingsAppearanceHelper {

    private final SeekBar seekPrimaryR, seekPrimaryG, seekPrimaryB;
    private final SeekBar seekSecondaryR, seekSecondaryG, seekSecondaryB;
    private final CardView previewPrimary, previewSecondary;
    private final EditText hexPrimary, hexSecondary;

    private int currentColorPrimary = Color.BLACK;
    private int currentColorSecondary = Color.BLACK;

    public SettingsAppearanceHelper(SeekBar[] primarySeekBars, SeekBar[] secondarySeekBars,
                                    CardView[] previews, EditText[] hexInputs) {
        this.seekPrimaryR = primarySeekBars[0];
        this.seekPrimaryG = primarySeekBars[1];
        this.seekPrimaryB = primarySeekBars[2];

        this.seekSecondaryR = secondarySeekBars[0];
        this.seekSecondaryG = secondarySeekBars[1];
        this.seekSecondaryB = secondarySeekBars[2];

        this.previewPrimary = previews[0];
        this.previewSecondary = previews[1];

        this.hexPrimary = hexInputs[0];
        this.hexSecondary = hexInputs[1];

        setupListeners();
    }

    private void setupListeners() {
        SeekBar.OnSeekBarChangeListener primaryListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updatePrimaryColor(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekPrimaryR.setOnSeekBarChangeListener(primaryListener);
        seekPrimaryG.setOnSeekBarChangeListener(primaryListener);
        seekPrimaryB.setOnSeekBarChangeListener(primaryListener);

        SeekBar.OnSeekBarChangeListener secondaryListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { updateSecondaryColor(); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekSecondaryR.setOnSeekBarChangeListener(secondaryListener);
        seekSecondaryG.setOnSeekBarChangeListener(secondaryListener);
        seekSecondaryB.setOnSeekBarChangeListener(secondaryListener);
    }

    private void updatePrimaryColor() {
        int r = seekPrimaryR.getProgress();
        int g = seekPrimaryG.getProgress();
        int b = seekPrimaryB.getProgress();
        currentColorPrimary = Color.rgb(r, g, b);
        previewPrimary.setCardBackgroundColor(currentColorPrimary);
        hexPrimary.setText(String.format("#%02X%02X%02X", r, g, b));
    }

    private void updateSecondaryColor() {
        int r = seekSecondaryR.getProgress();
        int g = seekSecondaryG.getProgress();
        int b = seekSecondaryB.getProgress();
        currentColorSecondary = Color.rgb(r, g, b);
        previewSecondary.setCardBackgroundColor(currentColorSecondary);
        hexSecondary.setText(String.format("#%02X%02X%02X", r, g, b));
    }

    public void setColors(int primary, int secondary) {
        currentColorPrimary = primary;
        currentColorSecondary = secondary;

        setSeekBarsFromColor(primary, seekPrimaryR, seekPrimaryG, seekPrimaryB);
        setSeekBarsFromColor(secondary, seekSecondaryR, seekSecondaryG, seekSecondaryB);

        updatePrimaryColor();
        updateSecondaryColor();
    }

    private void setSeekBarsFromColor(int color, SeekBar r, SeekBar g, SeekBar b) {
        r.setProgress(Color.red(color));
        g.setProgress(Color.green(color));
        b.setProgress(Color.blue(color));
    }

    public int getPrimaryColor() { return currentColorPrimary; }
    public int getSecondaryColor() { return currentColorSecondary; }
}