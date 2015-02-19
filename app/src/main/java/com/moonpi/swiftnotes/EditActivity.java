package com.moonpi.swiftnotes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;


public class EditActivity extends ActionBarActivity implements Toolbar.OnMenuItemClickListener {

    // Layout components
    private EditText titleEdit, bodyEdit;
    private RelativeLayout relativeLayoutEdit;
    private Toolbar toolbar;
    private MenuItem menuHideBody;

    private InputMethodManager imm;

    private Bundle bundle;

    private String [] colourArr; // Colours string array
    private String [] colourNameArr; // Colour names string array
    private int [] fontSizeArr; // Font sizes int array
    private String [] fontSizeNameArr; // Font size names string array

    // Defaults
    private String colour = "#FFFFFF"; // white default
    private int fontSize = 18; // Medium default
    private Boolean hideBody = false;

    private AlertDialog colourDialog, fontDialog, saveChangesDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If current Android version >= 18, set orientation fullUser
        if (Build.VERSION.SDK_INT >= 18)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);

            // If current Android version < 18, set orientation fullSensor
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);


        setContentView(R.layout.activity_edit);

        // Init toolbar
        toolbar = (Toolbar)findViewById(R.id.toolbar);

        if (toolbar != null)
            initToolbar();

        // Init layout components
        titleEdit = (EditText)findViewById(R.id.titleEdit);
        bodyEdit = (EditText)findViewById(R.id.bodyEdit);
        relativeLayoutEdit = (RelativeLayout)findViewById(R.id.relativeLayoutEdit);
        imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);

        // Initialize colours and font sizes arrays
        colourArr = this.getResources().getStringArray(R.array.colours);
        colourNameArr = this.getResources().getStringArray(R.array.colourNames);

        fontSizeArr = new int[] {14, 18, 22}; // 0 for small, 1 for medium, 2 for large
        fontSizeNameArr = this.getResources().getStringArray(R.array.fontSizeNames);

        // Get data bundle from MainActivity
        bundle = getIntent().getExtras();

        if (bundle != null) {
            // If current note is not new, initialize colour, font, hideBody and EditTexts
            if (bundle.getInt(DataUtils.NOTE_REQUEST_CODE) != DataUtils.NEW_NOTE_REQUEST) {
                colour = bundle.getString(DataUtils.NOTE_COLOUR);
                fontSize = bundle.getInt(DataUtils.NOTE_FONT_SIZE);
                hideBody = bundle.getBoolean(DataUtils.NOTE_HIDE_BODY);

                titleEdit.setText(bundle.getString(DataUtils.NOTE_TITLE));
                bodyEdit.setText(bundle.getString(DataUtils.NOTE_BODY));
                bodyEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);

                if (hideBody)
                    menuHideBody.setTitle(getResources().getString(R.string.action_show_body));
            }

            // If current note is new, request keyboard focus to Title
            if (bundle.getInt(DataUtils.NOTE_REQUEST_CODE) == DataUtils.NEW_NOTE_REQUEST) {
                titleEdit.requestFocus();
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }

            // Set background colour to note colour
            relativeLayoutEdit.setBackgroundColor(Color.parseColor(colour));
        }

        initDialogs(this);
    }


    /**
     * Initialize toolbar with required components such as
     * - navigation icon + listener, title, elevation (21+), menu/OnMenuItemClickListener, menuHideBody -
     */
    protected void initToolbar() {
        toolbar.bringToFront();

        // Set a 'Back' navigation icon in the Toolbar and handle the click
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        toolbar.setTitle("");

        if (Build.VERSION.SDK_INT >= 21)
            toolbar.setElevation(10.0f);

        // Set an OnMenuItemClickListener to handle menu item clicks
        toolbar.setOnMenuItemClickListener(this);

        // Inflate a menu to be displayed in the toolbar
        toolbar.inflateMenu(R.menu.edit);

        Menu menu = toolbar.getMenu();

        if (menu != null)
            menuHideBody = menu.findItem(R.id.action_hide_show_body);
    }


    /**
     * Implementation of AlertDialogs such as
     * - colourDialog, fontDialog, saveChangesDialog -
     * @param context The Activity context of the dialogs; in this case EditActivity context
     */
    protected void initDialogs(Context context) {
        // Colour picker dialog
        colourDialog = new AlertDialog.Builder(context)
                .setTitle(getResources().getString(R.string.dialog_note_colour))
                .setItems(colourNameArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Colour is updated with new pick
                        colour = colourArr[which];

                        // Background colour is changed
                        relativeLayoutEdit.setBackgroundColor(Color.parseColor(colour));
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();


        // Font size picker dialog
        fontDialog = new AlertDialog.Builder(context)
                .setTitle(getResources().getString(R.string.dialog_font_size))
                .setItems(fontSizeNameArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Font size is updated with new pick
                        fontSize = fontSizeArr[which];

                        // Font size is changed
                        bodyEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                    }
                })
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();


        // 'Save changes?' dialog
        saveChangesDialog = new AlertDialog.Builder(context)
                .setMessage(getResources().getString(R.string.dialog_save_changes))
                .setPositiveButton(getResources().getString(R.string.yes_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If 'Yes' clicked, check if title is empty
                        // If title not empty, save and go back; Toast otherwise
                        if (!isEmpty(titleEdit))
                            saveChanges();

                        else
                            toastEditTextCannotBeEmpty();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // If 'No' clicked
                        if (bundle != null) {
                            // If new note -> put extra 'discard' to show toast
                            if (bundle.getInt(DataUtils.NOTE_REQUEST_CODE) == DataUtils.NEW_NOTE_REQUEST) {
                                Intent intent = new Intent();

                                intent.putExtra("request", "discard");

                                setResult(RESULT_CANCELED, intent);

                                imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                                dialog.dismiss();
                                finish();
                                overridePendingTransition(0, 0);
                            }

                            // If note is not new -> hide keyboard if showing and finish
                            else {
                                setResult(RESULT_CANCELED);

                                imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                                dialog.dismiss();
                                finish();
                                overridePendingTransition(0, 0);
                            }
                        }
                    }
                })
                .create();
    }



    /**
     * Item clicked in Toolbar menu callback method
     * @param item Item clicked
     * @return true if click detected and logic finished, false otherwise
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_note_colour) {
            // User picks colour from array
            colourDialog.show();

            return true;
        }

        if (id == R.id.action_save) {
            // If note title is not empty, save and finish; Else toast
            if (!isEmpty(titleEdit))
                saveChanges();

            else
                toastEditTextCannotBeEmpty();

            return true;
        }

        if (id == R.id.action_font_size) {
            // User picks font size from array
            fontDialog.show();

            return true;
        }

        // If 'Hide note body in list' or 'Show note body in list' clicked
        if (id == R.id.action_hide_show_body) {
            // If not true -> set to true and change menu item text to 'Show note body in list'
            if (!hideBody) {
                hideBody = true;
                menuHideBody.setTitle(getResources().getString(R.string.action_show_body));

                // Toast note body will be hidden
                Toast toast = Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.toast_note_body_hidden),
                        Toast.LENGTH_LONG);
                toast.show();
            }

            // If hideBody true -> set to false and change menu item text to 'Hide note body in list'
            else {
                hideBody = false;
                menuHideBody.setTitle(getResources().getString(R.string.action_hide_body));

                // Toast note body will be shown
                Toast toast = Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.toast_note_body_showing),
                        Toast.LENGTH_LONG);
                toast.show();
            }

            return true;
        }

        return false;
    }



    /**
     * Create an Intent with title, body, colour, font size and hideBody extras
     * Set RESULT_OK and go back to MainActivity
     */
    protected void saveChanges() {
        Intent intent = new Intent();

        // Package everything and send back to activity with OK
        intent.putExtra(DataUtils.NOTE_TITLE, titleEdit.getText().toString());
        intent.putExtra(DataUtils.NOTE_BODY, bodyEdit.getText().toString());
        intent.putExtra(DataUtils.NOTE_COLOUR, colour);
        intent.putExtra(DataUtils.NOTE_FONT_SIZE, fontSize);
        intent.putExtra(DataUtils.NOTE_HIDE_BODY, hideBody);

        setResult(RESULT_OK, intent);

        imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

        finish();
        overridePendingTransition(0, 0);
    }


    /**
     * Back or navigation '<-' pressed
     * Check if note new or existing
     *    If new -> Show 'Save changes?' dialog
     *    If existing -> Check for changes
     *        If changed -> Show 'Save changes?' dialog
     *        It not -> Finish
     */
    @Override
    public void onBackPressed() {
        // If new note, show 'Save changes?' dialog
        if (bundle.getInt(DataUtils.NOTE_REQUEST_CODE) == DataUtils.NEW_NOTE_REQUEST)
            saveChangesDialog.show();

        // If existing note, check if it changed and show 'Save changes?' dialog; finish otherwise
        else {
            if (!(titleEdit.getText().toString().equals(bundle.getString(DataUtils.NOTE_TITLE))) ||
                !(bodyEdit.getText().toString().equals(bundle.getString(DataUtils.NOTE_BODY))) ||
                !(colour.equals(bundle.getString(DataUtils.NOTE_COLOUR))) ||
                fontSize != bundle.getInt(DataUtils.NOTE_FONT_SIZE) ||
                hideBody != bundle.getBoolean(DataUtils.NOTE_HIDE_BODY)) {

                saveChangesDialog.show();
            }

            else {
                imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                finish();
                overridePendingTransition(0, 0);
            }
        }
    }


    /**
     * Check whether passed EditText text is empty or not
     * @param editText The EditText widget to check
     * @return true if empty, false otherwise
     */
    protected boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().length() == 0;
    }


    /**
     * Show Toast for 'Title cannot be empty'
     */
    protected void toastEditTextCannotBeEmpty() {
        Toast toast = Toast.makeText(getApplicationContext(),
                getResources().getString(R.string.toast_edittext_cannot_be_empty),
                Toast.LENGTH_LONG);
        toast.show();
    }



    /**
     * If current window loses focus, hide keyboard
     * @param hasFocus Parameter passed by system; true if focus changed, false otherwise
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus)
            imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);
    }


    /**
     * Orientation changed callback method
     * If orientation changed -> If any AlertDialog is showing, dismiss it to prevent WindowLeaks
     * @param newConfig New Configuration passed by system
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (colourDialog.isShowing())
            colourDialog.dismiss();

        if (fontDialog.isShowing())
            fontDialog.dismiss();

        if (saveChangesDialog.isShowing())
            fontDialog.dismiss();

        super.onConfigurationChanged(newConfig);
    }
}
