package com.moonpi.swiftnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;


public class EditActivity extends Activity {

    private static final int NEW_NOTE_REQUEST = 15000; // requestCode for new note activity

    private EditText titleEdit, bodyEdit;
    private RelativeLayout relativeLayoutEdit;

    private InputMethodManager imm;

    private Bundle bundle;

    private String [] colourArr = {"#44A1EB", // blue (0)
                                   "#77DDBB", // teal (1)
                                   "#BBE535", // green (2)
                                   "#EEEE22", // yellow (3)
                                   "#FF8800", // orange (4)
                                   "#F56545", // red (5)
                                   "#FF3D7F", // pink (6)
                                   "#BE80FF", // purple (7)
                                   "#FFFFFF"};// white (8)

    private String [] colourNameArray;

    private int [] fontSizeArr = {14, 18, 22}; // 0 for small, 1 for medium, 2 for large
    private String [] fontSizeNameArray;

    private String colour = "#FFFFFF"; // white default
    private int fontSize = 18; // Medium default


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle("");

        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }

        setContentView(R.layout.activity_edit);

        titleEdit = (EditText)findViewById(R.id.titleEdit);
        bodyEdit = (EditText)findViewById(R.id.bodyEdit);
        relativeLayoutEdit = (RelativeLayout)findViewById(R.id.relativeLayoutEdit);
        imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);

        // Initialize colours and font sizes array
        colourNameArray = new String [] {getResources().getString(R.string.blue),
                getResources().getString(R.string.teal),
                getResources().getString(R.string.green),
                getResources().getString(R.string.yellow),
                getResources().getString(R.string.orange),
                getResources().getString(R.string.red),
                getResources().getString(R.string.pink),
                getResources().getString(R.string.purple),
                getResources().getString(R.string.white)};

        fontSizeNameArray = new String [] {getResources().getString(R.string.font_small),
                getResources().getString(R.string.font_medium),
                getResources().getString(R.string.font_large)};

        bundle = getIntent().getExtras();

        if (bundle != null) {
            // If current note is not a new one, initialize colour, EditTexts and font size
            if (bundle.getInt("requestCode") != NEW_NOTE_REQUEST) {
                colour = bundle.getString("colour");
                titleEdit.setText(bundle.getString("title"));
                bodyEdit.setText(bundle.getString("body"));
                bodyEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, bundle.getInt("fontSize"));
            }

            // If current note is new, request keyboard focus to Title
            if (bundle.getInt("requestCode") == NEW_NOTE_REQUEST) {
                titleEdit.requestFocus();
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }

            // Set background colour to colour from JSON file
            relativeLayoutEdit.setBackgroundColor(Color.parseColor(colour));
        }
    }


    // Check whether EditTexts are empty or not
    protected boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().length() == 0;
    }

    protected void toastEditTextCannotBeEmpty() {
        Toast toast = Toast.makeText(getApplicationContext(),
                getResources().getString(R.string.toast_edittext_cannot_be_empty),
                Toast.LENGTH_LONG);
        toast.show();
    }


    protected void saveChanges() {
        Intent intent = new Intent();

        // Package everything and send back to activity with OK
        intent.putExtra("title", titleEdit.getText().toString());
        intent.putExtra("body", bodyEdit.getText().toString());
        intent.putExtra("colour", colour);
        intent.putExtra("fontSize", fontSize);

        setResult(RESULT_OK, intent);

        imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

        finish();
        overridePendingTransition(0, 0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_note_colour) {
            // User picks colour from array
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.dialog_note_colour))
                    .setItems(colourNameArray, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Color is updated with new pick
                            colour = colourArr[which];

                            // Background color is changed
                            relativeLayoutEdit.setBackgroundColor(Color.parseColor(colour));
                        }
                    }).setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            }).show();

            return true;
        }

        if (id == R.id.action_save) {
            // If EditTexts are not empty, save and finish, else toast
            if(!isEmpty(titleEdit))
                saveChanges();
            else
                toastEditTextCannotBeEmpty();

            return true;
        }

        if (id == R.id.action_font_size) {
            // User picks font size from array
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.dialog_font_size))
                    .setItems(fontSizeNameArray, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Font size is updated with new pick
                            fontSize = fontSizeArr[which];

                            // Font size is changed
                            bodyEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
                        }
                    }).setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            }).show();

            return true;
        }

        if (id == R.id.action_delete) {
            if (bundle != null) {
                // If current note is new, toast cannot delete
                if (bundle.getInt("requestCode") == NEW_NOTE_REQUEST) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.toast_cannot_delete_new_note),
                            Toast.LENGTH_LONG);
                    toast.show();
                }

                // If current note is not new, request note delete in onActivityResult
                else {
                    Intent intent = new Intent();

                    intent.putExtra("request", "delete");

                    setResult(RESULT_CANCELED, intent);

                    imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                    finish();
                    overridePendingTransition(0, 0);
                }
            }

            return true;
        }

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return false;
    }


    @Override
    public void onBackPressed() {
        // Back or home up pressed
        // Check if note has changed, if yes
        // Ask user if note should be saved or not
        // If yes, save and finish
        // If no, go back without saving; if new note, request discard empty note toast

        if (!(titleEdit.getText().toString().equals(bundle.getString("title"))) ||
            !(bodyEdit.getText().toString().equals(bundle.getString("body")))) {

            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.dialog_save_changes))
                    .setPositiveButton(getResources().getString(R.string.yes_button), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!isEmpty(titleEdit))
                                saveChanges();
                            else
                                toastEditTextCannotBeEmpty();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.no_button), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (bundle != null) {
                                if (bundle.getInt("requestCode") == NEW_NOTE_REQUEST) {
                                    Intent intent = new Intent();

                                    intent.putExtra("request", "discard");

                                    setResult(RESULT_CANCELED, intent);

                                    imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                                    finish();
                                    overridePendingTransition(0, 0);
                                }

                                else {
                                    setResult(RESULT_CANCELED);

                                    imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

                                    finish();
                                    overridePendingTransition(0, 0);
                                }
                            }
                        }
                    })
                    .show();
        }

        // If note hasn't changed, finish activity and go back
        else {
            imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);

            finish();
            overridePendingTransition(0, 0);
        }
    }


    // When window focus changed, hide keyboard
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus)
            imm.hideSoftInputFromWindow(titleEdit.getWindowToken(), 0);
    }
}
