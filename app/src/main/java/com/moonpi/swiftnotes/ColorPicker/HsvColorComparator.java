package com.moonpi.swiftnotes.ColorPicker;

import android.graphics.Color;

import java.util.Comparator;


/**
 * A color comparator which compares based on hue, saturation, and value
 */
public class HsvColorComparator implements Comparator<Integer> {

    @Override
    public int compare(Integer lhs, Integer rhs) {
        float[] hsv = new float[3];
        Color.colorToHSV(lhs, hsv);
        float hue1 = hsv[0];
        float sat1 = hsv[1];
        float val1 = hsv[2];

        float[] hsv2 = new float[3];
        Color.colorToHSV(rhs, hsv2);
        float hue2 = hsv2[0];
        float sat2 = hsv2[1];
        float val2 = hsv2[2];

        if (hue1 < hue2)
            return 1;

        else if (hue1 > hue2)
            return -1;

        else {
            if (sat1 < sat2)
                return 1;

            else if (sat1 > sat2)
                return -1;

            else {
                if (val1 < val2)
                    return 1;

                else if (val1 > val2)
                    return -1;
            }
        }

        return 0;
    }
}
