package com.moonpi.swiftnotes.ColorPicker;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;


/**
 * A drawable which sets its color filter to a color specified by the user, and changes to a
 * slightly darker color when pressed or focused
 */
public class ColorStateDrawable extends LayerDrawable {

    private static final float PRESSED_STATE_MULTIPLIER = 0.70f;

    private int mColor;

    public ColorStateDrawable(Drawable[] layers, int color) {
        super(layers);
        mColor = color;
    }

    @Override
    protected boolean onStateChange(int[] states) {
        boolean pressedOrFocused = false;

        for (int state : states) {
            if (state == android.R.attr.state_pressed || state == android.R.attr.state_focused) {
                pressedOrFocused = true;
                break;
            }
        }

        if (pressedOrFocused) {
            super.setColorFilter(getPressedColor(mColor), PorterDuff.Mode.SRC_ATOP);
        } else {
            super.setColorFilter(mColor, PorterDuff.Mode.SRC_ATOP);
        }

        return super.onStateChange(states);
    }

    /**
     * Given a particular color, adjusts its value by a multiplier
     */
    private static int getPressedColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * PRESSED_STATE_MULTIPLIER;

        return Color.HSVToColor(hsv);
    }

    @Override
    public boolean isStateful() {
        return true;
    }
}
