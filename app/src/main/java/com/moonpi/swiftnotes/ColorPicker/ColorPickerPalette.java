package com.moonpi.swiftnotes.ColorPicker;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.moonpi.swiftnotes.R;

import static com.moonpi.swiftnotes.ColorPicker.ColorPickerSwatch.*;


/**
 * A color picker custom view which creates an grid of color squares.  The number of squares per
 * row (and the padding between the squares) is determined by the user
 */
public class ColorPickerPalette extends TableLayout {

    public OnColorSelectedListener mOnColorSelectedListener;

    private int mSwatchLength;
    private int mMarginSize;
    private int mNumColumns;

    public ColorPickerPalette(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorPickerPalette(Context context) {
        super(context);
    }

    /**
     * Initialize the size, columns, and listener.  Size should be a pre-defined size (SIZE_LARGE
     * or SIZE_SMALL) from ColorPickerDialogFragment
     */
    public void init(int size, int columns, OnColorSelectedListener listener) {
        mNumColumns = columns;
        Resources res = getResources();

        if (size == ColorPickerDialog.SIZE_LARGE) {
            mSwatchLength = res.getDimensionPixelSize(R.dimen.color_swatch_large);
            mMarginSize = res.getDimensionPixelSize(R.dimen.color_swatch_margins_large);
        } else {
            mSwatchLength = res.getDimensionPixelSize(R.dimen.color_swatch_small);
            mMarginSize = res.getDimensionPixelSize(R.dimen.color_swatch_margins_small);
        }

        mOnColorSelectedListener = listener;
    }

    private TableRow createTableRow() {
        TableRow row = new TableRow(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(params);

        return row;
    }

    /**
     * Adds swatches to table in a serpentine format
     */
    public void drawPalette(int[] colors, int selectedColor) {
        drawPalette(colors, selectedColor, null);
    }

    /**
     * Adds swatches to table in a serpentine format
     */
    public void drawPalette(int[] colors, int selectedColor, String[] colorContentDescriptions) {
        if (colors == null)
            return;

        this.removeAllViews();
        int tableElements = 0;
        int rowElements = 0;
        int rowNumber = 0;

        // Fills the table with swatches based on the array of colors
        TableRow row = createTableRow();

        for (int color : colors) {
            View colorSwatch = createColorSwatch(color, selectedColor);
            addSwatchToRow(row, colorSwatch, rowNumber);

            tableElements++;
            rowElements++;
            
            if (rowElements == mNumColumns) {
                addView(row);
                row = createTableRow();
                rowElements = 0;
                rowNumber++;
            }
        }

        // Create blank views to fill the row if the last row has not been filled
        if (rowElements > 0) {
            while (rowElements != mNumColumns) {
                addSwatchToRow(row, createBlankSpace(), rowNumber);
                rowElements++;
            }

            addView(row);
        }
    }

    /**
     * Appends a swatch to the end of the row for even-numbered rows (starting with row 0),
     * to the beginning of a row for odd-numbered rows
     */
    private static void addSwatchToRow(TableRow row, View swatch, int rowNumber) {
        if (rowNumber % 2 == 0) {
            row.addView(swatch);
        } else {
            row.addView(swatch, 0);
        }
    }

    /**
     * Creates a blank space to fill the row
     */
    private ImageView createBlankSpace() {
        ImageView view = new ImageView(getContext());
        TableRow.LayoutParams params = new TableRow.LayoutParams(mSwatchLength, mSwatchLength);
        params.setMargins(mMarginSize, mMarginSize, mMarginSize, mMarginSize);
        view.setLayoutParams(params);

        return view;
    }

    /**
     * Creates a color swatch
     */
    private ColorPickerSwatch createColorSwatch(int color, int selectedColor) {
        ColorPickerSwatch view = new ColorPickerSwatch(getContext(), color,
                color == selectedColor, mOnColorSelectedListener);

        TableRow.LayoutParams params = new TableRow.LayoutParams(mSwatchLength, mSwatchLength);
        params.setMargins(mMarginSize, mMarginSize, mMarginSize, mMarginSize);
        view.setLayoutParams(params);

        return view;
    }
}
